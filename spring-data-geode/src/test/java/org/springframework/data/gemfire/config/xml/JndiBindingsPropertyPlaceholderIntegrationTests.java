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
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.internal.datasource.ConfigProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the configuration of a cache JNDI DataSource using property placeholders.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.4.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "jndi-binding-with-property-placeholders-ns.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
public class JndiBindingsPropertyPlaceholderIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@SuppressWarnings("unused")
	private ApplicationContext applicationContext;

	protected void assertPropertyValueExists(String expectedPropertyName, String expectedPropertyValue,
			List<ConfigProperty> properties) {

		for (ConfigProperty property : properties) {
			if (expectedPropertyName.equals(property.getName())) {
				assertThat(property.getValue()).isEqualTo(expectedPropertyValue);
				assertThat(property.getType()).isEqualTo(String.class.getName());
				return;
			}
		}

		fail("ConfigProperty with name [%1$s] was not found!", expectedPropertyName);
	}

	@Test
	public void testCacheJndiDataSourceConfiguration() {

		CacheFactoryBean factory = applicationContext.getBean("&gemfireCache", CacheFactoryBean.class);

		List<CacheFactoryBean.JndiDataSource> jndiDataSources = factory.getJndiDataSources();

		assertThat(jndiDataSources).isNotNull();
		assertThat(jndiDataSources.size()).isEqualTo(1);

		CacheFactoryBean.JndiDataSource dataSource = jndiDataSources.get(0);

		assertThat(dataSource).isNotNull();

		Map<String, String> attributes = dataSource.getAttributes();

		assertThat(attributes).isNotNull();
		assertThat(attributes.isEmpty()).isFalse();
		assertThat(attributes.get("jndi-name")).isEqualTo("testDataSource");
		assertThat(attributes.get("type")).isEqualTo("XAPooledDataSource");
		assertThat(attributes.get("blocking-timeout-seconds")).isEqualTo("60");
		assertThat(attributes.get("conn-pooled-datasource-class"))
			.isEqualTo("org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource");
		assertThat(attributes.get("connection-url")).isEqualTo("jdbc:derby:testDataStore;create=true");
		assertThat(attributes.get("idle-timeout-seconds")).isEqualTo("180");
		assertThat(attributes.get("init-pool-size")).isEqualTo("10");
		assertThat(attributes.get("jdbc-driver-class")).isEqualTo("org.apache.derby.jdbc.EmbeddedDriver");
		assertThat(attributes.get("login-timeout-seconds")).isEqualTo("30");
		assertThat(attributes.get("managed-connection-factory-class"))
			.isEqualTo("org.apache.derby.jdbc.NonExistingManagedConnectionFactoryClass");
		assertThat(attributes.get("max-pool-size")).isEqualTo("50");
		assertThat(attributes.get("password")).isEqualTo("test123");
		assertThat(attributes.get("transaction-type")).isEqualTo("XATransaction");
		assertThat(attributes.get("user-name")).isEqualTo("masterdba");
		assertThat(attributes.get("xa-datasource-class")).isEqualTo("org.apache.derby.jdbc.EmbeddedXADataSource");

		List<ConfigProperty> props = dataSource.getProps();

		assertThat(props).isNotNull();
		assertThat(props.isEmpty()).isFalse();
		assertPropertyValueExists("schemaName", "testSchema", props);
		assertPropertyValueExists("databaseName", "testDataStore", props);
		assertPropertyValueExists("description", "test", props);
		assertPropertyValueExists("email", "masterdba@xcompany.com", props);
		assertPropertyValueExists("phone", "501-555-1234", props);
	}
}
