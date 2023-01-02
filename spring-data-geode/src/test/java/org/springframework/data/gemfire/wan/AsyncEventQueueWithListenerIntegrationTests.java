/*
 * Copyright 2010-2023 the original author or authors.
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

import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.cache.asyncqueue.AsyncEventQueue;
import org.apache.geode.cache.wan.GatewaySender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Integration Tests with test cases testing the circular references between an {@link AsyncEventQueue}
 * and a registered {@link AsyncEventListener} that refers back to the {@link AsyncEventQueue} on which
 * the listener is registered.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.asyncqueue.AsyncEventListener
 * @see org.apache.geode.cache.asyncqueue.AsyncEventQueue
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.data.gemfire.wan.AsyncEventQueueFactoryBean
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class AsyncEventQueueWithListenerIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("Q1")
	private AsyncEventQueue queueOne;

	@Autowired
	@Qualifier("Q2")
	private AsyncEventQueue queueTwo;

	@Autowired
	@Qualifier("Q3")
	private AsyncEventQueue queueThree;

	@Test
	public void testAsyncEventQueueOneAndListenerConfiguration() {

		assertThat(queueOne).isNotNull();
		assertThat(queueOne.getId()).isEqualTo("QueueOne");
		assertThat(queueOne.isPersistent()).isFalse();
		assertThat(queueOne.isParallel()).isFalse();
		assertThat(queueOne.getMaximumQueueMemory()).isEqualTo(50);
		assertThat(queueOne.getDispatcherThreads()).isEqualTo(4);
		assertThat(queueOne.getAsyncEventListener() instanceof TestAsyncEventListener).isTrue();
		assertThat(((TestAsyncEventListener) queueOne.getAsyncEventListener()).getQueue()).isSameAs(queueOne);
	}
	@Test
	public void testAsyncEventQueueTwoAndListenerConfiguration() {

		assertThat(queueTwo).isNotNull();
		assertThat(queueTwo.getId()).isEqualTo("QueueTwo");
		assertThat(queueTwo.isPersistent()).isFalse();
		assertThat(queueTwo.isParallel()).isTrue();
		assertThat(queueTwo.getMaximumQueueMemory()).isEqualTo(150);
		assertThat(queueTwo.getDispatcherThreads()).isEqualTo(GatewaySender.DEFAULT_DISPATCHER_THREADS);
		assertThat(queueTwo.getAsyncEventListener() instanceof TestAsyncEventListener).isTrue();
		assertThat(((TestAsyncEventListener) queueTwo.getAsyncEventListener()).getName()).isEqualTo("ListenerTwo");
	}

	@Test
	public void testAsyncEventQueueThreeAndListenerConfiguration() {

		assertThat(queueThree).isNotNull();
		assertThat(queueThree.getId()).isEqualTo("QueueThree");
		assertThat(queueThree.isPersistent()).isFalse();
		assertThat(queueThree.isParallel()).isFalse();
		assertThat(queueThree.getMaximumQueueMemory()).isEqualTo(25);
		assertThat(queueThree.getDispatcherThreads()).isEqualTo(2);
		assertThat(queueThree.getAsyncEventListener() instanceof TestAsyncEventListener).isTrue();
		assertThat(((TestAsyncEventListener) queueThree.getAsyncEventListener()).getQueue()).isSameAs(queueThree);
	}

	/**
	 * The QueueAsyncEventListener class is an implementation of the AsyncEventListener interface that contains
	 * a reference to the AsyncEventQueue upon which it is registered.
	 *
	 * @see org.apache.geode.cache.asyncqueue.AsyncEvent
	 * @see org.apache.geode.cache.asyncqueue.AsyncEventListener
	 * @see org.apache.geode.cache.asyncqueue.AsyncEventQueue
	 */
	@SuppressWarnings("unused")
	public static class TestAsyncEventListener implements AsyncEventListener {

		private AsyncEventQueue queue;

		private String name;

		public TestAsyncEventListener() {
			this.queue = null;
		}

		public TestAsyncEventListener(AsyncEventQueue queue) {
			this.queue = queue;
		}

		public void init() {
			getQueue();
		}

		public AsyncEventQueue getQueue() {

			Assert.state(queue != null, String.format("A reference to the AsyncEventQueue on which this listener"
				+ " [%s] has been registered was not properly configured", this));

			return queue;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void setQueue(final AsyncEventQueue queue) {
			this.queue = queue;
		}

		@Override
		public boolean processEvents(final List<AsyncEvent> events) {
			return false;
		}

		@Override
		public void close() { }

		@Override
		public String toString() {
			return (StringUtils.hasText(getName()) ? getName() : getClass().getName());
		}
	}
}
