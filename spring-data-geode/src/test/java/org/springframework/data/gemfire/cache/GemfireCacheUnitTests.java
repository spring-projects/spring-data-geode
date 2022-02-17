/*
 * Copyright 2016-2022 the original author or authors.
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
package org.springframework.data.gemfire.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Region;

import org.springframework.cache.Cache;

/**
 * Unit Tests for {@link GemfireCache}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.cache.GemfireCache
 * @since 1.9.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class GemfireCacheUnitTests {

	@Mock
	private Callable mockCallable;

	@Mock
	private Region mockRegion;

	@Test
	@SuppressWarnings("unchecked")
	public void wrapIsSuccessful() {

		GemfireCache gemfireCache = GemfireCache.wrap(mockRegion);

		assertThat(gemfireCache).isNotNull();
		assertThat(gemfireCache.getNativeCache()).isEqualTo(mockRegion);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructGemfireCacheWithNullRegion() {

		try {
			new GemfireCache(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("GemFire Region must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void getNameReturnsRegionName() {

		when(mockRegion.getName()).thenReturn("Example");

		assertThat(GemfireCache.wrap(mockRegion).getName()).isEqualTo("Example");

		verify(mockRegion, times(1)).getName();
	}

	@Test
	public void clearCallsRegionClear() {

		GemfireCache.wrap(mockRegion).clear();

		verify(mockRegion, times(1)).clear();
	}

	@Test
	public void evictCallsRegionRemoveWithKey() {

		GemfireCache.wrap(mockRegion).evict("key");

		verify(mockRegion, never()).destroy(any());
		verify(mockRegion, times(1)).remove(eq("key"));
	}

	@Test
	public void getReturnsValueWrapperForKey() {

		when(mockRegion.get(eq("key"))).thenReturn("test");

		Cache.ValueWrapper value = GemfireCache.wrap(mockRegion).get("key");

		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo("test");

		verify(mockRegion, times(1)).get(eq("key"));
	}

	@Test
	public void getReturnsNullForKey() {

		when(mockRegion.get(anyString())).thenReturn(null);

		assertThat(GemfireCache.wrap(mockRegion).get("key")).isNull();

		verify(mockRegion, times(1)).get(eq("key"));
	}

	@Test
	public void getReturnsValueForKeyAsDesiredType() {

		when(mockRegion.get(eq("key"))).thenReturn(1);

		Object value = GemfireCache.wrap(mockRegion).get("key", Integer.class);

		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(1);

		verify(mockRegion, times(1)).get(eq("key"));
	}

	@Test
	public void getReturnsNullForKeyAsDesiredType() {

		when(mockRegion.get(eq("key"))).thenReturn(null);

		assertThat(GemfireCache.wrap(mockRegion).get("key", Double.class)).isNull();

		verify(mockRegion, times(1)).get(eq("key"));
	}

	@Test
	public void getReturnsValueForKeyWithNullDesiredType() {

		when(mockRegion.get(eq("key"))).thenReturn(true);

		assertThat(GemfireCache.wrap(mockRegion).get("key", (Class<Boolean>) null)).isTrue();

		verify(mockRegion, times(1)).get(eq("key"));
	}

	@Test(expected = IllegalStateException.class)
	public void getThrowsIllegalStateExceptionForKeyWhenValueIsNotAnInstanceOfDesiredType() {

		when(mockRegion.get(eq("key"))).thenReturn(1);

		try {
			GemfireCache.wrap(mockRegion).get("key", Boolean.class);
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("Cached value [1] is not an instance of type [%s]",
				Boolean.class.getName());

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockRegion, times(1)).get(eq("key"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getReturnsValueFromCacheForKeyWithValueLoader() {

		when(mockRegion.get(eq("key"))).thenReturn("test");

		assertThat(GemfireCache.wrap(mockRegion).get("key", mockCallable)).isEqualTo("test");

		verify(mockRegion, times(1)).get(eq("key"));
		verifyNoInteractions(mockCallable);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getReturnsValueFromCacheForKeyAfterSynchronizationWithValueLoader() {

		when(mockRegion.get(eq("key"))).thenReturn(null).thenReturn("test");

		assertThat(GemfireCache.wrap(mockRegion).get("key", mockCallable)).isEqualTo("test");

		verify(mockRegion, times(2)).get(eq("key"));
		verifyNoInteractions(mockCallable);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getReturnsValueFromValueLoaderForKeyWithValueLoader() throws Exception {

		when(mockRegion.get(anyString())).thenReturn(null);
		when(mockCallable.call()).thenReturn("mockValue");

		assertThat(GemfireCache.wrap(mockRegion).get("key", mockCallable)).isEqualTo("mockValue");

		verify(mockRegion, times(2)).get(eq("key"));
		verify(mockCallable, times(1)).call();
	}

	@SuppressWarnings("unchecked")
	@Test(expected = Cache.ValueRetrievalException.class)
	public void getThrowsValueRetrievalExceptionForKeyWithValueLoader() throws Exception {

		when(mockRegion.get(anyString())).thenReturn(null);
		when(mockCallable.call()).thenThrow(new IllegalStateException("test"));

		try {
			GemfireCache.wrap(mockRegion).get("key", mockCallable);
		}
		catch (Cache.ValueRetrievalException expected) {

			assertThat(expected).hasCauseInstanceOf(IllegalStateException.class);

			throw expected;
		}
		finally {
			verify(mockRegion, times(2)).get(eq("key"));
			verify(mockCallable, times(1)).call();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putCachesValue() {

		GemfireCache.wrap(mockRegion).put("key", "test");

		verify(mockRegion, times(1)).put(eq("key"), eq("test"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putDoesNotCacheNull() {

		GemfireCache.wrap(mockRegion).put("key", null);

		verify(mockRegion, never()).put(anyString(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putIfAbsentReturnsExistingValue() {

		when(mockRegion.putIfAbsent(eq("key"), any())).thenReturn("test");

		Cache.ValueWrapper value = GemfireCache.wrap(mockRegion).putIfAbsent("key", "mockValue");

		assertThat(value).isNotNull();
		assertThat(value.get()).isEqualTo("test");

		verify(mockRegion, times(1)).putIfAbsent(eq("key"), eq("mockValue"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putIfAbsentReturnsNull() {

		doReturn(null).when(mockRegion).putIfAbsent(eq("key"), any());

		Cache.ValueWrapper value = GemfireCache.wrap(mockRegion).putIfAbsent("key", "mockValue");

		assertThat(value).isNull();

		verify(mockRegion, times(1)).putIfAbsent(eq("key"), eq("mockValue"));
	}
}
