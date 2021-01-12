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

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.support.AbstractCachingCacheResolver;

/**
 * Cacheable {@link CacheResolver} implementation resolving a {@link ClientCache}
 * using the {@link ClientCacheFactory} API.
 *
 * @author John Blum
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingCacheResolver
 * @since 2.3.0.
 */
public class ClientCacheFactoryCacheResolver extends AbstractCachingCacheResolver<ClientCache> {

	public static final ClientCacheFactoryCacheResolver INSTANCE = new ClientCacheFactoryCacheResolver();

	@Override
	protected ClientCache doResolve() {
		return ClientCacheFactory.getAnyInstance();
	}
}
