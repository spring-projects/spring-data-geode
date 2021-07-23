/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for JIRA issue SGF-195,concerning collocated cache {@link Region Regions}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @link https://jira.springsource.org/browse/SGF-195
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(value = "colocated-region.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class CollocatedRegionIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "colocatedRegion")
	private Region<?, ?> colocatedRegion;

	@Resource(name = "sourceRegion")
	private Region<?, ?> sourceRegion;

	protected static void assertRegionExists(String expectedRegionName, Region<?, ?> region) {

		assertThat(region).isNotNull();

		assertThat(region.getName())
			.describedAs("Expected Region with name %1$s; but was %2$s!", expectedRegionName, region.getName())
			.isEqualTo(expectedRegionName);
	}

	@Test
	public void testRegionsColocated() {

		assertRegionExists("Source", sourceRegion);
		assertRegionExists("Colocated", colocatedRegion);
		assertThat(colocatedRegion.getAttributes()).isNotNull();
		assertThat(colocatedRegion.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(colocatedRegion.getAttributes().getPartitionAttributes().getColocatedWith()).isEqualTo(sourceRegion.getName());
	}
}
