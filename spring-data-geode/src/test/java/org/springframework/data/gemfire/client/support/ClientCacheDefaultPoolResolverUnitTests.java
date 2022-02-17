/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.gemfire.client.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.client.PoolResolver;

/**
 * Unit Tests for {@link ClientCacheDefaultPoolResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @see org.springframework.data.gemfire.client.support.ClientCacheDefaultPoolResolver
 * @since 2.3.0
 */
public class ClientCacheDefaultPoolResolverUnitTests {

	@Test
	@SuppressWarnings("unchecked")
	public void constructClientCacheDefaultPoolResolver() {

		CacheResolver<ClientCache> mockClientCacheResolver = mock(CacheResolver.class);

		ClientCacheDefaultPoolResolver poolResolver = new ClientCacheDefaultPoolResolver(mockClientCacheResolver);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getClientCacheResolver()).isEqualTo(mockClientCacheResolver);

		verifyNoInteractions(mockClientCacheResolver);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructClientCacheDefaultPoolResolverWithNullCacheResolver() {

		try {
			new ClientCacheDefaultPoolResolver(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("CacheResolver for ClientCache must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void resolvePoolFromClientCacheReturnsDefaultPool() {

		ClientCache mockClientCache = mock(ClientCache.class);

		Pool mockPool = mock(Pool.class, PoolResolver.DEFAULT_POOL_NAME);

		when(mockClientCache.getDefaultPool()).thenReturn(mockPool);

		CacheResolver<ClientCache> clientCacheResolver = () -> mockClientCache;

		ClientCacheDefaultPoolResolver poolResolver = new ClientCacheDefaultPoolResolver(clientCacheResolver);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getClientCacheResolver()).isEqualTo(clientCacheResolver);
		assertThat(poolResolver.resolve(PoolResolver.DEFAULT_POOL_NAME)).isEqualTo(mockPool);

		verify(mockClientCache, times(1)).getDefaultPool();
		verifyNoInteractions(mockPool);
	}

	@Test
	public void resolvePoolWhenClientCacheResolvesToNullIsNullSafe() {

		CacheResolver<ClientCache> clientCacheResolver = () -> null;

		ClientCacheDefaultPoolResolver poolResolver = new ClientCacheDefaultPoolResolver(clientCacheResolver);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getClientCacheResolver()).isEqualTo(clientCacheResolver);
		assertThat(poolResolver.resolve(PoolResolver.DEFAULT_POOL_NAME)).isNull();
	}

	@Test
	public void resolvePoolWithNonDefaultPool() {

		ClientCache mockClientCache = mock(ClientCache.class);

		CacheResolver<ClientCache> clientCacheResolver = () -> mockClientCache;

		ClientCacheDefaultPoolResolver poolResolver = new ClientCacheDefaultPoolResolver(clientCacheResolver);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getClientCacheResolver()).isEqualTo(clientCacheResolver);
		assertThat(poolResolver.resolve("CUSTOM")).isNull();

		verifyNoInteractions(mockClientCache);
	}
}
