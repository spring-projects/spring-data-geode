/*
 * Copyright 2018-2022 the original author or authors.
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

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.distributed.Locator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.GemFireProperties;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link LocatorApplication} and {@link LocatorApplicationConfiguration} asserting that
 * an Apache Geode / Pivotal GemFire {@link Cache} application can connect to the {@link Locator} configured
 * and bootstrapped by {@link LocatorApplication}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.apache.geode.distributed.Locator
 * @see org.springframework.data.gemfire.config.annotation.LocatorApplication
 * @see org.springframework.data.gemfire.config.annotation.LocatorApplicationConfiguration
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.2.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class LocatorApplicationIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private Locator locator;

	@Before
	public void setup() {
		assertThat(this.locator).isNotNull();
	}

	@Test
	public void gemfireCacheCanConnectToLocator() {

		DistributedSystem distributedSystem = this.locator.getDistributedSystem();

		assertThat(distributedSystem).isNotNull();

		Properties distributedSystemProperties = distributedSystem.getProperties();

		assertThat(distributedSystemProperties).isNotNull();

		Cache peerCache = null;

		assertThat(distributedSystemProperties.getProperty("locators"))
			.describedAs("Locators was [%s]", distributedSystemProperties.getProperty("locators"))
			.isNotEmpty();

		try {
			peerCache = new CacheFactory()
				.set(GemFireProperties.NAME.getName(), LocatorApplicationIntegrationTests.class.getSimpleName())
				.set(GemFireProperties.BIND_ADDRESS.getName(), distributedSystemProperties.getProperty(GemFireProperties.BIND_ADDRESS.getName()))
				.set(GemFireProperties.CACHE_XML_FILE.getName(), distributedSystemProperties.getProperty(GemFireProperties.CACHE_XML_FILE.getName()))
				.set(GemFireProperties.JMX_MANAGER.getName(), distributedSystemProperties.getProperty(GemFireProperties.JMX_MANAGER.getName()))
				.set(GemFireProperties.LOCATORS.getName(), distributedSystemProperties.getProperty(GemFireProperties.LOCATORS.getName()))
				//.set("locators", "localhost[0]") // This locators configuration setting causes the test to fail
				.set(GemFireProperties.LOG_FILE.getName(), distributedSystemProperties.getProperty(GemFireProperties.LOG_FILE.getName()))
				.set(GemFireProperties.LOG_LEVEL.getName(), distributedSystemProperties.getProperty(GemFireProperties.LOG_LEVEL.getName()))
				.set(GemFireProperties.USE_CLUSTER_CONFIGURATION.getName(), distributedSystemProperties.getProperty(GemFireProperties.USE_CLUSTER_CONFIGURATION.getName()))
				.create();

			assertThat(peerCache).isNotNull();
			assertThat(peerCache.getDistributedSystem()).isNotNull();
			assertThat(peerCache.getDistributedSystem().getProperties()).isNotNull();
			assertThat(peerCache.getDistributedSystem().getProperties().getProperty("locators"))
				.isEqualTo(distributedSystemProperties.getProperty("locators"));
		}
		finally {
			GemfireUtils.close(peerCache);
		}
	}

	@LocatorApplication(
		name = "LocatorApplicationIntegrationTests",
		bindAddress = "localhost",
		port = 0
	)
	static class TestConfiguration { }

}
