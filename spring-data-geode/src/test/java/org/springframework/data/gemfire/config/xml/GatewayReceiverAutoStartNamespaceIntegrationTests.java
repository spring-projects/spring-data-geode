/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.wan.GatewayReceiver;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

/**
 * Integration Tests with test cases testing the contract and functionality of {@link GatewayReceiver} configuration
 * in SDG using the XML namespace (XSD) configuration metadata.
 *
 * This test class tests the auto start configuration of the {@link GatewayReceiver} component in SDG.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.wan.GatewayReceiver
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "GatewayReceiverNamespaceTest-context.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@ActiveProfiles("autoStart")
@SuppressWarnings("unused")
public class GatewayReceiverAutoStartNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "&Auto")
	private GatewayReceiverFactoryBean autoGatewayReceiverFactory;

	@Test
	public void testAuto() throws Exception {

		assertThat(this.autoGatewayReceiverFactory)
			.describedAs("The 'Auto' GatewayReceiverFactoryBean was not properly configured and initialized!")
			.isNotNull();

		GatewayReceiver autoGatewayReceiver = this.autoGatewayReceiverFactory.getObject();

		try {
			assertThat(autoGatewayReceiver).isNotNull();
			assertThat(StringUtils.hasText(autoGatewayReceiver.getBindAddress())).isFalse();
			assertThat(autoGatewayReceiver.getHost()).isEqualTo("neo");
			assertThat(autoGatewayReceiver.getStartPort()).isEqualTo(15500);
			assertThat(autoGatewayReceiver.getEndPort()).isEqualTo(25500);
			assertThat(autoGatewayReceiver.getMaximumTimeBetweenPings()).isEqualTo(10000);
			assertThat(autoGatewayReceiver.isRunning()).isTrue();
			assertThat(autoGatewayReceiver.getSocketBufferSize()).isEqualTo(16384);
		}
		finally {
			autoGatewayReceiver.stop();
		}
	}
}
