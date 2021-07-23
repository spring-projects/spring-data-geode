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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNotNull;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.Cache;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;

/**
 * Integration Tests testing SDG support of Apache Geode Auto-Reconnect functionality.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @since 1.5.0
 */
public class CacheAutoReconnectIntegrationTests extends IntegrationTestsSupport {

	private ConfigurableApplicationContext applicationContext;

	@After
	public void tearDown() {
		assumeNotNull(applicationContext);
		applicationContext.close();
	}

	protected Cache getCache(String configLocation) {

		String baseConfigLocation =
			File.separator.concat(getClass().getPackage().getName().replace('.', File.separatorChar));

		applicationContext = new ClassPathXmlApplicationContext(baseConfigLocation.concat(File.separator).concat(configLocation));

		return applicationContext.getBean(GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME, Cache.class);
	}

	@Test
	public void testAutoReconnectDisabled() {

		Cache cache = getCache("cacheAutoReconnectDisabledIntegrationTests.xml");

		assertThat(cache).isNotNull();
		assertThat(cache.getDistributedSystem()).isNotNull();
		assertThat(cache.getDistributedSystem().getProperties()).isNotNull();
		assertThat(Boolean.valueOf(cache.getDistributedSystem().getProperties().getProperty("disable-auto-reconnect")))
			.isTrue();
	}

	@Test
	public void testAutoReconnectEnabled() {

		Cache cache = getCache("cacheAutoReconnectEnabledIntegrationTests.xml");

		assertThat(cache).isNotNull();
		assertThat(cache.getDistributedSystem()).isNotNull();
		assertThat(cache.getDistributedSystem().getProperties()).isNotNull();
		assertThat(Boolean.valueOf(cache.getDistributedSystem().getProperties().getProperty("disable-auto-reconnect")))
			.isFalse();
	}
}
