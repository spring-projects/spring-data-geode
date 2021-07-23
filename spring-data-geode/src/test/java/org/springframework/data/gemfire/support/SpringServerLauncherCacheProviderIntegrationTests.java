/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.AbstractLauncher.Status;
import org.apache.geode.distributed.ServerLauncher;
import org.apache.geode.distributed.ServerLauncher.ServerState;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.gemfire.GemfireUtils;

/**
 * Integration Tests {@link SpringServerLauncherCacheProvider} class.
 *
 * This test class focuses on testing isolated units of functionality in the
 * {@link org.apache.geode.distributed.ServerLauncherCacheProvider} class directly, mocking any dependencies
 * as appropriate, in order for the class to uphold it's contract.
 *
 * @author Dan Smith
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.distributed.ServerLauncher
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.data.gemfire.support.SpringServerLauncherCacheProvider
 */
public class SpringServerLauncherCacheProviderIntegrationTests {

	@After
	public void tearDown() {

		System.clearProperty(gemfireName());
		SpringContextBootstrappingInitializer.getApplicationContext().close();
		GemfireUtils.closeClientCache();
	}

	private String gemfireName() {
		return GemfireUtils.GEMFIRE_PREFIX + GemfireUtils.NAME_PROPERTY_NAME;
	}

	@Test
	public void createCacheWithSpring() {

		String springXmlLocation = getClass().getSimpleName() + "-context.xml";

		ServerLauncher.Builder builder = new ServerLauncher.Builder();

		builder.setSpringXmlLocation(springXmlLocation);
		builder.setMemberName("membername");
		builder.setDisableDefaultServer(true);

		ServerLauncher launcher = builder.build();

		ServerState state = launcher.start();

		assertThat(state.getStatus()).isEqualTo(Status.ONLINE);

		ConfigurableApplicationContext applicationContext =
			SpringContextBootstrappingInitializer.getApplicationContext();

		Cache cache = applicationContext.getBean(Cache.class);

		assertThat(cache).isNotNull();
		assertThat(cache.getResourceManager().getCriticalHeapPercentage()).isEqualTo(55.0f);

		state = launcher.stop();

		assertThat(state.getStatus()).isEqualTo(Status.STOPPED);
	}
}
