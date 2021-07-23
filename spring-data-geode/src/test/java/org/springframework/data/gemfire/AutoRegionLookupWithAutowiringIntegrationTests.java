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

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for SDG's Auto {@link Region} Lookup functionality when combined with Spring's component
 * auto-wiring capabilities.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class AutoRegionLookupWithAutowiringIntegrationTests extends IntegrationTestsSupport {

	private static void assertRegionMetaData(Region<?, ?> region, String expectedName, DataPolicy expectedDataPolicy) {
		assertRegionMetaData(region, expectedName, Region.SEPARATOR + expectedName, expectedDataPolicy);
	}

	private static void assertRegionMetaData(Region<?, ?> region, String expectedName, String expectedFullPath,
			DataPolicy expectedDataPolicy) {

		assertThat(region)
			.describedAs(String.format("Region (%1$s) was not properly configured and initialized!", expectedName))
			.isNotNull();

		assertThat(region.getName()).isEqualTo(expectedName);
		assertThat(region.getFullPath()).isEqualTo(expectedFullPath);

		assertThat(region.getAttributes())
			.describedAs(String.format("Region (%1$s) must have RegionAttributes defined!", expectedName))
			.isNotNull();

		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(expectedDataPolicy);
		assertThat(region.getAttributes().getDataPolicy().withPersistence()).isFalse();
	}

	@Autowired
	private TestComponent testComponent;

	@Test
	public void testAutowiredNativeRegions() {

		assertRegionMetaData(testComponent.nativePartitionedRegion, "NativePartitionedRegion", DataPolicy.PARTITION);
		assertRegionMetaData(testComponent.nativeReplicateParent, "NativeReplicateParent", DataPolicy.REPLICATE);
		assertRegionMetaData(testComponent.nativeReplicateChild, "NativeReplicateChild",
			"/NativeReplicateParent/NativeReplicateChild", DataPolicy.REPLICATE);
		assertRegionMetaData(testComponent.nativeReplicateGrandchild, "NativeReplicateGrandchild",
			"/NativeReplicateParent/NativeReplicateChild/NativeReplicateGrandchild", DataPolicy.REPLICATE);
	}

	@Component
	public static final class TestComponent {

		@Resource(name = "NativePartitionedRegion")
		Region<?, ?> nativePartitionedRegion;

		@Resource(name = "NativeReplicateParent")
		Region<?, ?> nativeReplicateParent;

		@Resource(name = "/NativeReplicateParent/NativeReplicateChild")
		Region<?, ?> nativeReplicateChild;

		@Resource(name = "/NativeReplicateParent/NativeReplicateChild/NativeReplicateGrandchild")
		Region<?, ?> nativeReplicateGrandchild;

	}
}
