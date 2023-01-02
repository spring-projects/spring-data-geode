/*
 * Copyright 2017-2023 the original author or authors.
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

import java.util.Properties;
import java.util.function.Function;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.control.ResourceManager;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.mock.env.MockPropertySource;

/**
 * Integration Tests for {@link PeerCacheApplication}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.springframework.core.env.MutablePropertySources
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheApplication
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @see org.springframework.mock.env.MockPropertySource
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class PeerCachePropertiesIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
		Class<?>... annotatedClasses) {

		Function<ConfigurableApplicationContext, ConfigurableApplicationContext> applicationContextInitializer = applicationContext -> {

			MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();

			propertySources.addFirst(testPropertySource);

			return applicationContext;
		};

		return newApplicationContext(applicationContextInitializer, annotatedClasses);
	}

	@Test
	public void peerCacheConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.cache.copy-on-read", true)
			.withProperty("spring.data.gemfire.cache.critical-heap-percentage", 95.0f)
			.withProperty("spring.data.gemfire.cache.critical-off-heap-percentage", 90.0f)
			.withProperty("spring.data.gemfire.cache.eviction-heap-percentage", 85.0f)
			.withProperty("spring.data.gemfire.cache.eviction-off-heap-percentage", 80.0f)
			.withProperty("spring.data.gemfire.cache.peer.lock-lease", 180)
			.withProperty("spring.data.gemfire.cache.peer.lock-timeout", 30)
			.withProperty("spring.data.gemfire.cache.peer.search-timeout", 120)
			.withProperty("spring.data.gemfire.pdx.ignore-unread-fields", false)
			.withProperty("spring.data.gemfire.pdx.persistent", true);

		newApplicationContext(testPropertySource, TestPeerCacheConfiguration.class);

		assertThat(containsBean("gemfireCache")).isTrue();
		assertThat(containsBean("mockPdxSerializer")).isTrue();

		Cache testPeerCache = getBean("gemfireCache", Cache.class);

		PdxSerializer mockPdxSerializer = getBean("mockPdxSerializer", PdxSerializer.class);

		assertThat(testPeerCache).isNotNull();
		assertThat(mockPdxSerializer).isNotNull();
		assertThat(testPeerCache.getLockLease()).isEqualTo(180);
		assertThat(testPeerCache.getLockTimeout()).isEqualTo(30);
		assertThat(testPeerCache.getPdxDiskStore()).isNull();
		assertThat(testPeerCache.getPdxIgnoreUnreadFields()).isFalse();
		assertThat(testPeerCache.getPdxPersistent()).isTrue();
		assertThat(testPeerCache.getPdxReadSerialized()).isTrue();
		assertThat(testPeerCache.getPdxSerializer()).isSameAs(mockPdxSerializer);
		assertThat(testPeerCache.getSearchTimeout()).isEqualTo(60);

		ResourceManager resourceManager = testPeerCache.getResourceManager();

		assertThat(resourceManager).isNotNull();
		assertThat(resourceManager.getCriticalHeapPercentage()).isEqualTo(95.0f);
		assertThat(resourceManager.getCriticalOffHeapPercentage()).isEqualTo(90.0f);
		assertThat(resourceManager.getEvictionHeapPercentage()).isEqualTo(85.0f);
		assertThat(resourceManager.getEvictionOffHeapPercentage()).isEqualTo(80.0f);
	}

	@Test
	public void dynamicPeerCacheConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.use-bean-factory-locator", true)
			.withProperty("spring.data.gemfire.cache.copy-on-read", true)
			.withProperty("spring.data.gemfire.cache.critical-heap-percentage", 95.0f)
			.withProperty("spring.data.gemfire.cache.eviction-heap-percentage", 80.0f)
			.withProperty("spring.data.gemfire.cache.log-level", "info")
			.withProperty("spring.data.gemfire.cache.name", "XYZ012")
			.withProperty("spring.data.gemfire.cache.peer.enable-auto-reconnect", true)
			.withProperty("spring.data.gemfire.cache.peer.locators", "skullbox[11235]")
			.withProperty("spring.data.gemfire.cache.peer.lock-lease", 180)
			.withProperty("spring.data.gemfire.cache.peer.lock-timeout", 30)
			.withProperty("spring.data.gemfire.cache.peer.message-sync-interval", 10)
			.withProperty("spring.data.gemfire.cache.peer.search-timeout", 120)
			.withProperty("spring.data.gemfire.cache.peer.use-cluster-configuration", true);

		newApplicationContext(testPropertySource, TestDynamicPeerCacheConfiguration.class);

		assertThat(containsBean("gemfireCache")).isTrue();

		CacheFactoryBean cacheFactoryBean = getBean("&gemfireCache", CacheFactoryBean.class);

		Cache cache = getBean("gemfireCache", Cache.class);

		assertThat(cacheFactoryBean).isNotNull();
		assertThat(cacheFactoryBean.isUseBeanFactoryLocator()).isTrue();
		assertThat(cache).isNotNull();
		assertThat(cache.getCopyOnRead()).isTrue();
		assertThat(cache.getDistributedSystem()).isNotNull();
		assertThat(cache.getLockLease()).isEqualTo(180);
		assertThat(cache.getLockTimeout()).isEqualTo(30);
		assertThat(cache.getMessageSyncInterval()).isEqualTo(10);
		assertThat(cache.getSearchTimeout()).isEqualTo(120);

		Properties gemfireProperties = cache.getDistributedSystem().getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties.getProperty("log-level")).isEqualTo("info");
		assertThat(gemfireProperties.getProperty("name")).isEqualTo("XYZ012");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("true");

		ResourceManager resourceManager = cache.getResourceManager();

		assertThat(resourceManager).isNotNull();
		assertThat(resourceManager.getCriticalHeapPercentage()).isEqualTo(95.0f);
		assertThat(resourceManager.getEvictionHeapPercentage()).isEqualTo(80.0f);
	}

	// TODO add more tests

	@EnableGemFireMockObjects
	@EnablePdx(ignoreUnreadFields = true, readSerialized = true, serializerBeanName = "mockPdxSerializer")
	@PeerCacheApplication(name = "TestPeerCache", criticalHeapPercentage = 90.0f, evictionHeapPercentage = 75.0f,
		lockLease = 300, lockTimeout = 120)
	static class TestPeerCacheConfiguration {

		@Bean
		PeerCacheConfigurer testPeerCacheConfigurer() {
			return (beanName, beanFactory) -> beanFactory.setSearchTimeout(60);
		}

		@Bean
		PdxSerializer mockPdxSerializer() {
			return mock(PdxSerializer.class);
		}
	}

	@EnableGemFireMockObjects
	@PeerCacheApplication(name = "TestPeerCache")
	static class TestDynamicPeerCacheConfiguration { }

}
