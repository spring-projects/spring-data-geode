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

import java.util.List;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.internal.DistributionConfig;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests to test and assert the property configuration of a {@link PeerCacheApplication} along with
 * added {@link CacheServer CacheServers} using the {@link EnableCacheServer} annotation.
 *
 * @author John Blum
 * @author Patrick Johnson
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.apache.geode.distributed.internal.DistributionConfig
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.2.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PeerCacheApplicationWithAddedCacheServerUnitTests.TestPeerCacheConfiguration.class)
public class PeerCacheApplicationWithAddedCacheServerUnitTests {

	private static int cacheServerPort = 77777;
	private static int locatorPort = 55555;

	@BeforeClass
	public static void setSystemProperties() {

		System.setProperty("spring.data.gemfire.cache.peer.locators", String.format("localhost[%d]", locatorPort));
		System.setProperty("spring.data.gemfire.cache.server.port", String.valueOf(cacheServerPort));
	}

	@Autowired
	private Cache cache;

	@Before
	public void setup() {

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo("PeerCacheApplicationWithAddedCacheServerUnitTests");
		assertThat(this.cache.getDistributedSystem()).isNotNull();
		assertThat(this.cache.getDistributedSystem().getProperties()).isNotNull();
		assertThat(this.cache.getDistributedSystem().getProperties().getProperty(DistributionConfig.LOCATORS_NAME))
			.isEqualTo(String.format("localhost[%d]", locatorPort));
		assertThat(this.cache.getDistributedSystem().getProperties().getProperty(DistributionConfig.NAME_NAME))
			.isEqualTo("PeerCacheApplicationWithAddedCacheServerUnitTests");
	}

	@Test
	public void cacheServerWasConfiguredCorrectly() {

		List<CacheServer> cacheServers = this.cache.getCacheServers();

		assertThat(cacheServers).isNotNull();
		assertThat(cacheServers).hasSize(1);

		CacheServer cacheServer = cacheServers.get(0);

		assertThat(cacheServer).isNotNull();
		assertThat(cacheServer.getPort()).isEqualTo(cacheServerPort);
	}

	@EnableCacheServer
	@EnableGemFireMockObjects
	@PeerCacheApplication(name = "PeerCacheApplicationWithAddedCacheServerUnitTests")
	static class TestPeerCacheConfiguration { }

}
