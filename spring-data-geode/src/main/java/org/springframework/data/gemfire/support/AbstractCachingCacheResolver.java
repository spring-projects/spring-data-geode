/*
 * Copyright 2017-2022 the original author or authors.
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

import org.apache.geode.cache.GemFireCache;

import org.springframework.data.gemfire.CacheResolver;

/**
 * Thread-safe, abstract {@link CacheResolver} implementation to "cache" the instance reference to the (single)
 * {@link GemFireCache} so that the {@link GemFireCache} object is only ever resolved once.
 *
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @since 2.3.0
 */
public abstract class AbstractCachingCacheResolver<T extends GemFireCache> implements CacheResolver<T> {

	private T cacheReference;

	/**
	 * @inheritDoc
	 */
	@Override
	public synchronized T resolve() {

		if (this.cacheReference == null) {
			this.cacheReference = doResolve();
		}

		return this.cacheReference;
	}

	/**
	 * Performs the actual resolution process of the {@link GemFireCache} object iff the cache reference
	 * is not already cached.
	 *
	 * @see #resolve()
	 */
	protected abstract T doResolve();

}
