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
import org.springframework.data.gemfire.eventing.config.RegionCacheListenerEventType;
import org.springframework.data.gemfire.eventing.config.RegionCacheWriterEventType;

/**
 * Tests for {@link AsCacheListener} configured for a
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
public class AsRegionEventCacheServerConfigurationTests extends AsRegionEventConfigurationTests {

	protected static ReplicatedRegionFactoryBean<String, String> createRegionFactoryBean(GemFireCache cache,
		String regionName) {
		ReplicatedRegionFactoryBean<String, String> replicateRegion = new ReplicatedRegionFactoryBean<>();
		replicateRegion.setName(regionName);
		replicateRegion.setCache(cache);
		return replicateRegion;
	}

	@Override
	protected Class<?> getRegionEventWithIncorrectRegionEventParameterConfiguration() {
		return TestConfigurationWithIncorrectRegionEventParameter.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationRegionEventClearConfiguration() {
		return TestConfigurationForCacheListenerRegionClear.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationWithInvalidRegion() {
		return TestConfigurationWithInvalidRegionNameConfiguration.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationRegionEventDestroyConfiguration() {
		return TestConfigurationForCacheListenerRegionDestroy.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationRegionDestroyConfiguration() {
		return TestConfigurationForCacheWriterRegionDestroy.class;
	}

	@Override
	protected Class<?> getCacheWriterAnnotationRegionClearConfiguration() {
		return TestConfigurationForCacheWriterRegionClear.class;
	}

	@Override
	protected Class<?> getCacheListenerAnnotationRegionEventInvalidateConfiguration() {
		return TestConfigurationForRegionInvalidate.class;
	}

	@Override
	protected Class<?> getRegionClearWithBothWriterAndListenerConfiguration() {
		return AsRegionEventClientCacheConfigurationTests.TestConfigurationForWithBothListenerAndWriterSingleHandler.class;
	}

	@Override
	protected Class<?> getRegionClearWithNoEventHandlersConfiguration() {
		return AsRegionEventClientCacheConfigurationTests.TestConfigurationForWithNoEventHandlers.class;
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithIncorrectRegionEventParameter {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_CREATE)
		public void afterCreateListener(EntryEvent<String, String> event) {
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForCacheListenerRegionClear {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_CLEAR)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationWithInvalidRegionNameConfiguration {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regions = "TestRegion2", regionListenerEventTypes = RegionCacheListenerEventType.ALL)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForRegionCreate {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_CREATE)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForRegionInvalidate {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_INVALIDATE)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForCacheListenerRegionDestroy {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_DESTROY)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForRegionLive {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_LIVE)
		public void afterCreateListener(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForCacheWriterRegionClear {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionWriterEventTypes = RegionCacheWriterEventType.BEFORE_REGION_CLEAR)
		public void beforeRegionClear(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForCacheWriterRegionDestroy {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionWriterEventTypes = RegionCacheWriterEventType.BEFORE_REGION_DESTROY)
		public void beforeRegionWriter(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForWithBothListenerAndWriterSingleHandler {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler(regionWriterEventTypes = RegionCacheWriterEventType.BEFORE_REGION_CLEAR,
			regionListenerEventTypes = RegionCacheListenerEventType.AFTER_REGION_CLEAR)
		public void regionClear(RegionEvent event) {
			recordEvent(event);
		}
	}

	@Configuration
	@CacheServerApplication
	@EnableEventProcessing
	public static class TestConfigurationForWithNoEventHandlers {

		@Bean("TestRegion1")
		ReplicatedRegionFactoryBean getTestRegion(GemFireCache cache) {
			return createRegionFactoryBean(cache, "TestRegion1");
		}

		@AsRegionEventHandler()
		public void regionClear(RegionEvent event) {
			recordEvent(event);
		}
	}
}
