/*
 * Copyright 2020 the original author or authors.
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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.process.ProcessWrapper;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import example.app.model.User;
import example.app.repo.UserRepository;
import lombok.Getter;
import lombok.Setter;

/**
 * Integration Tests asserting the concurrent interaction of multiple {@link ClientCache} (client/server) transactions
 * where 1 transaction commits and the other rolls back.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see edu.umd.cs.mtc.MultithreadedTestCase
 * @see edu.umd.cs.mtc.TestFramework
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.config.annotation.CacheServerApplication
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.mapping.annotation.Region
 * @see org.springframework.data.gemfire.repository.GemfireRepository
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport
 * @see org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.springframework.transaction.annotation.Transactional
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConcurrentClientDataAccessTransactionsWithRollbackIntegrationTests.TestGeodeClientConfiguration.class)
@SuppressWarnings("unused")
public class ConcurrentClientDataAccessTransactionsWithRollbackIntegrationTests
		extends ClientServerIntegrationTestsSupport {

	private static ProcessWrapper geodeServer;

	@BeforeClass
	public static void startGeodeServer() throws IOException {

		int availablePort = findAvailablePort();

		geodeServer = run(TestGeodeServerConfiguration.class,
			String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, availablePort));

		waitForServerToStart("localhost", availablePort);

		System.setProperty(GEMFIRE_POOL_SERVERS_PROPERTY, String.format("localhost[%d]", availablePort));
	}

	@AfterClass
	public static void stopGeodeServer() {
		stop(geodeServer);
		System.clearProperty(GEMFIRE_POOL_SERVERS_PROPERTY);
	}

	@Autowired
	private UserService userService;

	@Test
	public void concurrentTransactionalDataAccessOperations() throws Throwable {
		TestFramework.runOnce(new TwoConcurrentThreadsTransactionalDataAccessOperationsMultithreadedTestCase(this.userService));
	}

	@ClientCacheApplication(name = "ConcurrentClientDataAccessTransactionsWithRollbackIntegrationTests")
	@EnableEntityDefinedRegions(basePackageClasses = User.class)
	@EnableGemfireRepositories(basePackageClasses = UserRepository.class)
	@EnableGemfireCacheTransactions
	@EnablePdx
	static class TestGeodeClientConfiguration {

		@Bean
		GemfireRepositoryFactoryBean<UserRepository, User, Integer> userRepository(GemFireCache gemfireCache) {

			GemfireRepositoryFactoryBean<UserRepository, User, Integer> userRepositoryFactoryBean =
				new GemfireRepositoryFactoryBean<>(UserRepository.class);

			userRepositoryFactoryBean.setCache(gemfireCache);
			userRepositoryFactoryBean.setGemfireMappingContext(new GemfireMappingContext());

			return userRepositoryFactoryBean;
		}

		@Bean
		UserService userService(UserRepository userRepository) {
			return new UserService(userRepository);
		}
	}

	@CacheServerApplication(name = "ConcurrentClientTransactionsWithRollbackIntegrationTestsServer")
	@EnableEntityDefinedRegions(basePackageClasses = User.class, serverRegionShortcut = RegionShortcut.REPLICATE)
	static class TestGeodeServerConfiguration {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(TestGeodeServerConfiguration.class);

			applicationContext.registerShutdownHook();
		}
	}

	public static final class TwoConcurrentThreadsTransactionalDataAccessOperationsMultithreadedTestCase
			extends MultithreadedTestCase {

		@Getter
		private final UserService userService;

		public TwoConcurrentThreadsTransactionalDataAccessOperationsMultithreadedTestCase(
				@NonNull UserService userService) {

			Assert.notNull(userService, "UserService must not be null");

			this.userService = userService;
			this.userService.setTestCase(this);
		}

		private void assertUser(@NonNull User user, Integer expectedId, String expectedName) {

			assertThat(user).isNotNull();
			assertThat(user.getId()).isEqualTo(expectedId);
			assertThat(user.getName()).isEqualTo(expectedName);
		}

		@Override
		public void initialize() {

			super.initialize();

			User jonDoe = User.as("jonDoe").identifiedBy(1);

			getUserService().saveAndCommit(jonDoe);

			assertThat(getUserService().exists(jonDoe)).isTrue();
		}

		public void thread1() {

			Thread.currentThread().setName("Data Access Thread One Running Rollback");

			assertTick(0);

			User jonDoe = getUserService().findById(1);

			assertUser(jonDoe, 1, "jonDoe");

			jonDoe.withName("sourDoe");

			assertUser(jonDoe, 1, "sourDoe");

			AtomicBoolean optimisticLockingFailureExceptionIsPresent = new AtomicBoolean(false);

			try {
				getUserService().saveThrowRuntimeExceptionAndRollback(jonDoe);
			}
			catch (Throwable expected) {

				Throwable cause = expected;

				while (cause != null) {
					if (cause instanceof OptimisticLockingFailureException) {
						assertThat(cause).hasMessage("TEST");
						optimisticLockingFailureExceptionIsPresent.set(true);
					}
					else if (cause instanceof NoTransactionException) {
						fail(String.format("%s was incorrectly thrown", cause.getClass().getName()));
					}

					cause = cause.getCause();
				}
			}
			finally {
				assertThat(optimisticLockingFailureExceptionIsPresent).isTrue();
			}
		}

		public void thread2() {

			Thread.currentThread().setName("Data Access Thread Two Running Commit");

			waitForTick(1);
			assertTick(1);

			User jonDoe = getUserService().findById(1);

			assertUser(jonDoe, 1, "jonDoe");

			jonDoe.withName("pieDoe");

			assertUser(jonDoe, 1, "pieDoe");

			getUserService().saveAndCommit(jonDoe);
		}

		@Override
		public void finish() {

			super.finish();

			User pieDoe = getUserService().findById(1);

			assertUser(pieDoe, 1, "pieDoe");
		}
	}

	@Service
	static class UserService {

		@Setter
		private MultithreadedTestCase testCase;

		@Getter
		private final UserRepository userRepository;

		public UserService(@NonNull UserRepository userRepository) {
			Assert.notNull(userRepository, "UserRepository must not be null");
			this.userRepository = userRepository;
		}

		protected MultithreadedTestCase getTestCase() {

			Assert.state(this.testCase != null,
				"A reference to the TestCase was not configured correctly");

			return this.testCase;
		}

		@Transactional(readOnly = true)
		public boolean exists(@NonNull User user) {

			return user != null
				&& user.getId() != null
				&& getUserRepository().existsById(user.getId());
		}

		@Transactional(readOnly = true)
		public @NonNull User findById(@NonNull Integer id) {

			return Optional.ofNullable(id)
				.flatMap(getUserRepository()::findById)
				.orElseThrow(() -> new EmptyResultDataAccessException(1));
		}

		@Transactional
		public @NonNull User saveAndCommit(@NonNull User user) {
			return save(user);
		}

		@Transactional
		public @NonNull User saveThrowRuntimeExceptionAndRollback(@NonNull User user) {
			save(user);
			getTestCase().waitForTick(2);
			throw new OptimisticLockingFailureException("TEST");
		}

		private @NonNull User save(@NonNull User user) {
			return getUserRepository().save(user);
		}
	}
}
