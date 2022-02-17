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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.TransactionListener;
import org.apache.geode.cache.TransactionWriter;
import org.apache.geode.cache.control.ResourceManager;
import org.apache.geode.cache.util.GatewayConflictResolver;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;

/**
 * Unit Tests for {@link CacheFactoryBean}.
 *
 * @author John Blum
 * @author Patrick Johnson
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.Resource
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @since 1.7.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheFactoryBeanUnitTests {

	@Mock
	private Cache mockCache;

	@Test
	public void afterPropertiesSetAppliesCacheConfigurersAndThenInitializesBeanFactoryLocator() throws Exception {

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		cacheFactoryBean.setUseBeanFactoryLocator(true);
		cacheFactoryBean.afterPropertiesSet();

		InOrder orderVerifier = inOrder(cacheFactoryBean);

		orderVerifier.verify(cacheFactoryBean, times(1)).afterPropertiesSet();
		orderVerifier.verify(cacheFactoryBean, times(1)).applyCacheConfigurers();
		orderVerifier.verify(cacheFactoryBean, times(1)).getCompositePeerCacheConfigurer();
		orderVerifier.verify(cacheFactoryBean, times(1)).applyPeerCacheConfigurers(isA(PeerCacheConfigurer.class));
		orderVerifier.verify(cacheFactoryBean, times(1)).setBeanFactoryLocator(isA(GemfireBeanFactoryLocator.class));
	}

	@Test
	public void applyingCacheConfigurersDisablesAutoReconnectAndDoesNotUseClusterConfigurationByDefault() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.applyCacheConfigurers();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).hasSize(2);
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(gemfireProperties.getProperty("disable-auto-reconnect")).isEqualTo("true");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("false");
	}

	@Test
	public void applyCacheConfigurersWithAutoReconnectAndClusterConfigurationDisabled() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setEnableAutoReconnect(false);
		cacheFactoryBean.setUseClusterConfiguration(false);
		cacheFactoryBean.applyCacheConfigurers();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).hasSize(2);
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(gemfireProperties.getProperty("disable-auto-reconnect")).isEqualTo("true");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("false");
	}

	@Test
	public void applyCacheConfigurersWithAutoReconnectAndClusterConfigurationEnabled() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setEnableAutoReconnect(true);
		cacheFactoryBean.setUseClusterConfiguration(true);
		cacheFactoryBean.applyCacheConfigurers();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).hasSize(2);
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(gemfireProperties.getProperty("disable-auto-reconnect")).isEqualTo("false");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("true");
	}

	@Test
	public void applyCacheConfigurersWithAutoReconnectDisabledAndClusterConfigurationEnabled() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setEnableAutoReconnect(false);
		cacheFactoryBean.setUseClusterConfiguration(true);
		cacheFactoryBean.applyCacheConfigurers();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).hasSize(2);
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(gemfireProperties.getProperty("disable-auto-reconnect")).isEqualTo("true");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("true");
	}

	@Test
	public void applyCacheConfigurersWithAutoReconnectEnabledAndClusterConfigurationDisabled() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setEnableAutoReconnect(true);
		cacheFactoryBean.setUseClusterConfiguration(false);
		cacheFactoryBean.applyCacheConfigurers();

		Properties gemfireProperties = cacheFactoryBean.getProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).hasSize(2);
		assertThat(gemfireProperties.containsKey("disable-auto-reconnect")).isTrue();
		assertThat(gemfireProperties.containsKey("use-cluster-configuration")).isTrue();
		assertThat(gemfireProperties.getProperty("disable-auto-reconnect")).isEqualTo("false");
		assertThat(gemfireProperties.getProperty("use-cluster-configuration")).isEqualTo("false");
	}

	@Test
	public void getObjectCallsInit() throws Exception {

		Cache mockCache = mock(Cache.class);

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doReturn(mockCache).when(cacheFactoryBean).init();

		assertThat(cacheFactoryBean.getObject()).isSameAs(mockCache);

		verify(cacheFactoryBean, times(1)).getObject();
		verify(cacheFactoryBean, times(1)).getCache();
		verify(cacheFactoryBean, times(1)).doGetObject();
		verify(cacheFactoryBean, times(1)).init();

		verifyNoMoreInteractions(cacheFactoryBean);

		verifyNoInteractions(mockCache);
	}

	@Test
	public void getObjectReturnsExistingCache() throws Exception {

		Cache mockCache = mock(Cache.class);

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		cacheFactoryBean.setCache(mockCache);

		assertThat(cacheFactoryBean.<Cache>getCache()).isSameAs(mockCache);
		assertThat(cacheFactoryBean.getObject()).isSameAs(mockCache);

		verify(cacheFactoryBean, times(1)).getObject();
		verify(cacheFactoryBean, never()).doGetObject();
		verify(cacheFactoryBean, never()).init();

		verifyNoInteractions(mockCache);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void init() throws Exception {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		Cache mockCache = mock(Cache.class);

		CacheTransactionManager mockCacheTransactionManager = mock(CacheTransactionManager.class);

		DistributedMember mockDistributedMember = mock(DistributedMember.class, withSettings().lenient());

		DistributedSystem mockDistributedSystem = mock(DistributedSystem.class, withSettings().lenient());

		GatewayConflictResolver mockGatewayConflictResolver = mock(GatewayConflictResolver.class);

		PdxSerializer mockPdxSerializer = mock(PdxSerializer.class);

		Resource mockCacheXml = mock(Resource.class);

		ResourceManager mockResourceManager = mock(ResourceManager.class);

		TransactionListener mockTransactionLister = mock(TransactionListener.class);

		TransactionWriter mockTransactionWriter = mock(TransactionWriter.class);

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		when(mockBeanFactory.getAliases(anyString())).thenReturn(new String[0]);
		when(mockCacheFactory.create()).thenReturn(mockCache);
		when(mockCache.getCacheTransactionManager()).thenReturn(mockCacheTransactionManager);
		when(mockCache.getDistributedSystem()).thenReturn(mockDistributedSystem);
		when(mockCache.getResourceManager()).thenReturn(mockResourceManager);
		when(mockCacheXml.getInputStream()).thenReturn(mock(InputStream.class));
		when(mockDistributedSystem.getDistributedMember()).thenReturn(mockDistributedMember);
		when(mockDistributedSystem.getName()).thenReturn("MockDistributedSystem");
		when(mockDistributedMember.getId()).thenReturn("MockDistributedMember");
		when(mockDistributedMember.getGroups()).thenReturn(Collections.emptyList());
		when(mockDistributedMember.getRoles()).thenReturn(Collections.emptySet());
		when(mockDistributedMember.getHost()).thenReturn("skullbox");
		when(mockDistributedMember.getProcessId()).thenReturn(12345);

		ClassLoader expectedThreadContextClassLoader = Thread.currentThread().getContextClassLoader();

		Properties gemfireProperties = new Properties();

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doAnswer(invocation -> {

			Properties gemfirePropertiesArgument = invocation.getArgument(0);

			assertThat(gemfirePropertiesArgument).isEqualTo(gemfireProperties);
			assertThat(cacheFactoryBean.getBeanClassLoader()).isEqualTo(ClassLoader.getSystemClassLoader());

			return mockCacheFactory;

		}).when(cacheFactoryBean).createFactory(isA(Properties.class));

		cacheFactoryBean.setBeanClassLoader(ClassLoader.getSystemClassLoader());
		cacheFactoryBean.setBeanFactory(mockBeanFactory);
		cacheFactoryBean.setBeanName("TestGemFireCache");
		cacheFactoryBean.setCacheXml(mockCacheXml);
		cacheFactoryBean.setCopyOnRead(true);
		cacheFactoryBean.setCriticalHeapPercentage(0.90f);
		cacheFactoryBean.setCriticalOffHeapPercentage(0.95f);
		cacheFactoryBean.setEnableAutoReconnect(false);
		cacheFactoryBean.setEvictionHeapPercentage(0.75f);
		cacheFactoryBean.setEvictionOffHeapPercentage(0.90f);
		cacheFactoryBean.setGatewayConflictResolver(mockGatewayConflictResolver);
		cacheFactoryBean.setJndiDataSources(null);
		cacheFactoryBean.setLockLease(15000);
		cacheFactoryBean.setLockTimeout(5000);
		cacheFactoryBean.setMessageSyncInterval(20000);
		cacheFactoryBean.setPdxDiskStoreName("TestPdxDiskStore");
		cacheFactoryBean.setPdxIgnoreUnreadFields(false);
		cacheFactoryBean.setPdxPersistent(true);
		cacheFactoryBean.setPdxReadSerialized(true);
		cacheFactoryBean.setPdxSerializer(mockPdxSerializer);
		cacheFactoryBean.setProperties(gemfireProperties);
		cacheFactoryBean.setSearchTimeout(45000);
		cacheFactoryBean.setTransactionListeners(Collections.singletonList(mockTransactionLister));
		cacheFactoryBean.setTransactionWriter(mockTransactionWriter);
		cacheFactoryBean.setUseBeanFactoryLocator(true);

		cacheFactoryBean.afterPropertiesSet();
		cacheFactoryBean.init();

		assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(expectedThreadContextClassLoader);

		GemfireBeanFactoryLocator beanFactoryLocator = cacheFactoryBean.getBeanFactoryLocator();

		assertThat(beanFactoryLocator).isNotNull();

		BeanFactory beanFactoryReference = beanFactoryLocator.useBeanFactory("TestGemFireCache");

		assertThat(beanFactoryReference).isSameAs(mockBeanFactory);

		verify(mockBeanFactory, times(1)).getAliases(anyString());
		verify(mockCacheFactory, times(1)).setPdxDiskStore(eq("TestPdxDiskStore"));
		verify(mockCacheFactory, times(1)).setPdxIgnoreUnreadFields(eq(false));
		verify(mockCacheFactory, times(1)).setPdxPersistent(eq(true));
		verify(mockCacheFactory, times(1)).setPdxReadSerialized(eq(true));
		verify(mockCacheFactory, times(1)).setPdxSerializer(eq(mockPdxSerializer));
		verify(mockCacheFactory, times(1)).create();
		verify(mockCache, times(2)).getCacheTransactionManager();
		verify(mockCache, times(1)).loadCacheXml(any(InputStream.class));
		verify(mockCache, times(1)).setCopyOnRead(eq(true));
		verify(mockCache, times(1)).setGatewayConflictResolver(same(mockGatewayConflictResolver));
		verify(mockCache, times(1)).setLockLease(eq(15000));
		verify(mockCache, times(1)).setLockTimeout(eq(5000));
		verify(mockCache, times(1)).setMessageSyncInterval(eq(20000));
		verify(mockCache, times(4)).getResourceManager();
		verify(mockCache, times(1)).setSearchTimeout(eq(45000));
		verify(mockCacheTransactionManager, times(1)).addListener(same(mockTransactionLister));
		verify(mockCacheTransactionManager, times(1)).setWriter(same(mockTransactionWriter));
		verify(mockResourceManager, times(1)).setCriticalHeapPercentage(eq(0.90f));
		verify(mockResourceManager, times(1)).setCriticalOffHeapPercentage(eq(0.95f));
		verify(mockResourceManager, times(1)).setEvictionHeapPercentage(eq(0.75f));
		verify(mockResourceManager, times(1)).setEvictionOffHeapPercentage(eq(0.90f));
	}

	@Test
	public void resolveCacheCallsFetchCacheReturnsMock() {

		Cache mockCache = mock(Cache.class);

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doReturn(mockCache).when(cacheFactoryBean).fetchCache();

		assertThat(cacheFactoryBean.<GemFireCache>resolveCache()).isSameAs(mockCache);

		verifyNoInteractions(mockCache);
	}

	@Test
	public void resolveCacheCreatesCacheWhenFetchCacheThrowsCacheClosedException() {

		Cache mockCache = mock(Cache.class);

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		doReturn(mockCache).when(mockCacheFactory).create();

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doThrow(new CacheClosedException("TEST")).when(cacheFactoryBean).fetchCache();

		doAnswer(invocation -> {

			Properties gemfireProperties = invocation.getArgument(0);

			assertThat(gemfireProperties).isSameAs(cacheFactoryBean.getProperties());

			return mockCacheFactory;

		}).when(cacheFactoryBean).createFactory(isA(Properties.class));

		assertThat(cacheFactoryBean.<GemFireCache>resolveCache()).isEqualTo(mockCache);

		verify(mockCacheFactory, times(1)).create();

		verifyNoInteractions(mockCache);
	}

	@Test
	public void fetchExistingCache() {

		Cache mockCache = mock(Cache.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setCache(mockCache);

		Cache actualCache = cacheFactoryBean.fetchCache();

		assertThat(actualCache).isSameAs(mockCache);

		verifyNoInteractions(mockCache);
	}

	@Test
	public void resolveProperties() {

		Properties gemfireProperties = new Properties();

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setProperties(gemfireProperties);

		assertThat(cacheFactoryBean.resolveProperties()).isSameAs(gemfireProperties);
	}

	@Test
	public void resolvePropertiesWhenNull() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setProperties(null);

		Properties gemfireProperties = cacheFactoryBean.resolveProperties();

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties.isEmpty()).isTrue();
	}

	@Test
	public void createFactory() {

		Properties gemfireProperties = new Properties();

		Object cacheFactoryReference = new CacheFactoryBean().createFactory(gemfireProperties);

		assertThat(cacheFactoryReference).isInstanceOf(CacheFactory.class);
		assertThat(gemfireProperties.isEmpty()).isTrue();

		CacheFactory cacheFactory = (CacheFactory) cacheFactoryReference;

		cacheFactory.set("name", "TestCreateCacheFactory");

		assertThat(gemfireProperties.containsKey("name")).isTrue();
		assertThat(gemfireProperties.getProperty("name")).isEqualTo("TestCreateCacheFactory");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void initializesFactoryWithCacheFactoryInitializer() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		CacheFactoryBean.CacheFactoryInitializer<Object> mockCacheFactoryInitializer =
			mock(CacheFactoryBean.CacheFactoryInitializer.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setCacheFactoryInitializer(mockCacheFactoryInitializer);

		assertThat(cacheFactoryBean.getCacheFactoryInitializer()).isEqualTo(mockCacheFactoryInitializer);
		assertThat(cacheFactoryBean.initializeFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verify(mockCacheFactoryInitializer, times(1)).initialize(eq(mockCacheFactory));
		verifyNoInteractions(mockCacheFactory);
	}

	@Test
	public void initializesFactoryWhenNoCacheFactoryInitializerIsPresentIsNullSafe() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		assertThat(cacheFactoryBean.getCacheFactoryInitializer()).isNull();
		assertThat(cacheFactoryBean.initializeFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verifyNoInteractions(mockCacheFactory);
	}

	@Test
	public void configureFactoryWithUnspecifiedPdxOptions() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		assertThat(cacheFactoryBean.configureFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verify(mockCacheFactory, never()).setPdxDiskStore(any(String.class));
		verify(mockCacheFactory, never()).setPdxIgnoreUnreadFields(any(Boolean.class));
		verify(mockCacheFactory, never()).setPdxPersistent(any(Boolean.class));
		verify(mockCacheFactory, never()).setPdxReadSerialized(any(Boolean.class));
		verify(mockCacheFactory, never()).setPdxSerializer(any(PdxSerializer.class));

		verifyNoInteractions(mockCacheFactory);
	}

	@Test
	public void configureFactoryWithSpecificPdxOptions() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setPdxSerializer(mock(PdxSerializer.class));
		cacheFactoryBean.setPdxReadSerialized(true);
		cacheFactoryBean.setPdxIgnoreUnreadFields(false);

		assertThat(cacheFactoryBean.configureFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verify(mockCacheFactory, never()).setPdxDiskStore(any(String.class));
		verify(mockCacheFactory, times(1)).setPdxIgnoreUnreadFields(eq(false));
		verify(mockCacheFactory, never()).setPdxPersistent(any(Boolean.class));
		verify(mockCacheFactory, times(1)).setPdxReadSerialized(eq(true));
		verify(mockCacheFactory, times(1)).setPdxSerializer(any(PdxSerializer.class));

		verifyNoMoreInteractions(mockCacheFactory);
	}

	@Test
	public void configureFactoryWithAllPdxOptions() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setPdxDiskStoreName("testPdxDiskStoreName");
		cacheFactoryBean.setPdxIgnoreUnreadFields(false);
		cacheFactoryBean.setPdxPersistent(true);
		cacheFactoryBean.setPdxReadSerialized(true);
		cacheFactoryBean.setPdxSerializer(mock(PdxSerializer.class));

		assertThat(cacheFactoryBean.configureFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verify(mockCacheFactory, times(1)).setPdxDiskStore(eq("testPdxDiskStoreName"));
		verify(mockCacheFactory, times(1)).setPdxIgnoreUnreadFields(eq(false));
		verify(mockCacheFactory, times(1)).setPdxPersistent(eq(true));
		verify(mockCacheFactory, times(1)).setPdxReadSerialized(eq(true));
		verify(mockCacheFactory, times(1)).setPdxSerializer(any(PdxSerializer.class));

		verifyNoMoreInteractions(mockCacheFactory);
	}

	@Test
	public void configureFactoryWithSecurityManager() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		org.apache.geode.security.SecurityManager mockSecurityManager =
			mock(org.apache.geode.security.SecurityManager.class);

		doReturn(mockCacheFactory).when(mockCacheFactory).setSecurityManager(eq(mockSecurityManager));

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setSecurityManager(mockSecurityManager);

		assertThat(cacheFactoryBean.getSecurityManager()).isSameAs(mockSecurityManager);
		assertThat(cacheFactoryBean.configureFactory(mockCacheFactory)).isSameAs(mockCacheFactory);

		verify(mockCacheFactory, times(1)).setSecurityManager(eq(mockSecurityManager));
		verifyNoMoreInteractions(mockCacheFactory);
		verifyNoInteractions(mockSecurityManager);
	}

	@Test
	public void createCacheWithCacheFactory() {

		CacheFactory mockCacheFactory = mock(CacheFactory.class);

		doReturn(this.mockCache).when(mockCacheFactory).create();

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		Cache actualCache = cacheFactoryBean.createCache(mockCacheFactory);

		assertThat(actualCache).isEqualTo(this.mockCache);

		verify(mockCacheFactory, times(1)).create();
		verifyNoMoreInteractions(mockCacheFactory);
		verifyNoInteractions(this.mockCache);
	}

	@Test(expected = IllegalArgumentException.class)
	public void postProcessCacheWithInvalidCriticalHeapPercentage() {

		try {

			CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

			cacheFactoryBean.setCriticalHeapPercentage(200.0f);
			cacheFactoryBean.postProcess(this.mockCache);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("criticalHeapPercentage [200.0] is not valid; must be >= 0.0 and <= 100.0");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockCache);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postProcessCacheWithInvalidCriticalOffHeapPercentage() {

		try {

			CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

			cacheFactoryBean.setCriticalOffHeapPercentage(200.0f);
			cacheFactoryBean.postProcess(this.mockCache);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("criticalOffHeapPercentage [200.0] is not valid; must be >= 0.0 and <= 100.0");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockCache);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postProcessCacheWithInvalidEvictionHeapPercentage() {

		try {

			CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

			cacheFactoryBean.setEvictionHeapPercentage(-75.0f);
			cacheFactoryBean.postProcess(this.mockCache);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("evictionHeapPercentage [-75.0] is not valid; must be >= 0.0 and <= 100.0");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockCache);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void postProcessCacheWithInvalidEvictionOffHeapPercentage() {

		try {

			CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

			cacheFactoryBean.setEvictionOffHeapPercentage(-75.0f);
			cacheFactoryBean.postProcess(this.mockCache);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("evictionOffHeapPercentage [-75.0] is not valid; must be >= 0.0 and <= 100.0");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockCache);
		}
	}

	@Test
	public void getObjectTypeEqualsCacheClass() {
		assertThat(new CacheFactoryBean().getObjectType()).isEqualTo(Cache.class);
	}

	@Test
	public void getObjectTypeEqualsCacheInstanceType() {

		Cache mockCache = mock(Cache.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setCache(mockCache);

		assertThat(cacheFactoryBean.<GemFireCache>getCache()).isEqualTo(mockCache);
		assertThat(cacheFactoryBean.getObjectType()).isEqualTo(mockCache.getClass());
	}

	@Test
	public void isSingleton() {
		assertThat(new CacheFactoryBean().isSingleton()).isTrue();
	}

	@Test
	public void destroy() throws Exception {

		Cache mockCache = mock(Cache.class, "GemFireCache");

		doReturn(false).when(mockCache).isClosed();

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doReturn(mockCache).when(cacheFactoryBean).fetchCache();

		GemfireBeanFactoryLocator mockGemfireBeanFactoryLocator = mock(GemfireBeanFactoryLocator.class);

		doReturn(mockGemfireBeanFactoryLocator).when(cacheFactoryBean).getBeanFactoryLocator();

		cacheFactoryBean.setClose(true);
		cacheFactoryBean.setUseBeanFactoryLocator(true);
		cacheFactoryBean.destroy();

		verify(cacheFactoryBean, times(1)).destroy();
		verify(cacheFactoryBean, times(1)).isClose();
		verify(cacheFactoryBean, times(1)).fetchCache();
		verify(cacheFactoryBean, times(1)).close(eq(mockCache));
		verify(mockCache, times(1)).isClosed();
		verify(mockCache, times(1)).close();
		verify(mockGemfireBeanFactoryLocator, times(1)).destroy();

		verifyNoMoreInteractions(mockCache, mockGemfireBeanFactoryLocator);
	}

	@Test
	public void destroyWhenCacheIsNull() {

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		doReturn(null).when(cacheFactoryBean).fetchCache();

		cacheFactoryBean.setClose(true);
		cacheFactoryBean.setUseBeanFactoryLocator(true);
		cacheFactoryBean.destroy();

		verify(cacheFactoryBean, times(1)).destroy();
		verify(cacheFactoryBean, times(1)).isClose();
		verify(cacheFactoryBean, times(1)).fetchCache();
		verify(cacheFactoryBean, times(1)).close(isNull());
	}

	@Test
	public void destroyWhenCloseIsFalse() {

		CacheFactoryBean cacheFactoryBean = spy(new CacheFactoryBean());

		cacheFactoryBean.setClose(false);
		cacheFactoryBean.setUseBeanFactoryLocator(false);
		cacheFactoryBean.destroy();

		verify(cacheFactoryBean, times(1)).isClose();
		verify(cacheFactoryBean, never()).fetchCache();
		verify(cacheFactoryBean, never()).close(any());
	}

	@Test
	public void closeCache() {

		GemFireCache mockCache = mock(GemFireCache.class);

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setCache(mockCache);

		assertThat(cacheFactoryBean.<GemFireCache>getCache()).isEqualTo(mockCache);

		cacheFactoryBean.close(mockCache);

		assertThat(cacheFactoryBean.<GemFireCache>getCache()).isNull();

		verify(mockCache, times(1)).isClosed();
		verify(mockCache, times(1)).close();

		verifyNoMoreInteractions(mockCache);
	}

	@Test
	@SuppressWarnings("all")
	public void setAndGetCacheFactoryBeanProperties() throws Exception {

		BeanFactory mockBeanFactory = mock(BeanFactory.class, "SpringBeanFactory");

		GatewayConflictResolver mockGatewayConflictResolver =
			mock(GatewayConflictResolver.class, "GemFireGatewayConflictResolver");

		PdxSerializer mockPdxSerializer =
			mock(PdxSerializer.class, "GemFirePdxSerializer");

		Resource mockCacheXml = mock(Resource.class, "GemFireCacheXml");

		TransactionListener mockTransactionListener =
			mock(TransactionListener.class, "GemFireTransactionListener");

		TransactionWriter mockTransactionWriter =
			mock(TransactionWriter.class, "GemFireTransactionWriter");

		Properties gemfireProperties = new Properties();

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		cacheFactoryBean.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
		cacheFactoryBean.setBeanFactory(mockBeanFactory);
		cacheFactoryBean.setBeanName("TestCache");
		cacheFactoryBean.setCacheXml(mockCacheXml);
		cacheFactoryBean.setProperties(gemfireProperties);
		cacheFactoryBean.setUseBeanFactoryLocator(false);
		cacheFactoryBean.setClose(false);
		cacheFactoryBean.setCopyOnRead(true);
		cacheFactoryBean.setEnableAutoReconnect(true);
		cacheFactoryBean.setCriticalHeapPercentage(0.95f);
		cacheFactoryBean.setCriticalOffHeapPercentage(0.99f);
		cacheFactoryBean.setEvictionHeapPercentage(0.70f);
		cacheFactoryBean.setEvictionOffHeapPercentage(0.80f);
		cacheFactoryBean.setGatewayConflictResolver(mockGatewayConflictResolver);
		cacheFactoryBean.setJndiDataSources(Collections.singletonList(new CacheFactoryBean.JndiDataSource()));
		cacheFactoryBean.setLockLease(15000);
		cacheFactoryBean.setLockTimeout(5000);
		cacheFactoryBean.setMessageSyncInterval(10000);
		cacheFactoryBean.setPdxSerializer(mockPdxSerializer);
		cacheFactoryBean.setPdxReadSerialized(false);
		cacheFactoryBean.setPdxPersistent(true);
		cacheFactoryBean.setPdxIgnoreUnreadFields(true);
		cacheFactoryBean.setPdxDiskStoreName("TestPdxDiskStore");
		cacheFactoryBean.setSearchTimeout(30000);
		cacheFactoryBean.setTransactionListeners(Collections.singletonList(mockTransactionListener));
		cacheFactoryBean.setTransactionWriter(mockTransactionWriter);
		cacheFactoryBean.setUseClusterConfiguration(true);

		assertThat(cacheFactoryBean.getBeanClassLoader()).isEqualTo(Thread.currentThread().getContextClassLoader());
		assertThat(cacheFactoryBean.getBeanFactory()).isSameAs(mockBeanFactory);
		assertThat(cacheFactoryBean.getBeanFactoryLocator()).isNull();
		assertThat(cacheFactoryBean.getBeanName()).isEqualTo("TestCache");
		assertThat(cacheFactoryBean.getCacheXml()).isSameAs(mockCacheXml);
		assertThat(cacheFactoryBean.getProperties()).isSameAs(gemfireProperties);
		assertThat(Boolean.FALSE.equals(TestUtils.readField("useBeanFactoryLocator", cacheFactoryBean))).isTrue();
		assertThat(Boolean.FALSE.equals(TestUtils.readField("close", cacheFactoryBean))).isTrue();
		assertThat(cacheFactoryBean.getCopyOnRead()).isTrue();
		assertThat(cacheFactoryBean.getCriticalHeapPercentage()).isCloseTo(0.95f, offset(0.0f));
		assertThat(cacheFactoryBean.getCriticalOffHeapPercentage()).isCloseTo(0.99f, offset(0.0f));
		assertThat(cacheFactoryBean.getEnableAutoReconnect()).isTrue();
		assertThat(cacheFactoryBean.getEvictionHeapPercentage()).isCloseTo(0.70f, offset(0.0f));
		assertThat(cacheFactoryBean.getEvictionOffHeapPercentage()).isCloseTo(0.80f, offset(0.0f));
		assertThat(cacheFactoryBean.getGatewayConflictResolver()).isSameAs(mockGatewayConflictResolver);
		assertThat(cacheFactoryBean.getJndiDataSources()).isNotNull();
		assertThat(cacheFactoryBean.getJndiDataSources().size()).isEqualTo(1);
		assertThat(cacheFactoryBean.getLockLease().intValue()).isEqualTo(15000);
		assertThat(cacheFactoryBean.getLockTimeout().intValue()).isEqualTo(5000);
		assertThat(cacheFactoryBean.getMessageSyncInterval().intValue()).isEqualTo(10000);
		assertThat(cacheFactoryBean.getPdxSerializer()).isSameAs(mockPdxSerializer);
		assertThat(cacheFactoryBean.getPdxReadSerialized()).isFalse();
		assertThat(cacheFactoryBean.getPdxPersistent()).isTrue();
		assertThat(cacheFactoryBean.getPdxIgnoreUnreadFields()).isTrue();
		assertThat(cacheFactoryBean.getPdxDiskStoreName()).isEqualTo("TestPdxDiskStore");
		assertThat(cacheFactoryBean.getSearchTimeout().intValue()).isEqualTo(30000);
		assertThat(cacheFactoryBean.getTransactionListeners()).isNotNull();
		assertThat(cacheFactoryBean.getTransactionListeners().size()).isEqualTo(1);
		assertThat(cacheFactoryBean.getTransactionListeners().get(0)).isSameAs(mockTransactionListener);
		assertThat(cacheFactoryBean.getTransactionWriter()).isSameAs(mockTransactionWriter);
		assertThat(cacheFactoryBean.getUseClusterConfiguration()).isTrue();
	}
}
