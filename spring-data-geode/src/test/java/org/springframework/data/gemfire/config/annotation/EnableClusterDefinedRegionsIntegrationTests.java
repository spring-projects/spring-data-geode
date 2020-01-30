/*
 * Copyright 2018-2020 the original author or authors.
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

import java.util.Collections;

import javax.annotation.Resource;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.util.CacheUtils;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for {@link EnableClusterDefinedRegions} and {@link ClusterDefinedRegionsConfiguration}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.config.annotation.ClusterDefinedRegionsConfiguration
 * @see org.springframework.data.gemfire.config.annotation.EnableClusterDefinedRegions
 * @see org.springframework.data.gemfire.tests.process.ProcessWrapper
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @since 2.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EnableClusterDefinedRegionsIntegrationTests.ClientTestConfiguration.class)
@SuppressWarnings("unused")
public class EnableClusterDefinedRegionsIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private static final String GEMFIRE_LOG_LEVEL = "error";

	@BeforeClass
	public static void startGemFireServer() throws Exception {

		startGemFireServer(ServerTestConfiguration.class);
	}

	@Autowired
	private ClientCache cache;

	@Resource(name = "LocalRegion")
	private Region<?, ?> localClientProxyRegion;

	@Resource(name = "PartitionRegion")
	private Region<?, ?> partitionClientProxyRegion;

	@Resource(name = "ReplicateRegion")
	private Region<?, ?> replicateClientProxyRegion;

	private void assertRegion(Region<?, ?> region, String expectedName) {

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(expectedName);
		assertThat(region.getFullPath()).isEqualTo(RegionUtils.toRegionPath(expectedName));
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.EMPTY);
	}

	@Before
	public void setup() {

		assertThat(this.cache).isNotNull();
		assertThat(CacheUtils.isClient(this.cache));
	}

	@Test
	public void clusterRegionsExistOnClient() {

		assertRegion(this.localClientProxyRegion, "LocalRegion");
		assertRegion(this.partitionClientProxyRegion, "PartitionRegion");
		assertRegion(this.replicateClientProxyRegion, "ReplicateRegion");
	}

	@ClientCacheApplication(logLevel = GEMFIRE_LOG_LEVEL)
	@EnableClusterDefinedRegions
	static class ClientTestConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		ClientCacheConfigurer clientCachePoolPortConfigurer(
				@Value("${" + GEMFIRE_CACHE_SERVER_PORT_PROPERTY + ":40404}") int port) {

			return (bean, clientCacheFactoryBean) -> clientCacheFactoryBean.setServers(
				Collections.singletonList(new ConnectionEndpoint("localhost", port)));
		}

		@Bean("TestBean")
		Object testBean(@Qualifier("LocalRegion") Region<?, ?> localRegion) {
			return "TEST";
		}
	}

	@CacheServerApplication(name = "EnableClusterDefinedRegionsIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
	static class ServerTestConfiguration {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(ServerTestConfiguration.class);

			applicationContext.registerShutdownHook();
		}

		@Bean("LocalRegion")
		public LocalRegionFactoryBean<Object, Object> localRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> localRegion = new LocalRegionFactoryBean<>();

			localRegion.setCache(gemfireCache);
			localRegion.setClose(false);
			localRegion.setPersistent(false);

			return localRegion;
		}

		@Bean("PartitionRegion")
		public PartitionedRegionFactoryBean<Object, Object> partitionRegion(GemFireCache gemfireCache) {

			PartitionedRegionFactoryBean<Object, Object> partitionRegion = new PartitionedRegionFactoryBean<>();

			partitionRegion.setCache(gemfireCache);
			partitionRegion.setClose(false);
			partitionRegion.setPersistent(false);

			return partitionRegion;
		}

		@Bean("ReplicateRegion")
		public ReplicatedRegionFactoryBean<Object, Object> replicateRegion(GemFireCache gemfireCache) {

			ReplicatedRegionFactoryBean<Object, Object> replicateRegion = new ReplicatedRegionFactoryBean<>();

			replicateRegion.setCache(gemfireCache);
			replicateRegion.setClose(false);
			replicateRegion.setPersistent(false);

			return replicateRegion;
		}
	}
}
