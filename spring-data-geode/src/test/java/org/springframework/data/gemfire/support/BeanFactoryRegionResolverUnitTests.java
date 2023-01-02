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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.AttributesMutator;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.BeanFactory;

/**
 * Unit Tests for {@link BeanFactoryRegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Region
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.data.gemfire.RegionResolver
 * @see org.springframework.data.gemfire.support.BeanFactoryRegionResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class BeanFactoryRegionResolverUnitTests {

	@Mock
	private BeanFactory mockBeanFactory;

	@SuppressWarnings("unchecked")
	private <K, V> Region<K, V> mockRegion() {

		Region<K, V> mockRegion = mock(Region.class);

		AttributesMutator<K, V> mockAttributesMutator = mock(AttributesMutator.class);

		when(mockRegion.getAttributesMutator()).thenReturn(mockAttributesMutator);
		when(mockAttributesMutator.getRegion()).thenReturn(mockRegion);

		return mockRegion;
	}

	@Test
	public void constructBeanFactoryRegionResolverWithBeanFactory() {

		BeanFactoryRegionResolver regionResolver = new BeanFactoryRegionResolver(this.mockBeanFactory);

		assertThat(regionResolver).isNotNull();
		assertThat(regionResolver.getBeanFactory()).isEqualTo(this.mockBeanFactory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructBeanFactoryRegionResolverWithNull() {

		try {
			new BeanFactoryRegionResolver(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("BeanFactory must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void doResolveReturnsRegionForName() {

		Region mockRegion = mockRegion();

		when(this.mockBeanFactory.containsBean(anyString())).thenReturn(true);
		when(this.mockBeanFactory.getBean(anyString(), eq(Region.class))).thenReturn(mockRegion);

		BeanFactoryRegionResolver regionResolver = spy(new BeanFactoryRegionResolver(this.mockBeanFactory));

		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(mockRegion);
		assertThat(regionResolver.resolve("MockRegion")).isEqualTo(mockRegion);

		verify(this.mockBeanFactory, times(1)).containsBean(eq("MockRegion"));
		verify(this.mockBeanFactory, times(1)).getBean(eq("MockRegion"), eq(Region.class));
		verify(regionResolver, times(1)).doResolve(eq("MockRegion"));
	}

	@Test
	public void doResolveReturnsNullForNonRegionBean() {

		when(this.mockBeanFactory.containsBean(anyString())).thenReturn(false);

		BeanFactoryRegionResolver regionResolver = spy(new BeanFactoryRegionResolver(this.mockBeanFactory));

		assertThat(regionResolver.resolve("MockRegion")).isNull();
		assertThat(regionResolver.resolve("MockRegion")).isNull();

		verify(this.mockBeanFactory, times(2)).containsBean(eq("MockRegion"));
		verify(this.mockBeanFactory, never()).getBean(eq("MockRegion"), eq(Region.class));
		verify(regionResolver, times(2)).doResolve(eq("MockRegion"));
	}

	public void testDoResolveWithInvalidRegionBeanName(String regionBeanName) {

		BeanFactoryRegionResolver regionResolver = spy(new BeanFactoryRegionResolver(this.mockBeanFactory));

		assertThat(regionResolver.doResolve(regionBeanName)).isNull();

		verify(this.mockBeanFactory, never()).containsBean(anyString());
		verify(this.mockBeanFactory, never()).getBean(anyString(), eq(Region.class));
	}

	@Test
	public void doResolveWithBlankRegionBeanNameReturnsNull() {
		testDoResolveWithInvalidRegionBeanName("  ");
	}

	@Test
	public void doResolveWithEmptyRegionBeanNameReturnsNull() {
		testDoResolveWithInvalidRegionBeanName("");
	}

	@Test
	public void doResolveWithNullRegionBeanNameReturnsNull() {
		testDoResolveWithInvalidRegionBeanName(null);
	}
}
