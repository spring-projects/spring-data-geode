/*
 * Copyright 2012-2022 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.client.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.function.Supplier;

import org.junit.Test;

import org.apache.geode.cache.client.PoolFactory;

import org.springframework.data.gemfire.GemfireUtils;

/**
 * Unit Tests for {@link FactoryDefaultsPoolAdapter}.
 *
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolFactory
 * @see org.springframework.data.gemfire.client.support.FactoryDefaultsPoolAdapter
 * @since 1.8.0
 */
public class FactoryDefaultsPoolAdapterTest {

	protected static final int DEFAULT_CACHE_SERVER_PORT = GemfireUtils.DEFAULT_CACHE_SERVER_PORT;

	private FactoryDefaultsPoolAdapter poolAdapter = new FactoryDefaultsPoolAdapter() { };

	private InetSocketAddress newSocketAddress(String host, int port) {
		return new InetSocketAddress(host, port);
	}

	@Test
	public void defaultPoolAdapterConfigurationPropertiesReturnDefaultFactorySettings() {

		assertThat(this.poolAdapter.getFreeConnectionTimeout()).isEqualTo(PoolFactory.DEFAULT_FREE_CONNECTION_TIMEOUT);
		assertThat(this.poolAdapter.getIdleTimeout()).isEqualTo(PoolFactory.DEFAULT_IDLE_TIMEOUT);
		assertThat(this.poolAdapter.getLoadConditioningInterval()).isEqualTo(PoolFactory.DEFAULT_LOAD_CONDITIONING_INTERVAL);
		assertThat(this.poolAdapter.getMaxConnections()).isEqualTo(PoolFactory.DEFAULT_MAX_CONNECTIONS);
		assertThat(this.poolAdapter.getMinConnections()).isEqualTo(PoolFactory.DEFAULT_MIN_CONNECTIONS);
		assertThat(this.poolAdapter.getMultiuserAuthentication()).isEqualTo(PoolFactory.DEFAULT_MULTIUSER_AUTHENTICATION);
		assertThat(this.poolAdapter.getPRSingleHopEnabled()).isEqualTo(PoolFactory.DEFAULT_PR_SINGLE_HOP_ENABLED);
		assertThat(this.poolAdapter.getPingInterval()).isEqualTo(PoolFactory.DEFAULT_PING_INTERVAL);
		assertThat(this.poolAdapter.getReadTimeout()).isEqualTo(PoolFactory.DEFAULT_READ_TIMEOUT);
		assertThat(this.poolAdapter.getRetryAttempts()).isEqualTo(PoolFactory.DEFAULT_RETRY_ATTEMPTS);
		assertThat(this.poolAdapter.getServerConnectionTimeout()).isEqualTo(PoolFactory.DEFAULT_SERVER_CONNECTION_TIMEOUT);
		assertThat(this.poolAdapter.getServerGroup()).isEqualTo(PoolFactory.DEFAULT_SERVER_GROUP);
		assertThat(this.poolAdapter.getSocketBufferSize()).isEqualTo(PoolFactory.DEFAULT_SOCKET_BUFFER_SIZE);
		assertThat(this.poolAdapter.getSocketConnectTimeout()).isEqualTo(PoolFactory.DEFAULT_SOCKET_CONNECT_TIMEOUT);
		assertThat(this.poolAdapter.getSocketFactory()).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
		assertThat(this.poolAdapter.getStatisticInterval()).isEqualTo(PoolFactory.DEFAULT_STATISTIC_INTERVAL);
		assertThat(this.poolAdapter.getSubscriptionAckInterval()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_ACK_INTERVAL);
		assertThat(this.poolAdapter.getSubscriptionEnabled()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_ENABLED);
		assertThat(this.poolAdapter.getSubscriptionMessageTrackingTimeout()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_MESSAGE_TRACKING_TIMEOUT);
		assertThat(this.poolAdapter.getSubscriptionRedundancy()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_REDUNDANCY);
		assertThat(this.poolAdapter.getSubscriptionTimeoutMultiplier()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_TIMEOUT_MULTIPLIER);
		assertThat(this.poolAdapter.getThreadLocalConnections()).isEqualTo(PoolFactory.DEFAULT_THREAD_LOCAL_CONNECTIONS);
	}

	@Test
	public void locatorsReturnsEmptyList() {
		assertThat(this.poolAdapter.getLocators()).isEqualTo(Collections.<InetSocketAddress>emptyList());
	}

	@Test
	public void nameReturnsDefault() {
		assertThat(this.poolAdapter.getName()).isEqualTo(FactoryDefaultsPoolAdapter.DEFAULT_POOL_NAME);
	}

	@Test
	public void onlineLocatorsIsEmptyList() {
		assertThat(this.poolAdapter.getOnlineLocators()).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test
	public void queryServiceIsNull() {
		assertThat(this.poolAdapter.getQueryService()).isNull();
	}

	@Test
	public void serversReturnsLocalhostListeningOnDefaultCacheServerPort() {
		assertThat(this.poolAdapter.getServers()).isEqualTo(Collections.singletonList(
			newSocketAddress("localhost", DEFAULT_CACHE_SERVER_PORT)));
	}

	private <T> T testPoolOperationIsUnsupported(Supplier<T> poolOperation) {

		try {
			return poolOperation.get();
		}
		catch (UnsupportedOperationException expected) {

			assertThat(expected).hasMessage(FactoryDefaultsPoolAdapter.NOT_IMPLEMENTED);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void isDestroyedIsUnsupported() {
		testPoolOperationIsUnsupported(() -> this.poolAdapter.isDestroyed());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getPendingEventCountIsUnsupported() {
		testPoolOperationIsUnsupported(() -> this.poolAdapter.getPendingEventCount());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void destroyedIsUnsupported() {
		testPoolOperationIsUnsupported(() -> { this.poolAdapter.destroy(); return null; });
	}

	@Test(expected = UnsupportedOperationException.class)
	public void destroyedWithKeepAliveIsUnsupported() {
		testPoolOperationIsUnsupported(() -> { this.poolAdapter.destroy(false); return null; });
	}

	@Test(expected = UnsupportedOperationException.class)
	public void releaseThreadLocalConnectionsIsUnsupported() {
		testPoolOperationIsUnsupported(() -> { this.poolAdapter.releaseThreadLocalConnection(); return null; });
	}
}
