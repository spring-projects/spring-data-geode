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

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.lang.Nullable;

/**
 * {@link CacheResolver} implementation that resolves to a configured, single {@link GemFireCache} instance.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @since 2.3.0
 */
public abstract class SingleCacheCacheResolver {

	/**
	 * Factory method used to resolve a single, configured instance of a {@literal peer} {@link Cache}.
	 *
	 * @param cache {@link Cache} to resolve.
	 * @return a single, configured instance of a {@literal peer} {@link Cache}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see org.apache.geode.cache.Cache
	 */
	public static CacheResolver<Cache> from(@Nullable Cache cache) {
		return () -> cache;
	}

	/**
	 * Factory method used to resolve a single, configured instance of a {@literal peer} {@link ClientCache}.
	 *
	 * @param clientCache {@link ClientCache} to resolve.
	 * @return a single, configured instance of a {@literal peer} {@link ClientCache}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see org.apache.geode.cache.client.ClientCache
	 */
	public static CacheResolver<ClientCache> from(@Nullable ClientCache clientCache) {
		return () -> clientCache;
	}
}
