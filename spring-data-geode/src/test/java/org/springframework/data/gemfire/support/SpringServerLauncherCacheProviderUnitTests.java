/*
 * Copyright 2016-2022 the original author or authors.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.ServerLauncher;
import org.apache.geode.distributed.ServerLauncherCacheProvider;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.util.PropertiesBuilder;

/**
 * Unit Tests testing the contract and functionality of the {@link SpringServerLauncherCacheProvider} class.
 *
 * This test class focuses on testing isolated units of functionality in the {@link ServerLauncherCacheProvider} class
 * directly, mocking any dependencies as appropriate, in order for the class to uphold it's contract.
 *
 * @author Dan Smith
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.Spy
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.distributed.ServerLauncher
 * @see org.apache.geode.distributed.ServerLauncherCacheProvider
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.data.gemfire.support.SpringServerLauncherCacheProvider
 */
public class SpringServerLauncherCacheProviderUnitTests extends IntegrationTestsSupport {

	private Properties singletonProperties(String propertyName, String propertyValue) {
		return PropertiesBuilder.create().setProperty(propertyName, propertyValue).build();
	}

	@After
	public void tearDown() {
		SpringContextBootstrappingInitializer.destroy();
	}

	@Test
	public void createsCacheWhenSpringXmlLocationIsSpecified() {

		Cache mockCache = mock(Cache.class);
		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class);
		ServerLauncher mockServerLauncher = mock(ServerLauncher.class);

		SpringContextBootstrappingInitializer.applicationContext = mockApplicationContext;

		when(mockServerLauncher.isSpringXmlLocationSpecified()).thenReturn(true);
		when(mockServerLauncher.getSpringXmlLocation()).thenReturn("test-context.xml");
		when(mockServerLauncher.getMemberName()).thenReturn("TEST");
		when(mockApplicationContext.getBean(eq(Cache.class))).thenReturn(mockCache);

		final SpringContextBootstrappingInitializer initializer = mock(SpringContextBootstrappingInitializer.class);

		SpringServerLauncherCacheProvider provider = spy(new SpringServerLauncherCacheProvider());

		doReturn(initializer).when(provider).newSpringContextBootstrappingInitializer();

		Properties expectedParameters =
			singletonProperties(SpringContextBootstrappingInitializer.CONTEXT_CONFIG_LOCATIONS_PARAMETER,
				"test-context.xml");

		assertThat(provider.createCache(null, mockServerLauncher)).isEqualTo(mockCache);

		verify(mockServerLauncher, times(1)).isSpringXmlLocationSpecified();
		verify(mockServerLauncher, times(1)).getSpringXmlLocation();
		verify(mockServerLauncher, times(1)).getMemberName();
		verify(mockApplicationContext, times(1)).getBean(eq(Cache.class));
		verify(initializer).init(eq(expectedParameters));
	}

	@Test
	public void doesNothingWhenSpringXmlLocationNotSpecified() {

		ServerLauncher launcher = mock(ServerLauncher.class);

		when(launcher.isSpringXmlLocationSpecified()).thenReturn(false);

		assertThat(new SpringServerLauncherCacheProvider().createCache(null, launcher)).isNull();

		verify(launcher, times(1)).isSpringXmlLocationSpecified();
	}
}
