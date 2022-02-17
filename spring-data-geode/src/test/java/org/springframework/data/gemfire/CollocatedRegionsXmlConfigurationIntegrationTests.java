/*
 * Copyright 2010-2022 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for Apache Geode {@literal collocated} {@link DataPolicy#PARTITION} {@link Region Regions}
 * using SDG XML configuration.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see <a href="https://jira.springsource.org/browse/SGF-195">SGF-195</a>
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class CollocatedRegionsXmlConfigurationIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("collocatedRegion")
	private Region<?, ?> colocatedRegion;

	@Autowired
	@Qualifier("sourceRegion")
	private Region<?, ?> sourceRegion;

	private void assertRegionExists(Region<?, ?> region, String expectedRegionName) {

		assertThat(region).isNotNull();

		assertThat(region.getName())
			.describedAs("Expected Region with name [%1$s]; but was [%2$s]!", expectedRegionName, region.getName())
			.isEqualTo(expectedRegionName);
	}

	@Test
	public void collocatedRegionsAreConfiguredCorrectly() {

		assertRegionExists(this.sourceRegion, "Source");
		assertRegionExists(this.colocatedRegion, "Collocated");
		assertThat(this.colocatedRegion.getAttributes()).isNotNull();
		assertThat(this.colocatedRegion.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(this.colocatedRegion.getAttributes().getPartitionAttributes().getColocatedWith())
			.isEqualTo(this.sourceRegion.getName());
	}
}
