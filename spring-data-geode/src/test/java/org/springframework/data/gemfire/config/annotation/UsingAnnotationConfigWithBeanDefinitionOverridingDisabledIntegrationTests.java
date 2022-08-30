/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.distributed.DistributedSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.GemFireProperties;
import org.springframework.data.gemfire.support.DisableBeanDefinitionOverridingApplicationContextInitializer;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the use of SDG's Annotation configuration metadata when bean definition overriding
 * in the Spring container has been disabled.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see java.util.Properties
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.context.annotation.Profile
 * @see org.springframework.data.gemfire.GemFireProperties
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see <a href="https://stackoverflow.com/questions/69202828/error-bean-definition-overriding-clientgemfirepropertiesconfigurer">Error - Bean Definition Overriding - ClientGemFirePropertiesConfigurer</a>
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
//@ActiveProfiles("incorrect-test-configuration")
@ContextConfiguration(initializers = DisableBeanDefinitionOverridingApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class UsingAnnotationConfigWithBeanDefinitionOverridingDisabledIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private GemFireCache cache;

	@Before
	public void assertApplicationContextBeanDefinitionOverridingIsDisabled() {

		ConfigurableApplicationContext applicationContext = requireApplicationContext();

		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();

		assertThat(beanFactory).isInstanceOf(DefaultListableBeanFactory.class);

		assertThat(((DefaultListableBeanFactory) beanFactory).isAllowBeanDefinitionOverriding()).isFalse();

		assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
			.map(beanName -> applicationContext.getBeanFactory().getBeanDefinition(beanName))
			.map(BeanDefinition::getBeanClassName)
			.filter(beanClassName -> String.valueOf(beanClassName).contains("ClientGemFirePropertiesConfigurer"))
			.count()).isEqualTo(2);
	}

	@Test
	public void gemfireCacheSecurityAndSslConfigurationIsCorrect() {

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName())
			.isEqualTo(UsingAnnotationConfigWithBeanDefinitionOverridingDisabledIntegrationTests.class.getSimpleName());

		DistributedSystem distributedSystem = this.cache.getDistributedSystem();

		assertThat(distributedSystem).isNotNull();

		Properties gemfireProperties = distributedSystem.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).isNotEmpty();

		assertThat(gemfireProperties.getProperty(GemFireProperties.SECURITY_MANAGER.getName()))
			.isEqualTo(String.valueOf(TestSecurityManager.class.getName()));

		assertThat(gemfireProperties.getProperty(GemFireProperties.SSL_KEYSTORE.getName()))
			.isEqualTo("/path/to/test/keystore.jks");

		assertThat(gemfireProperties.getProperty(GemFireProperties.SSL_KEYSTORE_PASSWORD.getName()))
			.isEqualTo("p@55w0rd");
	}

	@EnableGemFireMockObjects
	@ClientCacheApplication(name = "UsingAnnotationConfigWithBeanDefinitionOverridingDisabledIntegrationTests")
	@EnableSecurity(securityManagerClass = TestSecurityManager.class)
	@EnableSsl(keystore = "/path/to/test/keystore.jks", keystorePassword = "p@55w0rd")
	static class TestConfiguration { }

	@Configuration
	@Import(TestConfiguration.class)
	@EnableSsl(keystore = "/spoofed/path/to/keystore.jks", keystorePassword = "h@cK3r")
	@Profile("incorrect-test-configuration")
	static class IncorrectTestConfiguration { }

}
