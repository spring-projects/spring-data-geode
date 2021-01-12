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
package org.springframework.data.gemfire.client.support;

import java.util.Optional;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link PoolResolver} implementation used to resolve the {@literal DEFAULT} {@link Pool} from a {@link ClientCache}
 * instance by lazily resolving the {@link ClientCache} instance and calling {@link ClientCache#getDefaultPool()}
 * on {@literal DEFAULT} {@link Pool} resolution.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @since 2.3.0
 */
public class ClientCacheDefaultPoolResolver implements PoolResolver {

	private final CacheResolver<ClientCache> clientCacheResolver;

	/**
	 * Constructs a new instance of {@link ClientCacheDefaultPoolResolver} initialized with a {@link CacheResolver}
	 * used to lazily resolve the {@link ClientCache} instance on {@link Pool} resolution.
	 *
	 * @param clientCacheResolver {@link CacheResolver} used to lazily resolve the {@link ClientCache} instance;
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link CacheResolver} is {@literal null}.
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	public ClientCacheDefaultPoolResolver(@NonNull CacheResolver<ClientCache> clientCacheResolver) {

		Assert.notNull(clientCacheResolver, "CacheResolver for ClientCache must not be null");

		this.clientCacheResolver = clientCacheResolver;
	}

	/**
	 * Returns a reference to the configured {@link CacheResolver} used to (lazily) resolve
	 * the {@link ClientCache} instance.
	 *
	 * @return the configured {@link CacheResolver} used to resolve the {@link ClientCache} instance.
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected @NonNull CacheResolver<ClientCache> getClientCacheResolver() {
		return this.clientCacheResolver;
	}

	/**
	 * @inheritDoc
	 */
	@Nullable @Override
	public Pool resolve(@Nullable String poolName) {

		return Optional.of(getClientCacheResolver())
			.filter(it -> DEFAULT_POOL_NAME.equals(poolName))
			.map(CacheResolver::resolve)
			.map(ClientCache::getDefaultPool)
			.orElse(null);
	}
}
