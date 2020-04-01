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

import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.RegionResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link RegionResolver} implementation resolving a single, configured {@link Region} object.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.RegionResolver
 * @since 2.3.0
 */
@SuppressWarnings("rawtypes")
public class SingleRegionRegionResolver implements RegionResolver {

	private final Region region;

	/**
	 * Constructs a new instance of {@link SingleRegionRegionResolver} with the given {@link Region}.
	 *
	 * @param region {@link Region} returned in the resolution process; must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link Region} is {@literal null}.
	 * @see org.apache.geode.cache.Region
	 */
	public SingleRegionRegionResolver(@NonNull Region region) {

		Assert.notNull(region, "Region must not be null");

		this.region = region;
	}

	/**
	 * Returns a reference to the configured {@link Region}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @return a reference to the configured {@link Region}.
	 * @see org.apache.geode.cache.Region
	 */
	@SuppressWarnings("unchecked")
	protected @NonNull <K, V> Region<K, V> getRegion() {
		return this.region;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public @Nullable <K, V> Region<K, V> resolve(@Nullable String regionName) {

		Region<K, V> region = getRegion();

		return region.getName().equals(regionName) ? region : null;
	}
}
