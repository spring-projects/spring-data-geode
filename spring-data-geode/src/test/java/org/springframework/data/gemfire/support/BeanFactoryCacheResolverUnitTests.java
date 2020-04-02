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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.GemFireCache;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.client.support.BeanFactoryPoolResolver;

/**
 * Unit Tests for {@link BeanFactoryCacheResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.Spy
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.data.gemfire.support.BeanFactoryCacheResolver
 * @since 2.3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class BeanFactoryCacheResolverUnitTests {

	@Mock
	private BeanFactory mockBeanFactory;

	@Mock
	private GemFireCache mockCache;

	@Test
	public void constructBeanFactoryCacheResolver() {

		BeanFactoryCacheResolver cacheResolver = new BeanFactoryCacheResolver(this.mockBeanFactory);

		assertThat(cacheResolver).isNotNull();
		assertThat(cacheResolver.getBeanFactory()).isSameAs(this.mockBeanFactory);
		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructWithNullBeanFactoryThrowsIllegalArgumentException() {

		try {
			new BeanFactoryPoolResolver(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("BeanFactory must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAndGetBeanFactory() {

		BeanFactory mockBeanFactoryTwo = mock(BeanFactory.class);

		BeanFactoryCacheResolver cacheResolver = new BeanFactoryCacheResolver(this.mockBeanFactory);

		assertThat(cacheResolver.getBeanFactory()).isSameAs(this.mockBeanFactory);

		cacheResolver.setBeanFactory(mockBeanFactoryTwo);

		assertThat(cacheResolver.getBeanFactory()).isSameAs(mockBeanFactoryTwo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setBeanFactoryToNullThrowsIllegalArgumentException() {

		BeanFactoryCacheResolver cacheResolver = new BeanFactoryCacheResolver(this.mockBeanFactory);

		try {

			assertThat(cacheResolver.getBeanFactory()).isEqualTo(this.mockBeanFactory);

			cacheResolver.setBeanFactory(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("BeanFactory must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(cacheResolver.getBeanFactory()).isEqualTo(this.mockBeanFactory);
		}
	}

	@Test
	public void setAndGetCacheBeanName() {

		BeanFactoryCacheResolver cacheResolver = new BeanFactoryCacheResolver(this.mockBeanFactory);

		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isNull();

		cacheResolver.setCacheBeanName("TestCacheBeanName");

		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isEqualTo("TestCacheBeanName");

		cacheResolver.setCacheBeanName("  ");

		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isNull();

		cacheResolver.setCacheBeanName("");

		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isNull();

		cacheResolver.setCacheBeanName(null);

		assertThat(cacheResolver.getCacheBeanName().orElse(null)).isNull();
	}

	@Test
	public void doResolveResolvesGemFireCache() {

		when(this.mockBeanFactory.getBean(eq(GemFireCache.class))).thenReturn(this.mockCache);

		BeanFactoryCacheResolver cacheResolver = spy(new BeanFactoryCacheResolver(this.mockBeanFactory));

		assertThat(cacheResolver.resolve()).isEqualTo(this.mockCache);
		assertThat(cacheResolver.resolve()).isEqualTo(this.mockCache);

		verify(this.mockBeanFactory, times(1)).getBean(eq(GemFireCache.class));
		verify(cacheResolver, times(1)).doResolve();
		verifyNoInteractions(this.mockCache);
	}

	@Test
	public void doResolveQualifiedGemFireCache() {

		when(this.mockBeanFactory.getBean(eq("QualifiedCache"), eq(GemFireCache.class)))
			.thenReturn(this.mockCache);

		BeanFactoryCacheResolver cacheResolver = new BeanFactoryCacheResolver(this.mockBeanFactory);

		assertThat(cacheResolver.doResolve()).isNull();

		cacheResolver.setCacheBeanName("NonExistingCache");

		assertThat(cacheResolver.doResolve()).isNull();

		cacheResolver.setCacheBeanName("QualifiedCache");

		assertThat(cacheResolver.doResolve()).isEqualTo(this.mockCache);
	}
}
