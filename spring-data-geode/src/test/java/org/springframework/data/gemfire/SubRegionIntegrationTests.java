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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.InterestPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.SubscriptionAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for Apache Geode {@link Region sub-Regions} defined in SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.4.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings({ "rawtypes", "unused" })
public class SubRegionIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private Cache cache;

	@Resource(name = "Customers")
	private Region customers;

	@Resource(name = "/Customers/Accounts")
	private Region accounts;

	@Test
	@SuppressWarnings("unchecked")
	public void testGemFireAccountsSubRegionCreation() {

		assertThat(cache).as("The GemFire Cache was not properly initialized!").isNotNull();

		Region customers = cache.getRegion("Customers");

		assertThat(customers).isNotNull();
		assertThat(customers.getName()).isEqualTo("Customers");
		assertThat(customers.getFullPath()).isEqualTo("/Customers");

		Region accounts = customers.getSubregion("Accounts");

		assertThat(accounts).isNotNull();
		assertThat(accounts.getName()).isEqualTo("Accounts");
		assertThat(accounts.getFullPath()).isEqualTo("/Customers/Accounts");

		Region cacheAccounts = cache.getRegion("/Customers/Accounts");

		assertThat(cacheAccounts).isSameAs(accounts);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSpringSubRegionConfiguration() {

		assertThat(accounts).as("The /Customers/Accounts SubRegion was not properly initialized!").isNotNull();
		assertThat(accounts.getName()).isEqualTo("Accounts");
		assertThat(accounts.getFullPath()).isEqualTo("/Customers/Accounts");

		RegionAttributes regionAttributes = accounts.getAttributes();

		assertThat(regionAttributes).isNotNull();
		assertThat(regionAttributes.getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
		assertThat(regionAttributes.getConcurrencyLevel()).isEqualTo(20);
		assertThat(regionAttributes.isDiskSynchronous()).isTrue();
		assertThat(regionAttributes.getIgnoreJTA()).isTrue();
		assertThat(regionAttributes.getIndexMaintenanceSynchronous()).isFalse();
		assertThat(regionAttributes.getInitialCapacity()).isEqualTo(1000);
		assertThat(regionAttributes.getKeyConstraint()).isEqualTo(Long.class);
		assertThat(regionAttributes.getScope()).isEqualTo(Scope.DISTRIBUTED_ACK);
		assertThat(regionAttributes.getStatisticsEnabled()).isTrue();
		assertThat(regionAttributes.getValueConstraint()).isEqualTo(String.class);
		assertThat(regionAttributes.getCacheListeners()).isNotNull();
		assertThat(regionAttributes.getCacheListeners().length).isEqualTo(1);
		assertThat(regionAttributes.getCacheListeners()[0] instanceof SimpleCacheListener).isTrue();
		assertThat(regionAttributes.getCacheLoader() instanceof SimpleCacheLoader).isTrue();
		assertThat(regionAttributes.getCacheWriter() instanceof SimpleCacheWriter).isTrue();

		EvictionAttributes evictionAttributes = regionAttributes.getEvictionAttributes();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(evictionAttributes.getMaximum()).isEqualTo(10000);

		SubscriptionAttributes subscriptionAttributes = regionAttributes.getSubscriptionAttributes();

		assertThat(subscriptionAttributes).isNotNull();
		assertThat(subscriptionAttributes.getInterestPolicy()).isEqualTo(InterestPolicy.CACHE_CONTENT);
	}
}
