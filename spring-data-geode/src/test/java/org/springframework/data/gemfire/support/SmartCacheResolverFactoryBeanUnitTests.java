/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.client.support.ClientCacheFactoryCacheResolver;
import org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean.CacheResolverProxy;
import org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean.CompositionStrategy;

/**
 * Unit Tests for {@link SmartCacheResolverFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean
 * @since 2.3.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SmartCacheResolverFactoryBeanUnitTests {

	private CacheResolver<GemFireCache> resolveCacheResolverFromProxy(CacheResolver<GemFireCache> cacheResolver) {

		return cacheResolver instanceof CacheResolverProxy
			? ((CacheResolverProxy) cacheResolver).getCacheResolver().orElse(null)
			: null;
	}

	@Test
	public void factoryBeanUsingDefaultCompositionStrategyIsFullyLoaded() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClientCache mockClientCache = mock(ClientCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCache(mockClientCache);
		factoryBean.setCacheBeanName("TestCacheBeanName");

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockClientCache);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.DEFAULT);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);

		ComposableCacheResolver<?> composed = (ComposableCacheResolver<?>) cacheResolver;

		CacheResolver<?> one = composed.getCacheResolverOne();
		CacheResolver<?> two = composed.getCacheResolverTwo();

		assertThat(one).isInstanceOf(ComposableCacheResolver.class);
		assertThat(two).isInstanceOf(CacheFactoryCacheResolver.class);

		two = ((ComposableCacheResolver<?>) one).getCacheResolverTwo();
		one = ((ComposableCacheResolver<?>) one).getCacheResolverOne();

		assertThat(one).isInstanceOf(ComposableCacheResolver.class);
		assertThat(two).isInstanceOf(ClientCacheFactoryCacheResolver.class);

		two = ((ComposableCacheResolver<?>) one).getCacheResolverTwo();
		one = ((ComposableCacheResolver<?>) one).getCacheResolverOne();

		assertThat(one.getClass().getName()).startsWith(SingleCacheCacheResolver.class.getName());
		assertThat(one.resolve()).isEqualTo(mockClientCache);
		assertThat(two).isInstanceOf(BeanFactoryCacheResolver.class);
		assertThat(((BeanFactoryCacheResolver) two).getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(((BeanFactoryCacheResolver) two).getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
	}

	@Test
	public void factoryBeanUsingDefaultCompositionStrategyIsConditionallyComposed() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getBeanFactory()).isNull();
		assertThat(factoryBean.getCache().orElse(null)).isNull();
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);
		assertThat(((ComposableCacheResolver) cacheResolver).getCacheResolverOne())
			.isInstanceOf(ClientCacheFactoryCacheResolver.class);
		assertThat(((ComposableCacheResolver) cacheResolver).getCacheResolverTwo())
			.isInstanceOf(CacheFactoryCacheResolver.class);
	}

	@Test
	public void factoryBeanUsingDefaultCompositionStrategyResolvesCacheInOrder() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		Cache mockPeerCache = mock(Cache.class);

		CacheResolver mockBeanFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockClientCacheFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockPeerCacheFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockSingleCacheCacheResolver = mock(CacheResolver.class);

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		doReturn(mockBeanFactoryCacheResolver).when(factoryBean).newBeanFactoryCacheResolver();
		doReturn(mockClientCacheFactoryCacheResolver).when(factoryBean).newClientCacheFactoryCacheResolver();
		doReturn(mockPeerCacheFactoryCacheResolver).when(factoryBean).newPeerCacheFactoryCacheResolver();
		doReturn(mockSingleCacheCacheResolver).when(factoryBean).newSingleCacheCacheResolver();

		doThrow(new NoSuchBeanDefinitionException("TEST")).when(mockBeanFactoryCacheResolver).resolve();
		doReturn(mockPeerCache).when(mockPeerCacheFactoryCacheResolver).resolve();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCache(mockPeerCache);
		factoryBean.setCacheBeanName("TestCacheBeanName");

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockPeerCache);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.DEFAULT);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.resolve()).isEqualTo(mockPeerCache);

		InOrder inOrder = inOrder(mockBeanFactoryCacheResolver, mockClientCacheFactoryCacheResolver,
			mockPeerCacheFactoryCacheResolver, mockSingleCacheCacheResolver);

		inOrder.verify(mockSingleCacheCacheResolver, times(1)).resolve();
		inOrder.verify(mockBeanFactoryCacheResolver, atLeastOnce()).resolve();
		inOrder.verify(mockClientCacheFactoryCacheResolver, times(1)).resolve();
		inOrder.verify(mockPeerCacheFactoryCacheResolver, times(1)).resolve();
	}

	@Test
	public void factoryBeanUsingGeodeCompositionStrategyIsFullyLoaded() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClientCache mockClientCache = mock(ClientCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCache(mockClientCache);
		factoryBean.setCompositionStrategy(CompositionStrategy.GEODE);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockClientCache);
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.GEODE);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);

		CacheResolver<?> one = ((ComposableCacheResolver) cacheResolver).getCacheResolverOne();
		CacheResolver<?> two = ((ComposableCacheResolver) cacheResolver).getCacheResolverTwo();

		assertThat(one).isInstanceOf(ComposableCacheResolver.class);
		assertThat(two).isInstanceOf(CacheFactoryCacheResolver.class);

		two = ((ComposableCacheResolver) one).getCacheResolverTwo();
		one = ((ComposableCacheResolver) one).getCacheResolverOne();

		assertThat(one.getClass().getName()).startsWith(SingleCacheCacheResolver.class.getName());
		assertThat(two).isInstanceOf(ClientCacheFactoryCacheResolver.class);
	}

	@Test
	public void factoryBeanUsingGeodeCompositionStrategyIsConditionallyComposed() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCompositionStrategy(CompositionStrategy.GEODE);

		assertThat(factoryBean.getCache().orElse(null)).isNull();
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.GEODE);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);

		CacheResolver<?> one = ((ComposableCacheResolver) cacheResolver).getCacheResolverOne();
		CacheResolver<?> two = ((ComposableCacheResolver) cacheResolver).getCacheResolverTwo();

		assertThat(one).isInstanceOf(ClientCacheFactoryCacheResolver.class);
		assertThat(two).isInstanceOf(CacheFactoryCacheResolver.class);
	}

	@Test
	public void factoryBeanUsingGeodeCompositionStrategyResolvesCacheInOrder() {

		Cache mockPeerCache = mock(Cache.class);

		CacheResolver mockClientCacheFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockPeerCacheFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockSingleCacheCacheResolver = mock(CacheResolver.class);

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		doReturn(mockClientCacheFactoryCacheResolver).when(factoryBean).newClientCacheFactoryCacheResolver();
		doReturn(mockPeerCacheFactoryCacheResolver).when(factoryBean).newPeerCacheFactoryCacheResolver();
		doReturn(mockSingleCacheCacheResolver).when(factoryBean).newSingleCacheCacheResolver();

		doThrow(new CacheClosedException("TEST")).when(mockClientCacheFactoryCacheResolver).resolve();
		doReturn(mockPeerCache).when(mockPeerCacheFactoryCacheResolver).resolve();

		factoryBean.setCache(mockPeerCache);
		factoryBean.setCompositionStrategy(CompositionStrategy.GEODE);

		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockPeerCache);
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.GEODE);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver.resolve()).isEqualTo(mockPeerCache);

		InOrder inOrder = inOrder(mockClientCacheFactoryCacheResolver,
			mockPeerCacheFactoryCacheResolver, mockSingleCacheCacheResolver);

		inOrder.verify(mockSingleCacheCacheResolver, times(1)).resolve();
		inOrder.verify(mockClientCacheFactoryCacheResolver, atLeastOnce()).resolve();
		inOrder.verify(mockPeerCacheFactoryCacheResolver, times(1)).resolve();
	}

	@Test
	public void factoryBeanUsingSpringCompositionStrategyIsFullyLoaded() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ClientCache mockClientCache = mock(ClientCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCache(mockClientCache);
		factoryBean.setCacheBeanName("TestCacheBeanName");
		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockClientCache);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);

		CacheResolver<?> one = ((ComposableCacheResolver) cacheResolver).getCacheResolverOne();
		CacheResolver<?> two = ((ComposableCacheResolver) cacheResolver).getCacheResolverTwo();

		assertThat(one.getClass().getName()).startsWith(SingleCacheCacheResolver.class.getName());
		assertThat(two).isInstanceOf(BeanFactoryCacheResolver.class);
		assertThat(((BeanFactoryCacheResolver) two).getCacheBeanName().orElse(null))
			.isEqualTo("TestCacheBeanName");
	}

	@Test
	public void factoryBeanUsingSpringCompositionStrategyIsConditionallyComposedWithBeanFactoryCacheLoader() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCache().orElse(null)).isNull();
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isInstanceOf(BeanFactoryCacheResolver.class);
		assertThat(((BeanFactoryCacheResolver) cacheResolver).getCacheBeanName().orElse(null)).isNull();
	}

	@Test
	public void factoryBeanUsingSpringCompositionStrategyIsConditionallyComposedWithSingleCacheCacheLoader() {

		ClientCache mockClientCache = mock(ClientCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCache(mockClientCache);
		factoryBean.setCacheBeanName("TestCacheBeanName");
		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getBeanFactory()).isNull();
		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockClientCache);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.getClass().getName()).startsWith(SingleCacheCacheResolver.class.getName());
	}

	@Test(expected = IllegalStateException.class)
	public void factoryBeanUsingSpringCompositionStrategyComposesNoCacheResolverThrowsIllegalStateException() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCacheBeanName("TestCacheBeanName");
		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getBeanFactory()).isNull();
		assertThat(factoryBean.getCache().orElse(null)).isNull();
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		try {
			factoryBean.afterPropertiesSet();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("No CacheResolver was composed with CompositionStrategy [%s]",
				CompositionStrategy.SPRING);

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(resolveCacheResolverFromProxy(factoryBean.getObject())).isNull();
		}
	}

	@Test
	public void factoryBeanUsingSpringCompositionStrategyResolvesCacheInOrder() {

		Cache mockCache = mock(Cache.class);

		CacheResolver mockBeanFactoryCacheResolver = mock(CacheResolver.class);
		CacheResolver mockSingleCacheCacheResolver = mock(CacheResolver.class);

		doReturn(mockCache).when(mockBeanFactoryCacheResolver).resolve();

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		doReturn(mockBeanFactoryCacheResolver).when(factoryBean).newBeanFactoryCacheResolver();
		doReturn(mockSingleCacheCacheResolver).when(factoryBean).newSingleCacheCacheResolver();

		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);
		assertThat(cacheResolver.resolve()).isEqualTo(mockCache);

		InOrder inOrder = inOrder(mockBeanFactoryCacheResolver, mockSingleCacheCacheResolver);

		inOrder.verify(mockSingleCacheCacheResolver, times(1)).resolve();
		inOrder.verify(mockBeanFactoryCacheResolver, times(1)).resolve();
	}

	@Test
	public void factoryBeanUsingUserDefinedCompositionStrategy() {

		// Questionable?? Simulate Spring container behavior (i.e. BeanFactory.getBeanProvider(..).orderedStream())
		// by returning an ordered (sorted) Stream based on @Order annotation/Ordered interface.
		Stream<CacheResolver<? extends GemFireCache>> cacheResolverStream = Stream.of(spy(new A()), spy(new B()))
			.sorted(AnnotationAwareOrderComparator.INSTANCE);

		ObjectProvider<CacheResolver<? extends GemFireCache>> mockObjectProvider = mock(ObjectProvider.class);

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		when(mockObjectProvider.orderedStream()).thenReturn(cacheResolverStream);
		when(mockBeanFactory.getBeanProvider(ArgumentMatchers.<Class>any())).thenReturn(mockObjectProvider);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCompositionStrategy(CompositionStrategy.USER_DEFINED);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.USER_DEFINED);

		factoryBean.afterPropertiesSet();

		CacheResolver<?> cacheResolver = resolveCacheResolverFromProxy(factoryBean.getObject());

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(ComposableCacheResolver.class);

		CacheResolver<?> one = ((ComposableCacheResolver) cacheResolver).getCacheResolverOne();
		CacheResolver<?> two = ((ComposableCacheResolver) cacheResolver).getCacheResolverTwo();

		assertThat(one).isInstanceOf(B.class);
		assertThat(two).isInstanceOf(A.class);
		assertThat(cacheResolver.resolve()).isEqualTo(B.MOCK_CACHE);

		InOrder inOrder = inOrder(one, two);

		inOrder.verify(one, times(1)).resolve();
		inOrder.verify(two, never()).resolve();

		verify(mockBeanFactory, times(1)).getBeanProvider(eq(CacheResolver.class));
		verify(mockObjectProvider, times(1)).orderedStream();
	}

	@Test(expected = IllegalStateException.class)
	public void factoryBeanUsingUserDefinedCompositionStrategyWithNoBeanFactory() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCompositionStrategy(CompositionStrategy.USER_DEFINED);

		assertThat(factoryBean.getBeanFactory()).isNull();
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.USER_DEFINED);

		try {
			factoryBean.afterPropertiesSet();
		}
		catch (IllegalStateException expected) {

			String expectedMessage = "A Spring context is not present and is required to use [%1$s.%2$s.%3$s]";

			assertThat(expected).hasMessage(expectedMessage, SmartCacheResolverFactoryBean.class.getSimpleName(),
				CompositionStrategy.class.getSimpleName(), CompositionStrategy.USER_DEFINED);

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(resolveCacheResolverFromProxy(factoryBean.getObject())).isNull();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void factoryBeanUsingUserDefinedCompositionStrategyWithNoCacheResolverBeans() {

		Stream<CacheResolver> mockStream = mock(Stream.class);

		ObjectProvider mockBeanProvider = mock(ObjectProvider.class);

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		when(mockBeanProvider.orderedStream()).thenReturn(mockStream);
		when(mockBeanFactory.getBeanProvider(eq(CacheResolver.class))).thenReturn(mockBeanProvider);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCompositionStrategy(CompositionStrategy.USER_DEFINED);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.USER_DEFINED);

		try {
			factoryBean.afterPropertiesSet();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("No CacheResolver beans were defined");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {

			assertThat(resolveCacheResolverFromProxy(factoryBean.getObject())).isNull();

			verify(mockBeanFactory, times(1)).getBeanProvider(eq(CacheResolver.class));
			verify(mockBeanProvider, times(1)).orderedStream();
		}
	}

	@Test
	public void getObjectReturnsProxy() {

		CacheResolver mockCacheResolver = mock(CacheResolver.class);

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		CacheResolver cacheResolver = factoryBean.getObject();

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver).isInstanceOf(CacheResolverProxy.class);
		assertThat(((CacheResolverProxy) cacheResolver).getCacheResolver().orElse(null)).isNull();
		assertThat(((CacheResolverProxy) cacheResolver).getCacheResolverFactoryBean()).isSameAs(factoryBean);

		doReturn(mockCacheResolver).when(factoryBean).getCacheResolver();

		assertThat(((CacheResolverProxy) cacheResolver).getCacheResolver().orElse(null))
			.isEqualTo(mockCacheResolver);
	}

	@Test
	public void getObjectTypeReturnsCacheResolverClassType() {

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		doReturn(null).when(factoryBean).getCacheResolver();

		assertThat(factoryBean.getObjectType()).isEqualTo(CacheResolver.class);

		verify(factoryBean, never()).getObject();
	}

	@Test
	public void getObjectTypeReturnsCacheResolverInstanceType() {

		CacheResolver mockCacheResolver = mock(CacheResolver.class);

		SmartCacheResolverFactoryBean factoryBean = spy(new SmartCacheResolverFactoryBean());

		doReturn(mockCacheResolver).when(factoryBean).getCacheResolver();

		assertThat(factoryBean.getObjectType()).isEqualTo(mockCacheResolver.getClass());

		verify(factoryBean, never()).getObject();
	}

	@Test
	public void newBeanFactoryCacheResolverWithCacheBeanName() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);
		factoryBean.setCacheBeanName("TestCacheBeanName");

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");

		CacheResolver beanFactoryCacheResolver = factoryBean.newBeanFactoryCacheResolver();

		assertThat(beanFactoryCacheResolver).isInstanceOf(BeanFactoryCacheResolver.class);
		assertThat(((BeanFactoryCacheResolver) beanFactoryCacheResolver).getCacheBeanName().orElse(null))
			.isEqualTo("TestCacheBeanName");

		verifyNoInteractions(mockBeanFactory);
	}

	@Test
	public void newBeanFactoryCacheResolverWithNoCacheBeanName() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setBeanFactory(mockBeanFactory);

		assertThat(factoryBean.getBeanFactory()).isEqualTo(mockBeanFactory);
		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();

		CacheResolver beanFactoryCacheResolver = factoryBean.newBeanFactoryCacheResolver();

		assertThat(beanFactoryCacheResolver).isInstanceOf(BeanFactoryCacheResolver.class);
		assertThat(((BeanFactoryCacheResolver) beanFactoryCacheResolver).getCacheBeanName().orElse(null)).isNull();

		verifyNoInteractions(mockBeanFactory);
	}

	@Test
	public void newBeanFactoryCacheResolverWithNullBeanFactory() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getBeanFactory()).isNull();
		assertThat(factoryBean.newBeanFactoryCacheResolver()).isNull();
	}

	@Test
	public void newClientCacheFactoryCacheResolverIsNotNull() {
		assertThat(new SmartCacheResolverFactoryBean().newClientCacheFactoryCacheResolver())
			.isInstanceOf(ClientCacheFactoryCacheResolver.class);
	}

	@Test
	public void newPeerCacheFactoryCacheResolverIsNotNull() {
		assertThat(new SmartCacheResolverFactoryBean().newPeerCacheFactoryCacheResolver())
			.isInstanceOf(CacheFactoryCacheResolver.class);
	}

	@Test
	public void newSingleCacheCacheResolverWithClientCache() {

		ClientCache mockClientCache = mock(ClientCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCache(mockClientCache);

		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockClientCache);
		assertThat(factoryBean.newSingleCacheCacheResolver().getClass().getName())
			.startsWith(SingleCacheCacheResolver.class.getName());
	}

	@Test
	public void newSingleCacheCacheResolverWithPeerCache() {

		Cache mockPeerCache = mock(Cache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		factoryBean.setCache(mockPeerCache);

		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockPeerCache);
		assertThat(factoryBean.newSingleCacheCacheResolver().getClass().getName())
			.startsWith(SingleCacheCacheResolver.class.getName());
	}

	@Test
	public void newSingleCacheCacheResolverWithNullCache() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getCache().orElse(null)).isNull();
		assertThat(factoryBean.newSingleCacheCacheResolver()).isNull();
	}

	@Test
	public void setAndGetCache() {

		GemFireCache mockCache = mock(GemFireCache.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getCache().orElse(null)).isNull();

		factoryBean.setCache(mockCache);

		assertThat(factoryBean.getCache().orElse(null)).isEqualTo(mockCache);

		factoryBean.setCache(null);

		assertThat(factoryBean.getCache().orElse(null)).isNull();
	}

	@Test
	public void setAndGetCacheBeanName() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();

		factoryBean.setCacheBeanName("TestCache");

		assertThat(factoryBean.getCacheBeanName().orElse(null)).isEqualTo("TestCache");

		factoryBean.setCacheBeanName("  ");

		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();

		factoryBean.setCacheBeanName("");

		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();

		factoryBean.setCacheBeanName(null);

		assertThat(factoryBean.getCacheBeanName().orElse(null)).isNull();
	}

	@Test
	public void setAndGetCompositionStrategy() {

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.DEFAULT);

		factoryBean.setCompositionStrategy(CompositionStrategy.SPRING);

		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.SPRING);

		factoryBean.setCompositionStrategy(null);

		assertThat(factoryBean.getCompositionStrategy()).isEqualTo(CompositionStrategy.DEFAULT);
	}

	@Test
	public void setAndGetConfiguredCacheResolvers() {

		CacheResolver a = mock(CacheResolver.class);
		CacheResolver b = mock(CacheResolver.class);
		CacheResolver c = mock(CacheResolver.class);

		List<CacheResolver> cacheResolvers = Arrays.asList(a, null, b, c, null, null);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		assertThat(factoryBean.getConfiguredCacheResolvers()).isNull();

		factoryBean.setConfiguredCacheResolvers(cacheResolvers);

		assertThat(factoryBean.getConfiguredCacheResolvers()).containsExactly(a, b, c);
	}

	@Test
	public void setAndGetConfiguredCacheResolversWithDifferentFactoryBeanCacheResolverReference() {

		SmartCacheResolverFactoryBean factoryBeanOne = new SmartCacheResolverFactoryBean();

		CacheResolver a = mock(CacheResolver.class);
		CacheResolver b = mock(CacheResolver.class);
		CacheResolver c = factoryBeanOne.getObject();

		SmartCacheResolverFactoryBean factoryBeanTwo = new SmartCacheResolverFactoryBean();

		List<CacheResolver> cacheResolvers = Arrays.asList(a, b, c);

		factoryBeanTwo.setConfiguredCacheResolvers(cacheResolvers);

		assertThat(factoryBeanTwo.getConfiguredCacheResolvers()).containsExactly(a, b, c);
	}

	@Test
	public void setAndGetConfiguredCacheResolversWithSelfReference() {

		CacheResolver a = mock(CacheResolver.class);
		CacheResolver b = mock(CacheResolver.class);

		SmartCacheResolverFactoryBean factoryBean = new SmartCacheResolverFactoryBean();

		List<CacheResolver> cacheResolvers = Arrays.asList(a, b, factoryBean.getObject());

		factoryBean.setConfiguredCacheResolvers(cacheResolvers);

		assertThat(factoryBean.getConfiguredCacheResolvers()).containsExactly(a, b);
	}

	static class A implements CacheResolver<ClientCache>, Ordered {

		@Override
		public ClientCache resolve() {
			throw new UnsupportedOperationException("Not Implemented");
		}

		@Override
		public int getOrder() {
			return 2;
		}
	}

	@Order(1)
	static class B implements CacheResolver<Cache> {

		static final Cache MOCK_CACHE = mock(Cache.class);

		@Override
		public Cache resolve() {
			return MOCK_CACHE;
		}
	}
}
