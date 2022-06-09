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
package org.springframework.data.gemfire.wan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.cache.asyncqueue.AsyncEventQueue;
import org.apache.geode.cache.wan.GatewaySender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the concurrent, parallel configuration and functional behavior of {@link AsyncEventQueue}
 * and {@link GatewaySender}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.asyncqueue.AsyncEventQueue
 * @see org.apache.geode.cache.wan.GatewaySender
 * @see org.springframework.data.gemfire.wan.AsyncEventQueueFactoryBean
 * @see org.springframework.data.gemfire.wan.GatewaySenderFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class CachePartitionRegionWithConcurrentParallelAsyncEventQueueAndGatewaySenderIntegrationTests
		extends IntegrationTestsSupport {

	@Autowired
	private AsyncEventQueue exampleQueue;

	@Autowired
	private GatewaySender exampleGateway;

	@Autowired
	@Qualifier("ExampleRegion")
	private Region<?, ?> exampleRegion;

	@Test
	public void testPartitionRegionWithConcurrentParallelAsyncEventQueueAndGatewaySenderConfiguration() {

		assertThat(exampleRegion)
			.describedAs("The 'ExampleRegion' PARTITION Region was not properly configured and initialized")
			.isNotNull();

		assertThat(exampleRegion.getName()).isEqualTo("ExampleRegion");
		assertThat(exampleRegion.getFullPath()).isEqualTo("/ExampleRegion");
		assertThat(exampleRegion.getAttributes()).isNotNull();
		assertThat(exampleRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(exampleRegion.getAttributes().getAsyncEventQueueIds().contains("ExampleQueue")).isTrue();
		assertThat(exampleRegion.getAttributes().getGatewaySenderIds().contains("ExampleGateway")).isTrue();
	}

	@Test
	public void testConcurrentParallelAsyncEventQueue() {

		assertThat(exampleQueue)
			.describedAs("The 'ExampleQueue' AsyncEventQueue was not properly configured and initialized")
			.isNotNull();

		assertThat(exampleQueue.getId()).isEqualTo("ExampleQueue");
		assertThat(exampleQueue.getAsyncEventListener()).isNotNull();
		assertThat(exampleQueue.getDispatcherThreads()).isEqualTo(4);
		assertThat(exampleQueue.isParallel()).isTrue();
	}

	@Test
	public void testConcurrentParallelGatewaySender() {

		assertThat(exampleGateway)
			.describedAs("The 'ExampleGateway' was not properly configured and initialized")
			.isNotNull();

		assertThat(exampleGateway.getId()).isEqualTo("ExampleGateway");
		assertThat(exampleGateway.getRemoteDSId()).isEqualTo(123);
		assertThat(exampleGateway.getDispatcherThreads()).isEqualTo(8);
		assertThat(exampleGateway.isParallel()).isTrue();
		assertThat(exampleGateway.isRunning()).isFalse();
	}

	@SuppressWarnings("unused")
	public static final class TestAsyncEventListener implements AsyncEventListener {

		@Override
		public boolean processEvents(final List<AsyncEvent> events) {
			return false;
		}

		@Override
		public void close() { }

	}
}
