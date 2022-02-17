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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.AttributesMutator;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionDestroyedException;

import org.springframework.data.gemfire.RegionResolver;

/**
 * Unit Tests for {@link ComposableRegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.RegionResolver
 * @see org.springframework.data.gemfire.support.ComposableRegionResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class ComposableRegionResolverUnitTests {

	@Mock
	private Region mockRegion;

	@Mock(name = "MockRegionResolverOne")
	private RegionResolver mockRegionResolverOne;

	@Mock(name = "MockRegionResolverTwo")
	private RegionResolver mockRegionResolverTwo;

	@Mock(name = "MockRegionResolverThree")
	private RegionResolver mockRegionResolverThree;

	@Before
	public void setupMockRegion() {

		AttributesMutator mockAttributesMutator = mock(AttributesMutator.class);

		when(this.mockRegion.getAttributesMutator()).thenReturn(mockAttributesMutator);
		when(mockAttributesMutator.getRegion()).thenReturn(this.mockRegion);
	}

	@Test
	public void constructComposableRegionResolver() {

		ComposableRegionResolver regionResolver =
			new ComposableRegionResolver(this.mockRegionResolverOne, this.mockRegionResolverTwo);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getRegionResolverOne()).isEqualTo(this.mockRegionResolverOne);
		assertThat(regionResolver.getRegionResolverTwo()).isEqualTo(this.mockRegionResolverTwo);
	}

	public void testConstructRegionResolverWithNullRegionResolver(Supplier<RegionResolver> regionResolverSupplier,
			String expectedMessage) {

		try {
			regionResolverSupplier.get();
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage(expectedMessage);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructComposableRegionResolverWithNullFirstArgument() {
		testConstructRegionResolverWithNullRegionResolver(
			() -> new ComposableRegionResolver(null, this.mockRegionResolverTwo),
			"RegionResolver 1 must not be null");
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructComposableRegionResolverWithNullSecondArgument() {
		testConstructRegionResolverWithNullRegionResolver(
			() -> new ComposableRegionResolver(this.mockRegionResolverOne, null),
			"RegionResolver 2 must not be null");
	}

	@Test
	public void composeWithAllNullRegionResolvers() {
		assertThat(ComposableRegionResolver.compose(null, null)).isNull();
	}

	@Test
	public void composeWithSingleNullRegionResolver() {

		assertThat(ComposableRegionResolver.compose(this.mockRegionResolverOne, null))
			.isSameAs(this.mockRegionResolverOne);

		assertThat(ComposableRegionResolver.compose(null, this.mockRegionResolverTwo))
			.isSameAs(this.mockRegionResolverTwo);
	}

	@Test
	public void composeWithTwoNonNullRegionResolvers() {

		RegionResolver regionResolver =
			ComposableRegionResolver.compose(this.mockRegionResolverOne, this.mockRegionResolverTwo);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver).isInstanceOf(ComposableRegionResolver.class);
		assertThat(((ComposableRegionResolver) regionResolver).getRegionResolverOne()).isEqualTo(this.mockRegionResolverOne);
		assertThat(((ComposableRegionResolver) regionResolver).getRegionResolverTwo()).isEqualTo(this.mockRegionResolverTwo);
	}

	@Test
	public void composeArrayOfRegionResolvers() {

		RegionResolver regionResolver = ComposableRegionResolver
			.compose(this.mockRegionResolverOne, this.mockRegionResolverTwo, this.mockRegionResolverThree);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver).isInstanceOf(ComposableRegionResolver.class);

		RegionResolver regionResolverOne = ((ComposableRegionResolver) regionResolver).getRegionResolverOne();
		RegionResolver regionResolverTwo = ((ComposableRegionResolver) regionResolver).getRegionResolverTwo();

		assertThat(regionResolverOne).isNotNull();
		assertThat(regionResolverOne).isInstanceOf(ComposableRegionResolver.class);
		assertThat(((ComposableRegionResolver) regionResolverOne).getRegionResolverOne()).isEqualTo(this.mockRegionResolverOne);
		assertThat(((ComposableRegionResolver) regionResolverOne).getRegionResolverTwo()).isEqualTo(this.mockRegionResolverTwo);
		assertThat(regionResolverTwo).isEqualTo(this.mockRegionResolverThree);
	}

	@Test
	public void composeArrayWithNullAndNonNullRegionResolvers() {

		RegionResolver regionResolver = ComposableRegionResolver
			.compose(null, this.mockRegionResolverOne, null, null, this.mockRegionResolverTwo);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver).isInstanceOf(ComposableRegionResolver.class);
		assertThat(((ComposableRegionResolver) regionResolver).getRegionResolverOne()).isEqualTo(this.mockRegionResolverOne);
		assertThat(((ComposableRegionResolver) regionResolver).getRegionResolverTwo()).isEqualTo(this.mockRegionResolverTwo);
	}

	@Test
	public void composeArrayWithSingleRegionResolver() {
		assertThat(ComposableRegionResolver.compose(this.mockRegionResolverOne)).isEqualTo(this.mockRegionResolverOne);
	}

	@Test
	public void composeEmptyArrayReturnsNull() {
		assertThat(ComposableRegionResolver.compose()).isNull();
	}

	@Test
	public void composeNullArrayIsNullSafeAndReturnsNull() {
		assertThat(ComposableRegionResolver.compose((RegionResolver[]) null)).isNull();
	}

	@Test
	public void composeNullIterableIsNullSafeAndReturnsNull() {
		assertThat(ComposableRegionResolver.compose((Iterable<RegionResolver>) null)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveReturnsRegionFromFirstRegionResolverInComposition() {

		RegionResolver regionResolver = ComposableRegionResolver
			.compose(this.mockRegionResolverOne, this.mockRegionResolverTwo, this.mockRegionResolverThree);

		when(this.mockRegionResolverOne.resolve(anyString())).thenReturn(this.mockRegion);

		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);

		verify(this.mockRegionResolverOne, times(1)).resolve(eq("MockRegion"));
		verify(this.mockRegionResolverTwo, never()).resolve(anyString());
		verify(this.mockRegionResolverThree, never()).resolve(anyString());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveReturnsRegionFromLastRegionResolverInCompositionAndCachesResolvedRegion() {

		RegionResolver regionResolver = ComposableRegionResolver
			.compose(this.mockRegionResolverOne, null, this.mockRegionResolverTwo,
				this.mockRegionResolverThree, null, null);

		when(this.mockRegionResolverThree.resolve(anyString())).thenReturn(this.mockRegion);

		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);
		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);

		InOrder inOrder = Mockito
			.inOrder(this.mockRegionResolverOne, this.mockRegionResolverTwo, this.mockRegionResolverThree);

		inOrder.verify(this.mockRegionResolverOne, times(1)).resolve(eq("MockRegion"));
		inOrder.verify(this.mockRegionResolverTwo, times(1)).resolve(eq("MockRegion"));
		inOrder.verify(this.mockRegionResolverThree, times(1)).resolve(eq("MockRegion"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveReturnsRegionFromSecondRegionResolverInCompositionAndCachesResolvedRegion() {

		RegionResolver regionResolver = ComposableRegionResolver
			.compose(this.mockRegionResolverOne, this.mockRegionResolverTwo, this.mockRegionResolverThree);

		when(this.mockRegionResolverOne.resolve(anyString()))
			.thenThrow(new RegionDestroyedException("TEST", "/MockRegion"));

		when(this.mockRegionResolverTwo.resolve(anyString())).thenReturn(this.mockRegion);

		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);
		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(this.mockRegion);

		InOrder inOrder = Mockito
			.inOrder(this.mockRegionResolverOne, this.mockRegionResolverTwo, this.mockRegionResolverThree);

		inOrder.verify(this.mockRegionResolverOne, times(1)).resolve(eq("MockRegion"));
		inOrder.verify(this.mockRegionResolverTwo, times(1)).resolve(eq("MockRegion"));
		inOrder.verify(this.mockRegionResolverThree, never()).resolve(eq("MockRegion"));
	}
}
