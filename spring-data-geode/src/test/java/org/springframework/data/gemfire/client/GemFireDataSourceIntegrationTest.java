/*
 * Copyright 2010-2020 the original author or authors.
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

import javax.annotation.Resource;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * The GemFireDataSourceIntegrationTest class is a test suite of test cases testing the contract and functionality
 * of the &lt;gfe-data:datasource&gt; element in the context of a GemFire cluster running both native,
 * non-Spring configured GemFire Server(s) in addition to Spring configured and bootstrapped GemFire Server(s).
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.data.gemfire.client.GemfireDataSourcePostProcessor
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@SuppressWarnings({ "rawtypes", "unused"})
public class GemFireDataSourceIntegrationTest extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		String serverName = GemFireDataSourceIntegrationTest.class.getSimpleName().concat("Server");

		List<String> arguments = new ArrayList<>();

		arguments.add(String.format("-Dgemfire.name=%s", serverName));
		arguments.add(String.format("-Dgemfire.log-level=%s", GEMFIRE_LOG_LEVEL));
		arguments.add(GemFireDataSourceIntegrationTest.class.getName()
			.replace(".", "/").concat("-server-context.xml"));

		startGemFireServer(ServerProcess.class, arguments.toArray(new String[0]));
	}

	private static void configureGemFireClient(int availablePort) {
		System.setProperty("gemfire.log-level", "error");
		System.setProperty("spring.data.gemfire.cache.server.port", String.valueOf(availablePort));
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ClientCache gemfireClientCache;

	@Resource(name = "ClientOnlyRegion")
	private Region clientOnlyRegion;

	@Resource(name = "ClientServerRegion")
	private Region clientServerRegion;

	@Resource(name = "ServerOnlyRegion")
	private Region serverOnlyRegion;

	@SuppressWarnings("unchecked")
	private void assertRegion(Region actualRegion, String expectedRegionName) {

		assertThat(actualRegion).isNotNull();
		assertThat(actualRegion.getName()).isEqualTo(expectedRegionName);
		assertThat(actualRegion.getFullPath()).isEqualTo(GemfireUtils.toRegionPath(expectedRegionName));
		assertThat(gemfireClientCache.getRegion(actualRegion.getFullPath())).isSameAs(actualRegion);
		assertThat(applicationContext.containsBean(expectedRegionName)).isTrue();
		assertThat(applicationContext.getBean(expectedRegionName, Region.class)).isSameAs(actualRegion);
	}

	@Test
	public void clientProxyRegionBeansExist() {

		assertRegion(clientOnlyRegion, "ClientOnlyRegion");
		assertRegion(clientServerRegion, "ClientServerRegion");
		assertRegion(serverOnlyRegion, "ServerOnlyRegion");
	}
}
