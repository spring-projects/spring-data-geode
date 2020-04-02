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

import java.util.Optional;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.RegionResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link RegionResolver} implementation used to resolve a cache {@link Region} identified by {@link String name}
 * using the configured {@link RegionService}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.RegionService
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.RegionResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingRegionResolver
 * @since 2.3.0
 */
public class RegionServiceRegionResolver<T extends RegionService> extends AbstractCachingRegionResolver {

	/**
	 * Factory method used to construct a {@link RegionServiceRegionResolver} from a {@link CacheResolver}.
	 *
	 * The {@link CacheResolver} will resolve an instance of {@link GemFireCache}, such as a {@literal peer}
	 * {@link Cache} or a {@link ClientCache}, which is a {@link RegionService} capable of resolving a {@link Region}
	 * identified by {@link String name}.
	 *
	 * @param <S> {@link Class subclass} of {@link GemFireCache}.
	 * @param cacheResolver {@link CacheResolver} used to resolve the {@link RegionService}.
	 * @return a new instance of {@link RegionServiceRegionResolver}.
	 * @throws IllegalArgumentException if {@link CacheResolver} is {@literal null}.
	 * @see #RegionServiceRegionResolver(RegionServiceResolver)
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	@NonNull
	public static <S extends GemFireCache> RegionServiceRegionResolver<S> from(@NonNull CacheResolver<S> cacheResolver) {

		Assert.notNull(cacheResolver, "CacheResolver must not be null");

		return new RegionServiceRegionResolver<>(() -> Optional.ofNullable(cacheResolver.resolve()));
	}

	/**
	 * Factory method used to construct a {@link RegionServiceRegionResolver} initialized with
	 * the given {@link RegionService}.
	 *
	 * The {@link RegionService} may be an instance of {@link GemFireCache}, such as a {@literal peer} {@link Cache}
	 * or {@link ClientCache}.
	 *
	 * @param regionService {@link RegionService} used to resolve cache {@link Region Regions}
	 * identified by {@link String name}; may be {@literal null}.
	 * @return a new instance of {@link RegionServiceRegionResolver}.
	 * @see #RegionServiceRegionResolver(RegionServiceResolver)
	 * @see org.apache.geode.cache.RegionService
	 */
	@NonNull
	public static RegionServiceRegionResolver<RegionService> from(@Nullable RegionService regionService) {
		return new RegionServiceRegionResolver<>(() -> Optional.ofNullable(regionService));
	}

	private final RegionServiceResolver<T> resolver;

	/**
	 * Constructs a new instance of {@link RegionServiceRegionResolver} initialized with
	 * the given {@link RegionServiceResolver}.
	 *
	 * @param resolver {@link RegionServiceResolver} used to resolve the {@link RegionService} that is used to resolve
	 * cache {@link Region Regions} by {@link String name}.
	 * @throws IllegalArgumentException if {@link RegionServiceResolver} is {@literal null}.
	 * @see RegionServiceResolver
	 */
	public RegionServiceRegionResolver(RegionServiceResolver<T> resolver) {

		Assert.notNull(resolver, "RegionServiceResolver must not be null");

		this.resolver = resolver;
	}

	/**
	 * Returns the configured {@link RegionServiceResolver} used to resolve the {@link RegionService} that is then used
	 * to resolve cache {@link Region Regions} by {@link String name}.
	 *
	 * @return the configured {@link RegionServiceResolver}.
	 * @see RegionServiceResolver
	 */
	protected RegionServiceResolver<T> getRegionServiceResolver() {
		return this.resolver;
	}

	/**
	 * Resolves a cache {@link Region} identified by the given {@link String name} using the configured
	 * {@link RegionService} resolved from the {@link RegionServiceResolver}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @param regionName {@link String name} of the {@link Region} to resolve.
	 * @return the resolved cache {@link Region} identified by the given {@link String name}; may be {@literal null}.
	 * @see org.apache.geode.cache.RegionService#getRegion(String)
	 * @see #getRegionServiceResolver()
	 */
	@Nullable @Override
	protected <K, V> Region<K, V> doResolve(@Nullable String regionName) {

		return getRegionServiceResolver().resolve()
			.<Region<K, V>>map(regionService -> regionService.getRegion(regionName))
			.orElse(null);
	}

	@FunctionalInterface
	protected interface RegionServiceResolver<T extends RegionService> {
		Optional<T> resolve();
	}
}
