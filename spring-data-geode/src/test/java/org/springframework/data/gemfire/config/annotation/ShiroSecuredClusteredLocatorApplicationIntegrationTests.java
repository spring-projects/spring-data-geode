/*
 * Copyright 2022 the original author or authors.
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

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.internal.security.shiro.GeodePermissionResolver;

import org.apache.shiro.realm.text.PropertiesRealm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Integration Tests for {@link LocatorApplication} and {@link LocatorApplicationConfiguration}
 * with {@link EnableSecurity} using Apache Shiro.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.distributed.Locator
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ShiroSecuredClusteredLocatorApplicationIntegrationTests.TestClientConfiguration.class)
public class ShiroSecuredClusteredLocatorApplicationIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	private static ProcessWrapper locatorProcessOne;
	private static ProcessWrapper locatorProcessTwo;

	private static final String CLUSTER_SECURITY_USERNAME = "root";
	private static final String CLUSTER_SECURITY_PASSWORD = "s3c3rt!";

	@BeforeClass
	public static void assertApacheShiroSecurityEnabled() {

		String propertyName = ApacheShiroSecurityConfiguration.ApacheShiroPresentCondition
			.SPRING_DATA_GEMFIRE_SECURITY_SHIRO_ENABLED;

		String apacheShiroEnabledValue = System.getProperty(propertyName, Boolean.TRUE.toString());

		boolean apacheShiroEnabled = Boolean.parseBoolean(apacheShiroEnabledValue);

		assertThat(apacheShiroEnabled).isTrue();
	}

	@BeforeClass
	public static void startApacheGeodeCluster() throws IOException {

		int locatorPort = findAndReserveAvailablePort();

		String locatorBaseName = ShiroSecuredClusteredLocatorApplicationIntegrationTests.class.getSimpleName().concat("%s");
		String locatorOneName = String.format(locatorBaseName, "LocatorOne");
		String locatorTwoName = String.format(locatorBaseName, "LocatorTwo");

		locatorProcessOne = run(createDirectory(locatorOneName), TestLocatorApplication.class,
			"-Dspring.profiles.active=auth-server",
			String.format("-Dspring.data.gemfire.locator.name=%s", locatorOneName),
			String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		waitForServerToStart("localhost", locatorPort);

		locatorProcessTwo = run(createDirectory(locatorTwoName), TestLocatorApplication.class,
			//String.format("-Dspring.data.gemfire.security.username=%s", CLUSTER_SECURITY_USERNAME),
			//String.format("-Dspring.data.gemfire.security.password=%s", CLUSTER_SECURITY_PASSWORD),
			String.format("-Dspring.data.gemfire.locator.name=%s", locatorTwoName),
			String.format("-Dspring.data.gemfire.locators=localhost[%d]", locatorPort));

		startGemFireServer(TestServerApplication.class,
			//String.format("-Dspring.data.gemfire.security.username=%s", CLUSTER_SECURITY_USERNAME),
			//String.format("-Dspring.data.gemfire.security.password=%s", CLUSTER_SECURITY_PASSWORD),
			//String.format("-Dspring.data.gemfire.security.username=%s", "guest"),
			//String.format("-Dspring.data.gemfire.security.password=%s", "guest"),
			String.format("-Dspring.data.gemfire.locators=localhost[%d]", locatorPort));
	}

	@AfterClass
	public static void shutdownApacheGeodeCluster() {

		// NOTE: The Apache Geode CacheServer process will be stopped automatically by the STDG framework
		// on test class (suite) teardown!
		stop(locatorProcessOne);
		stop(locatorProcessTwo);
	}

	@Autowired
	private GemfireTemplate customersTemplate;

	@Test
	public void secureClientCacheCustomersRegionPutAndGetOperationsAreSuccess() {

		Customer jonDoe = Customer.as("Jon Doe");

		this.customersTemplate.put(jonDoe.getName(), jonDoe);

		Customer jonDoeLoaded = this.customersTemplate.get(jonDoe.getName());

		assertThat(jonDoeLoaded).isNotNull();
		assertThat(jonDoeLoaded).isNotSameAs(jonDoe);
		assertThat(jonDoeLoaded).isEqualTo(jonDoe);
	}

	@Configuration
	@EnableEntityDefinedRegions(
		basePackageClasses = Customer.class,
		serverRegionShortcut = RegionShortcut.LOCAL,
		includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Customer.class)
	)
	@EnablePdx(includeDomainTypes = Customer.class)
	static class TestApplicationConfiguration { }

	@Configuration
	@EnableSecurity(securityUsername = CLUSTER_SECURITY_USERNAME, securityPassword = CLUSTER_SECURITY_PASSWORD)
	static class TestSecurityConfiguration {

		@Bean
		@Profile("auth-server")
		public PropertiesRealm shiroRealm() {

			PropertiesRealm propertiesRealm = new PropertiesRealm();

			propertiesRealm.setResourcePath("classpath:shiro.properties");
			propertiesRealm.setPermissionResolver(new GeodePermissionResolver());

			return propertiesRealm;
		}
	}

	@ClientCacheApplication(name = "ShiroSecuredLocatorApplicationIntegrationTestsClientCache")
	@EnableSecurity(securityUsername = "scientist", securityPassword = "w0rk!ng4u")
	@Import(TestApplicationConfiguration.class)
	static class TestClientConfiguration {

		@Bean
		GemfireTemplate customersTemplate(GemFireCache cache) {
			return new GemfireTemplate(cache.getRegion(GemfireUtils.toRegionPath("Customers")));
		}
	}

	@LocatorApplication(name = "ShiroSecuredLocatorApplicationIntegrationTestsLocator", port = 0)
	@Import(TestSecurityConfiguration.class)
	static class TestLocatorApplication {

		public static void main(String[] args) {
			assertApacheShiroSecurityEnabled();
			runSpringApplication(TestLocatorApplication.class, args);
			block();
		}
	}

	@CacheServerApplication(name = "ShiroSecuredLocatorApplicationIntegrationTestsCacheServer")
	@Import({ TestApplicationConfiguration.class, TestSecurityConfiguration.class })
	static class TestServerApplication {

		public static void main(String[] args) {
			assertApacheShiroSecurityEnabled();
			runSpringApplication(TestServerApplication.class, args);
		}
	}

	@Getter
	@EqualsAndHashCode
	@ToString(of = "name")
	@Region("Customers")
	@RequiredArgsConstructor(staticName = "as")
	@SuppressWarnings("all")
	static class Customer {

		@Id @lombok.NonNull
		private final String name;

	}
}
