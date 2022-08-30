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

import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.distributed.Locator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Apache Geode Security Integration Tests testing Apache Geode Locator to Locator (application) authentication.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.apache.geode.distributed.Locator
 * @see org.springframework.context.annotation.Profile
 * @see org.springframework.data.gemfire.LocatorFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.EnableSecurity
 * @see org.springframework.data.gemfire.config.annotation.LocatorApplication
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@ActiveProfiles("locator-auth-client")
@ContextConfiguration(classes = SecurityManagerSecuredLocatorToLocatorApplicationIntegrationTests.LocatorAuthClient.class)
@RunWith(SpringRunner.class)
@SuppressWarnings("unused")
public class SecurityManagerSecuredLocatorToLocatorApplicationIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	private static ProcessWrapper locatorProcess;

	@BeforeClass
	public static void startGeodeLocator() throws IOException {

		int locatorPort = findAndReserveAvailablePort();

		locatorProcess= run(LocatorAuthServer.class,
			"-Dspring.profiles.active=locator-auth-server",
			String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		waitForServerToStart("localhost", locatorPort);

		System.setProperty("spring.data.gemfire.locators", String.format("localhost[%d]", locatorPort));
	}

	@AfterClass
	public static void stopGeodeLocator() {
		stop(locatorProcess);
		System.clearProperty("spring.data.gemfire.locators");
	}

	@Autowired
	private Locator locator;

	@Test
	public void locatorIsRunning() {

		assertThat(this.locator).isNotNull();

		DistributedSystem distributedSystem = this.locator.getDistributedSystem();

		assertThat(distributedSystem).isNotNull();
		assertThat(distributedSystem.isConnected()).isTrue();
		assertThat(distributedSystem.getName()).isEqualTo("LocatorAuthClient");
		assertThat(distributedSystem.getDistributedMember().getName()).isEqualTo("LocatorAuthClient");
		assertThat(distributedSystem.getAllOtherMembers()).hasSize(1);
	}

	@LocatorApplication(name = "LocatorAuthServer")
	@EnableSecurity(securityManagerClass = TestSecurityManager.class)
	@Profile("locator-auth-server")
	static class LocatorAuthServer {

		public static void main(String[] args) {
			runSpringApplication(LocatorAuthServer.class);
			block();
		}
	}

	@LocatorApplication(name = "LocatorAuthClient", port = 0)
	@EnableSecurity(securityUsername = TestSecurityManager.SECURITY_USERNAME, securityPassword = TestSecurityManager.SECURITY_PASSWORD)
	@Profile("locator-auth-client")
	static class LocatorAuthClient { }

}
