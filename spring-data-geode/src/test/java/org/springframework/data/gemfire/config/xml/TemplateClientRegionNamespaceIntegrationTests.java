/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNotNull;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.apache.geode.cache.util.ObjectSizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

/**
 * Integration Tests for client {@link Region} templates using SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class TemplateClientRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("TemplateBasedClientRegion")
	private Region<Integer, Object> templateBasedClientRegion;

	private void assertCacheListeners(Region<?, ?> region, String... expectedNames) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getCacheListeners()).isNotNull();
		assertThat(region.getAttributes().getCacheListeners().length).isEqualTo(expectedNames.length);

		for (CacheListener<?, ?> cacheListener : region.getAttributes().getCacheListeners()) {
			assertThat(cacheListener instanceof TestCacheListener).isTrue();
			assertThat(Arrays.asList(expectedNames).contains(cacheListener.toString())).isTrue();
		}
	}

	private void assertCacheLoader(Region<?, ?> region, String expectedName) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getCacheLoader() instanceof TestCacheLoader).isTrue();
		assertThat(region.getAttributes().getCacheLoader().toString()).isEqualTo(expectedName);
	}

	private void assertCacheWriter(Region<?, ?> region, String expectedName) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getCacheWriter() instanceof TestCacheWriter).isTrue();
		assertThat(region.getAttributes().getCacheWriter().toString()).isEqualTo(expectedName);
	}

	private void assertDefaultEvictionAttributes(EvictionAttributes evictionAttributes) {

		assumeNotNull(evictionAttributes);
		assertEvictionAttributes(evictionAttributes, EvictionAction.NONE, EvictionAlgorithm.NONE, 0, null);
	}

	private void assertEvictionAttributes(EvictionAttributes evictionAttributes, EvictionAction expectedAction,
			EvictionAlgorithm expectedAlgorithm, int expectedMaximum, ObjectSizer expectedObjectSizer) {

		assertThat(evictionAttributes).describedAs("The 'EvictionAttributes' must not be null").isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(expectedAction);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(expectedAlgorithm);
		assertThat(evictionAttributes.getMaximum()).isEqualTo(expectedMaximum);
		assertThat(evictionAttributes.getObjectSizer()).isEqualTo(expectedObjectSizer);
	}

	private void assertDefaultExpirationAttributes(ExpirationAttributes expirationAttributes) {

		assumeNotNull(expirationAttributes);
		assertThat(expirationAttributes.getAction()).isEqualTo(ExpirationAction.INVALIDATE);
		assertThat(expirationAttributes.getTimeout()).isEqualTo(0);
	}

	private void assertExpirationAttributes(ExpirationAttributes expirationAttributes, ExpirationAction expectedAction,
			int expectedTimeout) {

		assertThat(expirationAttributes).as("The 'ExpirationAttributes' must not be null").isNotNull();
		assertThat(expirationAttributes.getAction()).isEqualTo(expectedAction);
		assertThat(expirationAttributes.getTimeout()).isEqualTo(expectedTimeout);
	}

	private void assertDefaultRegionAttributes(Region<?, ?> region) {

		assertThat(region).describedAs("The Region must not be null").isNotNull();
		assertThat(region.getAttributes())
			.describedAs("The Region (%1$s) must have 'RegionAttributes' defined", region.getFullPath())
			.isNotNull();
		assertThat(region.getAttributes().getCompressor()).isNull();
		assertThat(region.getAttributes().getCustomEntryIdleTimeout()).isNull();
		assertThat(region.getAttributes().getCustomEntryTimeToLive()).isNull();
		assertThat(region.getAttributes().getDiskStoreName()).isNull();
		assertThat(region.getAttributes().getMulticastEnabled()).isFalse();
		assertDefaultExpirationAttributes(region.getAttributes().getRegionTimeToLive());
		assertDefaultExpirationAttributes(region.getAttributes().getRegionIdleTimeout());
	}

	private static void assertEmpty(Object[] array) {
		assertThat(array == null || array.length == 0).isTrue();
	}

	private static void assertEmpty(Iterable<?> collection) {
		assertThat(collection == null || !collection.iterator().hasNext()).isTrue();
	}

	private static void assertNullEmpty(String value) {
		assertThat(StringUtils.hasText(value)).isFalse();
	}

	private static void assertRegionMetaData(Region<?, ?> region, String expectedRegionName) {
		assertRegionMetaData(region, expectedRegionName, Region.SEPARATOR + expectedRegionName);
	}

	private static void assertRegionMetaData(Region<?, ?> region, String expectedRegionName, String expectedRegionPath) {

		assertThat(region).as(String.format("The '%1$s' Region was not properly configured and initialized",
			expectedRegionName)).isNotNull();
		assertThat(region.getName()).isEqualTo(expectedRegionName);
		assertThat(region.getFullPath()).isEqualTo(expectedRegionPath);
		assertThat(region.getAttributes()).as(String.format("The '%1$s' Region must have RegionAttributes defined",
			expectedRegionName)).isNotNull();
	}

	@Test
	public void testTemplateBasedClientRegion() {

		assertRegionMetaData(templateBasedClientRegion, "TemplateBasedClientRegion");
		assertDefaultRegionAttributes(templateBasedClientRegion);
		assertCacheListeners(templateBasedClientRegion, "XYZ");
		assertCacheLoader(templateBasedClientRegion, "A");
		assertCacheWriter(templateBasedClientRegion, "B");
		assertThat(templateBasedClientRegion.getAttributes().getCloningEnabled()).isFalse();
		assertThat(templateBasedClientRegion.getAttributes().getConcurrencyChecksEnabled()).isFalse();
		assertThat(templateBasedClientRegion.getAttributes().getConcurrencyLevel()).isEqualTo(16);
		assertThat(templateBasedClientRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(templateBasedClientRegion.getAttributes().isDiskSynchronous()).isFalse();
		assertEvictionAttributes(templateBasedClientRegion.getAttributes().getEvictionAttributes(),
			EvictionAction.OVERFLOW_TO_DISK, EvictionAlgorithm.LRU_ENTRY, 1024, null);
		assertThat(templateBasedClientRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedClientRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
		assertThat(String.valueOf(templateBasedClientRegion.getAttributes().getLoadFactor())).isEqualTo("0.85");
		assertThat(templateBasedClientRegion.getAttributes().getPoolName()).isEqualTo("ServerPool");
		assertThat(templateBasedClientRegion.getAttributes().getStatisticsEnabled()).isTrue();
		assertThat(templateBasedClientRegion.getAttributes().getValueConstraint()).isEqualTo(Object.class);
		templateBasedClientRegion.getInterestList();
	}

	public static final class TestCacheListener extends CacheListenerAdapter<Object, Object> {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestCacheLoader implements CacheLoader<Object, Object> {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Object load(LoaderHelper loaderHelper) throws CacheLoaderException {
			return null;
		}

		@Override
		public void close() { }

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestCacheWriter extends CacheWriterAdapter<Object, Object> {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
