/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.lang.NonNull;

/**
 * Abstract class defining useful Java {@link Function Functions} for Apache Geode
 *
 * @author John Blum
 * @see java.util.function.Function
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @since 2.7.0
 */
@SuppressWarnings("unused")
public abstract class GemfireFunctions {

	public static @NonNull <K, V> Function<GemFireCache, Region<K, V>> getRegionFromCache(String regionName) {
		return cache -> cache.getRegion(regionName);
	}

	public static @NonNull <K, V> Supplier<Region<K, V>> getRegionFromCache(@NonNull GemFireCache cache,
			String regionName) {

		return () -> cache.getRegion(regionName);
	}

	public static @NonNull <K, V> Function<Region<?, ?>, Region<K, V>> getSubregionFromRegion(String regionName) {
		return parentRegion -> parentRegion.getSubregion(regionName);
	}
}
