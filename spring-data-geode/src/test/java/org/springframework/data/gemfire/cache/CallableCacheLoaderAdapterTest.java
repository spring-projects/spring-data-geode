/*
 * Copyright 2016-2021 the original author or authors.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;

/**
 * Unit Tests to test the adaption of the {@link java.util.concurrent.Callable}
 * into Apache Geode's {@link org.apache.geode.cache.CacheLoader} interface.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see java.util.concurrent.Callable
 * @see org.apache.geode.cache.CacheLoader
 * @see org.apache.geode.cache.LoaderHelper
 * @see org.apache.geode.cache.Region
 * @since 1.9.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CallableCacheLoaderAdapterTest {

	@Mock
	private CacheLoader<String, Object> mockCacheLoader;

	@Mock
	private LoaderHelper<String, Object> mockLoaderHelper;

	@Mock
	private Region<String, Object> mockRegion;

	@Test
	public void constructCallableCacheLoaderAdapterWithArgumentKeyAndRegion() {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader, "key", mockRegion, "test");

		assertThat(instance).isNotNull();
		assertThat(instance.getCacheLoader()).isSameAs(mockCacheLoader);
		assertThat(instance.getKey()).isEqualTo("key");
		assertThat(instance.getRegion()).isSameAs(mockRegion);
		assertThat(String.valueOf(instance.getArgument())).isEqualTo("test");
	}

	@Test
	public void constructCallableCacheLoaderAdapterWithKeyRegionAndNoArgument() {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader, "key", mockRegion);

		assertThat(instance).isNotNull();
		assertThat(instance.getCacheLoader()).isSameAs(mockCacheLoader);
		assertThat(instance.getKey()).isEqualTo("key");
		assertThat(instance.getRegion()).isSameAs(mockRegion);
		assertThat(instance.getArgument()).isNull();
	}

	@Test
	public void constructCallableCacheLoaderAdapterWithNoArgumentKeyOrRegion() {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader);

		assertThat(instance).isNotNull();
		assertThat(instance.getCacheLoader()).isSameAs(mockCacheLoader);
		assertThat(instance.getKey()).isNull();
		assertThat(instance.getRegion()).isNull();
		assertThat(instance.getArgument()).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructCallableCacheLoaderAdapterWithNullCacheLoader() {

		try {
			new CallableCacheLoaderAdapter<>(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("CacheLoader must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void callDelegatesToLoad() throws Exception {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader, "key", mockRegion, "test");

		when(mockCacheLoader.load(any(LoaderHelper.class))).thenAnswer((Answer<String>) invocation -> {

			LoaderHelper<String, Object> loaderHelper = invocation.getArgument(0);

			assertThat(loaderHelper).isNotNull();
			assertThat(loaderHelper.getArgument()).isEqualTo("test");
			assertThat(loaderHelper.getKey()).isEqualTo("key");
			assertThat(loaderHelper.getRegion()).isSameAs(mockRegion);

			return "mockValue";
		});

		assertThat(instance.call()).isEqualTo("mockValue");

		verify(mockCacheLoader, times(1)).load(isA(LoaderHelper.class));
	}

	@Test(expected = IllegalStateException.class)
	public void callThrowsIllegalStateExceptionForNullKey() throws Exception {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader, null, mockRegion);

		assertThat(instance.getKey()).isNull();
		assertThat(instance.getRegion()).isSameAs(mockRegion);

		try {
			instance.call();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("The key for which the value is loaded for cannot be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void callThrowsIllegalStateExceptionForNullRegion() throws Exception {

		CallableCacheLoaderAdapter<String, Object> instance =
			new CallableCacheLoaderAdapter<>(mockCacheLoader, "key", null);

		assertThat(instance.getKey()).isEqualTo("key");
		assertThat(instance.getRegion()).isNull();

		try {
			instance.call();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("The Region to load cannot be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void closeDelegatesToCacheLoaderClose() {

		new CallableCacheLoaderAdapter<>(mockCacheLoader).close();

		verify(mockCacheLoader, times(1)).close();
	}

	@Test
	public void loadDelegatesToCacheLoaderLoad() {

		CallableCacheLoaderAdapter<String, Object> instance = new CallableCacheLoaderAdapter<>(mockCacheLoader);

		when(mockCacheLoader.load(eq(mockLoaderHelper))).thenReturn("test");

		assertThat(instance.load(mockLoaderHelper)).isEqualTo("test");

		verify(mockCacheLoader, times(1)).load(eq(mockLoaderHelper));
	}
}
