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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link Region sub-Region}, sub-element SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @link https://jira.springsource.org/browse/SGF-219
 * @link https://jira.springsource.org/browse/SGF-220
 * @link https://jira.springsource.org/browse/SGF-221
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class SubRegionSubElementNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("/Customers/Accounts")
	private Region<?, ?> customersAccountsRegion;

	@Autowired
	@Qualifier("/Parent/Child")
	private Region<?, ?> parentChildRegion;

	@Test
	public void testCustomersAccountsSubRegionCacheListener() {

		assertThat(customersAccountsRegion).isNotNull();
		assertThat(customersAccountsRegion.getAttributes()).isNotNull();
		assertThat(customersAccountsRegion.getAttributes().getCacheListeners()).isNotNull();

		boolean found = false;

		for (CacheListener<?, ?> listener : customersAccountsRegion.getAttributes().getCacheListeners()) {
			found |= (listener instanceof TestNoOpCacheListener);
		}

		assertThat(found)
			.as(String.format("Expected a GemFire CacheListener of type (%1$s) to be registered on Region (%2$s)!",
				TestNoOpCacheListener.class.getName(), customersAccountsRegion.getName())).isTrue();
	}

	@Test
	public void testOrderItemsSubRegionGatewaySender() {

		Region<?, ?> orderItemsRegion = requireApplicationContext().getBean("/Orders/Items", Region.class);

		assertThat(orderItemsRegion).isNotNull();
		assertThat(orderItemsRegion.getAttributes()).isNotNull();
		assertThat(orderItemsRegion.getAttributes().getGatewaySenderIds()).isNotNull();
		assertThat(orderItemsRegion.getAttributes().getGatewaySenderIds().contains("testSender")).isTrue();
	}

	@Test
	public void testParentChildSubRegionAsyncEventQueue() {

		assertThat(parentChildRegion).isNotNull();
		assertThat(parentChildRegion.getAttributes()).isNotNull();
		assertThat(parentChildRegion.getAttributes().getAsyncEventQueueIds()).isNotNull();
		assertThat(parentChildRegion.getAttributes().getAsyncEventQueueIds().contains("testQueue")).isTrue();
	}

	public static final class TestNoOpCacheListener extends CacheListenerAdapter<Object, Object> { }

	public static final class TestNoOpAsyncEventListener implements AsyncEventListener {

		@Override
		public boolean processEvents(final List<AsyncEvent> events) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public void close() { }

	}
}
