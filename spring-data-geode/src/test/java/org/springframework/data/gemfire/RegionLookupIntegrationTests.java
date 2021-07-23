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
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;

import org.junit.Test;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionExistsException;
import org.apache.geode.cache.Scope;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;

/**
 * Integration Tests testing SDG lookup functionality for various peer {@link Region} types.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @since 1.4.0
 * @link https://jira.spring.io/browse/SGF-204
 */
// TODO: slow test; can this test use mocks?
public class RegionLookupIntegrationTests extends IntegrationTestsSupport {

	private void assertNoRegionLookup(String configLocation) {

		ConfigurableApplicationContext applicationContext = null;

		try {
			applicationContext = createApplicationContext(configLocation);
			fail("Spring ApplicationContext should have thrown a BeanCreationException caused by a RegionExistsException!");
		}
		catch (BeanCreationException expected) {

			assertThat(expected.getCause() instanceof RegionExistsException).as(expected.getMessage()).isTrue();

			throw (RegionExistsException) expected.getCause();
		}
		finally {
			closeApplicationContext(applicationContext);
		}
	}

	private ConfigurableApplicationContext createApplicationContext(String configLocation) {
		return new ClassPathXmlApplicationContext(configLocation);
	}

	private void closeApplicationContext(ConfigurableApplicationContext applicationContext) {
		Optional.ofNullable(applicationContext).ifPresent(ConfigurableApplicationContext::close);
	}

