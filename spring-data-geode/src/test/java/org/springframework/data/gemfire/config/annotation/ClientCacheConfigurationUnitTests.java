/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import org.apache.geode.cache.TransactionListener;
import org.apache.geode.cache.TransactionWriter;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.cache.util.GatewayConflictResolver;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;

/**
 * Unit Tests for {@link ClientCacheConfiguration}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.Resource
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfiguration
 * @since 2.4.0
 */
public class ClientCacheConfigurationUnitTests {

	@Test
	public void configuresClientCacheFactoryBean() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClassLoader mockBeanClassLoader = mock(ClassLoader.class);

		GatewayConflictResolver mockGatewayConflictResolver = mock(GatewayConflictResolver.class);

		List<ConnectionEndpoint> poolLocators =
			Collections.singletonList(new ConnectionEndpoint("localhost", 12345));

		Properties gemfireProperties = new Properties();

		Resource mockResource = mock(Resource.class, "cache.xml");

		SocketFactory mockSocketFactory = mock(SocketFactory.class);

		TransactionListener mockTransactionListener = mock(TransactionListener.class);

		TransactionWriter mockTransactionWriter = mock(TransactionWriter.class);

		ClientCacheFactoryBean clientCacheFactoryBean = spy(new ClientCacheFactoryBean());

		ClientCacheConfiguration configuration = spy(new ClientCacheConfiguration());

		doReturn(gemfireProperties).when(configuration).gemfireProperties();
		doReturn(clientCacheFactoryBean).when(configuration).newCacheFactoryBean();
		doReturn(mockSocketFactory).when(configuration).resolveSocketFactory();

		configuration.setBeanClassLoader(mockBeanClassLoader);
		configuration.setBeanFactory(mockBeanFactory);
		configuration.setCacheXml(mockResource);
		configuration.setClose(true);
		configuration.setCopyOnRead(true);
		configuration.setCriticalHeapPercentage(90.0f);
		configuration.setCriticalOffHeapPercentage(95.0f);
		configuration.setEvictionHeapPercentage(75.0f);
		configuration.setEvictionOffHeapPercentage(90.0f);
		configuration.setGatewayConflictResolver(mockGatewayConflictResolver);
		configuration.setTransactionListeners(Collections.singletonList(mockTransactionListener));
		configuration.setTransactionWriter(mockTransactionWriter);
		configuration.setUseBeanFactoryLocator(true);
		configuration.setDurableClientId("abc123");
		configuration.setDurableClientTimeout(300000);
		configuration.setFreeConnectionTimeout(30000);
		configuration.setIdleTimeout(300000L);
		configuration.setKeepAlive(true);
		configuration.setPoolLocators(poolLocators);
		configuration.setLoadConditioningInterval(120000);
		configuration.setMaxConnections(500);
		configuration.setMinConnections(51);
		configuration.setMultiUserAuthentication(false);
		configuration.setPingInterval(15000L);
		configuration.setPrSingleHopEnabled(true);
		configuration.setReadTimeout(60000);
		configuration.setReadyForEvents(true);
		configuration.setRetryAttempts(2);
		configuration.setServerConnectionTimeout(60000);
		configuration.setServerGroup("TestGroup");
		configuration.setSocketBufferSize(8192);
		configuration.setSocketConnectTimeout(30000);
		configuration.setSocketFactoryBeanName("testSocketFactory");
		configuration.setStatisticsInterval(5000);
		configuration.setSubscriptionAckInterval(15000);
		configuration.setSubscriptionEnabled(true);
		configuration.setSubscriptionMessageTrackingTimeout(60000);
		configuration.setSubscriptionRedundancy(1);
		configuration.setThreadLocalConnections(false);

		assertThat(configuration.gemfireCache()).isEqualTo(clientCacheFactoryBean);

