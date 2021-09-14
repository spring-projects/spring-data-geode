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

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the Eviction Policy Actions on Local {@link Region Regions} defined in
 * SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.5.0.M1
 * @link https://jira.spring.io/browse/SGF-295
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class LocalRegionWithEvictionPolicyActionNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "LocalDestroy")
	private Region<?, ?> localDestroyRegion;

	@Resource(name = "None")
	private Region<?, ?> noneRegion;

	@Resource(name = "Overflow")
	private Region<?, ?> overflowRegion;

	@Test
	public void testLocalRegionConfigurationWithEvictionPolicyActionSetToLocalDestroy() {

		assertThat(localDestroyRegion).as("The 'LocalDestroy' Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(localDestroyRegion.getName()).isEqualTo("LocalDestroy");
		assertThat(localDestroyRegion.getFullPath()).isEqualTo("/LocalDestroy");
		assertThat(localDestroyRegion.getAttributes()).isNotNull();
		assertThat(localDestroyRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(localDestroyRegion.getAttributes().getScope()).isEqualTo(Scope.LOCAL);
		assertThat(localDestroyRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(localDestroyRegion.getAttributes().getEvictionAttributes().getAction())
			.isEqualTo(EvictionAction.LOCAL_DESTROY);
	}

	@Test
	public void testLocalRegionConfigurationWithEvictionPolicyActionSetToNone() {

		assertThat(noneRegion).as("The 'None' Region was not properly configured and initialized!").isNotNull();
		assertThat(noneRegion.getName()).isEqualTo("None");
		assertThat(noneRegion.getFullPath()).isEqualTo("/None");
		assertThat(noneRegion.getAttributes()).isNotNull();
		assertThat(noneRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(noneRegion.getAttributes().getScope()).isEqualTo(Scope.LOCAL);
		assertThat(noneRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(noneRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.NONE);
	}

	@Test
	public void testLocalRegionConfigurationWithEvictionPolicyActionSetToOverflowToDisk() {

		assertThat(overflowRegion).as("The 'Overflow' Region was not properly configured and initialized!").isNotNull();
		assertThat(overflowRegion.getName()).isEqualTo("Overflow");
		assertThat(overflowRegion.getFullPath()).isEqualTo("/Overflow");
		assertThat(overflowRegion.getAttributes()).isNotNull();
		assertThat(overflowRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(overflowRegion.getAttributes().getScope()).isEqualTo(Scope.LOCAL);
		assertThat(overflowRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(overflowRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
	}
}
