/*
 * Copyright 2018-2021 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.eviction.EvictionActionType;
import org.springframework.data.gemfire.eviction.EvictionPolicyType;
import org.springframework.data.gemfire.test.model.Person;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.stereotype.Service;

/**
 * Integration Tests for {@link EnableEviction} and {@link EvictionConfiguration}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.config.annotation.EnableEviction
 * @see org.springframework.data.gemfire.config.annotation.EvictionConfiguration
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @since 1.9.0
 */
@SuppressWarnings("unused")
public class EnableEvictionConfigurationIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	private static final String GEMFIRE_LOG_LEVEL = "error";

	@After
	public void tearDown() {
		destroyAllGemFireMockObjects();
	}

	@SuppressWarnings("unchecked")
	private <K, V> Region<K, V> getRegion(ConfigurableApplicationContext applicationContext, String beanName) {
		return applicationContext.getBean(beanName, Region.class);
	}

	private void assertRegionEvictionConfiguration(ConfigurableApplicationContext applicationContext,
			String regionBeanName, EvictionActionType expectedEvictionActionType, int expectedEvictionMaximum) {

		Region<?, ?> region = getRegion(applicationContext, regionBeanName);

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(regionBeanName);
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(region.getAttributes().getEvictionAttributes()).isNotNull();

		assertThat(region.getAttributes().getEvictionAttributes().getAction())
			.isEqualTo(expectedEvictionActionType.getEvictionAction());

		assertThat(region.getAttributes().getEvictionAttributes().getAlgorithm())
			.isEqualTo(EvictionPolicyType.ENTRY_COUNT.getEvictionAlgorithm());

		assertThat(region.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(expectedEvictionMaximum);
	}

	@Test
	public void assertApplicationCachingDefinedRegionsEvictionPolicyIsCorrect() {

		ConfigurableApplicationContext applicationContext = newApplicationContext(ApplicationConfiguration.class);

		assertRegionEvictionConfiguration(applicationContext, "CacheOne",
			EvictionActionType.LOCAL_DESTROY, 100);

		assertRegionEvictionConfiguration(applicationContext, "CacheTwo",
			EvictionActionType.LOCAL_DESTROY, 100);
	}

	@Test
	public void assertClientCacheRegionEvictionPolicyIsCorrect() {
		assertRegionEvictionConfiguration(newApplicationContext(ClientCacheRegionEvictionConfiguration.class),
			"People", EvictionActionType.LOCAL_DESTROY, 100);
	}

	@Test
	public void assertPeerCacheRegionEvictionPolicyIsCorrect() {
		assertRegionEvictionConfiguration(newApplicationContext(PeerCacheRegionEvictionConfiguration.class),
			"People", EvictionActionType.OVERFLOW_TO_DISK, 10000);
	}

	@ClientCacheApplication(name = "EnableEvictionConfigurationIntegrationTests")
	@EnableCachingDefinedRegions(clientRegionShortcut = ClientRegionShortcut.LOCAL)
	@EnableEviction(policies = @EnableEviction.EvictionPolicy(maximum = 100))
	@EnableGemFireMockObjects
	static class ApplicationConfiguration {

		@Bean
		ApplicationService applicationService() {
			return new ApplicationService();
		}
	}

	@Service
	static class ApplicationService {

		@Cacheable("CacheOne")
		public Object someMethod(Object key) {
			return null;
		}

		@Cacheable("CacheTwo")
		public Object someOtherMethod(Object key) {
			return null;
		}
	}

	@ClientCacheApplication(name = "EnableEvictionConfigurationIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
	@EnableEntityDefinedRegions(basePackageClasses = Person.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
	@EnableEviction(policies = @EnableEviction.EvictionPolicy(regionNames = "People", maximum = 100))
	@EnableGemFireMockObjects
	static class ClientCacheRegionEvictionConfiguration { }

	@PeerCacheApplication(name = "EnableEvictionConfigurationIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
	@EnableEntityDefinedRegions(basePackageClasses = Person.class, serverRegionShortcut = RegionShortcut.LOCAL)
	@EnableEviction(policies = @EnableEviction.EvictionPolicy(regionNames = "People",
		action = EvictionActionType.OVERFLOW_TO_DISK, maximum = 10000))
	@EnableGemFireMockObjects
	static class PeerCacheRegionEvictionConfiguration { }

}