		verify(clientCacheFactoryBean, times(1)).setBeanClassLoader(eq(mockBeanClassLoader));
		verify(clientCacheFactoryBean, times(1)).setBeanFactory(eq(mockBeanFactory));
		verify(clientCacheFactoryBean, times(1)).setCacheXml(eq(mockResource));
		verify(clientCacheFactoryBean, times(1)).setClose(eq(true));
		verify(clientCacheFactoryBean, times(1)).setCopyOnRead(eq(true));
		verify(clientCacheFactoryBean, times(1)).setCriticalHeapPercentage(eq(90.0f));
		verify(clientCacheFactoryBean, times(1)).setCriticalOffHeapPercentage(eq(95.0f));
		verify(clientCacheFactoryBean, times(1)).setEvictionHeapPercentage(eq(75.0f));
		verify(clientCacheFactoryBean, times(1)).setEvictionOffHeapPercentage(eq(90.0f));
		verify(clientCacheFactoryBean, times(1)).setGatewayConflictResolver(eq(mockGatewayConflictResolver));
		verify(clientCacheFactoryBean, times(1)).setJndiDataSources(eq(Collections.emptyList()));
		verify(clientCacheFactoryBean, times(1)).setTransactionListeners(eq(Collections.singletonList(mockTransactionListener)));
		verify(clientCacheFactoryBean, times(1)).setTransactionWriter(eq(mockTransactionWriter));
		verify(clientCacheFactoryBean, times(1)).setUseBeanFactoryLocator(eq(true));
		verify(clientCacheFactoryBean, times(1)).setDurableClientId(eq("abc123"));
		verify(clientCacheFactoryBean, times(1)).setDurableClientTimeout(eq(300000));
		verify(clientCacheFactoryBean, times(1)).setFreeConnectionTimeout(eq(30000));
		verify(clientCacheFactoryBean, times(1)).setIdleTimeout(eq(300000L));
		verify(clientCacheFactoryBean, times(1)).setKeepAlive(eq(true));
		verify(clientCacheFactoryBean, times(1)).setLocators(eq(poolLocators));
		verify(clientCacheFactoryBean, times(1)).setLoadConditioningInterval(eq(120000));
		verify(clientCacheFactoryBean, times(1)).setMaxConnections(eq(500));
		verify(clientCacheFactoryBean, times(1)).setMinConnections(eq(51));
		verify(clientCacheFactoryBean, times(1)).setMultiUserAuthentication(eq(false));
		verify(clientCacheFactoryBean, times(1)).setPingInterval(eq(15000L));
		verify(clientCacheFactoryBean, times(1)).setPrSingleHopEnabled(eq(true));
		verify(clientCacheFactoryBean, times(1)).setReadTimeout(eq(60000));
		verify(clientCacheFactoryBean, times(1)).setReadyForEvents(eq(true));
		verify(clientCacheFactoryBean, times(1)).setRetryAttempts(eq(2));
		verify(clientCacheFactoryBean, times(1)).setServerConnectionTimeout(eq(60000));
		verify(clientCacheFactoryBean, times(1)).setServerGroup(eq("TestGroup"));
		verify(clientCacheFactoryBean, times(1)).setSocketBufferSize(eq(8192));
		verify(clientCacheFactoryBean, times(1)).setSocketConnectTimeout(eq(30000));
		verify(clientCacheFactoryBean, times(1)).setSocketFactory(eq(mockSocketFactory));
		verify(clientCacheFactoryBean, times(1)).setStatisticsInterval(eq(5000));
		verify(clientCacheFactoryBean, times(1)).setSubscriptionAckInterval(eq(15000));
		verify(clientCacheFactoryBean, times(1)).setSubscriptionEnabled(eq(true));
		verify(clientCacheFactoryBean, times(1)).setSubscriptionMessageTrackingTimeout(eq(60000));
		verify(clientCacheFactoryBean, times(1)).setSubscriptionRedundancy(eq(1));
		verify(clientCacheFactoryBean, times(1)).setThreadLocalConnections(eq(false));
	}

	@Test
	public void resolveSocketFactoryFromBeanFactory() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClientCacheConfiguration configuration = new ClientCacheConfiguration();

		SocketFactory mockSocketFactory = mock(SocketFactory.class);

		doReturn(true).when(mockBeanFactory)
			.isTypeMatch(eq("testSocketFactoryBean"), eq(SocketFactory.class));
		doReturn(mockSocketFactory).when(mockBeanFactory)
			.getBean(eq("testSocketFactoryBean"), eq(SocketFactory.class));

		configuration.setBeanFactory(mockBeanFactory);
		configuration.setSocketFactoryBeanName("testSocketFactoryBean");

		assertThat(configuration.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(configuration.getSocketFactoryBeanName()).isEqualTo("testSocketFactoryBean");
		assertThat(configuration.resolveSocketFactory()).isEqualTo(mockSocketFactory);

		verify(mockBeanFactory, times(1))
			.isTypeMatch(eq("testSocketFactoryBean"), eq(SocketFactory.class));
		verify(mockBeanFactory, times(1))
			.getBean(eq("testSocketFactoryBean"), eq(SocketFactory.class));
		verifyNoMoreInteractions(mockBeanFactory);
		verifyNoInteractions(mockSocketFactory);
	}

	public void testResolveSocketFactoryWithInvalidSocketFactoryBeanNameConfiguration(String socketFactoryBeanName) {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClientCacheConfiguration configuration = new ClientCacheConfiguration();

		configuration.setBeanFactory(mockBeanFactory);
		configuration.setSocketFactoryBeanName(socketFactoryBeanName);

		assertThat(configuration.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(configuration.getSocketFactoryBeanName()).isEqualTo(socketFactoryBeanName);
		assertThat(configuration.resolveSocketFactory()).isNull();

		verifyNoInteractions(mockBeanFactory);
	}

	@Test
	public void resolveSocketFactoryWhenSocketFactoryBeanNameIsNull() {
		testResolveSocketFactoryWithInvalidSocketFactoryBeanNameConfiguration(null);
	}

	@Test
	public void resolveSocketFactoryWhenSocketFactoryBeanNameIsEmpty() {
		testResolveSocketFactoryWithInvalidSocketFactoryBeanNameConfiguration("");
	}

	@Test
	public void resolveSocketFactoryWhenSocketFactoryBeanNameIsBlank() {
		testResolveSocketFactoryWithInvalidSocketFactoryBeanNameConfiguration("  ");
	}

	@Test(expected = BeanNotOfRequiredTypeException.class)
	public void resolveSocketFactoryWhenSocketFactoryBeanIsNotTypeMatch() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		doReturn(false).when(mockBeanFactory).isTypeMatch(anyString(), eq(SocketFactory.class));
		doReturn(true).when(mockBeanFactory).containsBean(eq("testSocketFactory"));
		doReturn(javax.net.SocketFactory.class).when(mockBeanFactory).getType(eq("testSocketFactory"));

		ClientCacheConfiguration configuration = new ClientCacheConfiguration();

		configuration.setBeanFactory(mockBeanFactory);
		configuration.setSocketFactoryBeanName("testSocketFactory");

		assertThat(configuration.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(configuration.getSocketFactoryBeanName()).isEqualTo("testSocketFactory");

		try {
			configuration.resolveSocketFactory();
		}
		catch (BeanNotOfRequiredTypeException expected) {

			assertThat(expected)
				.hasMessageContaining("Bean named 'testSocketFactory' is expected to be of type '%s' but was actually of type '%s'",
					SocketFactory.class.getName(), javax.net.SocketFactory.class.getName());

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockBeanFactory, times(1))
				.isTypeMatch(eq("testSocketFactory"), eq(SocketFactory.class));
			verify(mockBeanFactory, times(1)).containsBean(eq("testSocketFactory"));
			verify(mockBeanFactory, times(1)).getType(eq("testSocketFactory"));
			verifyNoMoreInteractions(mockBeanFactory);
		}
	}

	@Test
	public void resolveSocketFactoryWhenBeanOfSocketFactoryTypeIsNotFound() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		doReturn(false).when(mockBeanFactory)
			.isTypeMatch(eq("mockSocketFactory"), eq(SocketFactory.class));
		doReturn(false).when(mockBeanFactory).containsBean(eq("mockSocketFactory"));

		ClientCacheConfiguration configuration = new ClientCacheConfiguration();

		configuration.setBeanFactory(mockBeanFactory);
		configuration.setSocketFactoryBeanName("mockSocketFactory");

		assertThat(configuration.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(configuration.getSocketFactoryBeanName()).isEqualTo("mockSocketFactory");
		assertThat(configuration.resolveSocketFactory()).isNull();

		verify(mockBeanFactory, times(1))
			.isTypeMatch(eq("mockSocketFactory"), eq(SocketFactory.class));
		verify(mockBeanFactory, times(1)).containsBean(eq("mockSocketFactory"));
		verifyNoMoreInteractions(mockBeanFactory);
	}
}
