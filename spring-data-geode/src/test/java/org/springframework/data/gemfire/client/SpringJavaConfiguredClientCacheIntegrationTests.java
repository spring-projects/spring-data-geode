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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for the proper configuration of a {@link ClientCache} instance
 * using Spring Java-based configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @link https://jira.spring.io/browse/SGF-441
 * @since 1.8.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SpringJavaConfiguredClientCacheIntegrationTests.TestConfiguration.class)
@SuppressWarnings("unused")
public class SpringJavaConfiguredClientCacheIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "&clientCache")
	private ClientCacheFactoryBean clientCacheFactoryBean;

	@Autowired
	private Properties gemfireProperties;

	@Test
	public void clientCacheFactoryBeanConfiguration() {

		assertThat(clientCacheFactoryBean).isNotNull();
		assertThat(clientCacheFactoryBean.getBeanName()).isEqualTo("clientCache");
		assertThat(clientCacheFactoryBean.getProperties()).isEqualTo(gemfireProperties);
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		Properties gemfireProperties() {

			Properties gemfireProperties = new Properties();

			gemfireProperties.setProperty("name", SpringJavaConfiguredClientCacheIntegrationTests.class.getSimpleName());

			return gemfireProperties;
		}

		@Bean
		ClientCacheFactoryBean clientCache() {

			ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

			clientCacheFactoryBean.setUseBeanFactoryLocator(false);
			clientCacheFactoryBean.setProperties(gemfireProperties());

			return clientCacheFactoryBean;
		}
	}
}