	@Test
	public void testAllowRegionBeanDefinitionOverrides() {

		ConfigurableApplicationContext applicationContext = null;

		try {
			applicationContext = createApplicationContext(
				"/org/springframework/data/gemfire/allowRegionBeanDefinitionOverridesTest.xml");

			assertThat(applicationContext).isNotNull();
			assertThat(applicationContext.containsBean("regionOne")).isTrue();

			Region<?, ?> appDataRegion = applicationContext.getBean("regionOne", Region.class);

			assertThat(appDataRegion).isNotNull();
			assertThat(appDataRegion.getName()).isEqualTo("AppDataRegion");
			assertThat(appDataRegion.getFullPath()).isEqualTo("/AppDataRegion");
			assertThat(appDataRegion.getAttributes()).isNotNull();
			assertThat(appDataRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
			assertThat(appDataRegion.getAttributes().getMulticastEnabled()).isFalse();
			assertThat(appDataRegion.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_ACK);
			assertThat(appDataRegion.getAttributes().getInitialCapacity()).isEqualTo(101);
			assertThat(new Float(appDataRegion.getAttributes().getLoadFactor())).isEqualTo(new Float(0.85f));
			assertThat(appDataRegion.getAttributes().getCloningEnabled()).isTrue();
			assertThat(appDataRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
			assertThat(appDataRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
			assertThat(appDataRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);
		}
		finally {
			closeApplicationContext(applicationContext);
		}
	}

	@Test(expected = RegionExistsException.class)
	public void testNoDuplicateRegionDefinitions() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noDuplicateRegionDefinitionsTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoClientRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noClientRegionLookupTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoClientSubRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noClientSubRegionLookupTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoLocalRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noLocalRegionLookupTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoPartitionRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noPartitionRegionLookupTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoReplicateRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noReplicateRegionLookupTest.xml");
	}

	@Test(expected = RegionExistsException.class)
	public void testNoSubRegionLookups() {
		assertNoRegionLookup("/org/springframework/data/gemfire/noSubRegionLookupTest.xml");
	}

	@Test
	public void testEnableRegionLookups() {

		ConfigurableApplicationContext applicationContext = null;

		try {
			applicationContext = createApplicationContext("/org/springframework/data/gemfire/enableRegionLookupsTest.xml");

			assertThat(applicationContext).isNotNull();
			assertThat(applicationContext.containsBean("NativeLocalRegion")).isTrue();
			assertThat(applicationContext.containsBean("NativePartitionRegion")).isTrue();
			assertThat(applicationContext.containsBean("NativeReplicateRegion")).isTrue();
			assertThat(applicationContext.containsBean("NativeParentRegion")).isTrue();
			assertThat(applicationContext.containsBean("/NativeParentRegion/NativeChildRegion")).isTrue();
			assertThat(applicationContext.containsBean("SpringReplicateRegion")).isTrue();

			Region<?, ?> nativeLocalRegion = applicationContext.getBean("NativeLocalRegion", Region.class);

			assertThat(nativeLocalRegion).isNotNull();
			assertThat(nativeLocalRegion.getName()).isEqualTo("NativeLocalRegion");
			assertThat(nativeLocalRegion.getFullPath()).isEqualTo("/NativeLocalRegion");
			assertThat(nativeLocalRegion.getAttributes()).isNotNull();
			assertThat(nativeLocalRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
			assertThat(nativeLocalRegion.getAttributes().getCloningEnabled()).isFalse();
			assertThat(nativeLocalRegion.getAttributes().getConcurrencyChecksEnabled()).isFalse();
			assertThat(nativeLocalRegion.getAttributes().getConcurrencyLevel()).isEqualTo(80);
			assertThat(nativeLocalRegion.getAttributes().getInitialCapacity()).isEqualTo(101);
			assertThat(nativeLocalRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
			assertThat(new Float(nativeLocalRegion.getAttributes().getLoadFactor())).isEqualTo(new Float(0.95f));
			assertThat(nativeLocalRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);

			Region<?, ?> nativePartitionRegion = applicationContext.getBean("NativePartitionRegion", Region.class);

			assertThat(nativePartitionRegion).isNotNull();
			assertThat(nativePartitionRegion.getName()).isEqualTo("NativePartitionRegion");
			assertThat(nativePartitionRegion.getFullPath()).isEqualTo("/NativePartitionRegion");
			assertThat(nativePartitionRegion.getAttributes()).isNotNull();
			assertThat(nativePartitionRegion.getAttributes().getDataPolicy())
				.isEqualTo(DataPolicy.PERSISTENT_PARTITION);
			assertThat(nativePartitionRegion.getAttributes().getCloningEnabled()).isTrue();
			assertThat(nativePartitionRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
			assertThat(nativePartitionRegion.getAttributes().getConcurrencyLevel()).isEqualTo(40);
			assertThat(nativePartitionRegion.getAttributes().getInitialCapacity()).isEqualTo(51);
			assertThat(nativePartitionRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
			assertThat(new Float(nativePartitionRegion.getAttributes().getLoadFactor())).isEqualTo(new Float(0.85f));
			assertThat(nativePartitionRegion.getAttributes().getMulticastEnabled()).isFalse();
			assertThat(nativePartitionRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);

			Region<?, ?> nativeReplicateRegion = applicationContext.getBean("NativeReplicateRegion", Region.class);

			assertThat(nativeReplicateRegion).isNotNull();
			assertThat(nativeReplicateRegion.getName()).isEqualTo("NativeReplicateRegion");
			assertThat(nativeReplicateRegion.getFullPath()).isEqualTo("/NativeReplicateRegion");
			assertThat(nativeReplicateRegion.getAttributes()).isNotNull();
			assertThat(nativeReplicateRegion.getAttributes().getDataPolicy())
				.isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
			assertThat(nativeReplicateRegion.getAttributes().getCloningEnabled()).isFalse();
			assertThat(nativeReplicateRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
			assertThat(nativeReplicateRegion.getAttributes().getInitialCapacity()).isEqualTo(23);
			assertThat(new Float(nativeReplicateRegion.getAttributes().getLoadFactor())).isEqualTo(new Float(0.75f));
			assertThat(nativeReplicateRegion.getAttributes().getKeyConstraint()).isEqualTo(Integer.class);
			assertThat(nativeReplicateRegion.getAttributes().getMulticastEnabled()).isFalse();
			assertThat(nativeReplicateRegion.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
			assertThat(nativeReplicateRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);

			Region<?, ?> nativeChildRegion =
				applicationContext.getBean("/NativeParentRegion/NativeChildRegion", Region.class);

			assertThat(nativeChildRegion).isNotNull();
			assertThat(nativeChildRegion.getName()).isEqualTo("NativeChildRegion");
			assertThat(nativeChildRegion.getFullPath()).isEqualTo("/NativeParentRegion/NativeChildRegion");
			assertThat(nativeChildRegion.getAttributes()).isNotNull();
			assertThat(nativeChildRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);

			Region<?, ?> springReplicateRegion = applicationContext.getBean("SpringReplicateRegion", Region.class);

			assertThat(springReplicateRegion).isNotNull();
			assertThat(springReplicateRegion.getName()).isEqualTo("SpringReplicateRegion");
			assertThat(springReplicateRegion.getFullPath()).isEqualTo("/SpringReplicateRegion");
			assertThat(springReplicateRegion.getAttributes()).isNotNull();
			assertThat(springReplicateRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		}
		finally {
			closeApplicationContext(applicationContext);
		}
	}

	@Test
	public void testEnableClientRegionLookups() {

		ConfigurableApplicationContext applicationContext = null;

		try {

			applicationContext = createApplicationContext("/org/springframework/data/gemfire/enableClientRegionLookupsTest.xml");

			assertThat(applicationContext).isNotNull();
			assertThat(applicationContext.containsBean("NativeClientRegion")).isTrue();
			assertThat(applicationContext.containsBean("NativeClientParentRegion")).isTrue();
			assertThat(applicationContext.containsBean("/NativeClientParentRegion/NativeClientChildRegion")).isTrue();

			Region<?, ?> nativeClientRegion = applicationContext.getBean("NativeClientRegion", Region.class);

			assertThat(nativeClientRegion).isNotNull();
			assertThat(nativeClientRegion.getName()).isEqualTo("NativeClientRegion");
			assertThat(nativeClientRegion.getFullPath()).isEqualTo("/NativeClientRegion");
			assertThat(nativeClientRegion.getAttributes()).isNotNull();
			assertThat(nativeClientRegion.getAttributes().getCloningEnabled()).isFalse();
			assertThat(nativeClientRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);

			Region<?, ?> nativeClientChildRegion =
				applicationContext.getBean("/NativeClientParentRegion/NativeClientChildRegion", Region.class);

			assertThat(nativeClientChildRegion).isNotNull();
			assertThat(nativeClientChildRegion.getName()).isEqualTo("NativeClientChildRegion");
			assertThat(nativeClientChildRegion.getFullPath())
				.isEqualTo("/NativeClientParentRegion/NativeClientChildRegion");
			assertThat(nativeClientChildRegion.getAttributes()).isNotNull();
			assertThat(nativeClientChildRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		}
		finally {
			closeApplicationContext(applicationContext);
		}
	}
}
