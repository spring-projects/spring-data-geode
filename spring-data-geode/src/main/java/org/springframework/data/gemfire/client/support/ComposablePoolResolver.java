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
package org.springframework.data.gemfire.client.support;

import java.util.Arrays;

import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Composite of {@link PoolResolver PoolResolvers} functioning as a single {@link PoolResolver}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @since 2.3.0
 */
public class ComposablePoolResolver implements PoolResolver {

	/**
	 * Null-safe factory method to compose an array of {@link PoolResolver} objects.
	 *
	 * Preserves order in the composition.
	 *
	 * @param poolResolvers array of {@link PoolResolver} objects to compose.
	 * @return a composition from the array of {@link PoolResolver} objects; may return {@literal null}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 * @see #compose(Iterable)
	 */
	public static @Nullable PoolResolver compose(@Nullable PoolResolver... poolResolvers) {
		return compose(Arrays.asList(ArrayUtils.nullSafeArray(poolResolvers, PoolResolver.class)));
	}

	/**
	 * Null-safe factory method to compose an {@link Iterable} of {@link PoolResolver} objects.
	 *
	 * Preserves order in the composition if the {@link Iterable} collection-like data structure is ordered,
	 * like a {@link java.util.List}).
	 *
	 * @param poolResolvers {@link Iterable} of {@link PoolResolver} objects to compose.
	 * @return a composition from the {@link Iterable} of {@link PoolResolver} objects; may return {@literal null}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 * @see java.lang.Iterable
	 * @see #compose(PoolResolver, PoolResolver)
	 */
	public static @Nullable PoolResolver compose(@Nullable Iterable<PoolResolver> poolResolvers) {

		PoolResolver current = null;

		for (PoolResolver poolResolver : CollectionUtils.nullSafeIterable(poolResolvers)) {
			current = compose(current, poolResolver);
		}

		return current;
	}

	/**
	 * Null-safe factory method to compose two {@link PoolResolver} objects in a composition.
	 *
	 * @param one first {@link PoolResolver} in the composition.
	 * @param two second {@link PoolResolver} in the composition.
	 * @return a composition from the two {@link PoolResolver} objects.  Returns the first {@link PoolResolver}
	 * if the second {@link PoolResolver} is {@literal null}.  Returns the second {@link PoolResolver} if the first
	 * {@link PoolResolver} is {@literal null}.  Returns {@literal null} if both {@link PoolResolver} arguments
	 * are {@literal null}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 * @see #ComposablePoolResolver(PoolResolver, PoolResolver)
	 */
	public static @Nullable PoolResolver compose(@Nullable PoolResolver one, @Nullable PoolResolver two) {
		return one == null ? two : two == null ? one : new ComposablePoolResolver(one, two);
	}

	private final PoolResolver poolResolverOne;
	private final PoolResolver poolResolverTwo;

	/**
	 * Constructs a new instance of {@link ComposablePoolResolver} initialized and composed of two {@link PoolResolver}
	 * implementations that will function as one.
	 *
	 * @param poolResolverOne first {@link PoolResolver} in the composition order.
	 * @param poolResolverTwo second {@link PoolResolver} in the composition order.
	 * @throws IllegalArgumentException if either the first or second {@link PoolResolver} are {@literal null}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	protected ComposablePoolResolver(PoolResolver poolResolverOne, PoolResolver poolResolverTwo) {

		Assert.notNull(poolResolverOne, "PoolResolver 1 must not be null");
		Assert.notNull(poolResolverTwo, "PoolResolver 2 must not be null");

		this.poolResolverOne = poolResolverOne;
		this.poolResolverTwo = poolResolverTwo;
	}

	/**
	 * Returns a reference to the first {@link PoolResolver} in the composition.
	 *
	 * @return a reference to the first {@link PoolResolver} in the composition.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	protected @NonNull PoolResolver getPoolResolverOne() {
		return this.poolResolverOne;
	}

	/**
	 * Returns a reference to the second {@link PoolResolver} in the composition.
	 *
	 * @return a reference to the second {@link PoolResolver} in the composition.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	protected @NonNull PoolResolver getPoolResolverTwo() {
		return this.poolResolverTwo;
	}

	/**
	 * Attempts to resolve a {@link Pool} with the given {@link String name} by delegating to the composed
	 * {@link PoolResolver} objects.
	 *
	 * The first {@link PoolResolver} in the composition to resolve a {@link Pool} with the given {@link String name}
	 * stops the resolution process and returns the target {@link Pool}.  If no {@link Pool} with the given
	 * {@link String name} can be resolved by any {@link PoolResolver} in the composition, then {@literal null}
	 * will be returned.
	 *
	 * @param poolName {@link String name} of the {@link Pool} to resolve.
	 * @return the resolved {@link Pool} or {@literal null} if a {@link Pool} with {@link String name}
	 * cannot be resolved.
	 * @see org.apache.geode.cache.client.Pool
	 * @see #getPoolResolverOne()
	 * @see #getPoolResolverTwo()
	 */
	@Nullable @Override
	public Pool resolve(@Nullable String poolName) {

		Pool pool = getPoolResolverOne().resolve(poolName);

		return pool != null ? pool : getPoolResolverTwo().resolve(poolName);
	}
}
