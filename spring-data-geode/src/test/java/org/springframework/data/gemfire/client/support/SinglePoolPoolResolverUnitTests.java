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
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.client.PoolResolver;

/**
 * Unit Tests for {@link SinglePoolPoolResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.client.support.SinglePoolPoolResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SinglePoolPoolResolverUnitTests {

	@Mock
	private Pool mockPool;

	@Test
	public void constructSinglePoolPoolResolver() {

		SinglePoolPoolResolver poolResolver = new SinglePoolPoolResolver(this.mockPool);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getPool()).isEqualTo(this.mockPool);
	}

	@SuppressWarnings("all")
	@Test(expected = IllegalArgumentException.class)
	public void constructSinglePoolPoolResolverWithNullPoolThrowsIllegalArgumentException() {

		try {
			new SinglePoolPoolResolver(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pool must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void fromClientCacheWithDefaultPool() {

		ClientCache mockClientCache = mock(ClientCache.class);

		when(mockClientCache.getDefaultPool()).thenReturn(this.mockPool);

		SinglePoolPoolResolver poolResolver = SinglePoolPoolResolver.from(mockClientCache);

		assertThat(poolResolver).isNotNull();
		assertThat(poolResolver.getPool()).isEqualTo(this.mockPool);

		verify(mockClientCache, times(1)).getDefaultPool();
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromClientCacheWithNoDefaultPoolThrowsIllegalArgumentException() {

		ClientCache mockClientCache = mock(ClientCache.class);

		try {
			SinglePoolPoolResolver.from(mockClientCache);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pool must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockClientCache, times(1)).getDefaultPool();
		}
	}

	@SuppressWarnings("all")
	@Test(expected = IllegalArgumentException.class)
	public void fromNullClientCacheThrowsIllegalArgumentException() {

		try {
			SinglePoolPoolResolver.from(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("ClientCache must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void resolvesReturnsPoolWhenNamesMatch() {

		PoolResolver poolResolver = new SinglePoolPoolResolver(this.mockPool);

		when(this.mockPool.getName()).thenReturn("TestPool");

		assertThat(poolResolver.resolve("TestPool")).isEqualTo(this.mockPool);

		verify(this.mockPool, times(1)).getName();
	}

	@Test
	public void resolveReturnsNullWhenNamesDoNotMatch() {

		PoolResolver poolResolver = new SinglePoolPoolResolver(this.mockPool);

		when(this.mockPool.getName()).thenReturn("TestPool");

		assertThat(poolResolver.resolve("MockPool")).isNull();
		assertThat(poolResolver.resolve("  ")).isNull();
		assertThat(poolResolver.resolve("")).isNull();

		verify(this.mockPool, times(3)).getName();
	}

	@Test
	public void resolveIsNullSafe() {

		PoolResolver poolResolver = new SinglePoolPoolResolver(this.mockPool);

		when(this.mockPool.getName()).thenReturn("TestPool");

		assertThat(poolResolver.resolve((String) null)).isNull();

		verify(this.mockPool, times(1)).getName();
	}
}
