/*
 * Copyright 2020-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Composition of {@link CacheResolver CacheResolvers} using
 * the <a href="https://en.wikipedia.org/wiki/Composite_pattern">Composite Software Design Pattern</a> that acts,
 * and can be referred to, as a single instance of {@link CacheResolver}.
 *
 * This implementation also supports caching the result of the resolution of the {@link GemFireCache}
 * instance reference.
 *
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingCacheResolver
 * @since 2.3.0
 */
public class ComposableCacheResolver<T extends GemFireCache> extends AbstractCachingCacheResolver<T> {

	private final CacheResolver<T> cacheResolverOne;
	private final CacheResolver<T> cacheResolverTwo;

	/**
	 * Factory method used to compose an array of {@link CacheResolver CacheResolvers} in a composition.
	 *
	 * @param <T> {@link Class subclass} of {@link GemFireCache}.
	 * @param cacheResolvers array of {@link CacheResolver CacheResolvers} to compose; may be {@literal null}.
	 * @return a composition from the array of {@link CacheResolver CacheResolvers}; may be {@literal null}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see #compose(Iterable)
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends GemFireCache> CacheResolver<T> compose(@Nullable CacheResolver<T>... cacheResolvers) {
		return compose(Arrays.asList(ArrayUtils.nullSafeArray(cacheResolvers, CacheResolver.class)));
	}

	/**
	 * Factory method used to compose an {@link Iterable} collection of {@link CacheResolver CacheResolvers}
	 * in a composition.
	 *
	 * @param <T> {@link Class subclass} of {@link GemFireCache}.
	 * @param cacheResolvers {@link Iterable} collection of {@link CacheResolver CacheResolvers} to compose;
	 * may be {@literal null}.
	 * @return a composition from the {@link Iterable} collection of {@link CacheResolver CacheResolvers};
	 * may be {@literal null}.
	 * @see #compose(CacheResolver, CacheResolver)
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see java.lang.Iterable
	 */
	@Nullable
	public static <T extends GemFireCache> CacheResolver<T> compose(@Nullable Iterable<CacheResolver<T>> cacheResolvers) {

		CacheResolver<T> current = null;

		for (CacheResolver<T> cacheResolver : CollectionUtils.nullSafeIterable(cacheResolvers)) {
			current = compose(current, cacheResolver);
		}

		return current;
	}

	/**
	 * Null-safe factory method used to compose two {@link CacheResolver} objects in a composition.
	 *
	 * @param <T> {@link Class subclass} of {@link GemFireCache}.
	 * @param one first {@link CacheResolver} in the composition.
	 * @param two second {@link CacheResolver} in the composition.
	 * @return the first {@link CacheResolver} if the second {@link CacheResolver} is {@literal null}.
	 * Return the second {@link CacheResolver} if the first {@link CacheResolver} is {@literal null}.
	 * Otherwise, return a composition of both {@link CacheResolver} one and two as a {@link ComposableCacheResolver}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see #ComposableCacheResolver(CacheResolver, CacheResolver)
	 */
	@Nullable
	public static <T extends GemFireCache> CacheResolver<T> compose(@Nullable CacheResolver<T> one,
			@Nullable CacheResolver<T> two) {

		return one == null ? two : two == null ? one : new ComposableCacheResolver<>(one, two);
	}

	/**
	 * Constructs a new instance of {@link ComposableCacheResolver} initialized and composed with
	 * the given {@link CacheResolver CacheResolvers} forming the composition.
	 *
	 * @param cacheResolverOne first {@link CacheResolver} in the composition.
	 * @param cacheResolverTwo second {@link CacheResolver} in the composition.
	 * @throws IllegalArgumentException if either {@link CacheResolver} argument is {@literal null}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected ComposableCacheResolver(@NonNull CacheResolver<T> cacheResolverOne,
			@NonNull CacheResolver<T> cacheResolverTwo) {

		Assert.notNull(cacheResolverOne, "CacheResolver 1 must not be null");
		Assert.notNull(cacheResolverTwo, "CacheResolver 2 must not be null");

		this.cacheResolverOne = cacheResolverOne;
		this.cacheResolverTwo = cacheResolverTwo;
	}

	/**
	 * Returns a reference to the first, non-null, configured {@link CacheResolver} in the composition.
	 *
	 * @return a reference to the first {@link CacheResolver} in the composition.
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected @NonNull CacheResolver<T> getCacheResolverOne() {
		return this.cacheResolverOne;
	}

	/**
	 * Returns a reference to the second, non-null, configured {@link CacheResolver} in the composition.
	 *
	 * @return a reference to the second {@link CacheResolver} in the composition.
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected @NonNull CacheResolver<T> getCacheResolverTwo() {
		return this.cacheResolverTwo;
	}

	/**
	 * Resolves the first, single reference to a {@link GemFireCache}, handling any {@link Exception Exceptions}
	 * throwing by the composed {@link CacheResolver CacheResolvers}, such as a {@link CacheClosedException}.
	 *
	 * This method may ultimately still result in a thrown {@link Exception}, but it will make a best effort to
	 * exhaustively consult all composed {@link CacheResolver CacheResolvers}.
	 *
	 * @return the first, single resolved reference to a {@link GemFireCache}.
	 * @see org.springframework.data.gemfire.CacheResolver#resolve()
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #getCacheResolverOne()
	 * @see #getCacheResolverTwo()
	 */
	@Override
	public T doResolve() {

		Supplier<T> cacheSupplier = () -> getCacheResolverTwo().resolve();

		try {

			T cache = getCacheResolverOne().resolve();

			return cache != null ? cache : cacheSupplier.get();
		}
		catch (Throwable ignore) {
			return cacheSupplier.get();
		}
	}
}
