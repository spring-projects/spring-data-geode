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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.wan.GatewayEventSubstitutionFilter;
import org.apache.geode.cache.wan.GatewayReceiver;
import org.apache.geode.cache.wan.GatewaySender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests with test cases testing the contract and functionality of GemFire 8 {@link GatewaySender}
 * and {@link GatewayReceiver} support.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.wan.GatewaySender
 * @see org.apache.geode.cache.wan.GatewayEventSubstitutionFilter
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.data.gemfire.wan.GatewaySenderFactoryBean
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "gateway-v8-ns.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class GemfireV8GatewayNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("gateway-sender-with-event-substitution-filter")
	private GatewaySender gatewaySenderWithEventSubstitutionFilter;

	@Autowired
	@Qualifier("gateway-sender-with-event-substitution-filter-ref")
	private GatewaySender gatewaySenderWithEventSubstitutionFilterRef;

	@Test
	public void testGatewaySenderEventSubstitutionFilter() {

		assertThat(gatewaySenderWithEventSubstitutionFilter)
			.describedAs("The 'gatewaySenderEventSubtitutionFilter' bean was not properly configured and initialized!")
			.isNotNull();

		assertThat(gatewaySenderWithEventSubstitutionFilter.getId()).isEqualTo("gateway-sender-with-event-substitution-filter");
		assertThat(gatewaySenderWithEventSubstitutionFilter.getRemoteDSId()).isEqualTo(3);
		assertThat(gatewaySenderWithEventSubstitutionFilter.getDispatcherThreads()).isEqualTo(10);
		assertThat(gatewaySenderWithEventSubstitutionFilter.isParallel()).isTrue();
		assertThat(gatewaySenderWithEventSubstitutionFilter.isRunning()).isFalse();
		assertThat(gatewaySenderWithEventSubstitutionFilter.getGatewayEventSubstitutionFilter()).isNotNull();
		assertThat(gatewaySenderWithEventSubstitutionFilter.getGatewayEventSubstitutionFilter().toString())
			.isEqualTo("inner");
	}

	@Test
	public void testGatewaySenderEventSubstitutionFilterRef() {

		assertThat(gatewaySenderWithEventSubstitutionFilterRef)
			.describedAs("The 'gatewaySenderEventSubtitutionFilter' bean was not properly configured and initialized!")
			.isNotNull();

		assertThat(gatewaySenderWithEventSubstitutionFilterRef.getId()).isEqualTo("gateway-sender-with-event-substitution-filter-ref");
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.getRemoteDSId()).isEqualTo(33);
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.getDispatcherThreads()).isEqualTo(1);
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.isParallel()).isFalse();
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.isRunning()).isFalse();
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.getGatewayEventSubstitutionFilter()).isNotNull();
		assertThat(gatewaySenderWithEventSubstitutionFilterRef.getGatewayEventSubstitutionFilter().toString())
			.isEqualTo("ref");
	}

	public static class TestGatewayEventSubstitutionFilter implements GatewayEventSubstitutionFilter<Object, Object> {

		private String name;

		public final void setName(String name) {
			this.name = name;
		}

		protected String getName() {
			return this.name;
		}

		@Override
		public Object getSubstituteValue(EntryEvent<Object, Object> objectObjectEntryEvent) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public void close() { }

		@Override
		public String toString() {
			return getName();
		}
	}
}
