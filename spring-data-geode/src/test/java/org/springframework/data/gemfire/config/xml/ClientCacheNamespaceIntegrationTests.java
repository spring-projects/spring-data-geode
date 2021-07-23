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

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link ClientCacheParser}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.config.xml.ClientCacheParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.6.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ClientCacheNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier("&client-cache-with-no-name")
	private ClientCacheFactoryBean clientCacheFactoryBean;

	@Autowired
	private PdxSerializer reflectionBaseAutoSerializer;

	@Autowired
	private Properties gemfireProperties;

	@Test
	public void clientCacheFactoryBeanConfiguration() throws Exception {

		assertThat(clientCacheFactoryBean.getCacheXml().toString()).contains("empty-client-cache.xml");
		assertThat(clientCacheFactoryBean.getProperties()).isEqualTo(gemfireProperties);
		assertThat(clientCacheFactoryBean.getCopyOnRead()).isTrue();
		assertThat(clientCacheFactoryBean.getCriticalHeapPercentage()).isEqualTo(0.85f);
		assertThat(clientCacheFactoryBean.getDurableClientId()).isEqualTo("TestDurableClientId");
		assertThat(clientCacheFactoryBean.getDurableClientTimeout()).isEqualTo(600);
		assertThat(clientCacheFactoryBean.getEvictionHeapPercentage()).isEqualTo(0.65f);
		assertThat(clientCacheFactoryBean.isKeepAlive()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxIgnoreUnreadFields()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxPersistent()).isFalse();
		assertThat(clientCacheFactoryBean.getPdxReadSerialized()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxSerializer()).isEqualTo(reflectionBaseAutoSerializer);
		assertThat(TestUtils.<String>readField("poolName", clientCacheFactoryBean)).isEqualTo("serverPool");
		assertThat(clientCacheFactoryBean.getReadyForEvents()).isFalse();
	}

	@Test
	public void namedClientCacheWithNoPropertiesAndNoCacheXml() {

		assertThat(applicationContext.containsBean("client-cache-with-name")).isTrue();

		ClientCacheFactoryBean clientCacheFactoryBean =
			applicationContext.getBean("&client-cache-with-name", ClientCacheFactoryBean.class);

		assertThat(clientCacheFactoryBean.getCacheXml()).isNull();
		assertThat(clientCacheFactoryBean.getProperties()).isNull();
	}

	@Test
	public void clientCacheWithXmlNoProperties() {

		assertThat(applicationContext.containsBean("client-cache-with-xml")).isTrue();

		ClientCacheFactoryBean clientCacheFactoryBean =
			applicationContext.getBean("&client-cache-with-xml", ClientCacheFactoryBean.class);

		Resource cacheXmlResource = clientCacheFactoryBean.getCacheXml();

		assertThat(cacheXmlResource.getFilename()).isEqualTo("gemfire-client-cache.xml");

		assertThat(clientCacheFactoryBean.getProperties()).isNull();
	}
}
