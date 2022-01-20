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

import jakarta.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.config.annotation.test.partition.entities.NonPartitionEntity;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link EnableEntityDefinedRegions} annotation configuration
 * using {@literal real} {@link DataPolicy#PARTITION} {@link Region Regions}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class EntityDefinedRegionsForRealCollocatedPartitionRegionsIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private Cache peerCache;

	@Resource(name = "After")
	private Region<?, ?> after;

	@Resource(name = "Before")
	private Region<?, ?> before;

	@Before
	public void setup() {

		assertThat(this.peerCache).isNotNull();
		assertThat(this.peerCache.getName())
			.isEqualTo(EntityDefinedRegionsForRealCollocatedPartitionRegionsIntegrationTests.class.getSimpleName());
		assertThat(this.after).isNotNull();
		assertThat(this.before).isNotNull();
		assertThat(this.peerCache.getRegion(this.after.getFullPath())).isEqualTo(this.after);
		assertThat(this.peerCache.getRegion(this.before.getFullPath())).isEqualTo(this.before);
	}

	private void assertRegion(Region<?, ?> region, String name, int redundantCopies) {
		assertRegion(region, name, null, redundantCopies);
	}

	private void assertRegion(Region<?, ?> region, String name, String collocatedWith, int redundantCopies) {

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(name);
		assertThat(region.getFullPath()).isEqualTo(RegionUtils.toRegionPath(name));
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(region.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(region.getAttributes().getPartitionAttributes().getColocatedWith()).isEqualTo(collocatedWith);
		assertThat(region.getAttributes().getPartitionAttributes().getRedundantCopies()).isEqualTo(redundantCopies);
	}

	@Test
	public void partitionRegionConfigurationIsCorrect() {

		assertRegion(this.after, "After", "Before", 1);
		assertRegion(this.before, "Before", 1);
	}

	@PeerCacheApplication(name = "EntityDefinedRegionsForRealCollocatedPartitionRegionsIntegrationTests")
	@EnableEntityDefinedRegions(basePackageClasses = NonPartitionEntity.class)
	static class TestConfiguration { }

}
