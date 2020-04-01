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
package org.springframework.data.gemfire;

import org.apache.geode.cache.Region;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link RegionResolver} interface is a {@literal Strategy} interface used to encapsulate different algorithms
 * (Strategies) used to resolve a cache {@link Region}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @since 2.3.0
 */
@FunctionalInterface
public interface RegionResolver {

	/**
	 * Returns a {@link Region} resolved with the given {@link String name}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value;
	 * @param regionName {@link String name} of the {@link Region} to resolve; may be {@literal null}.
	 * @return the resolved {@link Region} with the given {@link String name}; may be {@literal null}.
	 * @see org.apache.geode.cache.Region
	 * @see java.lang.String
	 */
	@Nullable <K, V> Region<K, V> resolve(@Nullable String regionName);

	/**
	 * Requires a {@link Region} resolved from the given {@link String name}.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value;
	 * @param regionName {@link String name} of the {@link Region} to resolve; must not be {@literal null}.
	 * @return the resolved {@link Region} with the given {@link String name}; never {@literal null}.
	 * @throws IllegalStateException if the resolved {@link Region} is {@literal null}, i.e. does not exist.
	 * @see org.apache.geode.cache.Region
	 * @see java.lang.String
	 * @see #resolve(String)
	 */
	default @NonNull <K, V> Region<K, V> require(@NonNull String regionName) {

		Region<K, V> region = StringUtils.hasText(regionName) ? resolve(regionName) : null;

		Assert.state(region != null,
			() -> String.format("Region with name [%s] not found", regionName));

		return region;
	}
}
