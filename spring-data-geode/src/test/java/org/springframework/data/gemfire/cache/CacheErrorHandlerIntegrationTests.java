/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.gemfire.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.NoAvailableServersException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.cache.config.EnableGemfireCaching;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests demonstrating the use of Spring's {@link CacheErrorHandler} with Apache Geode
 * as the caching provider.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CachingConfigurerSupport
 * @see org.springframework.cache.interceptor.CacheErrorHandler
 * @see org.springframework.data.gemfire.cache.config.EnableGemfireCaching
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class CacheErrorHandlerIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ClientCache clientCache;

	@Autowired
	private TestCacheableService cacheableService;

	@Before
	public void assertSetup() {

		assertThat(this.clientCache).isNotNull();
		assertThat(this.cacheableService).isNotNull();

		Region<Object, Object> testCache = this.clientCache.getRegion("/TestCache");

		assertThat(testCache).isNotNull();
		assertThat(testCache.getName()).isEqualTo("TestCache");
		assertThat(testCache.getAttributes()).isNotNull();
		assertThat(testCache.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.EMPTY);
		assertThat(testCache.getAttributes().getPoolName()).isEqualTo("DEFAULT");
	}

	@Test
	public void cacheServiceOperationIsSuccessful() {
		assertThat(this.cacheableService.processCacheableOperation("one")).isEqualTo("TEST");
	}

	@ClientCacheApplication
	@EnableCachingDefinedRegions
	@EnableGemfireCaching
	static class GeodeClientTestConfiguration extends CachingConfigurerSupport {

		@Nullable @Override
		public CacheErrorHandler errorHandler() {
			return new TestCacheErrorHandler();
		}

		@Bean
		TestCacheableService testCacheableService() {
			return new TestCacheableService();
		}
	}

	@Service
	static class TestCacheableService {

		@Cacheable(cacheNames = "TestCache")
		public Object processCacheableOperation(String input) {
			return "TEST";
		}
	}

	static class TestCacheErrorHandler extends SimpleCacheErrorHandler {

		@Override
		public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
			handleCacheGetAndPutError(exception, cache, key, null);
		}

		@Override
		public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
			handleCacheGetAndPutError(exception, cache, key, value);
		}

		private void handleCacheGetAndPutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {

			// If the Apache Geode cluster of servers is not available, then effectively disable caching
			// by not rethrowing the Exception!
			if (!(exception instanceof NoAvailableServersException)) {
				throw exception;
			}
		}
	}
}
