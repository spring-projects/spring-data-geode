/*
 * Copyright 2011-2022 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.cache.server.ClientSubscriptionConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

/**
 * Integration Tests for the {@link CacheServer} SDG XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class CacheServerNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@SuppressWarnings("deprecation")
	public void basicCacheServerConfigurationIsCorrect() {

		CacheServer cacheServer = applicationContext.getBean("advanced-config", CacheServer.class);

		assertThat(cacheServer).isNotNull();
		assertThat(cacheServer.getGroups().length).isEqualTo(1);
		assertThat(cacheServer.getBindAddress()).isEqualTo("localhost");
		assertThat(cacheServer.getPort() != 0).isTrue();
		assertThat(cacheServer.getHostnameForClients()).isEqualTo("localhost");
		assertThat(cacheServer.getGroups()[0]).isEqualTo("test-server");
		assertThat(cacheServer.getLoadPollInterval()).isEqualTo(2000L);
		assertThat(cacheServer.getMaxConnections()).isEqualTo(22);
		assertThat(cacheServer.getMaxThreads()).isEqualTo(16);
		assertThat(cacheServer.getMaximumMessageCount()).isEqualTo(1000);
		assertThat(cacheServer.getMaximumTimeBetweenPings()).isEqualTo(30000);
		assertThat(cacheServer.isRunning()).isTrue();

		ClientSubscriptionConfig clientSubscriptionConfig = cacheServer.getClientSubscriptionConfig();

		assertThat(clientSubscriptionConfig).isNotNull();
		assertThat(clientSubscriptionConfig.getCapacity()).isEqualTo(1000);
		assertThat("ENTRY".equalsIgnoreCase(clientSubscriptionConfig.getEvictionPolicy())).isTrue();

		assertThat(StringUtils.isEmpty(clientSubscriptionConfig.getDiskStoreName()))
			.describedAs("Expected empty DiskStoreName; but was (%1$s)", clientSubscriptionConfig.getDiskStoreName())
			.isTrue();
	}
}
