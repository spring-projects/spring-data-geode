/*
 * Copyright 2012-2020 the original author or authors.
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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
 * Unit Tests for {@link DefaultableDelegatingPoolAdapter}.
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
 * @see org.springframework.data.gemfire.client.support.DefaultableDelegatingPoolAdapter
 * @since 1.8.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultableDelegatingPoolAdapterTest {

	private DefaultableDelegatingPoolAdapter poolAdapter;

	@Mock
	private Pool mockPool;

	@Mock
	private QueryService mockQueryService;

	@Mock
	private SocketFactory mockSocketFactory;

	@Mock
	@SuppressWarnings("rawtypes")
	private Supplier mockSupplier;

	private static InetSocketAddress newSocketAddress(String host, int port) {
		return new InetSocketAddress(host, port);
	}

	@Before
	public void setupMockPool() {

		when(this.mockPool.getFreeConnectionTimeout()).thenReturn(5000);
		when(this.mockPool.getIdleTimeout()).thenReturn(120000L);
		when(this.mockPool.getLoadConditioningInterval()).thenReturn(300000);
		when(this.mockPool.getLocators()).thenReturn(Collections.emptyList());
		when(this.mockPool.getMaxConnections()).thenReturn(500);
		when(this.mockPool.getMinConnections()).thenReturn(50);
		when(this.mockPool.getMultiuserAuthentication()).thenReturn(true);
		when(this.mockPool.getName()).thenReturn("TestPool");
		when(this.mockPool.getPendingEventCount()).thenReturn(2);
		when(this.mockPool.getPingInterval()).thenReturn(15000L);
		when(this.mockPool.getPRSingleHopEnabled()).thenReturn(true);
		when(this.mockPool.getQueryService()).thenReturn(null);
		when(this.mockPool.getReadTimeout()).thenReturn(30000);
		when(this.mockPool.getRetryAttempts()).thenReturn(1);
		when(this.mockPool.getServerConnectionTimeout()).thenReturn(10000);
		when(this.mockPool.getServerGroup()).thenReturn("TestGroup");
		when(this.mockPool.getServers()).thenReturn(Collections.singletonList(
			newSocketAddress("localhost", GemfireUtils.DEFAULT_CACHE_SERVER_PORT)));
		when(this.mockPool.getSocketBufferSize()).thenReturn(16384);
		when(this.mockPool.getSocketConnectTimeout()).thenReturn(5000);
		when(this.mockPool.getSocketFactory()).thenReturn(PoolFactory.DEFAULT_SOCKET_FACTORY);
		when(this.mockPool.getStatisticInterval()).thenReturn(1000);
		when(this.mockPool.getSubscriptionAckInterval()).thenReturn(200);
		when(this.mockPool.getSubscriptionEnabled()).thenReturn(true);
		when(this.mockPool.getSubscriptionMessageTrackingTimeout()).thenReturn(20000);
		when(this.mockPool.getSubscriptionRedundancy()).thenReturn(2);
		when(this.mockPool.getSubscriptionTimeoutMultiplier()).thenReturn(3);
		when(this.mockPool.getThreadLocalConnections()).thenReturn(false);
	}

	@Before
	public void setupPoolAdapter() {
		this.poolAdapter = DefaultableDelegatingPoolAdapter.from(this.mockPool);
	}

	@Test
	public void fromMockPool() {
		assertThat(this.poolAdapter.getDelegate()).isSameAs(this.mockPool);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromNull() {

		try {
			DefaultableDelegatingPoolAdapter.from(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pool delegate must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void prefersDefaultsToPoolDelegateAndIsMutable() {

		assertThat(this.poolAdapter.getPreference()).isEqualTo(DefaultableDelegatingPoolAdapter.Preference.PREFER_POOL);
		assertThat(this.poolAdapter.prefersDefault()).isFalse();
		assertThat(this.poolAdapter.prefersPool()).isTrue();

		this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter.getPreference()).isEqualTo(DefaultableDelegatingPoolAdapter.Preference.PREFER_DEFAULT);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.prefersPool()).isFalse();

		this.poolAdapter.preferPool();

		assertThat(this.poolAdapter.getPreference()).isEqualTo(DefaultableDelegatingPoolAdapter.Preference.PREFER_POOL);
		assertThat(this.poolAdapter.prefersDefault()).isFalse();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfNullWhenPrefersDefaultUsesDefault() {

		this.poolAdapter = this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.defaultIfNull("default", this.mockSupplier)).isEqualTo("default");

		verifyNoInteractions(this.mockSupplier);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfNullWhenPrefersDefaultUsesPoolValueWhenDefaultIsNull() {

		when(this.mockSupplier.get()).thenReturn("pool");

		this.poolAdapter = this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.defaultIfNull(null, this.mockSupplier)).isEqualTo("pool");

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfNullWhenPrefersPoolUsesPoolValue() {

		when(mockSupplier.get()).thenReturn("pool");

		this.poolAdapter = this.poolAdapter.preferPool();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
		assertThat(this.poolAdapter.defaultIfNull("default", this.mockSupplier)).isEqualTo("pool");

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfNullWhenPrefersPoolUsesDefaultWhenPoolValueIsNull() {

		when(this.mockSupplier.get()).thenReturn(null);

		this.poolAdapter = this.poolAdapter.preferPool();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
		assertThat(this.poolAdapter.defaultIfNull("default", this.mockSupplier)).isEqualTo("default");

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersDefaultUsesDefault() {

		List<Object> defaultList = Collections.singletonList("default");

		this.poolAdapter = this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat((List<Object>) this.poolAdapter.defaultIfEmpty(defaultList, this.mockSupplier)).isEqualTo(defaultList);

		verifyNoInteractions(this.mockSupplier);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersDefaultUsesPoolValueWhenDefaultIsNull() {

		List<Object> poolList = Collections.singletonList("pool");

		when(this.mockSupplier.get()).thenReturn(poolList);

		this.poolAdapter = this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat((List<Object>) this.poolAdapter.defaultIfEmpty(null, this.mockSupplier)).isEqualTo(poolList);

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersDefaultUsesPoolValueWhenDefaultIsEmpty() {

		List<Object> poolList = Collections.singletonList("pool");

		when(this.mockSupplier.get()).thenReturn(poolList);

		this.poolAdapter = this.poolAdapter.preferDefault();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat((List<Object>) this.poolAdapter.defaultIfEmpty(Collections.emptyList(), this.mockSupplier)).isEqualTo(poolList);

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersPoolUsesPoolValue() {

		List<Object> poolList = Collections.singletonList("pool");

		when(this.mockSupplier.get()).thenReturn(poolList);

		this.poolAdapter = this.poolAdapter.preferPool();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
		assertThat((List<Object>) this.poolAdapter
			.defaultIfEmpty(Collections.<Object>singletonList("default"), this.mockSupplier)).isEqualTo(poolList);

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersPoolUsesDefaultWhenPoolValueIsNull() {

		List<Object> defaultList = Collections.singletonList("default");

		when(this.mockSupplier.get()).thenReturn(null);

		this.poolAdapter = this.poolAdapter.preferPool();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
		assertThat((List<Object>) this.poolAdapter.defaultIfEmpty(defaultList, this.mockSupplier))
			.isEqualTo(defaultList);

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultIfEmptyWhenPrefersPoolUsesDefaultWhenPoolValueIsEmpty() {

		List<Object> defaultList = Collections.singletonList("default");

		when(this.mockSupplier.get()).thenReturn(Collections.emptyList());

		this.poolAdapter = this.poolAdapter.preferPool();

		assertThat(this.poolAdapter).isNotNull();
		assertThat(this.poolAdapter.prefersPool()).isTrue();
		assertThat((List<Object>) this.poolAdapter.defaultIfEmpty(defaultList, this.mockSupplier))
			.isEqualTo(defaultList);

		verify(this.mockSupplier, times(1)).get();
	}

	@Test
	public void poolAdapterPreferringDefaultsUsesNonNullDefaults() {

		assertThat(this.poolAdapter.preferDefault()).isSameAs(this.poolAdapter);

		List<InetSocketAddress> defaultLocator = Collections.singletonList(newSocketAddress("boombox", 21668));
		List<InetSocketAddress> defaultServer = Collections.singletonList(newSocketAddress("skullbox", 42424));

		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.getFreeConnectionTimeout(10000)).isEqualTo(10000);
		assertThat(this.poolAdapter.getIdleTimeout(300000L)).isEqualTo(300000L);
		assertThat(this.poolAdapter.getLoadConditioningInterval(60000)).isEqualTo(60000);
		assertThat(this.poolAdapter.getLocators(defaultLocator)).isEqualTo(defaultLocator);
		assertThat(this.poolAdapter.getMaxConnections(100)).isEqualTo(100);
		assertThat(this.poolAdapter.getMinConnections(10)).isEqualTo(10);
		assertThat(this.poolAdapter.getMultiuserAuthentication(false)).isFalse();
		assertThat(this.poolAdapter.getName()).isEqualTo("TestPool");
		assertThat(this.poolAdapter.getPendingEventCount()).isEqualTo(2);
		assertThat(this.poolAdapter.getPingInterval(20000L)).isEqualTo(20000L);
		assertThat(this.poolAdapter.getPRSingleHopEnabled(false)).isFalse();
		assertThat(this.poolAdapter.getQueryService(this.mockQueryService)).isEqualTo(this.mockQueryService);
		assertThat(this.poolAdapter.getReadTimeout(20000)).isEqualTo(20000);
		assertThat(this.poolAdapter.getRetryAttempts(2)).isEqualTo(2);
		assertThat(this.poolAdapter.getServerConnectionTimeout(123)).isEqualTo(123);
		assertThat(this.poolAdapter.getServerGroup("MockGroup")).isEqualTo("MockGroup");
		assertThat(this.poolAdapter.getServers(defaultServer)).isEqualTo(defaultServer);
		assertThat(this.poolAdapter.getSocketBufferSize(8192)).isEqualTo(8192);
		assertThat(this.poolAdapter.getSocketConnectTimeout(10000)).isEqualTo(10000);
		assertThat(this.poolAdapter.getSocketFactory(this.mockSocketFactory)).isEqualTo(this.mockSocketFactory);
		assertThat(this.poolAdapter.getStatisticInterval(2000)).isEqualTo(2000);
		assertThat(this.poolAdapter.getSubscriptionAckInterval(50)).isEqualTo(50);
		assertThat(this.poolAdapter.getSubscriptionEnabled(false)).isFalse();
		assertThat(this.poolAdapter.getSubscriptionMessageTrackingTimeout(15000)).isEqualTo(15000);
		assertThat(this.poolAdapter.getSubscriptionRedundancy(1)).isEqualTo(1);
		assertThat(this.poolAdapter.getSubscriptionTimeoutMultiplier(1)).isEqualTo(1);
		assertThat(this.poolAdapter.getThreadLocalConnections(true)).isTrue();

		verify(this.mockPool, times(1)).getName();
		verify(this.mockPool, times(1)).getPendingEventCount();
		verifyNoMoreInteractions(this.mockPool);
	}

	@Test
	public void poolAdapterPreferringDefaultsUsesPoolValuesWhenSomeDefaultValuesAreNull() {

		assertThat(this.poolAdapter.preferDefault()).isSameAs(this.poolAdapter);

		List<InetSocketAddress> defaultLocator = Collections.singletonList(newSocketAddress("boombox", 21668));
		List<InetSocketAddress> poolServer = Collections.singletonList(newSocketAddress("localhost", 40404));

		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.getFreeConnectionTimeout(null)).isEqualTo(5000);
		assertThat(this.poolAdapter.getIdleTimeout(null)).isEqualTo(120000L);
		assertThat(this.poolAdapter.getLoadConditioningInterval(60000)).isEqualTo(60000);
		assertThat(this.poolAdapter.getLocators(defaultLocator)).isEqualTo(defaultLocator);
		assertThat(this.poolAdapter.getMaxConnections(null)).isEqualTo(500);
		assertThat(this.poolAdapter.getMinConnections(50)).isEqualTo(50);
		assertThat(this.poolAdapter.getMultiuserAuthentication(null)).isTrue();
		assertThat(this.poolAdapter.getName()).isEqualTo("TestPool");
		assertThat(this.poolAdapter.getPendingEventCount()).isEqualTo(2);
		assertThat(this.poolAdapter.getPingInterval(null)).isEqualTo(15000L);
		assertThat(this.poolAdapter.getPRSingleHopEnabled(true)).isTrue();
		assertThat(this.poolAdapter.getQueryService(null)).isNull();
		assertThat(this.poolAdapter.getReadTimeout(20000)).isEqualTo(20000);
		assertThat(this.poolAdapter.getRetryAttempts(null)).isEqualTo(1);
		assertThat(this.poolAdapter.getServerConnectionTimeout(null)).isEqualTo(10000);
		assertThat(this.poolAdapter.getServerGroup("MockGroup")).isEqualTo("MockGroup");
		assertThat(this.poolAdapter.getServers(null)).isEqualTo(poolServer);
		assertThat(this.poolAdapter.getSocketBufferSize(32768)).isEqualTo(32768);
		assertThat(this.poolAdapter.getSocketConnectTimeout(null)).isEqualTo(5000);
		assertThat(this.poolAdapter.getSocketFactory(null)).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
		assertThat(this.poolAdapter.getStatisticInterval(null)).isEqualTo(1000);
		assertThat(this.poolAdapter.getSubscriptionAckInterval(50)).isEqualTo(50);
		assertThat(this.poolAdapter.getSubscriptionEnabled(true)).isTrue();
		assertThat(this.poolAdapter.getSubscriptionMessageTrackingTimeout(null)).isEqualTo(20000);
		assertThat(this.poolAdapter.getSubscriptionRedundancy(1)).isEqualTo(1);
		assertThat(this.poolAdapter.getSubscriptionTimeoutMultiplier(null)).isEqualTo(3);
		assertThat(this.poolAdapter.getThreadLocalConnections(null)).isFalse();

		verify(this.mockPool, times(1)).getFreeConnectionTimeout();
		verify(this.mockPool, times(1)).getIdleTimeout();
		verify(this.mockPool, times(1)).getMaxConnections();
		verify(this.mockPool, times(1)).getMultiuserAuthentication();
		verify(this.mockPool, times(1)).getName();
		verify(this.mockPool, times(1)).getPendingEventCount();
		verify(this.mockPool, times(1)).getPingInterval();
		verify(this.mockPool, times(1)).getQueryService();
		verify(this.mockPool, times(1)).getRetryAttempts();
		verify(this.mockPool, times(1)).getServerConnectionTimeout();
		verify(this.mockPool, times(1)).getServers();
		verify(this.mockPool, times(1)).getSocketConnectTimeout();
		verify(this.mockPool, times(1)).getSocketFactory();
		verify(this.mockPool, times(1)).getStatisticInterval();
		verify(this.mockPool, times(1)).getSubscriptionMessageTrackingTimeout();
		verify(this.mockPool, times(1)).getSubscriptionTimeoutMultiplier();
		verify(this.mockPool, times(1)).getThreadLocalConnections();
		verifyNoMoreInteractions(this.mockPool);
	}

	@Test
	public void poolAdapterPreferringDefaultsUsesPoolValuesExclusivelyWhenAllDefaultValuesAreNull() {

		assertThat(this.poolAdapter.preferDefault()).isSameAs(this.poolAdapter);

		List<InetSocketAddress> poolServer = Collections.singletonList(newSocketAddress("localhost", 40404));

		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();
		assertThat(this.poolAdapter.getFreeConnectionTimeout(null)).isEqualTo(5000);
		assertThat(this.poolAdapter.getIdleTimeout(null)).isEqualTo(120000L);
		assertThat(this.poolAdapter.getLoadConditioningInterval(null)).isEqualTo(300000);
		assertThat(this.poolAdapter.getLocators(null)).isEqualTo(Collections.<InetSocketAddress>emptyList());
		assertThat(this.poolAdapter.getMaxConnections(null)).isEqualTo(500);
		assertThat(this.poolAdapter.getMinConnections(null)).isEqualTo(50);
		assertThat(this.poolAdapter.getMultiuserAuthentication(null)).isTrue();
		assertThat(this.poolAdapter.getName()).isEqualTo("TestPool");
		assertThat(this.poolAdapter.getPendingEventCount()).isEqualTo(2);
		assertThat(this.poolAdapter.getPingInterval(null)).isEqualTo(15000L);
		assertThat(this.poolAdapter.getPRSingleHopEnabled(null)).isTrue();
		assertThat(this.poolAdapter.getQueryService(null)).isNull();
		assertThat(this.poolAdapter.getReadTimeout(null)).isEqualTo(30000);
		assertThat(this.poolAdapter.getRetryAttempts(null)).isEqualTo(1);
		assertThat(this.poolAdapter.getServerConnectionTimeout(null)).isEqualTo(10000);
		assertThat(this.poolAdapter.getServerGroup(null)).isEqualTo("TestGroup");
		assertThat(this.poolAdapter.getServers(null)).isEqualTo(poolServer);
		assertThat(this.poolAdapter.getSocketBufferSize(null)).isEqualTo(16384);
		assertThat(this.poolAdapter.getSocketConnectTimeout(null)).isEqualTo(5000);
		assertThat(this.poolAdapter.getSocketFactory(null)).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
		assertThat(this.poolAdapter.getStatisticInterval(null)).isEqualTo(1000);
		assertThat(this.poolAdapter.getSubscriptionAckInterval(null)).isEqualTo(200);
		assertThat(this.poolAdapter.getSubscriptionEnabled(null)).isTrue();
		assertThat(this.poolAdapter.getSubscriptionMessageTrackingTimeout(null)).isEqualTo(20000);
		assertThat(this.poolAdapter.getSubscriptionRedundancy(null)).isEqualTo(2);
		assertThat(this.poolAdapter.getSubscriptionTimeoutMultiplier(null)).isEqualTo(3);
		assertThat(this.poolAdapter.getThreadLocalConnections(null)).isFalse();

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
		verifyNoMoreInteractions(this.mockPool);
	}

	@Test
	public void poolAdapterPreferringPoolUsesUseNonNullPoolValues() {

		assertThat(this.poolAdapter.preferPool()).isSameAs(this.poolAdapter);

		List<InetSocketAddress> defaultServer = Collections.singletonList(newSocketAddress("jambox", 12480));
		List<InetSocketAddress> poolServer = Collections.singletonList(newSocketAddress("localhost", 40404));

		assertThat(this.poolAdapter.getFreeConnectionTimeout(15000)).isEqualTo(5000);
		assertThat(this.poolAdapter.getIdleTimeout(60000L)).isEqualTo(120000L);
		assertThat(this.poolAdapter.getLoadConditioningInterval(180000)).isEqualTo(300000);
		assertThat(this.poolAdapter.getLocators(Collections.emptyList()))
			.isEqualTo(Collections.<InetSocketAddress>emptyList());
		assertThat(this.poolAdapter.getMaxConnections(999)).isEqualTo(500);
		assertThat(this.poolAdapter.getMinConnections(99)).isEqualTo(50);
		assertThat(this.poolAdapter.getMultiuserAuthentication(false)).isTrue();
		assertThat(this.poolAdapter.getName()).isEqualTo("TestPool");
		assertThat(this.poolAdapter.getPendingEventCount()).isEqualTo(2);
		assertThat(this.poolAdapter.getPingInterval(20000L)).isEqualTo(15000L);
		assertThat(this.poolAdapter.getPRSingleHopEnabled(false)).isTrue();
		assertThat(this.poolAdapter.getQueryService(null)).isNull();
		assertThat(this.poolAdapter.getReadTimeout(20000)).isEqualTo(30000);
		assertThat(this.poolAdapter.getRetryAttempts(4)).isEqualTo(1);
		assertThat(this.poolAdapter.getServerConnectionTimeout(12345)).isEqualTo(10000);
		assertThat(this.poolAdapter.getServerGroup("MockGroup")).isEqualTo("TestGroup");
		assertThat(this.poolAdapter.getServers(defaultServer)).isEqualTo(poolServer);
		assertThat(this.poolAdapter.getSocketBufferSize(8192)).isEqualTo(16384);
		assertThat(this.poolAdapter.getSocketConnectTimeout(8192)).isEqualTo(5000);
		assertThat(this.poolAdapter.getSocketFactory(this.mockSocketFactory)).isEqualTo(PoolFactory.DEFAULT_SOCKET_FACTORY);
		assertThat(this.poolAdapter.getStatisticInterval(2000)).isEqualTo(1000);
		assertThat(this.poolAdapter.getSubscriptionAckInterval(50)).isEqualTo(200);
		assertThat(this.poolAdapter.getSubscriptionEnabled(false)).isTrue();
		assertThat(this.poolAdapter.getSubscriptionMessageTrackingTimeout(30000)).isEqualTo(20000);
		assertThat(this.poolAdapter.getSubscriptionRedundancy(1)).isEqualTo(2);
		assertThat(this.poolAdapter.getSubscriptionTimeoutMultiplier(2)).isEqualTo(3);
		assertThat(this.poolAdapter.getThreadLocalConnections(true)).isFalse();

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
		verifyNoMoreInteractions(this.mockPool);
	}

	@Test
	public void poolAdapterDestroyUsesPoolRegardlessOfPreference() {

		assertThat(this.poolAdapter.preferDefault()).isSameAs(this.poolAdapter);
		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();

		this.poolAdapter.destroy();

		assertThat(this.poolAdapter.preferPool()).isSameAs(this.poolAdapter);
		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersPool()).isTrue();

		this.poolAdapter.destroy(true);

		verify(this.mockPool, times(1)).destroy();
		verify(this.mockPool, times(1)).destroy(anyBoolean());
	}

	@Test
	public void poolAdapterReleaseThreadLocalConnections() {

		assertThat(this.poolAdapter.preferDefault()).isSameAs(this.poolAdapter);
		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersDefault()).isTrue();

		this.poolAdapter.releaseThreadLocalConnection();

		assertThat(this.poolAdapter.preferPool()).isSameAs(this.poolAdapter);
		assertThat(this.poolAdapter.getDelegate()).isEqualTo(this.mockPool);
		assertThat(this.poolAdapter.prefersPool()).isTrue();

		this.poolAdapter.releaseThreadLocalConnection();

		verify(this.mockPool, times(2)).releaseThreadLocalConnection();
	}
}
