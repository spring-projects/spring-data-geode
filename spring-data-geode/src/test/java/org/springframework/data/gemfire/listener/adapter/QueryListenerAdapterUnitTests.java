/*
 * Copyright 2011-2012-2012 the original author or authors.
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
package org.springframework.data.gemfire.listener.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.Operation;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqQuery;

import org.springframework.data.gemfire.listener.ContinuousQueryListener;

/**
 * Unit Tests for {@link ContinuousQueryListenerAdapter}.
 *
 * @author Costin Leau
 * @author Oliver Gierke
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.listener.adapter.ContinuousQueryListenerAdapter
 */
public class QueryListenerAdapterUnitTests {

	private ContinuousQueryListenerAdapter adapter;

	@Before
	public void setUp() {
		this.adapter = new ContinuousQueryListenerAdapter();
	}

	CqEvent event() {

		return new CqEvent() {

			final CqQuery cq = mock(CqQuery.class);
			final byte[] deltaValue = new byte[0];
			final Object key = new Object();
			final Object value = new Object();
			final Exception exception = new Exception();

			public Operation getBaseOperation() {
				return Operation.CACHE_CLOSE;
			}

			public CqQuery getCq() {
				return cq;
			}

			public byte[] getDeltaValue() {
				return deltaValue;
			}

			public Object getKey() {
				return key;
			}

			public Object getNewValue() {
				return value;
			}

			public Operation getQueryOperation() {
				return Operation.CACHE_CREATE;
			}

			public Throwable getThrowable() {
				return exception;
			}
		};
	}

	@SuppressWarnings("unused")
	interface Delegate {

		void handleEvent(CqEvent event);

		void handleQuery(CqQuery query);

		void handleOperation(Operation op);

		void handleArray(byte[] ba);

		void handleKey(Object key);

		void handleKV(Object k, Object v);

		void handleEx(Throwable th);

		void handleOps(Operation base, Operation query);

		void handleAll(CqEvent event, CqQuery query, byte[] ba, Object key, Operation op, Throwable th, Operation qOp, Object v);

		void handleInvalid(Object o1, Object o2, Object o3);

	}

	@Test
	public void testThatWhenNoDelegateIsSuppliedTheDelegateIsAssumedToBeTheListenerAdapterItself() {
		assertThat(adapter.getDelegate()).isSameAs(adapter);
	}

	@Test
	public void testThatTheDefaultHandlingMethodNameIsTheConstantDefault() {
		assertThat(adapter.getDefaultListenerMethod())
			.isEqualTo(ContinuousQueryListenerAdapter.DEFAULT_LISTENER_METHOD_NAME);
	}

	@Test
	public void testAdapterWithListenerAndDefaultMessage() {

		ContinuousQueryListener mockCqListener = mock(ContinuousQueryListener.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockCqListener);

		CqEvent event = event();

		cqListenerAdapter.onEvent(event);

		verify(mockCqListener).onEvent(same(event));
	}

	@Test
	public void testHandleEvent() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleEvent(same(event));
	}

	@Test
	public void testHandleArray() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleArray");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleArray(eq(event.getDeltaValue()));
	}

	@Test
	public void testHandleKey() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleKey");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleKey(eq(event.getKey()));
	}

	@Test
	public void testHandleKV() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleKV");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleKV(eq(event.getKey()), eq(event.getNewValue()));
	}

	@Test
	public void testHandleEx() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleEx");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleEx(eq(event.getThrowable()));
	}

	@Test
	public void testHandleOps() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleOps");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleOps(eq(event.getBaseOperation()), eq(event.getQueryOperation()));
	}

	@Test
	public void testHandleAll() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		CqEvent event = event();

		cqListenerAdapter.setDefaultListenerMethod("handleAll");
		cqListenerAdapter.onEvent(event);

		verify(mockDelegate).handleAll(eq(event), eq(event.getCq()), eq(event.getDeltaValue()), eq(event.getKey()),
			eq(event.getBaseOperation()), eq(event.getThrowable()), eq(event.getQueryOperation()),
				eq(event.getNewValue()));
	}

	@Test
	public void testInvalid() {

		Delegate mockDelegate = mock(Delegate.class);

		ContinuousQueryListenerAdapter cqListenerAdapter = new ContinuousQueryListenerAdapter(mockDelegate);

		cqListenerAdapter.setDefaultListenerMethod("handleInvalid");
		cqListenerAdapter.onEvent(event());

		doThrow(new IllegalArgumentException()).when(mockDelegate);
	}

	/**
	 * @link https://jira.spring.io/browse/SGF-89
	 */
	@Test
	public void triggersListenerImplementingInterfaceCorrectly() {

		SampleListener listener = new SampleListener();

		ContinuousQueryListener listenerAdapter = new ContinuousQueryListenerAdapter(listener) {
			protected void handleListenerException(Throwable cause) {
				throw new RuntimeException(cause);
			}
		};

		listenerAdapter.onEvent(event());
		assertThat(listener.count).isEqualTo(1);
	}

	static class SampleListener implements ContinuousQueryListener {

		int count;

		@Override
		public void onEvent(CqEvent event) {
			count++;
		}
	}
}
