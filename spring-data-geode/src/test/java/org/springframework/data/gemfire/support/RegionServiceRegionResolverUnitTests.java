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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.support.RegionServiceRegionResolver.RegionServiceResolver;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.AttributesMutator;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.data.gemfire.CacheResolver;

/**
 * Unit Tests for {@link RegionServiceRegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.RegionService
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.RegionServiceRegionResolver
 * @see org.springframework.data.gemfire.support.RegionServiceRegionResolver.RegionServiceResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RegionServiceRegionResolverUnitTests {

	@Mock
	private Region mockRegion;

	@Before
	public void setupMockRegion() {

		AttributesMutator mockAttributesMutator = mock(AttributesMutator.class);

		when(this.mockRegion.getAttributesMutator()).thenReturn(mockAttributesMutator);
		when(mockAttributesMutator.getRegion()).thenReturn(this.mockRegion);
	}

	@Test
	public void constructRegionServiceRegionResolverWithNonNullRegionServiceResolver() {

		RegionServiceResolver mockRegionServiceResolver = mock(RegionServiceResolver.class);

		RegionServiceRegionResolver regionResolver = new RegionServiceRegionResolver(mockRegionServiceResolver);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isEqualTo(mockRegionServiceResolver);

		verifyNoInteractions(mockRegionServiceResolver);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructRegionServiceRegionResolverWithNull() {

		try {
			new RegionServiceRegionResolver<>(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("RegionServiceResolver must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void fromNonNullCacheResolverResolvingClientCache() {

		ClientCache mockClientCache = mock(ClientCache.class);

		when(mockClientCache.getRegion(anyString())).thenReturn(this.mockRegion);

		CacheResolver<ClientCache> mockClientCacheResolver = mock(CacheResolver.class);

		when(mockClientCacheResolver.resolve()).thenReturn(mockClientCache);

		RegionServiceRegionResolver regionResolver = RegionServiceRegionResolver.from(mockClientCacheResolver);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isNotNull();
		assertThat(regionResolver.doResolve("TestRegion")).isEqualTo(this.mockRegion);

		verify(mockClientCache, times(1)).getRegion(eq("TestRegion"));
		verify(mockClientCacheResolver, times(1)).resolve();
		verifyNoInteractions(this.mockRegion);
	}

	@Test
	public void fromNonNullCacheResolveResolvingNullCacheResolvesNullRegion() {

		CacheResolver<GemFireCache> mockCacheResolver = mock(CacheResolver.class);

		when(mockCacheResolver.resolve()).thenReturn(null);

		RegionServiceRegionResolver regionResolver = RegionServiceRegionResolver.from(mockCacheResolver);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isNotNull();
		assertThat(regionResolver.doResolve("TestRegion")).isNull();

		verify(mockCacheResolver, times(1)).resolve();
	}

	@Test
	public void fromNonNullCacheResolverResolvingPeerCache() {

		CacheResolver<Cache> mockPeerCacheResolver = mock(CacheResolver.class);

		RegionServiceRegionResolver regionResolver = RegionServiceRegionResolver.from(mockPeerCacheResolver);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isNotNull();

		verify(mockPeerCacheResolver, never()).resolve();
	}

	@Test(expected = IllegalArgumentException.class)
	public void fromNullCacheResolverThrowsIllegalArgumentException() {

		try {
			RegionServiceRegionResolver.from((CacheResolver<GemFireCache>) null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("CacheResolver must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void fromNonNullRegionService() {

		RegionService mockRegionService = mock(RegionService.class);

		when(mockRegionService.getRegion(anyString())).thenReturn(this.mockRegion);

		RegionServiceRegionResolver regionResolver = RegionServiceRegionResolver.from(mockRegionService);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver().resolve().orElse(null)).isEqualTo(mockRegionService);
		assertThat(regionResolver.doResolve("TestRegion")).isEqualTo(this.mockRegion);

		verify(mockRegionService, times(1)).getRegion(eq("TestRegion"));
		verifyNoInteractions(this.mockRegion);
	}

	@Test
	public void fromNullRegionServiceIsNullSafe() {

		RegionServiceRegionResolver regionResolver = RegionServiceRegionResolver.from((RegionService) null);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver()).isNotNull();
		assertThat(regionResolver.getRegionServiceResolver().resolve().orElse(null)).isNull();
		assertThat(regionResolver.doResolve("TestRegion")).isNull();
	}

	@Test
	public void resolveCachesAndReturnsRegion() {

		RegionService mockRegionService = mock(RegionService.class);

		RegionServiceResolver mockRegionServiceResolver = mock(RegionServiceResolver.class);

		when(mockRegionService.getRegion(anyString())).thenReturn(this.mockRegion);
		when(mockRegionServiceResolver.resolve()).thenReturn(Optional.of(mockRegionService));

		RegionServiceRegionResolver regionResolver = spy(new RegionServiceRegionResolver(mockRegionServiceResolver));

		assertThat(regionResolver.getRegionServiceResolver()).isEqualTo(mockRegionServiceResolver);

		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);
		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);

		verify(mockRegionServiceResolver, times(1)).resolve();
		verify(mockRegionService, times(1)).getRegion("MockRegion");
		verify(regionResolver, times(1)).doResolve(eq("MockRegion"));
	}

	@Test
	public void resolveReturnsNullWhenRegionServiceReturnsNull() {

		RegionService mockRegionService = mock(RegionService.class);

		RegionServiceResolver mockRegionServiceResolver = mock(RegionServiceResolver.class);

		when(mockRegionService.getRegion(anyString())).thenReturn(null);
		when(mockRegionServiceResolver.resolve()).thenReturn(Optional.of(mockRegionService));

		RegionServiceRegionResolver regionResolver = spy(new RegionServiceRegionResolver(mockRegionServiceResolver));

		assertThat(regionResolver.getRegionServiceResolver()).isEqualTo(mockRegionServiceResolver);

		assertThat(regionResolver.resolve("ProxyRegion")).isNull();
		assertThat(regionResolver.resolve("ProxyRegion")).isNull();

		verify(mockRegionServiceResolver, times(2)).resolve();
		verify(mockRegionService, times(2)).getRegion("ProxyRegion");
		verify(regionResolver, times(2)).doResolve(eq("ProxyRegion"));
	}

	@Test
	public void resolveReturnsNullWhenRegionServiceResolvesToNull() {

		RegionServiceResolver mockRegionServiceResolver = mock(RegionServiceResolver.class);

		RegionServiceRegionResolver regionResolver = spy(new RegionServiceRegionResolver(mockRegionServiceResolver));

		assertThat(regionResolver.getRegionServiceResolver()).isEqualTo(mockRegionServiceResolver);
		assertThat(regionResolver.resolve("TestRegion")).isNull();
		assertThat(regionResolver.resolve("TestRegion")).isNull();

		verify(mockRegionServiceResolver, times(2)).resolve();
		verify(regionResolver, times(2)).doResolve(eq("TestRegion"));
	}
}
