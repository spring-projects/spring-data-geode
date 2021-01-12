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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean.CacheResolverProxy;
import org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean.CompositionStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link SmartCacheResolverFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings({ "rawtypes", "unused" })
public class SmartCacheResolverFactoryBeanIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	// Primary (SUT)
	private CacheResolver cacheResolver;

	@Autowired
	@Qualifier("A")
	private CacheResolver a;

	@Autowired
	@Qualifier("B")
	private CacheResolver b;

	@Autowired
	@Qualifier("C")
	private CacheResolver c;

	@Autowired
	@Qualifier("D")
	private CacheResolver d;

	@Autowired
	@Qualifier("U")
	private CacheResolver u;

	@Autowired
	@Qualifier("X")
	private CacheResolver x;

	@Autowired
	@Qualifier("Y")
	private CacheResolver y;

	@Autowired
	@Qualifier("Z")
	private CacheResolver z;

	@Autowired
	private CacheResolver[] cacheResolvers;

	@Autowired
	private ClientCache clientCache;

	@Test
	public void cacheResolversAreInjectedInOrder() {
		assertThat(this.cacheResolvers).containsExactly(this.y, this.x, this.z, this.b, this.c, this.a, this.d,
			this.cacheResolver, this.u);
	}

	@Test
	public void resolvesCache() {

		assertThat(this.cacheResolver).isInstanceOf(CacheResolverProxy.class);
		assertThat(((CacheResolverProxy) this.cacheResolver).getCacheResolver().orElse(null))
			.isInstanceOf(ComposableCacheResolver.class);
		assertThat(this.cacheResolver.resolve()).isEqualTo(this.clientCache);

		InOrder inOrder = inOrder(this.a, this.b, this.c, this.d, this.u, this.x, this.y, this.z);

		inOrder.verify(this.y, times(1)).resolve();
		inOrder.verify(this.x, times(1)).resolve();
		inOrder.verify(this.z, times(1)).resolve();
		inOrder.verify(this.b, atLeastOnce()).resolve();
		inOrder.verify(this.c, times(1)).resolve();
		inOrder.verify(this.a, times(1)).resolve();
		inOrder.verify(this.d, never()).resolve();
		inOrder.verify(this.u, never()).resolve();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		ClientCache mockClientCache() {
			return mock(ClientCache.class);
		}

		@Primary
		@Bean("SmartCacheResolver")
		SmartCacheResolverFactoryBean smartCacheResolver() {
			return SmartCacheResolverFactoryBean.create()
				.usingCompositionStrategy(CompositionStrategy.USER_DEFINED);
		}

		@Bean("A")
		@Order(3)
		CacheResolver a(ClientCache clientCache) {

			CacheResolver mockCacheResolver =  mock(CacheResolver.class, "A");

			when(mockCacheResolver.resolve()).thenReturn(clientCache);

			return mockCacheResolver;
		}

		@Bean("B")
		@Order(1)
		CacheResolver b() {

			CacheResolver mockCacheResolver =  mock(CacheResolver.class, "B");

			when(mockCacheResolver.resolve()).thenThrow(new CacheClosedException("TEST"));

			return mockCacheResolver;
		}

		@Bean("C")
		@Order(2)
		CacheResolver c() {
			return mock(CacheResolver.class, "C");
		}

		@Bean("D")
		@Order(4)
		CacheResolver d() {
			return mock(CacheResolver.class, "D");
		}

		@Bean("U")
		CacheResolver unorderedCacheResolver() {
			return mock(CacheResolver.class, "U");
		}

		@Bean("X")
		CacheResolver x() {
			return spy(new AtOrderCacheResolver());
		}

		@Bean("Y")
		CacheResolver y() {
			return spy(new OrderedCacheResolver());
		}

		@Bean("Z")
		CacheResolver z() {
			return spy(new Z());
		}
	}

	@Order(-1)
	static class AtOrderCacheResolver implements CacheResolver<GemFireCache> {

		@Override
		public GemFireCache resolve() {
			return null;
		}
	}

	static class OrderedCacheResolver implements CacheResolver<GemFireCache>, Ordered {

		@Override
		public int getOrder() {
			return -2;
		}

		@Override
		public GemFireCache resolve() {
			return null;
		}
	}

	@Order(0)
	static abstract class ZeroOrderedCacheResolver implements CacheResolver<GemFireCache> {

		@Override
		public GemFireCache resolve() {
			return null;
		}
	}

	static class Z extends ZeroOrderedCacheResolver { }

}
