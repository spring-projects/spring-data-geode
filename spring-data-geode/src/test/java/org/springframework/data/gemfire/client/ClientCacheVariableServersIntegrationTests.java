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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.server.CacheServer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the use of variable {@literal servers} attribute on &lt;gfe:pool/&lt; in SDG XML Namespace
 * configuration metadata when connecting a client and server.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.fork.ServerProcess
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.6.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ClientCacheVariableServersIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGeodeServer() throws IOException {

		final int cacheServerPortOne = findAndReserveAvailablePort();
		final int cacheServerPortTwo = findAndReserveAvailablePort();

		System.setProperty("test.cache.server.port.one", String.valueOf(cacheServerPortOne));
		System.setProperty("test.cache.server.port.two", String.valueOf(cacheServerPortTwo));

		List<String> arguments = new ArrayList<>();

		arguments.add(String.format("-Dtest.cache.server.port.one=%d", cacheServerPortOne));
		arguments.add(String.format("-Dtest.cache.server.port.two=%d", cacheServerPortTwo));
		arguments.add(getServerContextXmlFileLocation(ClientCacheVariableServersIntegrationTests.class));

		startGemFireServer(ServerProcess.class, arguments.toArray(new String[0]));
	}

	@AfterClass
	public static void cleanup() {
		System.clearProperty("test.cache.server.port.one");
		System.clearProperty("test.cache.server.port.two");
	}

	@Resource(name = "Example")
	private Region<String, Integer> example;

	@Before
	public void setup() {

		assertThat(this.example).isNotNull();
		assertThat(this.example.getName()).isEqualTo("Example");
		assertThat(this.example.getAttributes()).isNotNull();
		assertThat(this.example.getAttributes().getPoolName()).isEqualTo("serverPool");

		Pool pool = PoolManager.find("serverPool");

		assertThat(pool).isNotNull();
		assertThat(pool.getName()).isEqualTo("serverPool");
		assertThat(pool.getServers()).hasSize(3);
	}

	@Test
	public void clientServerConnectionSuccessful() {

		assertThat(example.get("one")).isEqualTo(1);
		assertThat(example.get("two")).isEqualTo(2);
		assertThat(example.get("three")).isEqualTo(3);
	}

	public static class CacheMissCounterCacheLoader implements CacheLoader<String, Integer> {

		private static final AtomicInteger cacheMissCounter = new AtomicInteger(0);

		@Override
		public Integer load(final LoaderHelper<String, Integer> helper) throws CacheLoaderException {
			return cacheMissCounter.incrementAndGet();
		}

		@Override
		public void close() {
			cacheMissCounter.set(0);
		}
	}

	public static final class CacheServerConfigurationApplicationListener
			implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

			ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();

			Map<String, CacheServer> cacheServers =
				CollectionUtils.nullSafeMap(applicationContext.getBeansOfType(CacheServer.class));

			for (CacheServer cacheServer : cacheServers.values()) {
				System.err.printf("CacheServer host:port [%s:%d]%n",
					cacheServer.getBindAddress(), cacheServer.getPort());
			}

			System.err.flush();
		}
	}
}
