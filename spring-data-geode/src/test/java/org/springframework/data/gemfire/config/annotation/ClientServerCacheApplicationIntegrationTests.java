/*
 * Copyright 2012-2021 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the contract and functionality of the {@link CacheServerApplication}
 * and {@link ClientCacheApplication} SDG annotations for configuring and bootstrapping an Apache Geode
 * client/server topology
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.springframework.data.gemfire.config.annotation.CacheServerApplication
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.9.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ClientServerCacheApplicationIntegrationTests.GeodeClientTestConfiguration.class)
@SuppressWarnings("all")
public class ClientServerCacheApplicationIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGeodeServer() throws Exception {
		startGemFireServer(GeodeServerTestConfiguration.class);
	}

	@Autowired
	private ClientCache clientCache;

	@Resource(name = "Echo")
	private Region<String, String> echo;

	@Test
	public void echoClientProxyRegionEchoesKeysForValues() {
		assertThat(echo.get("Hello")).isEqualTo("Hello");
		assertThat(echo.get("Test")).isEqualTo("Test");
	}

	@ClientCacheApplication
	static class GeodeClientTestConfiguration {

		@Bean(name = "Echo")
		ClientRegionFactoryBean<String, String> echoRegion(ClientCache gemfireCache) {

			ClientRegionFactoryBean<String, String> echoRegion = new ClientRegionFactoryBean<String, String>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setClose(false);
			echoRegion.setShortcut(ClientRegionShortcut.PROXY);

			return echoRegion;
		}
	}

	@CacheServerApplication(name = "ClientServerCacheApplicationIntegrationTests")
	public static class GeodeServerTestConfiguration {

		public static void main(String[] args) {
			runSpringApplication(GeodeServerTestConfiguration.class, args);
		}

		@Bean("Echo")
		public PartitionedRegionFactoryBean<String, String> echoRegion(Cache gemfireCache) {

			PartitionedRegionFactoryBean<String, String> echoRegion =
				new PartitionedRegionFactoryBean<String, String>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setCacheLoader(echoCacheLoader());
			echoRegion.setClose(false);
			echoRegion.setPersistent(false);

			return echoRegion;
		}

		CacheLoader<String, String> echoCacheLoader() {

			return new CacheLoader<String, String>() {
				@Override
				public String load(LoaderHelper<String, String> helper) throws CacheLoaderException {
					return helper.getKey();
				}

				@Override
				public void close() {
				}
			};
		}
	}
}
