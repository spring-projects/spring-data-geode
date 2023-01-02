/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.io.InputStream;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests trying various basic configurations of Apache Geode caches with Spring.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Cache
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "basic-cache.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
// TODO: What is the purpose of this test class?
//  An Apache Geode cache instance is a Singleton!
public class CacheIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	private Cache cache;

	@After
	public void tearDown() {
		GemfireUtils.close(this.cache);
	}

	@Test
	public void testBasicCache() {

		this.cache = this.applicationContext.getBean("default-cache", Cache.class);

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("default-cache");
	}

	@Test
	public void testCacheWithProps() {

		this.cache = this.applicationContext.getBean("cache-with-props", Cache.class);

		// the name property seems to be ignored
		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("cache-with-props");
	}

	@Test
	public void testCacheWithXml() {

		this.cache = this.applicationContext.getBean("cache-with-xml", Cache.class);

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("cache-with-xml");
	}

	@Test
	public void testNamedCache() {

		this.cache = this.applicationContext.getBean("named-cache", Cache.class);

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("named-cache");
	}

	@Test
	public void testPdxCache() {

		this.cache = this.applicationContext.getBean("pdx-cache", Cache.class);

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("pdx-cache");
	}

	public static final class CacheWithXmlFactoryBeanPostProcessor implements BeanPostProcessor {

		@Nullable @Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

			if (bean instanceof CacheFactoryBean && "cache-with-xml".equals(beanName)) {

				CacheFactoryBean cacheBean = spy((CacheFactoryBean) bean);

				doAnswer(invocation -> {

					GemFireCache cache = invocation.getArgument(0);

					doNothing().when(cache).loadCacheXml(any(InputStream.class));

					return cache;

				}).when(cacheBean).loadCacheXml(any(GemFireCache.class));

				bean = cacheBean;
			}

			return bean;
		}
	}
}
