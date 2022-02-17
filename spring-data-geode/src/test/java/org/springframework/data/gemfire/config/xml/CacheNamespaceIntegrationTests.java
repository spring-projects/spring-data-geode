/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.springframework.data.gemfire.support.GemfireBeanFactoryLocator.newBeanFactoryLocator;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.util.GatewayConflictHelper;
import org.apache.geode.cache.util.GatewayConflictResolver;
import org.apache.geode.cache.util.TimestampedEntryEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link CacheParser} and {@link CacheFactoryBean}.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.config.xml.CacheParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
public class CacheNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@SuppressWarnings("unused")
	private ApplicationContext applicationContext;

	@Before
	public void setup() {
		assertThat(this.applicationContext.getBean("gemfireCache"))
			.isNotEqualTo(this.applicationContext.getBean("cache-with-name"));
	}

	@Test
	public void noNamedCacheConfigurationIsCorrect() {

		assertThat(applicationContext.containsBean("gemfireCache")).isTrue();
		assertThat(applicationContext.containsBean("gemfire-cache")).isTrue();

		CacheFactoryBean cacheFactoryBean = applicationContext.getBean("&gemfireCache", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getBeanName()).isEqualTo("gemfireCache");
		assertThat(cacheFactoryBean.getCacheXml()).isNull();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isFalse();
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireProperties.getProperty("disable-auto-reconnect"))).isTrue();
		assertThat(cacheFactoryBean.getUseClusterConfiguration()).isFalse();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireProperties.getProperty("use-cluster-configuration"))).isFalse();

		Cache gemfireCache = applicationContext.getBean("gemfireCache", Cache.class);

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getDistributedSystem()).isNotNull();
		assertThat(gemfireCache.getDistributedSystem().getProperties()).isNotNull();
		assertThat(gemfireCache.getDistributedSystem().getProperties().containsKey("disable-auto-reconnect")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("disable-auto-reconnect"))).isTrue();
		assertThat(gemfireCache.getDistributedSystem().getProperties().containsKey("use-cluster-configuration")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("use-cluster-configuration"))).isFalse();
	}

	@Test
	public void namedCacheConfigurationIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-name")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-name", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getBeanName()).isEqualTo("cache-with-name");
		assertThat(cacheFactoryBean.getCacheXml()).isNull();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isFalse();
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireProperties.getProperty("disable-auto-reconnect"))).isTrue();
		assertThat(cacheFactoryBean.getUseClusterConfiguration()).isFalse();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(Boolean.parseBoolean(gemfireProperties.getProperty("use-cluster-configuration"))).isFalse();

		Cache gemfireCache = applicationContext.getBean("cache-with-name", Cache.class);

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getDistributedSystem()).isNotNull();

		Properties distributedSystemProperties = gemfireCache.getDistributedSystem().getProperties();

		assertThat(distributedSystemProperties).isNotNull();
		assertThat(Boolean.parseBoolean(distributedSystemProperties.getProperty("disable-auto-reconnect"))).isTrue();
		assertThat(Boolean.parseBoolean(distributedSystemProperties.getProperty("use-cluster-configuration"))).isFalse();
	}

	@Test
	public void cacheWithAutoReconnectDisabledIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-auto-reconnect-disabled")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-auto-reconnect-disabled", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isFalse();

		Cache gemfireCache = applicationContext.getBean("cache-with-auto-reconnect-disabled", Cache.class);

		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("disable-auto-reconnect"))).isTrue();
	}

	@Test
	public void cacheWithAutoReconnectEnabledIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-auto-reconnect-enabled")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-auto-reconnect-enabled", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isTrue();

		Cache gemfireCache = applicationContext.getBean("cache-with-auto-reconnect-enabled", Cache.class);

		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("disable-auto-reconnect"))).isFalse();
	}

	@Test
	public void cacheWithGatewayConflictResolverIsCorrect() {

		Cache cache = applicationContext.getBean("cache-with-gateway-conflict-resolver", Cache.class);

		assertThat(cache.getGatewayConflictResolver()).isInstanceOf(TestGatewayConflictResolver.class);
	}

	@Test(expected = IllegalStateException.class)
	public void cacheWithNoBeanFactoryLocatorIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-no-bean-factory-locator")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-no-bean-factory-locator", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getBeanFactoryLocator()).isNull();

		newBeanFactoryLocator().useBeanFactory("cache-with-no-bean-factory-locator");
	}

	@Test
	public void cacheWithUseClusterConfigurationDisabledIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-use-cluster-configuration-disabled")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-use-cluster-configuration-disabled", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isFalse();

		Cache gemfireCache =
			applicationContext.getBean("cache-with-use-cluster-configuration-disabled", Cache.class);

		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("use-cluster-configuration"))).isFalse();
	}

	@Test
	public void cacheWithUseClusterConfigurationEnabledIsCorrect() {

		assertThat(applicationContext.containsBean("cache-with-use-cluster-configuration-enabled")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-use-cluster-configuration-enabled", CacheFactoryBean.class);

		assertThat(cacheFactoryBean.getUseClusterConfiguration()).isTrue();

		Cache gemfireCache =
			applicationContext.getBean("cache-with-use-cluster-configuration-enabled", Cache.class);

		assertThat(Boolean.parseBoolean(gemfireCache.getDistributedSystem().getProperties()
			.getProperty("use-cluster-configuration"))).isTrue();
	}

	@Test
	public void cacheWithXmlAndPropertiesConfigurationIsCorrect() throws Exception {

		assertThat(applicationContext.containsBean("cache-with-xml-and-props")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&cache-with-xml-and-props", CacheFactoryBean.class);

		Resource cacheXmlResource = cacheFactoryBean.getCacheXml();

		assertThat(cacheXmlResource.getFilename()).isEqualTo("gemfire-cache.xml");
		assertThat(applicationContext.containsBean("gemfireProperties")).isTrue();
		assertThat(TestUtils.<Properties>readField("properties", cacheFactoryBean))
			.isEqualTo(applicationContext.getBean("gemfireProperties"));
		assertThat(TestUtils.<Boolean>readField("pdxReadSerialized", cacheFactoryBean)).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.<Boolean>readField("pdxIgnoreUnreadFields", cacheFactoryBean)).isEqualTo(Boolean.FALSE);
		assertThat(TestUtils.<Boolean>readField("pdxPersistent", cacheFactoryBean)).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void heapTunedCacheIsCorrect() {

		assertThat(applicationContext.containsBean("heap-tuned-cache")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&heap-tuned-cache", CacheFactoryBean.class);

		Float criticalHeapPercentage = cacheFactoryBean.getCriticalHeapPercentage();
		Float evictionHeapPercentage = cacheFactoryBean.getEvictionHeapPercentage();

		assertThat(criticalHeapPercentage).isCloseTo(70.0f, offset(0.0001f));
		assertThat(evictionHeapPercentage).isCloseTo(60.0f, offset(0.0001f));
	}

	@Test
	public void offHeapTunedCacheIsCorrect() {

		assertThat(applicationContext.containsBean("off-heap-tuned-cache")).isTrue();

		CacheFactoryBean cacheFactoryBean =
			applicationContext.getBean("&off-heap-tuned-cache", CacheFactoryBean.class);

		Float criticalOffHeapPercentage = cacheFactoryBean.getCriticalOffHeapPercentage();
		Float evictionOffHeapPercentage = cacheFactoryBean.getEvictionOffHeapPercentage();

		assertThat(criticalOffHeapPercentage).isCloseTo(90.0f, offset(0.0001f));
		assertThat(evictionOffHeapPercentage).isCloseTo(50.0f, offset(0.0001f));
	}

	public static class TestGatewayConflictResolver implements GatewayConflictResolver {

		@Override
		public void onEvent(TimestampedEntryEvent event, GatewayConflictHelper helper) {
			throw new UnsupportedOperationException("Not Implemented!");
		}
	}
}
