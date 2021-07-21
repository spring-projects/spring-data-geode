/*
 * Copyright 2017-2021 the original author or authors.
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
import static org.springframework.data.gemfire.config.annotation.TestSecurityManager.SECURITY_PASSWORD;
import static org.springframework.data.gemfire.config.annotation.TestSecurityManager.SECURITY_USERNAME;

import java.util.Optional;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.mock.env.MockPropertySource;

/**
 * Integration Tests for auto-configured authentication configuration.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @since 1.9.0
 */
@SuppressWarnings("unused")
public class AutoConfiguredAuthenticationConfigurationIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	private ConfigurableApplicationContext applicationContext;

	@BeforeClass
	public static void setupGemFireServer() throws Exception {
		startGemFireServer(TestGemFireServerConfiguration.class);
	}

	@After
	public void tearDown() {
		Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
	}

	private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
			Class<?>... annotatedClasses) {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();

		propertySources.addFirst(testPropertySource);

		applicationContext.registerShutdownHook();
		applicationContext.register(annotatedClasses);
		applicationContext.refresh();

		return applicationContext;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void clientAuthenticatesWithServer() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.security.username", SECURITY_USERNAME)
			.withProperty("spring.data.gemfire.security.password", SECURITY_PASSWORD);

		this.applicationContext = newApplicationContext(testPropertySource, TestGemFireClientConfiguration.class);

		assertThat(this.applicationContext).isNotNull();
		assertThat(this.applicationContext.containsBean("Echo")).isTrue();

		Region<Object, Object> echo = this.applicationContext.getBean("Echo", Region.class);

		assertThat(echo.get("Hello")).isEqualTo("Hello");
		assertThat(echo.get("TEST")).isEqualTo("TEST");
		assertThat(echo.get("Good-Bye")).isEqualTo("Good-Bye");
	}

	@ClientCacheApplication
	@EnableSecurity
	static class TestGemFireClientConfiguration {

		@Bean("Echo")
		ClientRegionFactoryBean<Object, Object> echoRegion(GemFireCache gemfireCache) {

			ClientRegionFactoryBean<Object, Object> echoRegion = new ClientRegionFactoryBean<>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setClose(false);
			echoRegion.setShortcut(ClientRegionShortcut.PROXY);

			return echoRegion;
		}
	}

	@CacheServerApplication
	@EnableSecurity(securityManagerClassName = "org.springframework.data.gemfire.config.annotation.TestSecurityManager")
	static class TestGemFireServerConfiguration {

		public static void main(String[] args) {
			runSpringApplication(TestGemFireServerConfiguration.class, args);
		}

		@Bean("Echo")
		LocalRegionFactoryBean<Object, Object> echoRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> echoRegion = new LocalRegionFactoryBean<>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setCacheLoader(newEchoCacheLoader());
			echoRegion.setClose(false);
			echoRegion.setPersistent(false);

			return echoRegion;
		}

		private CacheLoader<Object, Object> newEchoCacheLoader() {

			return new CacheLoader<Object, Object>() {

				@Override
				public Object load(LoaderHelper<Object, Object> loaderHelper) throws CacheLoaderException {
					return loaderHelper.getKey();
				}

				@Override
				public void close() {
				}
			};
		}
	}
}
