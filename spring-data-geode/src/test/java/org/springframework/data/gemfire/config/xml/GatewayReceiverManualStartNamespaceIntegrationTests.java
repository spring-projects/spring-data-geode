/*
 * Copyright 2010-2022 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.wan.GatewayReceiver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests with test cases testing the contract and functionality of {@link GatewayReceiver} configuration
 * in SDG using XML namespace (XSD) configuration metadata.
 *
 * This test class tests the manual start configuration of the {@link GatewayReceiver} Component in SDG.
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
@ContextConfiguration(locations = "GatewayReceiverNamespaceIntegrationTests-context.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@ActiveProfiles("manualStart")
@SuppressWarnings("unused")
public class GatewayReceiverManualStartNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("&Manual")
	private GatewayReceiverFactoryBean manualGatewayReceiverFactory;

	@Test
	public void testManual() throws Exception {

		assertThat(this.manualGatewayReceiverFactory)
			.describedAs("The 'Manual' GatewayReceiverFactoryBean was not properly configured and initialized!")
			.isNotNull();

		GatewayReceiver manualGatewayReceiver = this.manualGatewayReceiverFactory.getObject();

		assertThat(manualGatewayReceiver).isNotNull();
		assertThat(manualGatewayReceiver.getBindAddress()).isEqualTo("192.168.0.1");
		assertThat(manualGatewayReceiver.getHost()).isEqualTo("theOne");
		assertThat(manualGatewayReceiver.getStartPort()).isEqualTo(100);
		assertThat(manualGatewayReceiver.getEndPort()).isEqualTo(900);
		assertThat(manualGatewayReceiver.getMaximumTimeBetweenPings()).isEqualTo(1000);
		assertThat(manualGatewayReceiver.isRunning()).isFalse();
		assertThat(manualGatewayReceiver.getSocketBufferSize()).isEqualTo(8192);
	}
}
