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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import org.junit.Test;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.DistributedSystemUtils;

/**
 * Unit Tests for {@link ClientCacheFactoryBean}
 *
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @since 1.7.0
 */
public class ClientCacheFactoryBeanUnitTests {

	private Properties createProperties(String key, String value) {
		return addProperty(null, key, value);
	}

	private Properties addProperty(Properties properties, String key, String value) {

		properties = properties != null ? properties : new Properties();
		properties.setProperty(key, value);

		return properties;
	}

	private ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
	}

	@Test
	public void getObjectTypeEqualsClientCacheClass() {
		assertThat(new ClientCacheFactoryBean().getObjectType()).isEqualTo(ClientCache.class);
	}

	@Test
	public void getObjectTypeEqualsClientCacheInstanceType() {

		ClientCache mockClientCache = mock(ClientCache.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		assertThat(clientCacheFactoryBean.getObjectType()).isNotEqualTo(ClientCache.class);
		assertThat(clientCacheFactoryBean.getObjectType()).isEqualTo(mockClientCache.getClass());
		assertThat(ClientCache.class).isAssignableFrom(clientCacheFactoryBean.getObjectType());
	}

	@Test
	public void isSingleton() {
		assertThat(new ClientCacheFactoryBean().isSingleton()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolvePropertiesCallsResolvePropertiesWithSupplier() {

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		Properties properties = createProperties("key", "test");

		doReturn(properties).when(clientCacheFactoryBean).resolveProperties(isA(Supplier.class));

		assertThat(clientCacheFactoryBean.resolveProperties()).isEqualTo(properties);

		verify(clientCacheFactoryBean, times(1)).resolveProperties();
		verify(clientCacheFactoryBean, times(1)).resolveProperties(isA(Supplier.class));

		verifyNoMoreInteractions(clientCacheFactoryBean);
	}

	@Test
	public void resolvePropertiesWhenDistributedSystemIsConnected() {

		Properties gemfireProperties = createProperties("gf", "test");
		Properties distributedSystemProperties = createProperties("ds", "mock");

		DistributedSystem mockDistributedSystem = mock(DistributedSystem.class);

		doReturn(true).when(mockDistributedSystem).isConnected();
		doReturn(distributedSystemProperties).when(mockDistributedSystem).getProperties();

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		clientCacheFactoryBean.setProperties(gemfireProperties);

		Properties resolvedProperties = clientCacheFactoryBean.resolveProperties(() -> mockDistributedSystem);

		assertThat(resolvedProperties).isNotNull();
		assertThat(resolvedProperties).isNotSameAs(gemfireProperties);
		assertThat(resolvedProperties).isNotSameAs(distributedSystemProperties);
		assertThat(resolvedProperties.containsKey(DistributedSystemUtils.DURABLE_CLIENT_ID_PROPERTY_NAME)).isFalse();
		assertThat(resolvedProperties.containsKey(DistributedSystemUtils.DURABLE_CLIENT_TIMEOUT_PROPERTY_NAME)).isFalse();
		assertThat(resolvedProperties.getProperty("gf")).isEqualTo("test");
		assertThat(resolvedProperties.getProperty("ds")).isEqualTo("mock");
		assertThat(resolvedProperties.size()).isEqualTo(2);

		verify(mockDistributedSystem, times(1)).isConnected();
		verify(mockDistributedSystem, times(1)).getProperties();

		verifyNoMoreInteractions(mockDistributedSystem);
	}

	@Test
	public void resolvePropertiesWhenDistributedSystemIsConnectedAndClientIsDurable() {

		Properties gemfireProperties = DistributedSystemUtils
			.configureDurableClient(createProperties("gf", "test"), "123", 600);

		Properties distributedSystemProperties = DistributedSystemUtils
			.configureDurableClient(createProperties("ds", "mock"), "987", 300);

		DistributedSystem mockDistributedSystem = mock(DistributedSystem.class);

		doReturn(true).when(mockDistributedSystem).isConnected();
		doReturn(distributedSystemProperties).when(mockDistributedSystem).getProperties();

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		clientCacheFactoryBean.setProperties(gemfireProperties);

		Properties resolvedProperties = clientCacheFactoryBean.resolveProperties(() -> mockDistributedSystem);

		assertThat(resolvedProperties).isNotNull();
		assertThat(resolvedProperties).isNotSameAs(gemfireProperties);
		assertThat(resolvedProperties).isNotSameAs(distributedSystemProperties);
		assertThat(resolvedProperties.getProperty("gf")).isEqualTo("test");
		assertThat(resolvedProperties.getProperty("ds")).isEqualTo("mock");
		assertThat(resolvedProperties.getProperty(DistributedSystemUtils.DURABLE_CLIENT_ID_PROPERTY_NAME)).isEqualTo("123");
		assertThat(resolvedProperties.getProperty(DistributedSystemUtils.DURABLE_CLIENT_TIMEOUT_PROPERTY_NAME)).isEqualTo("600");
		assertThat(resolvedProperties.size()).isEqualTo(4);

		verify(mockDistributedSystem, times(1)).isConnected();
		verify(mockDistributedSystem, times(1)).getProperties();

		verifyNoMoreInteractions(mockDistributedSystem);
	}

	@Test
	public void resolvePropertiesWhenDistributedSystemIsDisconnected() {

		Properties gemfireProperties = createProperties("gf", "test");
		Properties distributedSystemProperties = createProperties("ds", "mock");

		DistributedSystem mockDistributedSystem = mock(DistributedSystem.class);

		doReturn(false).when(mockDistributedSystem).isConnected();
		doReturn(distributedSystemProperties).when(mockDistributedSystem).getProperties();

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		clientCacheFactoryBean.setProperties(gemfireProperties);

		Properties resolvedProperties = clientCacheFactoryBean.resolveProperties(() -> mockDistributedSystem);

		assertThat(resolvedProperties).isSameAs(gemfireProperties);

		verify(mockDistributedSystem, times(1)).isConnected();
		verify(mockDistributedSystem, never()).getProperties();

		verifyNoMoreInteractions(mockDistributedSystem);
	}

	@Test
	public void resolvePropertiesWhenDistributedSystemIsNull() {

		Properties gemfireProperties = createProperties("gf", "test");

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		clientCacheFactoryBean.setDurableClientId("123");
		clientCacheFactoryBean.setProperties(gemfireProperties);

		Properties resolvedProperties = clientCacheFactoryBean.resolveProperties(() -> null);

		assertThat(resolvedProperties).isSameAs(gemfireProperties);
		assertThat(resolvedProperties.getProperty("gf")).isEqualTo("test");
		assertThat(resolvedProperties.getProperty(DistributedSystemUtils.DURABLE_CLIENT_ID_PROPERTY_NAME)).isEqualTo("123");
		assertThat(resolvedProperties.size()).isEqualTo(2);
	}

	@Test
	public void createClientCacheFactory() {

		Properties gemfireProperties = new Properties();

		Object clientCacheFactoryReference = new ClientCacheFactoryBean().createFactory(gemfireProperties);

		assertThat(clientCacheFactoryReference).isInstanceOf(ClientCacheFactory.class);
		assertThat(gemfireProperties.isEmpty()).isTrue();

		ClientCacheFactory clientCacheFactory = (ClientCacheFactory) clientCacheFactoryReference;

		clientCacheFactory.set("testKey", "testValue");

		assertThat(gemfireProperties.containsKey("testKey")).isTrue();
		assertThat(gemfireProperties.getProperty("testKey")).isEqualTo("testValue");
	}

	@Test
	public void configureClientCacheFactoryCallsConfigurePdxAndConfigurePool() {

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCacheFactory).when(clientCacheFactoryBean).configurePdx(eq(mockClientCacheFactory));
		doReturn(mockClientCacheFactory).when(clientCacheFactoryBean).configurePool(eq(mockClientCacheFactory));

		assertThat(clientCacheFactoryBean.configureFactory(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(clientCacheFactoryBean, times(1)).configureFactory(eq(mockClientCacheFactory));
		verify(clientCacheFactoryBean, times(1)).configurePdx(eq(mockClientCacheFactory));
		verify(clientCacheFactoryBean, times(1)).configurePool(eq(mockClientCacheFactory));

		verifyNoMoreInteractions(mockClientCacheFactory);
	}

	@Test
	public void configurePdxWithAllPdxOptions() {

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		PdxSerializer mockPdxSerializer = mock(PdxSerializer.class);

		clientCacheFactoryBean.setPdxDiskStoreName("MockPdxDiskStoreName");
		clientCacheFactoryBean.setPdxIgnoreUnreadFields(false);
		clientCacheFactoryBean.setPdxPersistent(true);
		clientCacheFactoryBean.setPdxReadSerialized(false);
		clientCacheFactoryBean.setPdxSerializer(mockPdxSerializer);

		assertThat(clientCacheFactoryBean.getPdxDiskStoreName()).isEqualTo("MockPdxDiskStoreName");
		assertThat(clientCacheFactoryBean.getPdxIgnoreUnreadFields()).isFalse();
		assertThat(clientCacheFactoryBean.getPdxPersistent()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxReadSerialized()).isFalse();
		assertThat(clientCacheFactoryBean.getPdxSerializer()).isSameAs(mockPdxSerializer);

		assertThat(clientCacheFactoryBean.configurePdx(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockClientCacheFactory, times(1)).setPdxSerializer(eq(mockPdxSerializer));
		verify(mockClientCacheFactory, times(1)).setPdxDiskStore(eq("MockPdxDiskStoreName"));
		verify(mockClientCacheFactory, times(1)).setPdxIgnoreUnreadFields(eq(false));
		verify(mockClientCacheFactory, times(1)).setPdxPersistent(eq(true));
		verify(mockClientCacheFactory, times(1)).setPdxReadSerialized(eq(false));

		verifyNoMoreInteractions(mockClientCacheFactory);
	}

	@Test
	public void configurePdxWithPartialPdxOptions() {

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPdxReadSerialized(true);
		clientCacheFactoryBean.setPdxIgnoreUnreadFields(true);

		assertThat(clientCacheFactoryBean.getPdxDiskStoreName()).isNull();
		assertThat(clientCacheFactoryBean.getPdxIgnoreUnreadFields()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxPersistent()).isNull();
		assertThat(clientCacheFactoryBean.getPdxReadSerialized()).isTrue();
		assertThat(clientCacheFactoryBean.getPdxSerializer()).isNull();

		assertThat(clientCacheFactoryBean.configurePdx(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockClientCacheFactory, never()).setPdxDiskStore(anyString());
		verify(mockClientCacheFactory, times(1)).setPdxIgnoreUnreadFields(eq(true));
		verify(mockClientCacheFactory, never()).setPdxPersistent(anyBoolean());
		verify(mockClientCacheFactory, times(1)).setPdxReadSerialized(eq(true));
		verify(mockClientCacheFactory, never()).setPdxSerializer(any(PdxSerializer.class));

		verifyNoMoreInteractions(mockClientCacheFactory);
	}

	@Test
	public void configurePdxWithNoPdxOptions() {

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getPdxDiskStoreName()).isNull();
		assertThat(clientCacheFactoryBean.getPdxIgnoreUnreadFields()).isNull();
		assertThat(clientCacheFactoryBean.getPdxPersistent()).isNull();
		assertThat(clientCacheFactoryBean.getPdxReadSerialized()).isNull();
		assertThat(clientCacheFactoryBean.getPdxSerializer()).isNull();

		assertThat(clientCacheFactoryBean.configurePdx(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verifyNoInteractions(mockClientCacheFactory);
	}

	@Test
	public void configurePoolWithClientCacheFactoryBean() {

		Pool mockPool = mock(Pool.class);

		SocketFactory mockSocketFactory = mock(SocketFactory.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setFreeConnectionTimeout(5000);
		clientCacheFactoryBean.setIdleTimeout(300000L);
		clientCacheFactoryBean.setLoadConditioningInterval(120000);
		clientCacheFactoryBean.setMaxConnections(99);
		clientCacheFactoryBean.setMinConnections(9);
		clientCacheFactoryBean.setMultiUserAuthentication(true);
		clientCacheFactoryBean.setPingInterval(15000L);
		clientCacheFactoryBean.setPool(mockPool);
		clientCacheFactoryBean.setPrSingleHopEnabled(true);
		clientCacheFactoryBean.setReadTimeout(20000);
		clientCacheFactoryBean.setRetryAttempts(2);
		clientCacheFactoryBean.setServerConnectionTimeout(12345);
		clientCacheFactoryBean.setServerGroup("TestGroup");
		clientCacheFactoryBean.setSocketBufferSize(16384);
		clientCacheFactoryBean.setSocketConnectTimeout(5000);
		clientCacheFactoryBean.setSocketFactory(mockSocketFactory);
		clientCacheFactoryBean.setStatisticsInterval(1000);
		clientCacheFactoryBean.setSubscriptionAckInterval(100);
		clientCacheFactoryBean.setSubscriptionEnabled(true);
		clientCacheFactoryBean.setSubscriptionMessageTrackingTimeout(500);
		clientCacheFactoryBean.setSubscriptionRedundancy(2);
		clientCacheFactoryBean.setThreadLocalConnections(false);
		clientCacheFactoryBean.addLocators(newConnectionEndpoint("localhost", 11235),
			newConnectionEndpoint("skullbox", 10334));

		assertThat(clientCacheFactoryBean.getFreeConnectionTimeout()).isEqualTo(5000);
		assertThat(clientCacheFactoryBean.getIdleTimeout()).isEqualTo(300000L);
		assertThat(clientCacheFactoryBean.getLoadConditioningInterval()).isEqualTo(120000);
		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(2);
		assertThat(clientCacheFactoryBean.getMaxConnections()).isEqualTo(99);
		assertThat(clientCacheFactoryBean.getMinConnections()).isEqualTo(9);
		assertThat(clientCacheFactoryBean.getMultiUserAuthentication()).isTrue();
		assertThat(clientCacheFactoryBean.getPingInterval()).isEqualTo(15000L);
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getPoolName()).isNull();
		assertThat(clientCacheFactoryBean.getPrSingleHopEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getReadTimeout()).isEqualTo(20000);
		assertThat(clientCacheFactoryBean.getRetryAttempts()).isEqualTo(2);
		assertThat(clientCacheFactoryBean.getServerConnectionTimeout()).isEqualTo(12345);
		assertThat(clientCacheFactoryBean.getServerGroup()).isEqualTo("TestGroup");
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getSocketBufferSize()).isEqualTo(16384);
		assertThat(clientCacheFactoryBean.getSocketConnectTimeout()).isEqualTo(5000);
		assertThat(clientCacheFactoryBean.getSocketFactory()).isEqualTo(mockSocketFactory);
		assertThat(clientCacheFactoryBean.getStatisticsInterval()).isEqualTo(1000);
		assertThat(clientCacheFactoryBean.getSubscriptionAckInterval()).isEqualTo(100);
		assertThat(clientCacheFactoryBean.getSubscriptionEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getSubscriptionMessageTrackingTimeout()).isEqualTo(500);
		assertThat(clientCacheFactoryBean.getSubscriptionRedundancy()).isEqualTo(2);
		assertThat(clientCacheFactoryBean.getThreadLocalConnections()).isFalse();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockClientCacheFactory, times(1)).setPoolFreeConnectionTimeout(eq(5000));
		verify(mockClientCacheFactory, times(1)).setPoolIdleTimeout(eq(300000L));
		verify(mockClientCacheFactory, times(1)).setPoolLoadConditioningInterval(eq(120000));
		verify(mockClientCacheFactory, times(1)).setPoolMaxConnections(eq(99));
		verify(mockClientCacheFactory, times(1)).setPoolMinConnections(eq(9));
		verify(mockClientCacheFactory, times(1)).setPoolMultiuserAuthentication(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolPingInterval(eq(15000L));
		verify(mockClientCacheFactory, times(1)).setPoolPRSingleHopEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolReadTimeout(eq(20000));
		verify(mockClientCacheFactory, times(1)).setPoolRetryAttempts(eq(2));
		verify(mockClientCacheFactory, times(1)).setPoolServerConnectionTimeout(eq(12345));
		verify(mockClientCacheFactory, times(1)).setPoolServerGroup(eq("TestGroup"));
		verify(mockClientCacheFactory, times(1)).setPoolSocketBufferSize(eq(16384));
		verify(mockClientCacheFactory, times(1)).setPoolSocketConnectTimeout(eq(5000));
		verify(mockClientCacheFactory, times(1)).setPoolSocketFactory(eq(mockSocketFactory));
		verify(mockClientCacheFactory, times(1)).setPoolStatisticInterval(eq(1000));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionAckInterval(eq(100));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionMessageTrackingTimeout(eq(500));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionRedundancy(eq(2));
		verify(mockClientCacheFactory, times(1)).setPoolThreadLocalConnections(eq(false));
		verify(mockClientCacheFactory, times(1)).addPoolLocator(eq("localhost"), eq(11235));
		verify(mockClientCacheFactory, times(1)).addPoolLocator(eq("skullbox"), eq(10334));
		verify(mockClientCacheFactory, never()).addPoolServer(anyString(), anyInt());
		verifyNoInteractions(mockPool);
	}

	@Test
	public void configurePoolWithPool() {

		Pool mockPool = mock(Pool.class);

		SocketFactory mockSocketFactory = mock(SocketFactory.class);

		when(mockPool.getFreeConnectionTimeout()).thenReturn(10000);
		when(mockPool.getIdleTimeout()).thenReturn(120000L);
		when(mockPool.getLoadConditioningInterval()).thenReturn(30000);
		when(mockPool.getLocators()).thenReturn(Collections.emptyList());
		when(mockPool.getMaxConnections()).thenReturn(100);
		when(mockPool.getMinConnections()).thenReturn(10);
		when(mockPool.getMultiuserAuthentication()).thenReturn(true);
		when(mockPool.getPRSingleHopEnabled()).thenReturn(true);
		when(mockPool.getPingInterval()).thenReturn(15000L);
		when(mockPool.getReadTimeout()).thenReturn(20000);
		when(mockPool.getRetryAttempts()).thenReturn(1);
		when(mockPool.getServerConnectionTimeout()).thenReturn(1248);
		when(mockPool.getServerGroup()).thenReturn("TestGroup");
		when(mockPool.getSocketBufferSize()).thenReturn(8192);
		when(mockPool.getSocketConnectTimeout()).thenReturn(5000);
		when(mockPool.getSocketFactory()).thenReturn(mockSocketFactory);
		when(mockPool.getStatisticInterval()).thenReturn(5000);
		when(mockPool.getSubscriptionAckInterval()).thenReturn(500);
		when(mockPool.getSubscriptionEnabled()).thenReturn(true);
		when(mockPool.getSubscriptionMessageTrackingTimeout()).thenReturn(500);
		when(mockPool.getSubscriptionRedundancy()).thenReturn(2);
		when(mockPool.getThreadLocalConnections()).thenReturn(false);
		when(mockPool.getServers()).thenReturn(Arrays.asList(
			new InetSocketAddress("localhost", 11235), new InetSocketAddress("localhost", 12480)));

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPool(mockPool);

		assertThat(clientCacheFactoryBean.getFreeConnectionTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getIdleTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getLoadConditioningInterval()).isNull();
		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getMaxConnections()).isNull();
		assertThat(clientCacheFactoryBean.getMinConnections()).isNull();
		assertThat(clientCacheFactoryBean.getMultiUserAuthentication()).isNull();
		assertThat(clientCacheFactoryBean.getPingInterval()).isNull();
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getPoolName()).isNull();
		assertThat(clientCacheFactoryBean.getPrSingleHopEnabled()).isNull();
		assertThat(clientCacheFactoryBean.getReadTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getRetryAttempts()).isNull();
		assertThat(clientCacheFactoryBean.getServerConnectionTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getServerGroup()).isNull();
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getSocketBufferSize()).isNull();
		assertThat(clientCacheFactoryBean.getSocketConnectTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getSocketFactory()).isNull();
		assertThat(clientCacheFactoryBean.getStatisticsInterval()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionAckInterval()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionEnabled()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionMessageTrackingTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionRedundancy()).isNull();
		assertThat(clientCacheFactoryBean.getThreadLocalConnections()).isNull();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, times(1)).getFreeConnectionTimeout();
		verify(mockPool, times(1)).getIdleTimeout();
		verify(mockPool, times(1)).getLoadConditioningInterval();
		verify(mockPool, never()).getLocators();
		verify(mockPool, times(1)).getMaxConnections();
		verify(mockPool, times(1)).getMinConnections();
		verify(mockPool, times(1)).getMultiuserAuthentication();
		verify(mockPool, times(1)).getPRSingleHopEnabled();
		verify(mockPool, times(1)).getPingInterval();
		verify(mockPool, times(1)).getReadTimeout();
		verify(mockPool, times(1)).getRetryAttempts();
		verify(mockPool, times(1)).getServerConnectionTimeout();
		verify(mockPool, times(1)).getServerGroup();
		verify(mockPool, times(1)).getServers();
		verify(mockPool, times(1)).getSocketBufferSize();
		verify(mockPool, times(1)).getSocketConnectTimeout();
		verify(mockPool, times(1)).getSocketFactory();
		verify(mockPool, times(1)).getStatisticInterval();
		verify(mockPool, times(1)).getSubscriptionAckInterval();
		verify(mockPool, times(1)).getSubscriptionEnabled();
		verify(mockPool, times(1)).getSubscriptionMessageTrackingTimeout();
		verify(mockPool, times(1)).getSubscriptionRedundancy();
		verify(mockPool, times(1)).getThreadLocalConnections();
		verify(mockClientCacheFactory, times(1)).setPoolFreeConnectionTimeout(eq(10000));
		verify(mockClientCacheFactory, times(1)).setPoolIdleTimeout(eq(120000L));
		verify(mockClientCacheFactory, times(1)).setPoolLoadConditioningInterval(eq(30000));
		verify(mockClientCacheFactory, times(1)).setPoolMaxConnections(eq(100));
		verify(mockClientCacheFactory, times(1)).setPoolMinConnections(eq(10));
		verify(mockClientCacheFactory, times(1)).setPoolMultiuserAuthentication(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolPRSingleHopEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolPingInterval(eq(15000L));
		verify(mockClientCacheFactory, times(1)).setPoolReadTimeout(eq(20000));
		verify(mockClientCacheFactory, times(1)).setPoolRetryAttempts(eq(1));
		verify(mockClientCacheFactory, times(1)).setPoolServerConnectionTimeout(eq(1248));
		verify(mockClientCacheFactory, times(1)).setPoolServerGroup(eq("TestGroup"));
		verify(mockClientCacheFactory, times(1)).setPoolSocketBufferSize(eq(8192));
		verify(mockClientCacheFactory, times(1)).setPoolSocketConnectTimeout(eq(5000));
		verify(mockClientCacheFactory, times(1)).setPoolSocketFactory(eq(mockSocketFactory));
		verify(mockClientCacheFactory, times(1)).setPoolStatisticInterval(eq(5000));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionAckInterval(eq(500));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionMessageTrackingTimeout(eq(500));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionRedundancy(eq(2));
		verify(mockClientCacheFactory, times(1)).setPoolThreadLocalConnections(eq(false));
		verify(mockClientCacheFactory, times(1)).addPoolServer(eq("localhost"), eq(11235));
		verify(mockClientCacheFactory, times(1)).addPoolServer(eq("localhost"), eq(12480));
		verify(mockClientCacheFactory, never()).addPoolLocator(anyString(), anyInt());
	}

	@Test
	public void configurePoolWithClientCacheFactoryBeanAndPoolButClientCacheFactoryBeanOverridesPool() {

		Pool mockPool = mock(Pool.class);

		SocketFactory mockSocketFactory = mock(SocketFactory.class);

		when(mockPool.getFreeConnectionTimeout()).thenReturn(5000);
		when(mockPool.getIdleTimeout()).thenReturn(120000L);
		when(mockPool.getLoadConditioningInterval()).thenReturn(300000);
		when(mockPool.getLocators()).thenReturn(Collections.emptyList());
		when(mockPool.getMaxConnections()).thenReturn(200);
		when(mockPool.getMinConnections()).thenReturn(10);
		when(mockPool.getMultiuserAuthentication()).thenReturn(false);
		when(mockPool.getPingInterval()).thenReturn(15000L);
		when(mockPool.getPRSingleHopEnabled()).thenReturn(false);
		when(mockPool.getReadTimeout()).thenReturn(30000);
		when(mockPool.getRetryAttempts()).thenReturn(1);
		when(mockPool.getServerConnectionTimeout()).thenReturn(12345);
		when(mockPool.getServerGroup()).thenReturn("TestServerGroup");
		when(mockPool.getServers()).thenReturn(Collections.singletonList(new InetSocketAddress("localhost", 12480)));
		when(mockPool.getSocketBufferSize()).thenReturn(8192);
		when(mockPool.getSocketConnectTimeout()).thenReturn(5000);
		when(mockPool.getSocketFactory()).thenReturn(SocketFactory.DEFAULT);
		when(mockPool.getStatisticInterval()).thenReturn(5000);
		when(mockPool.getSubscriptionAckInterval()).thenReturn(1000);
		when(mockPool.getSubscriptionEnabled()).thenReturn(false);
		when(mockPool.getSubscriptionMessageTrackingTimeout()).thenReturn(20000);
		when(mockPool.getSubscriptionRedundancy()).thenReturn(1);
		when(mockPool.getThreadLocalConnections()).thenReturn(true);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setIdleTimeout(180000L);
		clientCacheFactoryBean.setMaxConnections(500);
		clientCacheFactoryBean.setMinConnections(50);
		clientCacheFactoryBean.setMultiUserAuthentication(true);
		clientCacheFactoryBean.setPool(mockPool);
		clientCacheFactoryBean.setPrSingleHopEnabled(true);
		clientCacheFactoryBean.setServerConnectionTimeout(1248);
		clientCacheFactoryBean.setServerGroup("TestGroup");
		clientCacheFactoryBean.setSocketBufferSize(16384);
		clientCacheFactoryBean.setSocketConnectTimeout(10000);
		clientCacheFactoryBean.setSocketFactory(mockSocketFactory);
		clientCacheFactoryBean.setStatisticsInterval(500);
		clientCacheFactoryBean.setSubscriptionAckInterval(100);
		clientCacheFactoryBean.setSubscriptionEnabled(true);
		clientCacheFactoryBean.setSubscriptionRedundancy(2);
		clientCacheFactoryBean.setThreadLocalConnections(false);
		clientCacheFactoryBean.addLocators(newConnectionEndpoint("localhost", 11235));

		assertThat(clientCacheFactoryBean.getFreeConnectionTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getIdleTimeout()).isEqualTo(180000L);
		assertThat(clientCacheFactoryBean.getLoadConditioningInterval()).isNull();
		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getMaxConnections()).isEqualTo(500);
		assertThat(clientCacheFactoryBean.getMinConnections()).isEqualTo(50);
		assertThat(clientCacheFactoryBean.getMultiUserAuthentication()).isTrue();
		assertThat(clientCacheFactoryBean.getPingInterval()).isNull();
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getPoolName()).isNull();
		assertThat(clientCacheFactoryBean.getPrSingleHopEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getReadTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getRetryAttempts()).isNull();
		assertThat(clientCacheFactoryBean.getServerConnectionTimeout()).isEqualTo(1248);
		assertThat(clientCacheFactoryBean.getServerGroup()).isEqualTo("TestGroup");
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getSocketBufferSize()).isEqualTo(16384);
		assertThat(clientCacheFactoryBean.getSocketConnectTimeout()).isEqualTo(10000);
		assertThat(clientCacheFactoryBean.getSocketFactory()).isEqualTo(mockSocketFactory);
		assertThat(clientCacheFactoryBean.getStatisticsInterval()).isEqualTo(500);
		assertThat(clientCacheFactoryBean.getSubscriptionAckInterval()).isEqualTo(100);
		assertThat(clientCacheFactoryBean.getSubscriptionEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getSubscriptionMessageTrackingTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionRedundancy()).isEqualTo(2);
		assertThat(clientCacheFactoryBean.getThreadLocalConnections()).isFalse();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, times(1)).getFreeConnectionTimeout();
		verify(mockPool, never()).getIdleTimeout();
		verify(mockPool, times(1)).getLoadConditioningInterval();
		verify(mockPool, never()).getLocators();
		verify(mockPool, never()).getMaxConnections();
		verify(mockPool, never()).getMinConnections();
		verify(mockPool, never()).getMultiuserAuthentication();
		verify(mockPool, times(1)).getPingInterval();
		verify(mockPool, never()).getPRSingleHopEnabled();
		verify(mockPool, times(1)).getReadTimeout();
		verify(mockPool, times(1)).getRetryAttempts();
		verify(mockPool, never()).getServerConnectionTimeout();
		verify(mockPool, never()).getServerGroup();
		verify(mockPool, never()).getServers();
		verify(mockPool, never()).getSocketBufferSize();
		verify(mockPool, never()).getSocketConnectTimeout();
		verify(mockPool, never()).getSocketFactory();
		verify(mockPool, never()).getStatisticInterval();
		verify(mockPool, never()).getSubscriptionAckInterval();
		verify(mockPool, never()).getSubscriptionEnabled();
		verify(mockPool, times(1)).getSubscriptionMessageTrackingTimeout();
		verify(mockPool, never()).getSubscriptionRedundancy();
		verify(mockPool, never()).getThreadLocalConnections();
		verify(mockClientCacheFactory, times(1)).setPoolFreeConnectionTimeout(eq(5000));
		verify(mockClientCacheFactory, times(1)).setPoolIdleTimeout(eq(180000L));
		verify(mockClientCacheFactory, times(1)).setPoolLoadConditioningInterval(eq(300000));
		verify(mockClientCacheFactory, times(1)).setPoolMaxConnections(eq(500));
		verify(mockClientCacheFactory, times(1)).setPoolMinConnections(eq(50));
		verify(mockClientCacheFactory, times(1)).setPoolMultiuserAuthentication(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolPingInterval(eq(15000L));
		verify(mockClientCacheFactory, times(1)).setPoolPRSingleHopEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolReadTimeout(eq(30000));
		verify(mockClientCacheFactory, times(1)).setPoolRetryAttempts(eq(1));
		verify(mockClientCacheFactory, times(1)).setPoolServerConnectionTimeout(eq(1248));
		verify(mockClientCacheFactory, times(1)).setPoolServerGroup(eq("TestGroup"));
		verify(mockClientCacheFactory, times(1)).setPoolSocketBufferSize(eq(16384));
		verify(mockClientCacheFactory, times(1)).setPoolSocketConnectTimeout(eq(10000));
		verify(mockClientCacheFactory, times(1)).setPoolSocketFactory(eq(mockSocketFactory));
		verify(mockClientCacheFactory, times(1)).setPoolStatisticInterval(eq(500));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionAckInterval(eq(100));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionEnabled(eq(true));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionMessageTrackingTimeout(eq(20000));
		verify(mockClientCacheFactory, times(1)).setPoolSubscriptionRedundancy(eq(2));
		verify(mockClientCacheFactory, times(1)).addPoolLocator(eq("localhost"), eq(11235));
		verify(mockClientCacheFactory, never()).addPoolServer(anyString(), anyInt());
	}

	@Test
	public void configurePoolWithFactoryLocator() {

		Pool mockPool = mock(Pool.class);

		when(mockPool.getLocators()).thenReturn(Collections.singletonList(new InetSocketAddress("localhost", 21668)));
		when(mockPool.getServers()).thenReturn(Collections.singletonList(new InetSocketAddress("localhost", 41414)));

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPool(mockPool);
		clientCacheFactoryBean.addLocators(newConnectionEndpoint("boombox", 11235));

		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, never()).getLocators();
		verify(mockPool, never()).getServers();
		verify(mockClientCacheFactory, times(1)).addPoolLocator(eq("boombox"), eq(11235));
		verify(mockClientCacheFactory, never()).addPoolServer(anyString(), anyInt());
	}

	@Test
	public void configurePoolWithFactoryServer() {

		Pool mockPool = mock(Pool.class);

		when(mockPool.getLocators()).thenReturn(Collections.singletonList(new InetSocketAddress("localhost", 21668)));
		when(mockPool.getServers()).thenReturn(Collections.singletonList(new InetSocketAddress("localhost", 41414)));

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPool(mockPool);
		clientCacheFactoryBean.addServers(newConnectionEndpoint("skullbox", 12480));

		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getServers().size()).isEqualTo(1);

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, never()).getLocators();
		verify(mockPool, never()).getServers();
		verify(mockClientCacheFactory, never()).addPoolLocator(anyString(), anyInt());
		verify(mockClientCacheFactory, times(1)).addPoolServer(eq("skullbox"), eq(12480));
	}

	@Test
	public void configurePoolWithPoolLocator() {

		Pool mockPool = mock(Pool.class);

		when(mockPool.getLocators()).thenReturn(Collections.singletonList(new InetSocketAddress("skullbox", 21668)));
		when(mockPool.getServers()).thenReturn(Collections.emptyList());

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPool(mockPool);

		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, times(1)).getLocators();
		verify(mockPool, times(1)).getServers();
		verify(mockClientCacheFactory, times(1)).addPoolLocator(eq("skullbox"), eq(21668));
		verify(mockClientCacheFactory, never()).addPoolServer(anyString(), anyInt());
	}

	@Test
	public void configurePoolWithPoolServer() {

		Pool mockPool = mock(Pool.class);

		when(mockPool.getLocators()).thenReturn(Collections.emptyList());
		when(mockPool.getServers()).thenReturn(Collections.singletonList(new InetSocketAddress("boombox", 41414)));

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setPool(mockPool);

		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockPool, never()).getLocators();
		verify(mockPool, times(1)).getServers();
		verify(mockClientCacheFactory, never()).addPoolLocator(anyString(), anyInt());
		verify(mockClientCacheFactory, times(1)).addPoolServer(eq("boombox"), eq(41414));
	}

	@Test
	public void configurePoolWithDefaultServer() {

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(null).when(clientCacheFactoryBean).resolvePool();

		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
		assertThat(clientCacheFactoryBean.getPool()).isNull();
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		assertThat(clientCacheFactoryBean.configurePool(mockClientCacheFactory)).isSameAs(mockClientCacheFactory);

		verify(mockClientCacheFactory, never()).addPoolLocator(anyString(), anyInt());
		verify(mockClientCacheFactory, times(1)).addPoolServer(eq("localhost"),
			eq(GemfireUtils.DEFAULT_CACHE_SERVER_PORT));
	}

	@Test
	public void createCache() {

		ClientCache mockClientCache = mock(ClientCache.class);

		ClientCacheFactory mockClientCacheFactory = mock(ClientCacheFactory.class);

		doReturn(mockClientCache).when(mockClientCacheFactory).create();

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.<GemFireCache>createCache(mockClientCacheFactory)).isSameAs(mockClientCache);

		verify(mockClientCacheFactory, times(1)).create();
		verifyNoMoreInteractions(mockClientCacheFactory);
		verifyNoInteractions(mockClientCache);
	}

	@Test
	public void resolvePoolReturnsConfiguredPool() {

		Pool mockPool = mock(Pool.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		clientCacheFactoryBean.setPool(mockPool);

		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);
		assertThat(clientCacheFactoryBean.resolvePool()).isEqualTo(mockPool);

		verify(clientCacheFactoryBean, never()).getPoolName();
		verifyNoInteractions(mockPool);
	}

	@Test
	public void resolvesPoolReturnsNamedPool() {

		Pool mockPool = mock(Pool.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		when(clientCacheFactoryBean.findPool(eq("TestPool"))).thenReturn(mockPool);

		clientCacheFactoryBean.setPoolName("TestPool");

		assertThat(clientCacheFactoryBean.getPool()).isNull();
		assertThat(clientCacheFactoryBean.getPoolName()).isEqualTo("TestPool");
		assertThat(clientCacheFactoryBean.resolvePool()).isEqualTo(mockPool);

		verify(clientCacheFactoryBean, times(1)).findPool(eq("TestPool"));
		verifyNoInteractions(mockPool);
	}

	@Test
	public void resolvesPoolReturnsNamedPoolFromBeanFactory() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		Pool mockPool = mock(Pool.class);

		PoolFactoryBean mockPoolFactoryBean = mock(PoolFactoryBean.class);

		when(mockBeanFactory.containsBean(eq("TestPool"))).thenReturn(true);
		when(mockBeanFactory.getBean(eq("&TestPool"), eq(PoolFactoryBean.class))).thenReturn(mockPoolFactoryBean);
		when(mockPoolFactoryBean.getPool()).thenReturn(mockPool);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setBeanFactory(mockBeanFactory);
		clientCacheFactoryBean.setPoolName("TestPool");

		assertThat(clientCacheFactoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(clientCacheFactoryBean.getPool()).isNull();
		assertThat(clientCacheFactoryBean.getPoolName()).isEqualTo("TestPool");
		assertThat(clientCacheFactoryBean.resolvePool()).isEqualTo(mockPool);

		verify(mockBeanFactory, times(1)).containsBean(eq("TestPool"));
		verify(mockBeanFactory, times(1))
			.getBean(eq("&TestPool"), eq(PoolFactoryBean.class));
		verify(mockPoolFactoryBean, times(1)).getPool();
		verifyNoInteractions(mockPool);
	}

	@Test
	public void resolvePoolWhenBeanFactoryHasNoPoolBeansReturnsNull() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		when(mockBeanFactory.containsBean(anyString())).thenReturn(false);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setBeanFactory(mockBeanFactory);
		clientCacheFactoryBean.setPoolName("TestPool");

		assertThat(clientCacheFactoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(clientCacheFactoryBean.getPool()).isNull();
		assertThat(clientCacheFactoryBean.getPoolName()).isEqualTo("TestPool");
		assertThat(clientCacheFactoryBean.resolvePool()).isNull();

		verify(mockBeanFactory, times(1)).containsBean(eq("TestPool"));
		verify(mockBeanFactory, never()).getBean(anyString(), eq(PoolFactoryBean.class));
	}

	@Test
	public void onApplicationEventCallsClientCacheReadyForEvents() {

		ClientCache mockClientCache = mock(ClientCache.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(true);

		assertThat(clientCacheFactoryBean.isReadyForEvents()).isTrue();

		clientCacheFactoryBean.onApplicationEvent(mock(ContextRefreshedEvent.class, "MockContextRefreshedEvent"));

		verify(mockClientCache, times(1)).readyForEvents();
	}

	@Test
	public void onApplicationEventDoesNotCallClientCacheReadyForEventsWhenClientCacheFactoryBeanReadyForEventsIsFalse() {

		ClientCache mockClientCache = mock(ClientCache.class);

		doThrow(new RuntimeException("test")).when(mockClientCache).readyForEvents();

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(false);

		assertThat(clientCacheFactoryBean.isReadyForEvents()).isFalse();

		clientCacheFactoryBean.onApplicationEvent(mock(ContextRefreshedEvent.class, "MockContextRefreshedEvent"));

		verify(mockClientCache, never()).readyForEvents();
	}

	@Test
	public void onApplicationEventHandlesIllegalStateException() {

		ClientCache mockClientCache = mock(ClientCache.class);

		doThrow(new IllegalStateException("test")).when(mockClientCache).readyForEvents();

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(true);

		assertThat(clientCacheFactoryBean.isReadyForEvents()).isTrue();

		clientCacheFactoryBean.onApplicationEvent(mock(ContextRefreshedEvent.class));

		verify(mockClientCache, times(1)).readyForEvents();
	}

	@Test
	public void onApplicationEventHandlesCacheClosedException() {

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doThrow(new CacheClosedException("test")).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(true);

		assertThat(clientCacheFactoryBean.isReadyForEvents()).isTrue();

		clientCacheFactoryBean.onApplicationEvent(mock(ContextRefreshedEvent.class));
	}

	@Test
	public void closeClientCacheWithKeepAlive() {

		ClientCache mockClientCache = mock(ClientCache.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setKeepAlive(true);

		assertThat(clientCacheFactoryBean.isKeepAlive()).isTrue();

		clientCacheFactoryBean.close(mockClientCache);

		verify(mockClientCache, times(1)).close(eq(true));
	}

	@Test
	public void closeClientCacheWithoutKeepAlive() {

		ClientCache mockClientCache = mock(ClientCache.class);

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		clientCacheFactoryBean.setKeepAlive(false);

		assertThat(clientCacheFactoryBean.isKeepAlive()).isFalse();

		clientCacheFactoryBean.close(mockClientCache);

		verify(mockClientCache, times(1)).close(eq(false));
	}

	@Test
	public void autoReconnectDisabled() {
		assertThat(new ClientCacheFactoryBean().getEnableAutoReconnect()).isFalse();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void autoReconnectEnabled() {
		new ClientCacheFactoryBean().setEnableAutoReconnect(true);
	}

	@Test
	public void clusterConfigurationNotUsed() {
		assertThat(new ClientCacheFactoryBean().getUseClusterConfiguration()).isFalse();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void usesClusterConfiguration() {
		new ClientCacheFactoryBean().setUseClusterConfiguration(true);
	}

	@Test
	public void setAndGetKeepAlive() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getKeepAlive()).isFalse();
		assertThat(clientCacheFactoryBean.isKeepAlive()).isFalse();

		clientCacheFactoryBean.setKeepAlive(true);

		assertThat(clientCacheFactoryBean.getKeepAlive()).isTrue();
		assertThat(clientCacheFactoryBean.isKeepAlive()).isTrue();

		clientCacheFactoryBean.setKeepAlive(null);

		assertThat(clientCacheFactoryBean.getKeepAlive()).isNull();
		assertThat(clientCacheFactoryBean.isKeepAlive()).isFalse();
	}

	@Test
	public void setAndGetPool() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		Pool mockPool = mock(Pool.class);

		assertThat(clientCacheFactoryBean.getPool()).isNull();

		clientCacheFactoryBean.setPool(mockPool);

		assertThat(clientCacheFactoryBean.getPool()).isSameAs(mockPool);

		clientCacheFactoryBean.setPool(null);

		assertThat(clientCacheFactoryBean.getPool()).isNull();
	}

	@Test
	public void setAndGetPoolName() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getPoolName()).isNull();

		clientCacheFactoryBean.setPoolName("TestPool");

		assertThat(clientCacheFactoryBean.getPoolName()).isEqualTo("TestPool");

		clientCacheFactoryBean.setPoolName(null);

		assertThat(clientCacheFactoryBean.getPoolName()).isNull();
	}

	@Test
	public void setAndGetPoolSettings() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getFreeConnectionTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getIdleTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getLoadConditioningInterval()).isNull();
		assertThat(clientCacheFactoryBean.getMaxConnections()).isNull();
		assertThat(clientCacheFactoryBean.getMinConnections()).isNull();
		assertThat(clientCacheFactoryBean.getMultiUserAuthentication()).isNull();
		assertThat(clientCacheFactoryBean.getPingInterval()).isNull();
		assertThat(clientCacheFactoryBean.getPrSingleHopEnabled()).isNull();
		assertThat(clientCacheFactoryBean.getReadTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getRetryAttempts()).isNull();
		assertThat(clientCacheFactoryBean.getServerGroup()).isNull();
		assertThat(clientCacheFactoryBean.getSocketBufferSize()).isNull();
		assertThat(clientCacheFactoryBean.getSocketConnectTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getStatisticsInterval()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionAckInterval()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionEnabled()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionMessageTrackingTimeout()).isNull();
		assertThat(clientCacheFactoryBean.getSubscriptionRedundancy()).isNull();
		assertThat(clientCacheFactoryBean.getThreadLocalConnections()).isNull();

		clientCacheFactoryBean.setFreeConnectionTimeout(5000);
		clientCacheFactoryBean.setIdleTimeout(120000L);
		clientCacheFactoryBean.setLoadConditioningInterval(300000);
		clientCacheFactoryBean.setMaxConnections(500);
		clientCacheFactoryBean.setMinConnections(50);
		clientCacheFactoryBean.setMultiUserAuthentication(true);
		clientCacheFactoryBean.setPingInterval(15000L);
		clientCacheFactoryBean.setPrSingleHopEnabled(true);
		clientCacheFactoryBean.setReadTimeout(30000);
		clientCacheFactoryBean.setRetryAttempts(1);
		clientCacheFactoryBean.setServerGroup("test");
		clientCacheFactoryBean.setSocketBufferSize(16384);
		clientCacheFactoryBean.setSocketConnectTimeout(5000);
		clientCacheFactoryBean.setStatisticsInterval(500);
		clientCacheFactoryBean.setSubscriptionAckInterval(200);
		clientCacheFactoryBean.setSubscriptionEnabled(true);
		clientCacheFactoryBean.setSubscriptionMessageTrackingTimeout(20000);
		clientCacheFactoryBean.setSubscriptionRedundancy(2);
		clientCacheFactoryBean.setThreadLocalConnections(false);

		assertThat(clientCacheFactoryBean.getFreeConnectionTimeout()).isEqualTo(5000);
		assertThat(clientCacheFactoryBean.getIdleTimeout()).isEqualTo(120000L);
		assertThat(clientCacheFactoryBean.getLoadConditioningInterval()).isEqualTo(300000);
		assertThat(clientCacheFactoryBean.getMaxConnections()).isEqualTo(500);
		assertThat(clientCacheFactoryBean.getMinConnections()).isEqualTo(50);
		assertThat(clientCacheFactoryBean.getMultiUserAuthentication()).isTrue();
		assertThat(clientCacheFactoryBean.getPingInterval()).isEqualTo(15000L);
		assertThat(clientCacheFactoryBean.getPrSingleHopEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getReadTimeout()).isEqualTo(30000);
		assertThat(clientCacheFactoryBean.getRetryAttempts()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getServerGroup()).isEqualTo("test");
		assertThat(clientCacheFactoryBean.getSocketBufferSize()).isEqualTo(16384);
		assertThat(clientCacheFactoryBean.getSocketConnectTimeout()).isEqualTo(5000);
		assertThat(clientCacheFactoryBean.getStatisticsInterval()).isEqualTo(500);
		assertThat(clientCacheFactoryBean.getSubscriptionAckInterval()).isEqualTo(200);
		assertThat(clientCacheFactoryBean.getSubscriptionEnabled()).isTrue();
		assertThat(clientCacheFactoryBean.getSubscriptionMessageTrackingTimeout()).isEqualTo(20000);
		assertThat(clientCacheFactoryBean.getSubscriptionRedundancy()).isEqualTo(2);
		assertThat(clientCacheFactoryBean.getThreadLocalConnections()).isFalse();
	}

	@Test
	public void setAndGetReadyForEvents() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isNull();

		clientCacheFactoryBean.setReadyForEvents(true);

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isTrue();

		clientCacheFactoryBean.setReadyForEvents(null);

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isNull();

		clientCacheFactoryBean.setReadyForEvents(false);

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isFalse();
	}

	private ClientCache mockClientCache(String durableClientId) {

		ClientCache mockClientCache = mock(ClientCache.class);

		DistributedSystem mockDistributedSystem = mock(DistributedSystem.class);

		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty(DistributedSystemUtils.DURABLE_CLIENT_ID_PROPERTY_NAME, durableClientId);
		gemfireProperties.setProperty(DistributedSystemUtils.DURABLE_CLIENT_TIMEOUT_PROPERTY_NAME, "300");

		when(mockClientCache.getDistributedSystem()).thenReturn(mockDistributedSystem);
		when(mockDistributedSystem.isConnected()).thenReturn(true);
		when(mockDistributedSystem.getProperties()).thenReturn(gemfireProperties);

		return mockClientCache;
	}

	@Test
	public void isReadyForEventsIsTrueWhenClientCacheFactoryBeanReadyForEventsIsTrue() {

		ClientCache mockClientCache = mockClientCache("");

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(true);

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isTrue();
		assertThat(clientCacheFactoryBean.isReadyForEvents()).isTrue();

		verifyNoInteractions(mockClientCache);
	}

	@Test
	public void isReadyForEventsIsFalseWhenClientCacheFactoryBeanReadyForEventsIsFalse() {

		ClientCache mockClientCache = mockClientCache("TestDurableClientId");

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		clientCacheFactoryBean.setReadyForEvents(false);

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isFalse();
		assertThat(clientCacheFactoryBean.isReadyForEvents()).isFalse();

		verifyNoInteractions(mockClientCache);
	}

	@Test
	public void isReadyForEventsIsFalseWhenClientCacheNotInitialized() {

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doThrow(new CacheClosedException("test")).when(clientCacheFactoryBean).getCache();

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isNull();
		assertThat(clientCacheFactoryBean.isReadyForEvents()).isFalse();
	}

	@Test
	public void isReadyForEventsIsTrueWhenDurableClientIdIsSet() {

		ClientCache mockClientCache = mockClientCache("TestDurableClientId");

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isNull();
		assertThat(clientCacheFactoryBean.isReadyForEvents()).isTrue();

		verify(mockClientCache, times(1)).getDistributedSystem();
	}

	@Test
	public void isReadyForEventsIsFalseWhenDurableClientIdIsNotSet() {

		ClientCache mockClientCache = mockClientCache("  ");

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		doReturn(mockClientCache).when(clientCacheFactoryBean).getCache();

		assertThat(clientCacheFactoryBean.getReadyForEvents()).isNull();
		assertThat(clientCacheFactoryBean.isReadyForEvents()).isFalse();

		verify(mockClientCache, times(1)).getDistributedSystem();
	}

	@Test
	public void addSetAndGetLocators() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getLocators()).isNotNull();
		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();

		ConnectionEndpoint localhost = newConnectionEndpoint("localhost", 21668);

		clientCacheFactoryBean.addLocators(localhost);

		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getLocators().findOne("localhost")).isEqualTo(localhost);

		ConnectionEndpoint skullbox = newConnectionEndpoint("skullbox", 10334);
		ConnectionEndpoint boombox = newConnectionEndpoint("boombox", 10334);

		clientCacheFactoryBean.addLocators(skullbox, boombox);

		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(3);
		assertThat(clientCacheFactoryBean.getLocators().findOne("localhost")).isEqualTo(localhost);
		assertThat(clientCacheFactoryBean.getLocators().findOne("skullbox")).isEqualTo(skullbox);
		assertThat(clientCacheFactoryBean.getLocators().findOne("boombox")).isEqualTo(boombox);

		clientCacheFactoryBean.setLocators(ArrayUtils.asArray(localhost));

		assertThat(clientCacheFactoryBean.getLocators().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getLocators().findOne("localhost")).isEqualTo(localhost);

		clientCacheFactoryBean.setLocators(Collections.emptyList());

		assertThat(clientCacheFactoryBean.getLocators()).isNotNull();
		assertThat(clientCacheFactoryBean.getLocators().isEmpty()).isTrue();
	}

	@Test
	public void addSetAndGetServers() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.getServers()).isNotNull();
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();

		ConnectionEndpoint localhost = newConnectionEndpoint("localhost", 21668);

		clientCacheFactoryBean.addServers(localhost);

		assertThat(clientCacheFactoryBean.getServers().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getServers().findOne("localhost")).isEqualTo(localhost);

		ConnectionEndpoint skullbox = newConnectionEndpoint("skullbox", 10334);
		ConnectionEndpoint boombox = newConnectionEndpoint("boombox", 10334);

		clientCacheFactoryBean.addServers(skullbox, boombox);

		assertThat(clientCacheFactoryBean.getServers().size()).isEqualTo(3);
		assertThat(clientCacheFactoryBean.getServers().findOne("localhost")).isEqualTo(localhost);
		assertThat(clientCacheFactoryBean.getServers().findOne("skullbox")).isEqualTo(skullbox);
		assertThat(clientCacheFactoryBean.getServers().findOne("boombox")).isEqualTo(boombox);

		clientCacheFactoryBean.setServers(ArrayUtils.asArray(localhost));

		assertThat(clientCacheFactoryBean.getServers().size()).isEqualTo(1);
		assertThat(clientCacheFactoryBean.getServers().findOne("localhost")).isEqualTo(localhost);

		clientCacheFactoryBean.setServers(Collections.emptyList());

		assertThat(clientCacheFactoryBean.getServers()).isNotNull();
		assertThat(clientCacheFactoryBean.getServers().isEmpty()).isTrue();
	}
}
