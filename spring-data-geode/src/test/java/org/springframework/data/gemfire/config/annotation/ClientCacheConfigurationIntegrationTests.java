/*
 * Copyright 2020-2022 the original author or authors.
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

import org.junit.Test;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.SocketFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;

/**
 * Integration Tests for {@link ClientCacheConfiguration}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfiguration
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @since 2.4.0
 */
@SuppressWarnings("unused")
public class ClientCacheConfigurationIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	@Test
	public void clientCacheDefaultPoolWithCustomSocketFactory() {

		newApplicationContext(ClientCacheDefaultPoolWithCustomSocketFactoryConfiguration.class);

		ClientCache clientCache = getBean(ClientCache.class);

		assertThat(clientCache).isNotNull();

		Pool defaultPool = clientCache.getDefaultPool();

		SocketFactory mockSocketFactory = getBean("mockSocketFactory", SocketFactory.class);

		assertThat(defaultPool).isNotNull();
		assertThat(defaultPool.getName()).isEqualTo("DEFAULT");
		assertThat(defaultPool.getServerConnectionTimeout()).isEqualTo(60000);
		assertThat(mockSocketFactory).isNotNull();
		assertThat(defaultPool.getSocketFactory()).isEqualTo(mockSocketFactory);
	}

	@Test
	public void clientCacheDefaultPoolWithDefaultSocketFactory() {

		newApplicationContext(ClientCacheDefaultPoolWithDefaultSocketFactoryConfiguration.class);

		ClientCache clientCache = getBean(ClientCache.class);

		assertThat(clientCache).isNotNull();

		Pool defaultPool = clientCache.getDefaultPool();

		assertThat(defaultPool).isNotNull();
		assertThat(defaultPool.getName()).isEqualTo("DEFAULT");
		assertThat(defaultPool.getSocketFactory()).isEqualTo(SocketFactory.DEFAULT);
	}

	@EnableGemFireMockObjects
	@ClientCacheApplication(
		name = "ClientCacheDefaultPoolWithCustomSocketFactoryConfiguration",
		serverConnectionTimeout = 60000,
		socketFactoryBeanName = "mockSocketFactory"
	)
	static class ClientCacheDefaultPoolWithCustomSocketFactoryConfiguration {

		@Bean
		SocketFactory mockSocketFactory() {
			return mock(SocketFactory.class);
		}
	}

	@EnableGemFireMockObjects
	@ClientCacheApplication(name = "ClientCacheDefaultPoolWithDefaultSocketFactoryConfiguration")
	static class ClientCacheDefaultPoolWithDefaultSocketFactoryConfiguration {

		@Bean
		javax.net.SocketFactory mockSocketFactory() {
			return mock(javax.net.SocketFactory.class);
		}
	}
}
