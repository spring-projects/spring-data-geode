/*
 * Copyright 2016-2022 the original author or authors.
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
 *
 */

package org.springframework.data.gemfire.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Integration tests for the {@link GemfireTransactionManager}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.data.gemfire.transaction.GemfireTransactionManager
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.springframework.transaction.annotation.EnableTransactionManagement
 * @see org.springframework.transaction.annotation.Propagation
 * @since 1.9.1
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class GemfireTransactionManagerIntegrationTests {

	private static final String GEMFIRE_LOG_LEVEL = "error";

	@Resource(name = "Example")
	private Region<Object, Object> example;

	@Autowired
	@SuppressWarnings("all")
	private SuspendAndResumeCacheTransactionsService service;

	@Test(expected = IllegalArgumentException.class)
	public void suspendAndResumeIsSuccessful() {

		try {

			assertThat(this.example).isEmpty();

			this.service.doCacheTransactions();
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("BOOM!");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(this.example).hasSize(1);
			assertThat(this.example.containsKey("tx-1-op-1")).isFalse();
			assertThat(this.example.containsKey("tx-2-op-1")).isTrue();
		}
	}

	@PeerCacheApplication(
		name = "GemfireTransactionManagerIntegrationTests",
		logLevel = GEMFIRE_LOG_LEVEL
	)
	@EnableGemfireCacheTransactions
	static class TestConfiguration {

		@Bean(name = "Example")
		LocalRegionFactoryBean<Object, Object> exampleRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> example = new LocalRegionFactoryBean<>();

			example.setCache(gemfireCache);
			example.setClose(false);
			example.setPersistent(false);

			return example;
		}

		@Bean
		SuspendAndResumeCacheTransactionsRepository suspendAndResumeCacheTransactionsRepository(
				GemFireCache gemFireCache) {

			return new SuspendAndResumeCacheTransactionsRepository(gemFireCache.getRegion("Example"));
		}

		@Bean
		SuspendAndResumeCacheTransactionsService suspendAndResumeCacheTransactionsService(
				SuspendAndResumeCacheTransactionsRepository repository) {

			return new SuspendAndResumeCacheTransactionsService(repository);
		}
	}

	@Service
	static class SuspendAndResumeCacheTransactionsService {

		SuspendAndResumeCacheTransactionsRepository repository;

		SuspendAndResumeCacheTransactionsService(SuspendAndResumeCacheTransactionsRepository repository) {
			Assert.notNull(repository, "Repository must not be null");
			this.repository = repository;
		}

		@Transactional
		public void doCacheTransactions() {
			this.repository.doOperationOneInTransactionOne();
			this.repository.doOperationOneInTransactionTwo();
			this.repository.doOperationTwoInTransactionOne();
		}
	}

	@Repository
	static class SuspendAndResumeCacheTransactionsRepository {

		@SuppressWarnings("all")
		Region<Object, Object> example;

		SuspendAndResumeCacheTransactionsRepository(Region<Object, Object> example) {
			Assert.notNull(example, "Region must not be null");
			this.example = example;
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void doOperationOneInTransactionOne() {
			this.example.put("tx-1-op-1", "test");
		}

		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void doOperationOneInTransactionTwo() {
			this.example.put("tx-2-op-1", "test");
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void doOperationTwoInTransactionOne() {
			throw new IllegalArgumentException("BOOM!");
		}
	}
}
