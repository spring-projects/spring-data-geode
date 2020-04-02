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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.data.gemfire.RegionResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link RegionResolver} implementation capable of caching the results of a Region resolution (lookup) operation.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.CacheListener
 * @see org.apache.geode.cache.util.CacheListenerAdapter
 * @see org.springframework.data.gemfire.RegionResolver
 * @since 2.3.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCachingRegionResolver extends CacheListenerAdapter implements RegionResolver {

	private final Map<String, Region> nameToRegionCache = new ConcurrentHashMap<>();

	/**
	 * Internal, utility method to cache a {@link Region} by {@link Region#getName() name},
	 * overriding any existing cache entry for the given {@link Region} if it already exists.
	 *
	 * @param region {@link Region} to cache.
	 */
	synchronized void cache(@Nullable Region region) {
		Optional.ofNullable(region)
			.filter(it -> StringUtils.hasText(region.getName()))
			.ifPresent(it -> this.nameToRegionCache.put(region.getName(), region));
	}

	/**
	 * @inheritDoc
	 */
	@Nullable @Override @SuppressWarnings("unchecked")
	public synchronized <K, V> Region<K, V> resolve(@Nullable String regionName) {

		return StringUtils.hasText(regionName)
			? this.nameToRegionCache.computeIfAbsent(regionName, this::doResolveAndRegisterResolverAsCacheListener)
			: null;
	}

	/**
	 * Performs the actual {@link Region} resolution operation to resolve a {@link Region} with
	 * the given {@link String name} by calling {@link #doResolve(String)} and then registers
	 * this {@link RegionResolver} as a {@link CacheListener} with the resolved {@link Region}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @param regionName {@link String name} of the {@link Region} to resolve.
	 * @return the resolved {@link Region} with the given {@link String name}; may be {@literal null}.
	 * @see org.apache.geode.cache.Region
	 * @see java.lang.String
	 * @see #doResolve(String)
	 */
	@SuppressWarnings("unchecked")
	<K, V> Region<K, V> doResolveAndRegisterResolverAsCacheListener(String regionName) {

		return Optional.<Region<K, V>>ofNullable(doResolve(regionName))
			//.filter(this::isResolverNotRegisteredAsCacheListener)
			.map(Region::getAttributesMutator)
			.map(attributesMutator -> {
				attributesMutator.addCacheListener(AbstractCachingRegionResolver.this);
				return attributesMutator.getRegion();
			})
			.orElse(null);
	}

	@SuppressWarnings("unused")
	private boolean isResolverNotRegisteredAsCacheListener(Region region) {
		return region != null && !isResolverRegisteredAsCacheListener(region);
	}

	private boolean isResolverRegisteredAsCacheListener(Region region) {

		return Optional.ofNullable(region)
			.map(Region::getAttributes)
			.map(RegionAttributes::getCacheListeners)
			.map(Arrays::asList)
			.filter(cacheListeners -> cacheListeners.contains(this))
			.isPresent();
	}

	/**
	 * Performs the actual {@link Region} resolution operation to resolve a {@link Region} with
	 * the given {@link String name}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @param regionName {@link String name} of the {@link Region} to resolve.
	 * @return the resolved {@link Region} with the given {@link String name}; may be {@literal null}.
	 * @see org.apache.geode.cache.Region
	 * @see java.lang.String
	 */
	@Nullable
	protected abstract <K, V> Region<K, V> doResolve(@Nullable String regionName);

	/**
	 * Clears the cache entry for the {@link Region} identified by the {@link RegionEvent}.
	 *
	 * @param event {@link RegionEvent} object capturing the details of the {@link Region} destroyed event.
	 * @see org.apache.geode.cache.RegionEvent
	 * @see #remove(String)
	 */
	@Override
	public void afterRegionDestroy(@Nullable RegionEvent event) {

		Optional.ofNullable(event)
			.map(RegionEvent::getRegion)
			.map(Region::getName)
			.filter(StringUtils::hasText)
			.ifPresent(this::remove);
	}

	/**
	 * Removes the cache entry for the cached {@link Region} with the given {@link String name}.
	 *
	 * @param regionName {@link String name} of the {@link Region} to remove from the cache.
	 * @see java.util.concurrent.ConcurrentHashMap#remove(Object)
	 */
	synchronized void remove(@NonNull String regionName) {
		this.nameToRegionCache.remove(regionName);
	}
}
