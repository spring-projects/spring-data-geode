/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.data.gemfire.config.annotation;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.eventing.config.CacheListenerEventType;

/**
 * Tests for {@link AsCacheListener} configured for a {@link org.apache.geode.cache.client.ClientCache}
 *
 * @author Udo Kohlmeyer
 * @see Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.client.ClientCache
 * @see Configuration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
public class AsCacheListenerClientCacheConfigurationTests extends AsCacheListenerConfigurationTests {

	protected static ClientRegionFactoryBean<String, String> createRegionFactoryBean(GemFireCache cache,
		String regionName) {
		ClientRegionFactoryBean<String, String> clientRegion = new ClientRegionFactoryBean<>();
		clientRegion.setName(regionName);
		clientRegion.setCache(cache);
		clientRegion.setShortcut(ClientRegionShortcut.LOCAL);
		return clientRegion;
	}

	@Override
	protected Class<?> getCacheListenerWithIncorrectRegionEventParameterConfiguration() {
		return TestConfigurationWithIncorrectEventParameter.class;
	}

	protected Class<?> getCacheListenerAnnotationSingleDefaultRegionsConfiguration() {
		return TestConfigurationWithSimpleCacheListener.class;
	}

	protected Class<TestConfigurationWithInvalidRegion> getCacheListenerAnnotationWithInvalidRegion() {
		return TestConfigurationWithInvalidRegion.class;
	}

	protected Class<TestConfigurationWith2RegionsAnd2CacheListenersDefaulted> getCacheListenerAnnotationMultipleRegionsDefault() {
		return TestConfigurationWith2RegionsAnd2CacheListenersDefaulted.class;
	}

	protected Class<?> getCacheListenerAnnotationSingleRegionAllEvents() {
		return TestConfigurationWithSimpleCacheListenerAllEvents.class;
	}

	protected Class<?> getCacheListenerAnnotationAgainst2NamedRegions() {
		return TestConfigurationWithSimpleCacheListenerWith2Regions.class;
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWithIncorrectEventParameter {

		@Bean("TestRegion")
		ClientRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE)
		public void afterCreateListener(RegionEvent<String, String> event) {
		}
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListener {

		@Bean("TestRegion1")
		ClientRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE)
		public void afterCreateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_UPDATE)
		public void afterUpdateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWithInvalidRegion {

		@Bean("TestRegion")
		ClientRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion2")
		public void afterCreateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListenerAllEvents {

		@Bean("TestRegion1")
		ClientRegionFactoryBean<String, String> getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWithSimpleCacheListenerWith2Regions {

		@Bean("TestRegion1")
		ClientRegionFactoryBean<String, String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ClientRegionFactoryBean<String, String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion1")
		public void afterCreateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.AFTER_CREATE, regions = "TestRegion2")
		public void afterUpdateListener(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}

	@Configuration
	@ClientCacheApplication
	@EnableEventProcessing
	public static class TestConfigurationWith2RegionsAnd2CacheListenersDefaulted {

		@Bean("TestRegion1")
		ClientRegionFactoryBean<String, String> getTestRegion1(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@Bean("TestRegion2")
		ClientRegionFactoryBean<String, String> getTestRegion2(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion2");
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener1(EntryEvent<String, String> event) {
			recordEvent(event);
		}

		@AsCacheListener(eventTypes = CacheListenerEventType.ALL)
		public void afterCreateListener2(EntryEvent<String, String> event) {
			recordEvent(event);
		}
	}
}
