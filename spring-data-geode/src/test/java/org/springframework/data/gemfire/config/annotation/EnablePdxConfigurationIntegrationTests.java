/*
 * Copyright 2018-2023 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.DiskStoreFactoryBean;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.mapping.MappingPdxSerializer;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;

/**
 * Integration Tests for {@link EnablePdx} and {@link PdxConfiguration}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.data.gemfire.config.annotation.EnablePdx
 * @see org.springframework.data.gemfire.config.annotation.PdxConfiguration
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @since 1.9.0
 */
public class EnablePdxConfigurationIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	@After
	public void cleanupAfterTests() {
		destroyAllGemFireMockObjects();
	}

	@Test
	public void regionBeanDefinitionDependsOnPdxDiskStoreBean() {

		newApplicationContext(TestEnablePdxWithDiskStoreConfiguration.class);

		assertThat(containsBean("gemfireCache")).isTrue();
		assertThat(containsBean("MockPdxSerializer")).isTrue();
		assertThat(containsBean("TestDiskStore")).isTrue();
		assertThat(containsBean("TestRegion")).isTrue();

		CacheFactoryBean gemfireCache = getBean("&gemfireCache", CacheFactoryBean.class);

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getPdxSerializer()).isEqualTo(getBean("MockPdxSerializer", PdxSerializer.class));

		BeanDefinition testDiskStoreBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("TestDiskStore");

		assertThat(testDiskStoreBeanDefinition).isNotNull();
		assertThat(testDiskStoreBeanDefinition.getDependsOn()).isNullOrEmpty();

		BeanDefinition testRegionBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("TestRegion");

		assertThat(testRegionBeanDefinition).isNotNull();
		assertThat(testRegionBeanDefinition.getDependsOn()).containsExactly("TestDiskStore");
	}

	@Test
	public void regionBeanDefinitionHasNoDependencies() {

		newApplicationContext(TestEnablePdxConfigurationWithNoDiskStoreConfiguration.class);

		assertThat(containsBean("gemfireCache")).isTrue();
		assertThat(containsBean("TestDiskStore")).isTrue();
		assertThat(containsBean("TestRegion")).isTrue();

		CacheFactoryBean gemfireCache = getBean("&gemfireCache", CacheFactoryBean.class);

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getPdxSerializer()).isInstanceOf(MappingPdxSerializer.class);

		BeanDefinition testDiskStoreBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("TestDiskStore");

		assertThat(testDiskStoreBeanDefinition).isNotNull();
		assertThat(testDiskStoreBeanDefinition.getDependsOn()).isNullOrEmpty();

		BeanDefinition testRegionBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("TestRegion");

		assertThat(testRegionBeanDefinition).isNotNull();
		assertThat(testRegionBeanDefinition.getDependsOn()).isNullOrEmpty();

	}

	@PeerCacheApplication
	@EnableGemFireMockObjects
	@EnablePdx(diskStoreName = "TestDiskStore", serializerBeanName = "MockPdxSerializer")
	@SuppressWarnings("unused")
	static class TestEnablePdxWithDiskStoreConfiguration {

		@Bean("TestDiskStore")
		DiskStoreFactoryBean testPdxDiskStore(GemFireCache gemfireCache) {

			DiskStoreFactoryBean testDiskStore = new DiskStoreFactoryBean();

			testDiskStore.setCache(gemfireCache);

			return testDiskStore;
		}

		@Bean("TestRegion")
		LocalRegionFactoryBean<Object, Object> testRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> testRegion = new LocalRegionFactoryBean<>();

			testRegion.setCache(gemfireCache);
			testRegion.setClose(false);
			testRegion.setPersistent(false);

			return testRegion;
		}

		@Bean("MockPdxSerializer")
		PdxSerializer mockPdxSerializer() {
			return mock(PdxSerializer.class);
		}
	}

	@ClientCacheApplication
	@EnableGemFireMockObjects
	@EnablePdx
	@SuppressWarnings("unused")
	static class TestEnablePdxConfigurationWithNoDiskStoreConfiguration {

		@Bean("TestDiskStore")
		DiskStoreFactoryBean testPdxDiskStore(GemFireCache gemfireCache) {

			DiskStoreFactoryBean testDiskStore = new DiskStoreFactoryBean();

			testDiskStore.setCache(gemfireCache);

			return testDiskStore;
		}

		@Bean("TestRegion")
		public ClientRegionFactoryBean<Object, Object> testRegion(GemFireCache gemfireCache) {

			ClientRegionFactoryBean<Object, Object> testRegion = new ClientRegionFactoryBean<>();

			testRegion.setCache(gemfireCache);
			testRegion.setClose(false);
			testRegion.setShortcut(ClientRegionShortcut.LOCAL);

			return testRegion;
		}
	}
}
