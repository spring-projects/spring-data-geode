/*
 * Copyright 2017-2022 the original author or authors.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link AutoConfiguredAuthenticationConfiguration}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.config.annotation.AutoConfiguredAuthenticationConfiguration
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.9.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = AutoConfiguredAuthenticationConfigurationIntegrationTests.TestGemFireClientConfiguration.class
)
@SuppressWarnings("unused")
public class AutoConfiguredAuthenticationConfigurationIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void setupGemFireServer() throws Exception {
		startGemFireServer(TestGemFireServerConfiguration.class);
	}

	@Autowired
	private GemfireTemplate echoTemplate;

	@Test
	public void clientAuthenticatesWithServer() {

		assertThat(this.echoTemplate.<String, String>get("Hello")).isEqualTo("Hello");
		assertThat(this.echoTemplate.<String, String>get("TEST")).isEqualTo("TEST");
		assertThat(this.echoTemplate.<String, String>get("Good-Bye")).isEqualTo("Good-Bye");
	}

	@ClientCacheApplication
	@EnableSecurity(securityUsername = SECURITY_USERNAME, securityPassword = SECURITY_PASSWORD)
	static class TestGemFireClientConfiguration {

		@Bean("Echo")
		ClientRegionFactoryBean<Object, Object> echoRegion(GemFireCache gemfireCache) {

			ClientRegionFactoryBean<Object, Object> echoRegion = new ClientRegionFactoryBean<>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setShortcut(ClientRegionShortcut.PROXY);

			return echoRegion;
		}

		@Bean
		GemfireTemplate echoTemplate(GemFireCache cache) {
			return new GemfireTemplate(cache.getRegion(GemfireUtils.toRegionPath("Echo")));
		}
	}

	@CacheServerApplication
	@EnableSecurity(securityManagerClass = TestSecurityManager.class)
	static class TestGemFireServerConfiguration {

		public static void main(String[] args) {
			runSpringApplication(TestGemFireServerConfiguration.class, args);
		}

		@Bean("Echo")
		LocalRegionFactoryBean<Object, Object> echoRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> echoRegion = new LocalRegionFactoryBean<>();

			echoRegion.setCache(gemfireCache);
			echoRegion.setCacheLoader(newEchoCacheLoader());
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
				public void close() { }

			};
		}
	}
}
