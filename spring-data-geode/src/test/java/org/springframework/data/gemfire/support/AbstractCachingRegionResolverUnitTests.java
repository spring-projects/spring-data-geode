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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.apache.geode.cache.AttributesMutator;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;

/**
 * Unit Tests for {@link AbstractCachingRegionResolver}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see edu.umd.cs.mtc.MultithreadedTestCase
 * @see edu.umd.cs.mtc.TestFramework
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.support.AbstractCachingRegionResolver
 * @since 2.3.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AbstractCachingRegionResolverUnitTests {

	@Test
	public void resolveCallsDoResolveAndCachesResult() {

		Region mockRegion = mock(Region.class);

		AttributesMutator mockAttributesMutator = mock(AttributesMutator.class);

		when(mockRegion.getAttributesMutator()).thenReturn(mockAttributesMutator);
		when(mockAttributesMutator.getRegion()).thenReturn(mockRegion);

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		when(regionResolver.resolve(anyString())).thenCallRealMethod();
		when(regionResolver.doResolveAndRegisterResolverAsCacheListener(anyString())).thenCallRealMethod();
		when(regionResolver.doResolve(anyString())).thenReturn(mockRegion);

		assertThat(regionResolver.resolve("TestRegion")).isEqualTo(mockRegion);
		assertThat(regionResolver.resolve("TestRegion")).isEqualTo(mockRegion);

		InOrder inOrder = Mockito.inOrder(regionResolver, mockRegion, mockAttributesMutator);

		inOrder.verify(regionResolver, times(1)).doResolveAndRegisterResolverAsCacheListener(eq("TestRegion"));
		inOrder.verify(regionResolver, times(1)).doResolve(eq("TestRegion"));
		inOrder.verify(mockRegion, times(1)).getAttributesMutator();
		inOrder.verify(mockAttributesMutator, times(1)).addCacheListener(eq(regionResolver));
		inOrder.verify(mockAttributesMutator, times(1)).getRegion();
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void afterRegionDestroyClearsCacheEntryForCachedRegionWithName() {

		Region mockRegion = mock(Region.class);

		when(mockRegion.getName()).thenReturn("MockRegion");

		RegionEvent mockRegionEvent = mock(RegionEvent.class);

		when(mockRegionEvent.getRegion()).thenReturn(mockRegion);

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		doCallRealMethod().when(regionResolver).remove(anyString());

		regionResolver.afterRegionDestroy(mockRegionEvent);

		verify(mockRegion, times(1)).getName();
		verify(mockRegionEvent, times(1)).getRegion();
		verify(regionResolver, times(1)).remove(eq("MockRegion"));
	}

	@Test
	public void afterRegionDestroyWillNotClearCacheEntriesForNonCachedRegion() {

		Region mockCachedRegion = mock(Region.class);
		Region mockNonCachedRegion = mock(Region.class);

		when(mockCachedRegion.getName()).thenReturn("CachedRegion");
		when(mockNonCachedRegion.getName()).thenReturn("NonCachedRegion");

		RegionEvent mockRegionEvent = mock(RegionEvent.class);

		when(mockRegionEvent.getRegion()).thenReturn(mockNonCachedRegion);

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		doCallRealMethod().when(regionResolver).afterRegionDestroy(any(RegionEvent.class));
		doCallRealMethod().when(regionResolver).cache(any(Region.class));
		doCallRealMethod().when(regionResolver).resolve(anyString());

		regionResolver.cache(mockCachedRegion);

		assertThat(regionResolver.resolve("CachedRegion")).isEqualTo(mockCachedRegion);

		regionResolver.afterRegionDestroy(mockRegionEvent);

		assertThat(regionResolver.resolve("CachedRegion")).isEqualTo(mockCachedRegion);

		verify(mockNonCachedRegion, times(1)).getName();
		verify(mockRegionEvent, times(1)).getRegion();
		verify(regionResolver, times(1)).afterRegionDestroy(eq(mockRegionEvent));
		verify(regionResolver, never()).require(anyString());
		verify(regionResolver, times(1)).remove(eq("NonCachedRegion"));
		verify(regionResolver, times(2)).resolve(eq("CachedRegion"));
	}

	@Test
	public void afterRegionDestroyWithNullRegionEventIsNullSafe() {

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		regionResolver.afterRegionDestroy(null);

		verify(regionResolver, never()).remove(any());
	}

	@Test
	public void afterRegionDestroyWithNullRegionIsNullSafe() {

		RegionEvent mockRegionEvent = mock(RegionEvent.class);

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		regionResolver.afterRegionDestroy(mockRegionEvent);

		verify(mockRegionEvent, times(1)).getRegion();
		verify(regionResolver, never()).remove(any());
	}

	public void testAfterRegionDestroyWithNamelessRegionIsNullSafe(String regionName) {

		Region mockRegion = mock(Region.class);

		when(mockRegion.getName()).thenReturn(regionName);

		RegionEvent mockRegionEvent = mock(RegionEvent.class);

		when(mockRegionEvent.getRegion()).thenReturn(mockRegion);

		AbstractCachingRegionResolver regionResolver = spy(AbstractCachingRegionResolver.class);

		regionResolver.afterRegionDestroy(mockRegionEvent);

		verify(mockRegion, times(1)).getName();
		verify(mockRegionEvent, times(1)).getRegion();
		verify(regionResolver, never()).remove(any());
	}

	@Test
	public void afterRegionDestroyWithBlankRegionNameIsNullSafe() {
		testAfterRegionDestroyWithNamelessRegionIsNullSafe("  ");
	}

	@Test
	public void afterRegionDestroyWithEmptyRegionNameIsNullSafe() {
		testAfterRegionDestroyWithNamelessRegionIsNullSafe("");
	}

	@Test
	public void afterRegionDestroyWithNullRegionNameIsNullSafe() {
		testAfterRegionDestroyWithNamelessRegionIsNullSafe(null);
	}

	@Test
	public void abstractCachingRegionResolverIsThreadSafe() throws Throwable {
		TestFramework.runOnce(new AbstractCachingRegionResolverMultithreadedTestCase());
	}

	static final class AbstractCachingRegionResolverMultithreadedTestCase extends MultithreadedTestCase {

		private AbstractCachingRegionResolver regionResolver;

		private AtomicReference<Region> regionResolvedFromThreadOne = new AtomicReference<>(null);
		private AtomicReference<Region> regionResolvedFromThreadTwo = new AtomicReference<>(null);

		private AttributesMutator mockAttributesMutator = mock(AttributesMutator.class);

		private Region mockRegion = mock(Region.class);

		@Override
		public void initialize() {

			super.initialize();

			when(this.mockRegion.getAttributesMutator()).thenReturn(this.mockAttributesMutator);
			when(this.mockAttributesMutator.getRegion()).thenReturn(this.mockRegion);

			this.regionResolver = spy(AbstractCachingRegionResolver.class);

			when(this.regionResolver.resolve(anyString())).thenCallRealMethod();
			when(this.regionResolver.doResolveAndRegisterResolverAsCacheListener(anyString())).thenCallRealMethod();

			when(this.regionResolver.doResolve(eq("MockRegion"))).thenAnswer(invocation -> {

				waitForTick(2);

				return this.mockRegion;
			});
		}

		public void thread1() {

			Thread.currentThread().setName("Region Resolver Thread 1");

			assertTick(0);

			this.regionResolvedFromThreadOne.set(this.regionResolver.resolve("MockRegion"));
		}

		public void thread2() {

			Thread.currentThread().setName("Region Resolver Thread 2");

			assertTick(0);
			waitForTick(1);

			this.regionResolvedFromThreadTwo.set(this.regionResolver.resolve("MockRegion"));
		}

		@Override
		public void finish() {

			assertThat(this.regionResolvedFromThreadOne.get()).isEqualTo(this.mockRegion);
			assertThat(this.regionResolvedFromThreadTwo.get()).isSameAs(this.regionResolvedFromThreadOne.get());

			verify(this.regionResolver, times(2)).resolve(eq("MockRegion"));
			verify(this.regionResolver, times(1)).doResolveAndRegisterResolverAsCacheListener(eq("MockRegion"));
			verify(this.regionResolver, times(1)).doResolve(eq("MockRegion"));
			verify(this.mockRegion, times(1)).getAttributesMutator();
			verify(this.mockAttributesMutator, times(1)).addCacheListener(eq(this.regionResolver));
			verify(this.mockAttributesMutator, times(1)).getRegion();
		}
	}
}
