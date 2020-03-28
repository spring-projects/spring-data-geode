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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.data.gemfire.CacheResolver;

/**
 * Unit Tests for {@link SingleCacheCacheResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.SingleCacheCacheResolver
 * @since 2.3.0
 */
public class SingleCacheCacheResolverUnitTests {

	@Test
	public void fromCacheReturnsCacheResolverResolvingCache() {

		Cache mockCache = mock(Cache.class);

		CacheResolver<Cache> cacheResolver = SingleCacheCacheResolver.from(mockCache);

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.resolve()).isSameAs(mockCache);

		verifyNoInteractions(mockCache);
	}

	@Test
	public void fromNullCacheReturnsCacheResolverReturningNull() {

		CacheResolver<Cache> cacheResolver = SingleCacheCacheResolver.from((Cache) null);

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.resolve()).isNull();
	}

	@Test
	public void fromClientCacheReturnsCacheResolverResolvingClientCache() {

		ClientCache mockClientCache = mock(ClientCache.class);

		CacheResolver<ClientCache> clientCacheResolver = SingleCacheCacheResolver.from(mockClientCache);

		assertThat(clientCacheResolver).isNotNull();
		assertThat(clientCacheResolver.resolve()).isSameAs(mockClientCache);

		verifyNoInteractions(mockClientCache);
	}

	@Test
	public void fromNullClientCacheReturnsCacheResolverReturningNull() {

		CacheResolver<ClientCache> cacheResolver = SingleCacheCacheResolver.from((ClientCache) null);

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.resolve()).isNull();
	}
}
