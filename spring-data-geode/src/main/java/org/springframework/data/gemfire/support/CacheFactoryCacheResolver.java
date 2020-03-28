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

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;

import org.springframework.data.gemfire.CacheResolver;

/**
 * Cacheable {@link CacheResolver} implementation resolving a {@link Cache}
 * using the {@link CacheFactory} API.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingCacheResolver
 * @since 2.3.0
 */
public class CacheFactoryCacheResolver extends AbstractCachingCacheResolver<Cache> {

	public static final CacheFactoryCacheResolver INSTANCE = new CacheFactoryCacheResolver();

	@Override
	protected Cache doResolve() {
		return CacheFactory.getAnyInstance();
	}
}
