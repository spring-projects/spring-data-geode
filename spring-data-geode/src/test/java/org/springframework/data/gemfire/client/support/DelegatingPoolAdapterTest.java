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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.cache.query.QueryService;

import org.springframework.data.gemfire.GemfireUtils;

/**
 * Unit Tests for {@link DelegatingPoolAdapter}.
 *
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolFactory
 * @see org.apache.geode.cache.client.SocketFactory
 * @see org.apache.geode.cache.query.QueryService
 * @see org.springframework.data.gemfire.client.support.DelegatingPoolAdapter
 * @since 1.8.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingPoolAdapterTest {

	@Mock
	private Pool mockPool;

	@Mock
	private QueryService mockQueryService;

	@Mock
	private SocketFactory mockSocketFactory;

	private InetSocketAddress newSocketAddress(String host, int port) {
		return new InetSocketAddress(host, port);
	}

	@Before
	public void setup() {

		when(this.mockPool.isDestroyed()).thenReturn(false);
		when(this.mockPool.getFreeConnectionTimeout()).thenReturn(10000);
		when(this.mockPool.getIdleTimeout()).thenReturn(120000L);
		when(this.mockPool.getLoadConditioningInterval()).thenReturn(300000);
		when(this.mockPool.getLocators()).thenReturn(Collections.singletonList(newSocketAddress("skullbox", 11235)));
		when(this.mockPool.getMaxConnections()).thenReturn(500);
		when(this.mockPool.getMinConnections()).thenReturn(50);
		when(this.mockPool.getMultiuserAuthentication()).thenReturn(true);
		when(this.mockPool.getName()).thenReturn("MockPool");
		when(this.mockPool.getOnlineLocators()).thenReturn(Collections.singletonList(newSocketAddress("trinity", 10101)));
		when(this.mockPool.getPendingEventCount()).thenReturn(2);
		when(this.mockPool.getPingInterval()).thenReturn(15000L);
		when(this.mockPool.getPRSingleHopEnabled()).thenReturn(true);
		when(this.mockPool.getQueryService()).thenReturn(this.mockQueryService);
		when(this.mockPool.getReadTimeout()).thenReturn(30000);
		when(this.mockPool.getRetryAttempts()).thenReturn(1);
		when(this.mockPool.getServerConnectionTimeout()).thenReturn(10000);
		when(this.mockPool.getServerGroup()).thenReturn("TestGroup");
		when(this.mockPool.getServers()).thenReturn(Collections.singletonList(newSocketAddress("xghost", 12480)));
		when(this.mockPool.getSocketBufferSize()).thenReturn(16384);
		when(this.mockPool.getSocketConnectTimeout()).thenReturn(5000);
		when(this.mockPool.getSocketFactory()).thenReturn(this.mockSocketFactory);
		when(this.mockPool.getStatisticInterval()).thenReturn(1000);
		when(this.mockPool.getSubscriptionAckInterval()).thenReturn(200);
		when(this.mockPool.getSubscriptionEnabled()).thenReturn(true);
		when(this.mockPool.getSubscriptionMessageTrackingTimeout()).thenReturn(60000);
		when(this.mockPool.getSubscriptionRedundancy()).thenReturn(2);
		when(this.mockPool.getSubscriptionTimeoutMultiplier()).thenReturn(3);
		when(this.mockPool.getThreadLocalConnections()).thenReturn(false);
	}

	@Test
	public void delegateEqualsMockPool() {
		assertThat(DelegatingPoolAdapter.from(this.mockPool).getDelegate()).isEqualTo(this.mockPool);
	}

	@Test
	public void mockPoolDelegateUsesMockPool() {

		Pool pool = DelegatingPoolAdapter.from(this.mockPool);

		assertThat(pool.isDestroyed()).isFalse();
		assertThat(pool.getFreeConnectionTimeout()).isEqualTo(10000);
		assertThat(pool.getIdleTimeout()).isEqualTo(120000L);
		assertThat(pool.getLoadConditioningInterval()).isEqualTo(300000);
		assertThat(pool.getMaxConnections()).isEqualTo(500);
		assertThat(pool.getMinConnections()).isEqualTo(50);
		assertThat(pool.getMultiuserAuthentication()).isTrue();
		assertThat(pool.getLocators()).isEqualTo(Collections.singletonList(newSocketAddress("skullbox", 11235)));
		assertThat(pool.getName()).isEqualTo("MockPool");
		assertThat(pool.getOnlineLocators()).isEqualTo(Collections.singletonList(newSocketAddress("trinity", 10101)));
		assertThat(pool.getPendingEventCount()).isEqualTo(2);
		assertThat(pool.getPingInterval()).isEqualTo(15000L);
		assertThat(pool.getPRSingleHopEnabled()).isTrue();
		assertThat(pool.getQueryService()).isEqualTo(this.mockQueryService);
		assertThat(pool.getReadTimeout()).isEqualTo(30000);
		assertThat(pool.getRetryAttempts()).isEqualTo(1);
		assertThat(pool.getServerConnectionTimeout()).isEqualTo(10000);
		assertThat(pool.getServerGroup()).isEqualTo("TestGroup");
		assertThat(pool.getServers()).isEqualTo(Collections.singletonList(newSocketAddress("xghost", 12480)));
		assertThat(pool.getSocketBufferSize()).isEqualTo(16384);
		assertThat(pool.getSocketConnectTimeout()).isEqualTo(5000);
		assertThat(pool.getSocketFactory()).isEqualTo(this.mockSocketFactory);
		assertThat(pool.getStatisticInterval()).isEqualTo(1000);
		assertThat(pool.getSubscriptionAckInterval()).isEqualTo(200);
		assertThat(pool.getSubscriptionEnabled()).isTrue();
		assertThat(pool.getSubscriptionMessageTrackingTimeout()).isEqualTo(60000);
		assertThat(pool.getSubscriptionRedundancy()).isEqualTo(2);
		assertThat(pool.getSubscriptionTimeoutMultiplier()).isEqualTo(3);
		assertThat(pool.getThreadLocalConnections()).isFalse();

		verify(this.mockPool, times(1)).isDestroyed();
		verify(this.mockPool, times(1)).getFreeConnectionTimeout();
		verify(this.mockPool, times(1)).getIdleTimeout();
		verify(this.mockPool, times(1)).getLoadConditioningInterval();
		verify(this.mockPool, times(1)).getLocators();
		verify(this.mockPool, times(1)).getMaxConnections();
		verify(this.mockPool, times(1)).getMinConnections();
		verify(this.mockPool, times(1)).getMultiuserAuthentication();
		verify(this.mockPool, times(1)).getName();
		verify(this.mockPool, times(1)).getPendingEventCount();
		verify(this.mockPool, times(1)).getPingInterval();
		verify(this.mockPool, times(1)).getPRSingleHopEnabled();
		verify(this.mockPool, times(1)).getQueryService();
		verify(this.mockPool, times(1)).getReadTimeout();
		verify(this.mockPool, times(1)).getRetryAttempts();
		verify(this.mockPool, times(1)).getServerConnectionTimeout();
		verify(this.mockPool, times(1)).getServerGroup();
		verify(this.mockPool, times(1)).getServers();
		verify(this.mockPool, times(1)).getSocketBufferSize();
		verify(this.mockPool, times(1)).getSocketConnectTimeout();
		verify(this.mockPool, times(1)).getSocketFactory();
		verify(this.mockPool, times(1)).getStatisticInterval();
		verify(this.mockPool, times(1)).getSubscriptionAckInterval();
		verify(this.mockPool, times(1)).getSubscriptionEnabled();
		verify(this.mockPool, times(1)).getSubscriptionMessageTrackingTimeout();
		verify(this.mockPool, times(1)).getSubscriptionRedundancy();
		verify(this.mockPool, times(1)).getSubscriptionTimeoutMultiplier();
		verify(this.mockPool, times(1)).getThreadLocalConnections();
	}

	@Test
	public void destroyUsingDelegateCallsDestroy() {
		DelegatingPoolAdapter.from(this.mockPool).destroy();
		verify(this.mockPool, times(1)).destroy();
	}

	@Test
	public void destroyWithKeepAliveUsingDelegateCallsDestroy() {
		DelegatingPoolAdapter.from(this.mockPool).destroy(true);
		verify(this.mockPool, times(1)).destroy(eq(true));
	}

	@Test
	public void releaseThreadLocalConnectionWithDelegateCallsReleaseThreadLocalConnection() {
		DelegatingPoolAdapter.from(this.mockPool).releaseThreadLocalConnection();
		verify(this.mockPool, times(1)).releaseThreadLocalConnection();
	}

	@Test
	public void nullDelegateUsesDefaultFactorySettings() {

		Pool pool = DelegatingPoolAdapter.from(null);

		assertThat(pool.getFreeConnectionTimeout()).isEqualTo(PoolFactory.DEFAULT_FREE_CONNECTION_TIMEOUT);
		assertThat(pool.getIdleTimeout()).isEqualTo(PoolFactory.DEFAULT_IDLE_TIMEOUT);
		assertThat(pool.getLoadConditioningInterval()).isEqualTo(PoolFactory.DEFAULT_LOAD_CONDITIONING_INTERVAL);
		assertThat(pool.getMaxConnections()).isEqualTo(PoolFactory.DEFAULT_MAX_CONNECTIONS);
		assertThat(pool.getMinConnections()).isEqualTo(PoolFactory.DEFAULT_MIN_CONNECTIONS);
		assertThat(pool.getMultiuserAuthentication()).isEqualTo(PoolFactory.DEFAULT_MULTIUSER_AUTHENTICATION);
		assertThat(pool.getOnlineLocators()).isEqualTo(Collections.EMPTY_LIST);
		assertThat(pool.getPingInterval()).isEqualTo(PoolFactory.DEFAULT_PING_INTERVAL);
		assertThat(pool.getPRSingleHopEnabled()).isEqualTo(PoolFactory.DEFAULT_PR_SINGLE_HOP_ENABLED);
		assertThat(pool.getReadTimeout()).isEqualTo(PoolFactory.DEFAULT_READ_TIMEOUT);
		assertThat(pool.getRetryAttempts()).isEqualTo(PoolFactory.DEFAULT_RETRY_ATTEMPTS);
		assertThat(pool.getServerConnectionTimeout()).isEqualTo(PoolFactory.DEFAULT_SERVER_CONNECTION_TIMEOUT);
		assertThat(pool.getServerGroup()).isEqualTo(PoolFactory.DEFAULT_SERVER_GROUP);
		assertThat(pool.getSocketBufferSize()).isEqualTo(PoolFactory.DEFAULT_SOCKET_BUFFER_SIZE);
		assertThat(pool.getSocketConnectTimeout()).isEqualTo(PoolFactory.DEFAULT_SOCKET_CONNECT_TIMEOUT);
		assertThat(pool.getSocketFactory()).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
		assertThat(pool.getStatisticInterval()).isEqualTo(PoolFactory.DEFAULT_STATISTIC_INTERVAL);
		assertThat(pool.getSubscriptionAckInterval()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_ACK_INTERVAL);
		assertThat(pool.getSubscriptionEnabled()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_ENABLED);
		assertThat(pool.getSubscriptionMessageTrackingTimeout()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_MESSAGE_TRACKING_TIMEOUT);
		assertThat(pool.getSubscriptionRedundancy()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_REDUNDANCY);
		assertThat(pool.getSubscriptionTimeoutMultiplier()).isEqualTo(PoolFactory.DEFAULT_SUBSCRIPTION_TIMEOUT_MULTIPLIER);
		assertThat(pool.getThreadLocalConnections()).isEqualTo(PoolFactory.DEFAULT_THREAD_LOCAL_CONNECTIONS);

		verifyNoInteractions(this.mockPool);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void destroyedWithNullIsUnsupported() {

		try {
			DelegatingPoolAdapter.from(null).isDestroyed();
		}
		catch (UnsupportedOperationException expected) {

			assertThat(expected).hasMessage(DelegatingPoolAdapter.NOT_IMPLEMENTED);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void locatorsWithNullIsEqualToEmptyList() {
		assertThat(DelegatingPoolAdapter.from(null).getLocators())
			.isEqualTo(Collections.<InetSocketAddress>emptyList());
	}

	@Test
	public void nameWithNullIsEqualToDefault() {
		assertThat(DelegatingPoolAdapter.from(null).getName()).isEqualTo(DelegatingPoolAdapter.DEFAULT_POOL_NAME);
	}

	@Test
	public void pendingEventCountWithNullIsEqualToZero() {
		assertThat(DelegatingPoolAdapter.from(null).getPendingEventCount()).isEqualTo(0);
	}

	@Test
	public void queryServiceWithNullIsNull() {
		assertThat(DelegatingPoolAdapter.from(null).getQueryService()).isNull();
	}

	@Test
	public void socketFactoryWithNullIsEqualToDefaultSocketFactory() {
		assertThat(DelegatingPoolAdapter.from(null).getSocketFactory()).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
	}

	@Test
	public void serversWithNullIsEqualToLocalhostListeningOnDefaultCacheServerPort() {
		assertThat(DelegatingPoolAdapter.from(null).getServers()).isEqualTo(Collections.singletonList(
			newSocketAddress("localhost", GemfireUtils.DEFAULT_CACHE_SERVER_PORT)));
	}

	@Test
	public void destroyWithNullIsNoOp() {
		DelegatingPoolAdapter.from(null).destroy();
		verify(this.mockPool, never()).destroy();
	}

	@Test
	public void destroyWithKeepAliveUsingNullIsNoOp() {
		DelegatingPoolAdapter.from(null).destroy(false);
		verify(this.mockPool, never()).destroy(anyBoolean());
	}

	@Test
	public void releaseThreadLocalConnectionWithNullIsNoOp() {
		DelegatingPoolAdapter.from(null).releaseThreadLocalConnection();
		verify(this.mockPool, never()).releaseThreadLocalConnection();
	}
}
