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
import org.springframework.data.gemfire.eventing.config.CacheListenerEventType;

/**
 * Tests for {@link org.springframework.data.gemfire.config.annotation.AsCacheListener} configured for a
 * {@link org.apache.geode.cache.server.CacheServer}
 *
 * @author Udo Kohlmeyer
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
public class AsCacheListenerCacheServerConfigurationTests extends AsCacheListenerConfigurationTests {

	protected static ReplicatedRegionFactoryBean<String, String> createRegionFactoryBean(GemFireCache cache,
		String regionName) {
		ReplicatedRegionFactoryBean<String, String> replicateRegion = new ReplicatedRegionFactoryBean<>();
		replicateRegion.setName(regionName);
		replicateRegion.setCache(cache);
		return replicateRegion;
	}

	@Override
	protected Class<?> getCacheListenerWithIncorrectRegionEventParameterConfiguration() {
		return TestConfigurationWithIncorrectRegionEventParameter.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationSingleDefaultRegionsConfiguration() {
		return TestConfigurationWithSimpleCacheListener.class;
	}

	@Override
	protected Class<TestConfigurationWithInvalidRegion> getCacheListenerAnnotationWithInvalidRegion() {
		return TestConfigurationWithInvalidRegion.class;
	}

	@Override
	protected Class<TestConfigurationWith2RegionsAnd2CacheListenersDefaulted> getCacheListenerAnnotationMultipleRegionsDefault() {
		return TestConfigurationWith2RegionsAnd2CacheListenersDefaulted.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationSingleRegionAllEvents() {
		return TestConfigurationWithSimpleCacheListenerAllEvents.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationAgainst2NamedRegions() {
		return TestConfigurationWithSimpleCacheListenerWith2Regions.class;
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithIncorrectRegionEventParameter {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE)
		public void afterCreateListener(RegionEvent<String,String> event) {
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListener {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE)
		public void afterCreateListener(EntryEvent<String,String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_UPDATE)
		public void afterUpdateListener(EntryEvent<String,String> event) {
			recordEvent(event);

		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithInvalidRegion {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion2")
		public void afterCreateListener(EntryEvent<String,String> event) {
			recordEvent(event);
		}

	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListenerAllEvents {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener(EntryEvent<String,String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListenerWith2Regions {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ReplicatedRegionFactoryBean<String,String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion1")
		public void afterCreateListener(EntryEvent<String,String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion2")
		public void afterUpdateListener(EntryEvent<String,String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWith2RegionsAnd2CacheListenersDefaulted {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean<String,String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ReplicatedRegionFactoryBean<String,String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener1(EntryEvent<String,String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener2(EntryEvent<String,String> event) {
			recordEvent(event);
		}
	}
}
