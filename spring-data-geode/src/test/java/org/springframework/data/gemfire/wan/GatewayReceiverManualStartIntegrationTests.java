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
package org.springframework.data.gemfire.wan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.wan.GatewayReceiver;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the manual start capability of {@link GatewayReceiver GatewayReceivers}
 * when configured with SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.wan.GatewayReceiver
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class GatewayReceiverManualStartIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "Auto")
	private GatewayReceiver autoGatewayReceiver;

	@Resource(name = "Manual")
	private GatewayReceiver manualGatewayReceiver;

	private void assertGreaterThanEqualToLessThanEqualTo(String message,
			int actualValue, int lowerBound, int upperBound) {

		assertThat(actualValue >= lowerBound && actualValue <= upperBound).as(message).isTrue();
	}

	@Test
	public void autoGatewayReceiverConfigurationIsCorrect() {

		assertThat(autoGatewayReceiver)
			.describedAs("The 'Auto' GatewayReceiver was not properly configured or initialized!")
			.isNotNull();

		assertThat(autoGatewayReceiver.isRunning()).isTrue();
		assertThat(autoGatewayReceiver.getStartPort()).isEqualTo(7070);
		assertThat(autoGatewayReceiver.getEndPort()).isEqualTo(7700);

		int gatewayReceiverPort = autoGatewayReceiver.getPort();

		assertGreaterThanEqualToLessThanEqualTo(String.format(
			"GatewayReceiver 'port' [%1$d] was not greater than equal to [%2$d] and less than equal to [%3$d]!",
				gatewayReceiverPort, autoGatewayReceiver.getStartPort(), autoGatewayReceiver.getEndPort()),
					gatewayReceiverPort, autoGatewayReceiver.getStartPort(), autoGatewayReceiver.getEndPort());

		autoGatewayReceiver.stop();

		assertThat(autoGatewayReceiver.isRunning()).isFalse();
	}

	@Test
	public void manualGatewayReceiverConfigurationIsCorrect() throws IOException {

		assertThat(manualGatewayReceiver)
			.describedAs("The 'Manual' GatewayReceiver was not properly configured or initialized!")
			.isNotNull();

		assertThat(manualGatewayReceiver.isRunning()).isFalse();
		assertThat(manualGatewayReceiver.getStartPort()).isEqualTo(6060);
		assertThat(manualGatewayReceiver.getEndPort()).isEqualTo(6600);

		manualGatewayReceiver.start();

		assertThat(manualGatewayReceiver.isRunning()).isTrue();

		int gatewayReceiverPort = manualGatewayReceiver.getPort();

		assertGreaterThanEqualToLessThanEqualTo(String.format(
			"GatewayReceiver 'port' [%1$d] was not greater than equal to [%2$d] and less than equal to [%3$d]!",
				gatewayReceiverPort, manualGatewayReceiver.getStartPort(), manualGatewayReceiver.getEndPort()),
					gatewayReceiverPort, manualGatewayReceiver.getStartPort(), manualGatewayReceiver.getEndPort());

		manualGatewayReceiver.stop();

		assertThat(manualGatewayReceiver.isRunning()).isFalse();
	}
}
