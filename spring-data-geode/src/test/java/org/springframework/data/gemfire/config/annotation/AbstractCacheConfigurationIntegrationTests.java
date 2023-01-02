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

import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.mock.env.MockPropertySource;

/**
 * Integration Tests for {@link AbstractCacheConfiguration}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.data.gemfire.config.annotation.AbstractCacheConfiguration
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @since 2.0.2
 */
public class AbstractCacheConfigurationIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	private void assertName(GemFireCache gemfireCache, String name) {

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getDistributedSystem()).isNotNull();
		assertThat(gemfireCache.getDistributedSystem().getProperties()).isNotNull();
		assertThat(gemfireCache.getDistributedSystem().getProperties().getProperty("name")).isEqualTo(name);
	}

	@Override
	protected ConfigurableApplicationContext newApplicationContext(Class<?>... annotatedClasses) {
		return newApplicationContext((PropertySource<?>) null, annotatedClasses);
	}

	private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
			Class<?>... annotatedClasses) {

		Function<ConfigurableApplicationContext, ConfigurableApplicationContext> applicationContextInitializer =
			testPropertySource != null ? applicationContext -> {
				Optional.ofNullable(testPropertySource).ifPresent(it -> {

					MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();

					propertySources.addFirst(testPropertySource);
				});

				return applicationContext;
			}
			: Function.identity();

		return newApplicationContext(applicationContextInitializer, annotatedClasses);
	}

	@Test
	public void clientCacheNameUsesAnnotationNameAttributeDefaultValue() {

		newApplicationContext(TestClientCacheConfiguration.class);

		GemFireCache peerCache = getBean("gemfireCache", ClientCache.class);

		assertName(peerCache, ClientCacheConfiguration.DEFAULT_NAME);
	}

	@Test
	public void clientCacheNameUsesSpringDataGemFireNameProperty() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.name", "TestClient");

		newApplicationContext(testPropertySource, TestClientCacheConfiguration.class);

		GemFireCache peerCache = getBean("gemfireCache", ClientCache.class);

		assertName(peerCache, "TestClient");
	}

	@Test
	public void peerCacheNameUsesAnnotationNameAttributeConfiguredValue() {

		newApplicationContext(TestPeerCacheConfiguration.class);

		GemFireCache peerCache = getBean("gemfireCache", Cache.class);

		assertName(peerCache, "TestPeerCacheApp");
	}

	@Test
	public void peerCacheNameUsesSpringDataGemFireCacheNameProperty() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.cache.name", "TestPeer");

		newApplicationContext(testPropertySource, TestPeerCacheConfiguration.class);

		GemFireCache peerCache = getBean("gemfireCache", Cache.class);

		assertName(peerCache, "TestPeer");
	}

	@ClientCacheApplication
	@EnableGemFireMockObjects
	static class TestClientCacheConfiguration { }

	@EnableGemFireMockObjects
	@PeerCacheApplication(name = "TestPeerCacheApp")
	static class TestPeerCacheConfiguration { }

}
