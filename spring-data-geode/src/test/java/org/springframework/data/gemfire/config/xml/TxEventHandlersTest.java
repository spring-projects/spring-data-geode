/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.TransactionEvent;
import org.apache.geode.cache.TransactionListener;
import org.apache.geode.cache.TransactionWriter;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.test.GemfireTestApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author David Turanski
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	locations="tx-listeners-and-writers.xml",
	initializers = GemfireTestApplicationContextInitializer.class
)
@SuppressWarnings("unused")
public class TxEventHandlersTest {

	@Autowired
	TestTransactionListener txListener1;

	@Autowired
	private TestTransactionListener txListener2;

	@Autowired
	private TestTransactionWriter txWriter;

	@Autowired
	private Cache cache;

	@Test
	public void transactionEventHandlersConfiguredCorrectly() {

		TransactionListener[] listeners = cache.getCacheTransactionManager().getListeners();

		assertEquals(2, listeners.length);
		assertSame(txListener1, listeners[0]);
		assertSame(txListener2, listeners[1]);
		assertSame(txWriter, cache.getCacheTransactionManager().getWriter());
	}

	public static class TestTransactionListener implements TransactionListener, BeanNameAware {

		private String name;

		public String value;

		public boolean closed;

		public boolean afterCommit;

		@Override
		public void setBeanName(String name) {
			this.name = name;
		}

		@Override
		public void afterCommit(TransactionEvent event) {
			afterCommit = true;
			value = name;
		}

		@Override
		public void afterFailedCommit(TransactionEvent event) { }

		@Override
		public void afterRollback(TransactionEvent event) { }

		@Override
		public void close() {
			closed = true;
		}
	}

	public static class TestTransactionWriter implements TransactionWriter, BeanNameAware {

		private String name;

		public String value;

		@Override
		public void setBeanName(String name) {
			this.name = name;
		}

		@Override
		public void beforeCommit(TransactionEvent event) {
			this.value = name;
		}

		@Override
		public void close() { }

	}
}
