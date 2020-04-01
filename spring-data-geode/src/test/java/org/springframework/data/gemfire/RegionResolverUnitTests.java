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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.apache.geode.cache.Region;

/**
 * Unit Tests for {@link RegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.RegionResolver
 * @since 2.3.0
 */
public class RegionResolverUnitTests {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void requiresRegionForNameReturnsRegion() {

		Region mockRegion = mock(Region.class);

		RegionResolver regionResolver = mock(RegionResolver.class);

		when(regionResolver.require(anyString())).thenCallRealMethod();
		when(regionResolver.resolve(eq("MockRegion"))).thenReturn(mockRegion);

		assertThat(regionResolver.require("MockRegion")).isEqualTo(mockRegion);

		verify(regionResolver, times(1)).resolve(eq("MockRegion"));
		verifyNoInteractions(mockRegion);
	}

	@Test(expected = IllegalStateException.class)
	public void requiredRegionResolvingToNullThrowsIllegalStateException() {

		RegionResolver regionResolver = mock(RegionResolver.class);

		when(regionResolver.require(anyString())).thenCallRealMethod();
		when(regionResolver.resolve(anyString())).thenReturn(null);

		try {
			regionResolver.require("TestRegion");
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("Region with name [TestRegion] not found");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(regionResolver, times(1)).resolve(eq("TestRegion"));
		}
	}
}
