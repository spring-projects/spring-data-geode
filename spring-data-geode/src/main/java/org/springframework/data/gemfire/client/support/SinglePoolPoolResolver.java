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

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link PoolResolver} implementation that resolves a single, configured {@link Pool}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @since 2.3.0
 */
public class SinglePoolPoolResolver implements PoolResolver {

	/**
	 * Factory method used to construct a new instance of {@link SinglePoolPoolResolver} from an instance of
	 * {@link ClientCache} using the {@link ClientCache#getDefaultPool()}  DEFAULT} {@link Pool}.
	 *
	 * @param clientCache {@link ClientCache} instance used to resolve the {@link ClientCache#getDefaultPool() DEFAULT}
	 * {@link Pool}.
	 * @return a new {@link SinglePoolPoolResolver} initialized with the {@literal DEFAULT} {@link Pool}.
	 * @throws IllegalArgumentException if the {@link ClientCache} or the {@link ClientCache#getDefaultPool()} DEFAULT}
	 * {@link Pool} is {@literal null}.
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.apache.geode.cache.client.ClientCache#getDefaultPool()
	 */
	public static SinglePoolPoolResolver from(@NonNull ClientCache clientCache) {

		Assert.notNull(clientCache, "ClientCache must not be null");

		return new SinglePoolPoolResolver(clientCache.getDefaultPool());
	}

	private final Pool pool;

	/**
	 * Constructs an instance of {@link SinglePoolPoolResolver} initialized with the given {@link Pool}
	 * returned during resolution.
	 *
	 * @param pool {@link Pool} object resolved by this {@link PoolResolver}.
	 * @throws IllegalArgumentException if {@link Pool} is {@literal null}.
	 * @see org.apache.geode.cache.client.Pool
	 */
	public SinglePoolPoolResolver(@NonNull Pool pool) {

		Assert.notNull(pool, "Pool must not be null");

		this.pool = pool;
	}

	/**
	 * Returns a reference to the configured, "resolvable" {@link Pool}.
	 *
	 * @return a reference to the configured, "resolvable" {@link Pool}.
	 * @see org.apache.geode.cache.client.Pool
	 */
	protected @NonNull Pool getPool() {
		return this.pool;
	}

	/**
	 * Returns the configured {@link Pool} iff the given {@link String poolName} matches
	 * the configured {@link Pool} {@link Pool#getName() name}.
	 *
	 * @param poolName {@link String name} of the {@link Pool} to resolve.
	 * @return the configured {@link Pool} if the configured {@link Pool} {@link Pool#getName() name}
	 * and the given {@link String poolName} match.
	 * @see org.apache.geode.cache.client.Pool#getName()
	 */
	@Nullable @Override
	public Pool resolve(@Nullable String poolName) {

		Pool pool = getPool();

		return pool.getName().equals(poolName) ? pool : null;
	}
}
