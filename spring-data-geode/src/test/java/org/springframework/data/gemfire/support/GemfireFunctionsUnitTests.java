/*
 * Copyright 2021-2023 the original author or authors.
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

/**
 * Unit Tests for {@link GemfireFunctions}
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.support.GemfireFunctions
 * @since 2.7.0
 */
public class GemfireFunctionsUnitTests {

	@Test
	public void getRegionFromCacheFunctionReturnsRegion() {

		GemFireCache mockCache = mock(GemFireCache.class);

		Region<?, ?> mockRegion = mock(Region.class);

		doReturn(mockRegion).when(mockCache).getRegion(eq("TestRegion"));

		Function<GemFireCache, Region<Object, Object>> function =
			GemfireFunctions.getRegionFromCache("TestRegion");

		assertThat(function).isNotNull();
		assertThat(function.apply(mockCache)).isEqualTo(mockRegion);

		verify(mockCache, times(1)).getRegion(eq("TestRegion"));
		verifyNoMoreInteractions(mockCache);
		verifyNoInteractions(mockRegion);
	}

	@Test
	public void getRegionFromCacheSupplierReturnsRegions() {

		GemFireCache mockCache = mock(GemFireCache.class);

		Region<?, ?> mockRegion = mock(Region.class);

		doReturn(mockRegion).when(mockCache).getRegion(eq("TestRegion"));

		Supplier<Region<Object, Object>> supplier =
			GemfireFunctions.getRegionFromCache(mockCache, "TestRegion");

		assertThat(supplier).isNotNull();
		assertThat(supplier.get()).isEqualTo(mockRegion);

		verify(mockCache, times(1)).getRegion(eq("TestRegion"));
		verifyNoMoreInteractions(mockCache);
		verifyNoInteractions(mockRegion);
	}

	@Test
	public void getSubregionFromRegionFunctionReturnsRegion() {

		Region<?, ?> mockParentRegion = mock(Region.class);
		Region<?, ?> mockSubregion = mock(Region.class);

		doReturn(mockSubregion).when(mockParentRegion).getSubregion(eq("TestSubregion"));

		Function<Region<?, ?>, Region<Object, Object>> function =
			GemfireFunctions.getSubregionFromRegion("TestSubregion");

		assertThat(function).isNotNull();
		assertThat(function.apply(mockParentRegion)).isEqualTo(mockSubregion);

		verify(mockParentRegion, times(1)).getSubregion(eq("TestSubregion"));
		verifyNoMoreInteractions(mockParentRegion);
		verifyNoInteractions(mockSubregion);
	}
}
