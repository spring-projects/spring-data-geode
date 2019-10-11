/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.data.gemfire.config.annotation;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.eventing.config.CacheWriterEventType;
import org.springframework.data.gemfire.eventing.config.RegionCacheWriterEventType;

/**
 * Tests for {@link AsCacheWriter} configured for a
 * {@link org.apache.geode.cache.server.CacheServer}
 *
 * @author Udo Kohlmeyer
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.server.CacheServer
 * @see Configuration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
public class AsCacheWriterCacheServerConfigurationTests extends AsCacheWriterConfigurationTests {

	protected static ReplicatedRegionFactoryBean<String, String> createRegionFactoryBean(GemFireCache cache,
		String regionName) {
		ReplicatedRegionFactoryBean<String, String> replicateRegion = new ReplicatedRegionFactoryBean<>();
		replicateRegion.setName(regionName);
		replicateRegion.setCache(cache);
		return replicateRegion;
	}

	@Override
	protected Class<?> getCacheWriterWithIncorrectRegionEventParameterConfiguration() {
		return TestConfigurationWithIncorrectRegionEventParameter.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationSingleDefaultRegionsConfiguration() {
		return TestConfigurationWithSimpleCacheWriter.class;
	}

	@Override
	protected Class<TestConfigurationWithInvalidRegion> getCacheWriterAnnotationWithInvalidRegion() {
		return TestConfigurationWithInvalidRegion.class;
	}

	@Override
	protected Class<TestConfigurationWith2RegionsAnd2CacheWritersDefaulted> getCacheWriterAnnotationMultipleRegionsDefault() {
		return TestConfigurationWith2RegionsAnd2CacheWritersDefaulted.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationSingleRegionAllEvents() {
		return TestConfigurationWithSimpleCacheWriterAllEvents.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationAgainst2NamedRegions() {
		return TestConfigurationWithSimpleCacheWriterWith2Regions.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationWithRegionEventAndCacheWriter() {
		return TestConfigurationWithRegionEventAndCacheWriter.class;
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithIncorrectRegionEventParameter {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE)
		public void beforeCreateWriter(RegionEvent<String, String> event) {
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheWriter {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE)
		public void beforeCreateWriter(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_UPDATE)
		public void beforeUpdateWriter(EntryEvent<String, String> event) {
			recordEvent(event);

		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithInvalidRegion {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE, regions = "TestRegion2")
		public void beforeCreateWriter(EntryEvent<String, String> event) {
			recordEvent(event);
		}

	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheWriterAllEvents {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.ALL)
		public void beforeCreateWriter(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheWriterWith2Regions {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ReplicatedRegionFactoryBean<String, String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE, regions = "TestRegion1")
		public void beforeCreateWriter(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE, regions = "TestRegion2")
		public void beforeUpdateWriter(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWith2RegionsAnd2CacheWritersDefaulted {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ReplicatedRegionFactoryBean<String, String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.ALL)
		public void beforeCreateWriter1(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.ALL)
		public void beforeCreateWriter2(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithRegionEventAndCacheWriter {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String, String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheWriter(eventTypes = CacheWriterEventType.BEFORE_CREATE)
		public void beforeCreateWriter1(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsRegionEventHandler(regionWriterEventTypes = RegionCacheWriterEventType.BEFORE_REGION_CLEAR)
		public void beforeClear(RegionEvent<String, String> event) {
			recordEvent(event);
		}
	}
}
