/*
 * Copyright 2010-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheWriterAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the addition of {@link CacheLoader CacheLoaders} and {@link CacheWriter CacheWriters}
 * to a client, local {@link Region} cache.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.CacheLoader
 * @see org.apache.geode.cache.CacheWriter
 * @see org.apache.geode.cache.Region
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ClientRegionWithCacheLoaderCacheWriterIntegrationTests extends IntegrationTestsSupport {

	private static final int REGION_SIZE = 100;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier("localAppDataRegion")
	private Region<Integer, Integer> localAppData;

	@Test
	public void testCacheLoaderWriter() {

		assertThat(localAppData).isNotNull();
		assertThat(localAppData.size()).isEqualTo(0);

		for (int key = 0; key < REGION_SIZE; key++) {
			assertThat(localAppData.get(key).intValue()).isEqualTo(key + 1);
		}

		assertThat(localAppData.size()).isEqualTo(REGION_SIZE);

		for (int key = 0; key < REGION_SIZE; key++) {
			assertThat(localAppData.put(key, REGION_SIZE - key).intValue()).isEqualTo(key + 1);
		}

		LocalAppDataCacheWriter localCacheWriter = applicationContext
			.getBean("localCacheWriter", LocalAppDataCacheWriter.class);

		assertThat(localCacheWriter).isNotNull();

		for (int key = 0; key < REGION_SIZE; key++) {
			assertThat(localCacheWriter.get(key).intValue()).isEqualTo(REGION_SIZE - key);
		}
	}

	public static class LocalAppDataCacheLoader implements CacheLoader<Integer, Integer> {

		private static final AtomicInteger VALUE_GENERATOR = new AtomicInteger(0);

		@Override
		public Integer load(final LoaderHelper<Integer, Integer> helper) throws CacheLoaderException {
			return VALUE_GENERATOR.incrementAndGet();
		}

		@Override
		public void close() { }

	}

	public static class LocalAppDataCacheWriter extends CacheWriterAdapter<Integer, Integer> {

		private static final Map<Integer, Integer> data = new ConcurrentHashMap<>(REGION_SIZE);

		@Override
		public void beforeUpdate(EntryEvent<Integer, Integer> event) throws CacheWriterException {
			data.put(event.getKey(), event.getNewValue());
		}

		public Integer get(Integer key) {
			return data.get(key);
		}
	}
}
