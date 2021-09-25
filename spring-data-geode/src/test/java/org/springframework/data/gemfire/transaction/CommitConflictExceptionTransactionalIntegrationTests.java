/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Integration Tests asserting the proper configuration and behavior of Apache Geode {@link GemFireCache} Transactions
 * within the context of the Spring container when using SDG to configure the {@link CacheTransactionManager}.
 *
 * Specifically, this test asserts that 2 concurrent threads modifying the same entity inside a cache transaction
 * leading to a {@link CommitConflictException}.
 *
 * @author John Blum
 * @see java.util.function.Function
 * @see edu.umd.cs.mtc.MultithreadedTestCase
 * @see edu.umd.cs.mtc.TestFramework
 * @see org.junit.Test
 * @see org.apache.geode.cache.CacheTransactionManager
 * @see org.apache.geode.cache.CommitConflictException
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @see org.springframework.data.gemfire.mapping.annotation.Region
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions
 * @see org.springframework.stereotype.Service
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.springframework.transaction.annotation.Transactional
 * @since 2.2.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class CommitConflictExceptionTransactionalIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private GemFireCache cache;

	@Autowired
	private AutomatedTellerMachine atm;

	@Test
	public void cacheTransactionManagementIsConfigured() {

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("CommitConflictExceptionTransactionalIntegrationTests");
		assertThat(this.cache.getCopyOnRead()).isTrue();
		assertThat(this.cache.getCacheTransactionManager()).isNotNull();
	}

	@Test
	public void concurrentTransactionalThreadsCauseCommitConflictException() throws Throwable {
		TestFramework.runOnce(new TransactionalCommitConflictMultithreadedTestCase(this.atm));
	}

	static class TransactionalCommitConflictMultithreadedTestCase extends MultithreadedTestCase {

		@Getter(AccessLevel.PROTECTED)
		private final AutomatedTellerMachine atm;

		TransactionalCommitConflictMultithreadedTestCase(AutomatedTellerMachine atm) {

			Assert.notNull(atm, "The ATM must not be null");

			this.atm = atm;
		}

		@Override
		public void initialize() {

			super.initialize();

			Account account = getAtm().save(Account.open(1).deposit(BigDecimal.valueOf(100.0d)));
			Account accountLoaded = getAtm().findByAccountNumber(account.getNumber());

			assertThat(accountLoaded).isEqualTo(account);
		}

		public void thread1() {

			assertTick(0);

			Thread.currentThread().setName("Account Processing Thread One");

			getAtm().process(1, account -> {

				assertThat(account.getNumber()).isEqualTo(1);
				assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100.0d));

				account.withdrawal(BigDecimal.valueOf(50.0d));

				waitForTick(2);
				assertTick(2);

				return account;

			}, Function.identity());
		}

		public void thread2() {

			assertTick(0);

			Thread.currentThread().setName("Account Processing Thread Two");

			waitForTick(1);
			assertTick(1);

			try {

				getAtm().process(1, account -> {

					assertThat(account.getNumber()).isEqualTo(1);
					assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100.0d));

					account.withdrawal(BigDecimal.valueOf(75.0d));

					waitForTick(3);
					assertTick(3);

					return account;

				}, Function.identity());

				fail("Expected CommitConflictException!");

			}
			catch (RuntimeException expected) {
				assertThat(expected).isInstanceOf(GemfireTransactionCommitException.class);
				assertThat(expected).hasCauseInstanceOf(CommitConflictException.class);
			}
		}

		@Override
		public void finish() {

			Account account = getAtm().findByAccountNumber(1);

			assertThat(account).isNotNull();
			assertThat(account.getNumber()).isEqualTo(1);
			assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(50.0d));
		}
	}

	@ClientCacheApplication(name = "CommitConflictExceptionTransactionalIntegrationTests")
	@EnableEntityDefinedRegions(basePackageClasses = Account.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
	@EnableGemfireCacheTransactions
	static class TestConfiguration {

		@Bean
		GemfireRepositoryFactoryBean<AccountRepository, Account, Integer> accountRepository() {

			GemfireRepositoryFactoryBean<AccountRepository, Account, Integer> accountRepository
				= new GemfireRepositoryFactoryBean<>(AccountRepository.class);

			accountRepository.setGemfireMappingContext(new GemfireMappingContext());

			return accountRepository;
		}

		@Bean
		AutomatedTellerMachine automatedTellerMachine(AccountRepository accountRepository) {
			return new AutomatedTellerMachine(accountRepository);
		}
	}

	@Getter
	@ToString
	@EqualsAndHashCode
	@Region("Accounts")
	@RequiredArgsConstructor(staticName = "open")
	static class Account implements Serializable {

		@Id @lombok.NonNull
		private final Integer number;

		private BigDecimal balance = BigDecimal.valueOf(0.0d);

		public synchronized Account deposit(@NonNull BigDecimal value) {

			this.balance = Optional.ofNullable(value)
				.map(BigDecimal::abs)
				.map(this.balance::add)
				.orElse(this.balance);

			return this;
		}

		public synchronized @NonNull Account withdrawal(@NonNull BigDecimal value) {

			this.balance = Optional.ofNullable(value)
				.map(BigDecimal::abs)
				.map(this::verifyWithdrawal)
				.map(this.balance::subtract)
				.orElse(this.balance);

			return this;
		}

		private BigDecimal verifyWithdrawal(@NonNull BigDecimal value) {

			Assert.state(getBalance().compareTo(value) >= 0,
				String.format("Withdrawal [%1$s] cannot exceed balance [%2$s]", value, getBalance()));

			return value;
		}
	}

	public interface AccountRepository extends CrudRepository<Account, Integer> { }

	@Service
	public static class AutomatedTellerMachine {

		@Getter(AccessLevel.PROTECTED)
		private final AccountRepository accountRepository;

		public AutomatedTellerMachine(@NonNull AccountRepository accountRepository) {
			Assert.notNull(accountRepository, "AccountRepository must not be null");
			this.accountRepository = accountRepository;
		}

		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public @NonNull Account findByAccountNumber(@NonNull Integer accountNumber) {

			return getAccountRepository().findById(accountNumber)
				.orElseThrow(() -> newIllegalStateException("Failed to find Account by number [%d]", accountNumber));
		}

		@Transactional
		public @NonNull Account process(@NonNull Integer accountNumber, @NonNull Function<Account, Account> beforeSave,
				@NonNull Function<Account, Account> afterSave) {

			return afterSave.apply(save(beforeSave.apply(findByAccountNumber(accountNumber))));
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public @NonNull Account save(@NonNull Account account) {

			Assert.notNull(account, "Account must not be null");

			return getAccountRepository().save(account);
		}
	}

	static class OverdraftException extends RuntimeException {

		public OverdraftException() { }

		public OverdraftException(String message) {
			super(message);
		}

		public OverdraftException(Throwable cause) {
			super(cause);
		}

		public OverdraftException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
