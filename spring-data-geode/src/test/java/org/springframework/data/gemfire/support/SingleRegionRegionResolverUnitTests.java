/*
 * Copyright 2020-2023 the original author or authors.
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Region;

/**
 * Unit Tests for {@link SingleRegionRegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.support.SingleRegionRegionResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class SingleRegionRegionResolverUnitTests {

	@Mock
	private Region mockRegion;

	@Test
	public void constructSingleRegionRegionResolverWithRegion() {

		SingleRegionRegionResolver regionResolver = new SingleRegionRegionResolver(this.mockRegion);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegion()).isSameAs(this.mockRegion);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructSingleRegionRegionResolverWithNullThrowsIllegalArgumentException() {

		try {
			new SingleRegionRegionResolver(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Region must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void resolveReturnsConfiguredRegionMatchingName() {

		when(this.mockRegion.getName()).thenReturn("MockRegion");

		assertThat(new SingleRegionRegionResolver(this.mockRegion).resolve("MockRegion"))
			.isEqualTo(this.mockRegion);

		verify(this.mockRegion, times(1)).getName();
	}

	@Test
	public void resolveReturnsNullWhenRegionNameDoesNotMatch() {

		when(this.mockRegion.getName()).thenReturn("MockRegion");

		SingleRegionRegionResolver regionResolver = new SingleRegionRegionResolver(this.mockRegion);

		assertThat(regionResolver.resolve("TestRegion")).isNull();
		assertThat(regionResolver.resolve("  ")).isNull();
		assertThat(regionResolver.resolve("")).isNull();
		assertThat(regionResolver.resolve(null)).isNull();

		verify(this.mockRegion, times(4)).getName();
	}
}
