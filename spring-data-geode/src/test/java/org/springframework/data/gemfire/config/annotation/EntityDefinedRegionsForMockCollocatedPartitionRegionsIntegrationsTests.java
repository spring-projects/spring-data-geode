/*
 * Copyright 2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.PartitionResolver;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.gemfire.config.annotation.test.entities.NonEntity;
import org.springframework.data.gemfire.mapping.annotation.PartitionRegion;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link EnableEntityDefinedRegions} annotation configuration
 * using {@literal mock} {@link DataPolicy#PARTITION} {@link Region Regions}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.DataPolicy
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.PartitionedRegionFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheApplication
 * @see org.springframework.data.gemfire.mapping.annotation.PartitionRegion
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class EntityDefinedRegionsForMockCollocatedPartitionRegionsIntegrationsTests extends IntegrationTestsSupport {

	@Autowired
	private Cache peerCache;

	@Resource(name = "ContactEvents")
	private Region<?, ?> contactEvents;

	@Resource(name = "Customers")
	private Region<?, ?> customers;

	@Before
	public void setup() {

		assertThat(this.peerCache).isNotNull();
		assertThat(this.peerCache.getName())
			.isEqualTo(EntityDefinedRegionsForMockCollocatedPartitionRegionsIntegrationsTests.class.getSimpleName());
		assertThat(this.contactEvents).isNotNull();
		assertThat(this.customers).isNotNull();
		assertThat(this.peerCache.getRegion(this.contactEvents.getFullPath())).isEqualTo(this.contactEvents);
		assertThat(this.peerCache.getRegion(this.customers.getFullPath())).isEqualTo(this.customers);
	}

	private void assertRegion(Region<?, ?> region, String name, int redundantCopies) {
		assertRegion(region, name, null, redundantCopies);
	}

	private void assertRegion(Region<?, ?> region, String name, String collocatedWith, int redundantCopies) {

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(name);
		assertThat(region.getFullPath()).isEqualTo(RegionUtils.toRegionPath(name));
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(region.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes().getColocatedWith()).isEqualTo(collocatedWith);
		assertThat(region.getAttributes().getPartitionAttributes().getRedundantCopies()).isEqualTo(redundantCopies);
	}

	@Test
	public void partitionRegionConfiguration() {

		assertRegion(this.contactEvents, "ContactEvents", "Customers", 2);
		assertRegion(this.customers, "Customers", 1);
	}

	@PeerCacheApplication(name = "EntityDefinedRegionsForMockCollocatedPartitionRegionsIntegrationsTests")
	@EnableEntityDefinedRegions(basePackageClasses = NonEntity.class,
		includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = PartitionRegion.class))
	static class TestConfiguration {

		@Bean
		DiskStore mockDiskStore() {
			return mock(DiskStore.class);
		}

		@Bean
		@SuppressWarnings("rawtypes")
		PartitionResolver mockPartitionResolver() {
			return mock(PartitionResolver.class);
		}
	}
}
