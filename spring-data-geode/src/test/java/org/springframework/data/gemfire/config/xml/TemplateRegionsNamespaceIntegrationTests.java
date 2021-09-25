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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.Offset.offset;
import static org.junit.Assume.assumeNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EntryOperation;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.InterestPolicy;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.PartitionResolver;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.SubscriptionAttributes;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.cache.partition.PartitionListener;
import org.apache.geode.cache.partition.PartitionListenerAdapter;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.apache.geode.cache.util.ObjectSizer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

/**
 * Integration Tests for {@link Region} Templates.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings({ "rawtypes", "unused"})
public class TemplateRegionsNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier("NonTemplateBasedReplicateRegion")
	private Region<Integer, String> nonTemplateBasedReplicateRegion;

	@Autowired
	@Qualifier("TemplateBasedReplicateRegion")
	private Region<String, Object> templateBasedReplicateRegion;

	@Autowired
	@Qualifier("/TemplateBasedReplicateRegion/TemplateBasedReplicateSubRegion")
	private Region<Integer, String> templateBasedReplicateSubRegion;

	@Autowired
	@Qualifier("TemplateBasedReplicateRegionNoOverrides")
	private Region<String, Object> templateBasedReplicateRegionNoOverrides;

	@Autowired
	@Qualifier("TemplateBasedPartitionRegion")
	private Region<Date, Object> templateBasedPartitionRegion;

	@Autowired
	@Qualifier("TemplateBasedLocalRegion")
	private Region<Long, String> templateBasedLocalRegion;

	private void assertAsyncEventQueues(Region<?, ?> region, String... expectedNames) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getAsyncEventQueueIds()).isNotNull();
		assertThat(region.getAttributes().getAsyncEventQueueIds().size()).isEqualTo(expectedNames.length);

		for (String asyncEventQueueId : region.getAttributes().getAsyncEventQueueIds()) {
			assertThat(Arrays.asList(expectedNames).contains(asyncEventQueueId)).isTrue();
		}
	}

	private void assertCacheListeners(Region<?, ?> region, String... expectedNames) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getCacheListeners()).isNotNull();
		assertThat(region.getAttributes().getCacheListeners().length).isEqualTo(expectedNames.length);

		for (CacheListener cacheListener : region.getAttributes().getCacheListeners()) {
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

		assertThat(evictionAttributes).as("The 'EvictionAttributes' must not be null!").isNotNull();
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

		assertThat(expirationAttributes).as("The 'ExpirationAttributes' must not be null!").isNotNull();
		assertThat(expirationAttributes.getAction()).isEqualTo(expectedAction);
		assertThat(expirationAttributes.getTimeout()).isEqualTo(expectedTimeout);
	}

	private void assertGatewaySenders(Region<?, ?> region, String... gatewaySenderIds) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getGatewaySenderIds()).isNotNull();
		assertThat(region.getAttributes().getGatewaySenderIds().size()).isEqualTo(gatewaySenderIds.length);
		assertThat(Arrays.asList(gatewaySenderIds).containsAll(region.getAttributes().getGatewaySenderIds())).isTrue();
	}

	private void assertPartitionListener(Region<?, ?> region, String... expectedNames) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes().getPartitionListeners()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes().getPartitionListeners().length)
			.isEqualTo(expectedNames.length);

		for (PartitionListener partitionListener : region.getAttributes().getPartitionAttributes().getPartitionListeners()) {
			assertThat(partitionListener instanceof TestPartitionListener).isTrue();
			assertThat(Arrays.asList(expectedNames).contains(partitionListener.toString())).isTrue();
		}
	}

	private void assertPartitionResolver(Region<?, ?> region, String expectedName) {

		assertThat(region).isNotNull();
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(
			region.getAttributes().getPartitionAttributes().getPartitionResolver() instanceof TestPartitionResolver)
			.isTrue();
		assertThat(region.getAttributes().getPartitionAttributes().getPartitionResolver().toString())
			.isEqualTo(expectedName);
	}

	@SuppressWarnings("unchecked")
	private void assertDefaultRegionAttributes(Region region) {

		assertThat(region).describedAs("The Region must not be null!").isNotNull();

		assertThat(region.getAttributes())
			.describedAs(String.format("Region (%1$s) must have 'RegionAttributes' defined!",region.getFullPath()))
			.isNotNull();

		assertThat(region.getAttributes().getCompressor()).isNull();
		assertThat(region.getAttributes().getCustomEntryIdleTimeout()).isNull();
		assertThat(region.getAttributes().getCustomEntryTimeToLive()).isNull();
		assertThat(region.getAttributes().getDiskStoreName()).isNull();
		assertThat(region.getAttributes().getMulticastEnabled()).isFalse();
		assertNullEmpty(region.getAttributes().getPoolName());
		assertDefaultExpirationAttributes(region.getAttributes().getRegionTimeToLive());
		assertDefaultExpirationAttributes(region.getAttributes().getRegionIdleTimeout());
	}

	private void assertDefaultSubscriptionAttributes(SubscriptionAttributes subscriptionAttributes) {

		assumeNotNull(subscriptionAttributes);
		assertSubscriptionAttributes(subscriptionAttributes, InterestPolicy.DEFAULT);
	}

	private void assertSubscriptionAttributes(SubscriptionAttributes subscriptionAttributes,
			InterestPolicy expectedInterestPolicy) {

		assertThat(subscriptionAttributes).as("The 'SubscriptionAttributes' must not be null!").isNotNull();
		assertThat(subscriptionAttributes.getInterestPolicy()).isEqualTo(expectedInterestPolicy);
	}

	private static void assertEmpty(Object[] array) {
		assertThat((array == null || array.length == 0)).isTrue();
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

		assertThat(region).as(String.format("The '%1$s' Region was not properly configured and initialized!",
			expectedRegionName)).isNotNull();
		assertThat(region.getName()).isEqualTo(expectedRegionName);
		assertThat(region.getFullPath()).isEqualTo(expectedRegionPath);
		assertThat(region.getAttributes()).as(String.format("The '%1$s' Region must have RegionAttributes defined!",
			expectedRegionName)).isNotNull();
	}

	@Test
	public void testNoAbstractRegionTemplateBeans() {

		String[] beanNames = {
			"BaseRegionTemplate",
			"ExtendedRegionTemplate",
			"ReplicateRegionTemplate",
			"PartitionRegionTemplate",
			"LocalRegionTemplate"
		};

		for (String beanName : beanNames) {
			assertThat(applicationContext.containsBean(beanName)).isTrue();
			assertThat(applicationContext.containsBeanDefinition(beanName)).isTrue();

			try {
				applicationContext.getBean(beanName);
				fail(String
					.format("The abstract bean definition '%1$s' should not exist as a bean in the Spring context!",
						beanName));
			}
			catch (BeansException expected) {
				assertThat(expected instanceof BeanIsAbstractException).isTrue();
				assertThat(expected.getMessage().contains(beanName)).isTrue();
			}
		}
	}

	@Test
	public void testNonTemplateBasedReplicateRegion() {

		assertRegionMetaData(nonTemplateBasedReplicateRegion, "NonTemplateBasedReplicateRegion");
		assertDefaultRegionAttributes(nonTemplateBasedReplicateRegion);
		assertEmpty(nonTemplateBasedReplicateRegion.getAttributes().getAsyncEventQueueIds());
		assertEmpty(nonTemplateBasedReplicateRegion.getAttributes().getCacheListeners());
		assertCacheLoader(nonTemplateBasedReplicateRegion, "ABC");
		assertCacheWriter(nonTemplateBasedReplicateRegion, "DEF");
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getCloningEnabled()).isFalse();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getConcurrencyLevel()).isEqualTo(12);
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().isDiskSynchronous()).isTrue();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getEnableAsyncConflation()).isFalse();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getEnableSubscriptionConflation()).isFalse();
		assertDefaultEvictionAttributes(nonTemplateBasedReplicateRegion.getAttributes().getEvictionAttributes());
		assertDefaultExpirationAttributes(nonTemplateBasedReplicateRegion.getAttributes().getEntryIdleTimeout());
		assertDefaultExpirationAttributes(nonTemplateBasedReplicateRegion.getAttributes().getEntryTimeToLive());
		assertEmpty(nonTemplateBasedReplicateRegion.getAttributes().getGatewaySenderIds());
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getIgnoreJTA()).isFalse();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getIndexMaintenanceSynchronous()).isTrue();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getInitialCapacity()).isEqualTo(97);
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getKeyConstraint()).isNull();
		assertThat(String.valueOf(nonTemplateBasedReplicateRegion.getAttributes().getLoadFactor())).isEqualTo("0.65");
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().isLockGrantor()).isFalse();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getPartitionAttributes()).isNull();
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getStatisticsEnabled()).isFalse();
		assertDefaultSubscriptionAttributes(nonTemplateBasedReplicateRegion.getAttributes().getSubscriptionAttributes());
		assertThat(nonTemplateBasedReplicateRegion.getAttributes().getValueConstraint()).isNull();
	}

	@Test
	public void testTemplateBasedReplicateRegion() {

		assertRegionMetaData(templateBasedReplicateRegion, "TemplateBasedReplicateRegion");
		assertDefaultRegionAttributes(templateBasedReplicateRegion);
		assertEmpty(templateBasedReplicateRegion.getAttributes().getAsyncEventQueueIds());
		assertCacheListeners(templateBasedReplicateRegion, "XYZ");
		assertCacheLoader(templateBasedReplicateRegion, "dbLoader");
		assertCacheWriter(templateBasedReplicateRegion, "dbWriter");
		assertThat(templateBasedReplicateRegion.getAttributes().getCloningEnabled()).isTrue();
		assertThat(templateBasedReplicateRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(templateBasedReplicateRegion.getAttributes().getConcurrencyLevel()).isEqualTo(24);
		assertThat(templateBasedReplicateRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(templateBasedReplicateRegion.getAttributes().isDiskSynchronous()).isFalse();
		assertThat(templateBasedReplicateRegion.getAttributes().getEnableAsyncConflation()).isFalse();
		assertThat(templateBasedReplicateRegion.getAttributes().getEnableSubscriptionConflation()).isTrue();
		assertEvictionAttributes(templateBasedReplicateRegion.getAttributes().getEvictionAttributes(),
			EvictionAction.OVERFLOW_TO_DISK, EvictionAlgorithm.LRU_ENTRY, 2024, null);
		assertExpirationAttributes(templateBasedReplicateRegion.getAttributes().getEntryIdleTimeout(),
			ExpirationAction.DESTROY, 600);
		assertExpirationAttributes(templateBasedReplicateRegion.getAttributes().getEntryTimeToLive(),
			ExpirationAction.INVALIDATE, 300);
		assertEmpty(templateBasedReplicateRegion.getAttributes().getGatewaySenderIds());
		assertThat(templateBasedReplicateRegion.getAttributes().getIgnoreJTA()).isTrue();
		assertThat(templateBasedReplicateRegion.getAttributes().getIndexMaintenanceSynchronous()).isTrue();
		assertThat(templateBasedReplicateRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedReplicateRegion.getAttributes().getKeyConstraint()).isEqualTo(String.class);
		assertThat(String.valueOf(templateBasedReplicateRegion.getAttributes().getLoadFactor())).isEqualTo("0.85");
		assertThat(templateBasedReplicateRegion.getAttributes().isLockGrantor()).isTrue();
		assertThat(templateBasedReplicateRegion.getAttributes().getPartitionAttributes()).isNull();
		assertThat(templateBasedReplicateRegion.getAttributes().getScope()).isEqualTo(Scope.GLOBAL);
		assertThat(templateBasedReplicateRegion.getAttributes().getStatisticsEnabled()).isTrue();
		assertSubscriptionAttributes(templateBasedReplicateRegion.getAttributes().getSubscriptionAttributes(),
			InterestPolicy.CACHE_CONTENT);
		assertThat(templateBasedReplicateRegion.getAttributes().getValueConstraint()).isEqualTo(Object.class);
	}

	@Test
	public void testTemplateBasedReplicateSubRegion() {

		assertRegionMetaData(templateBasedReplicateSubRegion, "TemplateBasedReplicateSubRegion",
			"/TemplateBasedReplicateRegion/TemplateBasedReplicateSubRegion");
		assertDefaultRegionAttributes(templateBasedReplicateSubRegion);
		assertEmpty(templateBasedReplicateSubRegion.getAttributes().getAsyncEventQueueIds());
		assertCacheListeners(templateBasedReplicateSubRegion, "testListener");
		assertCacheLoader(templateBasedReplicateSubRegion, "A");
		assertCacheWriter(templateBasedReplicateSubRegion, "B");
		assertThat(templateBasedReplicateSubRegion.getAttributes().getCloningEnabled()).isFalse();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getConcurrencyLevel()).isEqualTo(16);
		assertThat(templateBasedReplicateSubRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(templateBasedReplicateSubRegion.getAttributes().isDiskSynchronous()).isFalse();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getEnableAsyncConflation()).isTrue();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getEnableSubscriptionConflation()).isFalse();
		assertDefaultEvictionAttributes(templateBasedReplicateSubRegion.getAttributes().getEvictionAttributes());
		assertExpirationAttributes(templateBasedReplicateSubRegion.getAttributes().getEntryIdleTimeout(),
			ExpirationAction.DESTROY, 600);
		assertExpirationAttributes(templateBasedReplicateSubRegion.getAttributes().getEntryTimeToLive(),
			ExpirationAction.DESTROY, 600);
		assertEmpty(templateBasedReplicateSubRegion.getAttributes().getGatewaySenderIds());
		assertThat(templateBasedReplicateSubRegion.getAttributes().getIgnoreJTA()).isTrue();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getIndexMaintenanceSynchronous()).isFalse();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedReplicateSubRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
		assertThat(String.valueOf(templateBasedReplicateSubRegion.getAttributes().getLoadFactor())).isEqualTo("0.95");
		assertThat(templateBasedReplicateSubRegion.getAttributes().isLockGrantor()).isFalse();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getPartitionAttributes()).isNull();
		assertThat(templateBasedReplicateSubRegion.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
		assertThat(templateBasedReplicateSubRegion.getAttributes().getStatisticsEnabled()).isTrue();
		assertDefaultSubscriptionAttributes(templateBasedReplicateSubRegion.getAttributes().getSubscriptionAttributes());
		assertThat(templateBasedReplicateSubRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);
	}

	@Test
	public void testTemplateBasedReplicateRegionNoOverrides() {

		assertRegionMetaData(templateBasedReplicateRegionNoOverrides, "TemplateBasedReplicateRegionNoOverrides");
		assertDefaultRegionAttributes(templateBasedReplicateRegionNoOverrides);
		assertEmpty(templateBasedReplicateRegionNoOverrides.getAttributes().getAsyncEventQueueIds());
		assertCacheListeners(templateBasedReplicateRegionNoOverrides, "XYZ");
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getCacheLoader()).isNull();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getCacheWriter()).isNull();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getCloningEnabled()).isTrue();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getConcurrencyLevel()).isEqualTo(24);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getDataPolicy())
			.isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().isDiskSynchronous()).isFalse();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getEnableAsyncConflation()).isFalse();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getEnableSubscriptionConflation()).isTrue();
		assertEvictionAttributes(templateBasedReplicateRegionNoOverrides.getAttributes().getEvictionAttributes(),
			EvictionAction.OVERFLOW_TO_DISK, EvictionAlgorithm.LRU_ENTRY, 2024, null);
		assertExpirationAttributes(templateBasedReplicateRegionNoOverrides.getAttributes().getEntryIdleTimeout(),
			ExpirationAction.DESTROY, 600);
		assertExpirationAttributes(templateBasedReplicateRegionNoOverrides.getAttributes().getEntryTimeToLive(),
			ExpirationAction.INVALIDATE, 300);
		assertEmpty(templateBasedReplicateRegionNoOverrides.getAttributes().getGatewaySenderIds());
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getIgnoreJTA()).isTrue();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getIndexMaintenanceSynchronous()).isTrue();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getKeyConstraint()).isEqualTo(String.class);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getLoadFactor())
			.isCloseTo(0.85f, offset(0.0f));
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().isLockGrantor()).isFalse();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getPartitionAttributes()).isNull();
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_ACK);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getStatisticsEnabled()).isTrue();
		assertSubscriptionAttributes(
			templateBasedReplicateRegionNoOverrides.getAttributes().getSubscriptionAttributes(),
			InterestPolicy.CACHE_CONTENT);
		assertThat(templateBasedReplicateRegionNoOverrides.getAttributes().getValueConstraint())
			.isEqualTo(Object.class);
	}

	@Test
	public void testTemplateBasedPartitionRegion() {

		assertRegionMetaData(templateBasedPartitionRegion, "TemplateBasedPartitionRegion");
		assertDefaultRegionAttributes(templateBasedPartitionRegion);
		assertAsyncEventQueues(templateBasedPartitionRegion, "TestAsyncEventQueue");
		assertCacheListeners(templateBasedPartitionRegion, "X", "Y", "Z");
		assertCacheLoader(templateBasedPartitionRegion, "A");
		assertCacheWriter(templateBasedPartitionRegion, "dbWriter");
		assertThat(templateBasedPartitionRegion.getAttributes().getCloningEnabled()).isFalse();
		assertThat(templateBasedPartitionRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(templateBasedPartitionRegion.getAttributes().getDataPolicy())
			.isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(templateBasedPartitionRegion.getAttributes().isDiskSynchronous()).isTrue();
		assertThat(templateBasedPartitionRegion.getAttributes().getEnableAsyncConflation()).isTrue();
		assertThat(templateBasedPartitionRegion.getAttributes().getEnableSubscriptionConflation()).isTrue();
		assertEvictionAttributes(templateBasedPartitionRegion.getAttributes().getEvictionAttributes(),
			EvictionAction.OVERFLOW_TO_DISK, EvictionAlgorithm.LRU_ENTRY, 8192000, null);
		assertExpirationAttributes(templateBasedPartitionRegion.getAttributes().getEntryIdleTimeout(),
			ExpirationAction.DESTROY, 600);
		assertExpirationAttributes(templateBasedPartitionRegion.getAttributes().getEntryTimeToLive(),
			ExpirationAction.INVALIDATE, 300);
		assertGatewaySenders(templateBasedPartitionRegion, "TestGatewaySender");
		assertThat(templateBasedPartitionRegion.getAttributes().getIgnoreJTA()).isFalse();
		assertThat(templateBasedPartitionRegion.getAttributes().getIndexMaintenanceSynchronous()).isFalse();
		assertThat(templateBasedPartitionRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedPartitionRegion.getAttributes().getKeyConstraint()).isEqualTo(Date.class);
		assertThat(String.valueOf(templateBasedPartitionRegion.getAttributes().getLoadFactor())).isEqualTo("0.7");
		assertThat(templateBasedPartitionRegion.getAttributes().isLockGrantor()).isFalse();
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getColocatedWith())
			.isEqualTo("Neighbor");
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getLocalMaxMemory())
			.isEqualTo(8192);
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getRedundantCopies())
			.isEqualTo(2);
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getRecoveryDelay())
			.isEqualTo(60000L);
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getStartupRecoveryDelay())
			.isEqualTo(15000L);
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getTotalMaxMemory())
			.isEqualTo(16384);
		assertThat(templateBasedPartitionRegion.getAttributes().getPartitionAttributes().getTotalNumBuckets())
			.isEqualTo(91);
		assertPartitionListener(templateBasedPartitionRegion, "testListener");
		assertPartitionResolver(templateBasedPartitionRegion, "testResolver");
		assertThat(templateBasedPartitionRegion.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
		assertThat(templateBasedPartitionRegion.getAttributes().getStatisticsEnabled()).isTrue();
		assertSubscriptionAttributes(templateBasedPartitionRegion.getAttributes().getSubscriptionAttributes(),
			InterestPolicy.ALL);
		assertThat(templateBasedPartitionRegion.getAttributes().getValueConstraint()).isEqualTo(Object.class);
	}

	@Test
	public void testTemplateBasedLocalRegion() {

		assertRegionMetaData(templateBasedLocalRegion, "TemplateBasedLocalRegion");
		assertDefaultRegionAttributes(templateBasedLocalRegion);
		assertEmpty(templateBasedLocalRegion.getAttributes().getAsyncEventQueueIds());
		assertCacheListeners(templateBasedLocalRegion, "X", "Y", "Z");
		assertThat(templateBasedLocalRegion.getAttributes().getCacheLoader()).isNull();
		assertThat(templateBasedLocalRegion.getAttributes().getCacheWriter()).isNull();
		assertThat(templateBasedLocalRegion.getAttributes().getCloningEnabled()).isTrue();
		assertThat(templateBasedLocalRegion.getAttributes().getConcurrencyChecksEnabled()).isFalse();
		assertThat(templateBasedLocalRegion.getAttributes().getConcurrencyLevel()).isEqualTo(8);
		assertThat(templateBasedLocalRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(templateBasedLocalRegion.getAttributes().isDiskSynchronous()).isFalse();
		assertThat(templateBasedLocalRegion.getAttributes().getEnableAsyncConflation()).isFalse();
		assertThat(templateBasedLocalRegion.getAttributes().getEnableSubscriptionConflation()).isFalse();
		assertEvictionAttributes(templateBasedLocalRegion.getAttributes().getEvictionAttributes(),
			EvictionAction.LOCAL_DESTROY, EvictionAlgorithm.LRU_ENTRY, 4096, null);
		assertExpirationAttributes(templateBasedLocalRegion.getAttributes().getEntryIdleTimeout(),
			ExpirationAction.DESTROY, 600);
		assertExpirationAttributes(templateBasedLocalRegion.getAttributes().getEntryTimeToLive(),
			ExpirationAction.INVALIDATE, 300);
		assertEmpty(templateBasedLocalRegion.getAttributes().getGatewaySenderIds());
		assertThat(templateBasedLocalRegion.getAttributes().getIgnoreJTA()).isTrue();
		assertThat(templateBasedLocalRegion.getAttributes().getIndexMaintenanceSynchronous()).isTrue();
		assertThat(templateBasedLocalRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
		assertThat(templateBasedLocalRegion.getAttributes().getKeyConstraint()).isEqualTo(Long.class);
		assertThat(String.valueOf(templateBasedLocalRegion.getAttributes().getLoadFactor())).isEqualTo("0.85");
		assertThat(templateBasedLocalRegion.getAttributes().isLockGrantor()).isFalse();
		assertThat(templateBasedLocalRegion.getAttributes().getPartitionAttributes()).isNull();
		assertThat(templateBasedLocalRegion.getAttributes().getScope()).isEqualTo(Scope.LOCAL);
		assertThat(templateBasedLocalRegion.getAttributes().getStatisticsEnabled()).isTrue();
		assertDefaultSubscriptionAttributes(templateBasedLocalRegion.getAttributes().getSubscriptionAttributes());
		assertThat(templateBasedLocalRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);
	}

	public static final class TestAsyncEventListener implements AsyncEventListener {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean processEvents(List<AsyncEvent> asyncEvents) {
			return false;
		}

		@Override
		public void close() { }

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestCacheListener extends CacheListenerAdapter {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestCacheLoader implements CacheLoader {

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

	public static final class TestCacheWriter extends CacheWriterAdapter {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestPartitionListener extends PartitionListenerAdapter {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static final class TestPartitionResolver implements PartitionResolver {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Object getRoutingObject(EntryOperation entryOperation) {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void close() { }

		@Override
		public String toString() {
			return name;
		}
	}
}
