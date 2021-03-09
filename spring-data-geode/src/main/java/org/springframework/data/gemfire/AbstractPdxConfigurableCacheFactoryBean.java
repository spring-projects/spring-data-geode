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

import java.util.Optional;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Abstract base class encapsulating PDX configuration metadata applied to both Apache Geode {@link ClientCache}
 * and {@literal peer} {@link Cache} instances.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.data.gemfire.AbstractBasicCacheFactoryBean
 * @since 2.5.0
 */
public abstract class AbstractPdxConfigurableCacheFactoryBean extends AbstractBasicCacheFactoryBean {

	private Boolean pdxIgnoreUnreadFields;
	private Boolean pdxPersistent;
	private Boolean pdxReadSerialized;

	private PdxSerializer pdxSerializer;

	private String pdxDiskStoreName;

	/**
	 * Sets the {@link String name} of the Apache Geode {@link DiskStore} used to store PDX metadata.
	 *
	 * @param pdxDiskStoreName {@link String name} for the PDX {@link DiskStore}.
	 * @see org.apache.geode.cache.CacheFactory#setPdxDiskStore(String)
	 * @see org.apache.geode.cache.DiskStore#getName()
	 */
	public void setPdxDiskStoreName(@Nullable String pdxDiskStoreName) {
		this.pdxDiskStoreName = pdxDiskStoreName;
	}

	/**
	 * Gets the {@link String name} of the Apache Geode {@link DiskStore} used to store PDX metadata.
	 *
	 * @return the {@link String name} of the PDX {@link DiskStore}.
	 * @see org.apache.geode.cache.GemFireCache#getPdxDiskStore()
	 * @see org.apache.geode.cache.DiskStore#getName()
	 */
	public @Nullable String getPdxDiskStoreName() {
		return this.pdxDiskStoreName;
	}

	/**
	 * Configures whether PDX will ignore unread fields when deserializing PDX bytes back to an {@link Object}.
	 *
	 * Defaults to {@literal false}.
	 *
	 * @param pdxIgnoreUnreadFields {@link Boolean} value controlling ignoring unread fields.
	 * @see org.apache.geode.cache.CacheFactory#setPdxIgnoreUnreadFields(boolean)
	 */
	public void setPdxIgnoreUnreadFields(@Nullable Boolean pdxIgnoreUnreadFields) {
		this.pdxIgnoreUnreadFields = pdxIgnoreUnreadFields;
	}

	/**
	 * Gets the configuration determining whether PDX will ignore unread fields when deserializing PDX bytes
	 * back to an {@link Object}.
	 *
	 * Defaults to {@literal false}.
	 *
	 * @return a {@link Boolean} value controlling ignoring unread fields.
	 * @see org.apache.geode.cache.GemFireCache#getPdxIgnoreUnreadFields()
	 */
	public @Nullable Boolean getPdxIgnoreUnreadFields() {
		return this.pdxIgnoreUnreadFields;
	}

	/**
	 * Configures whether {@link Class type} metadata for {@link Object objects} serialized to PDX
	 * will be persisted to disk.
	 *
	 * @param pdxPersistent {@link Boolean} value controlling whether PDX {@link Class type} metadata
	 * will be persisted to disk.
	 * @see org.apache.geode.cache.CacheFactory#setPdxPersistent(boolean)
	 */
	public void setPdxPersistent(@Nullable Boolean pdxPersistent) {
		this.pdxPersistent = pdxPersistent;
	}

	/**
	 * Gets the configuration determining whether {@link Class type} metadata for {@link Object objects} serialized
	 * to PDX will be persisted to disk.
	 *
	 * @return a {@link Boolean} value controlling whether PDX {@link Class type} metadata will be persisted to disk.
	 * @see org.apache.geode.cache.GemFireCache#getPdxPersistent()
	 */
	public @Nullable Boolean getPdxPersistent() {
		return this.pdxPersistent;
	}

	/**
	 * Configures whether {@link Object objects} stored in the Apache Geode {@link GemFireCache cache} as PDX
	 * will be read back as PDX bytes or (deserialized) as an {@link Object} when {@link Region#get(Object)}
	 * is called.
	 *
	 * @param pdxReadSerialized {@link Boolean} value controlling the PDX read serialized function.
	 * @see org.apache.geode.cache.CacheFactory#setPdxReadSerialized(boolean)
	 */
	public void setPdxReadSerialized(@Nullable Boolean pdxReadSerialized) {
		this.pdxReadSerialized = pdxReadSerialized;
	}

	/**
	 * Gets the configuration determining whether {@link Object objects} stored in the Apache Geode
	 * {@link GemFireCache cache} as PDX will be read back as PDX bytes or (deserialized) as an {@link Object}
	 * when {@link Region#get(Object)} is called.
	 *
	 * @return a {@link Boolean} value controlling the PDX read serialized function.
	 * @see org.apache.geode.cache.GemFireCache#getPdxReadSerialized()
	 */
	public @Nullable Boolean getPdxReadSerialized() {
		return this.pdxReadSerialized;
	}

	/**
	 * Configures a reference to {@link PdxSerializer} used by this cache to de/serialize {@link Object objects}
	 * stored in the cache and distributed/transferred across the distributed system as PDX bytes.
	 *
	 * @param serializer {@link PdxSerializer} used by this cache to de/serialize {@link Object objects} as PDX.
	 * @see org.apache.geode.cache.CacheFactory#setPdxSerializer(PdxSerializer)
	 * @see org.apache.geode.pdx.PdxSerializer
	 */
	public void setPdxSerializer(@Nullable PdxSerializer serializer) {
		this.pdxSerializer = serializer;
	}

	/**
	 * Get a reference to the configured {@link PdxSerializer} used by this cache to de/serialize {@link Object objects}
	 * stored in the cache and distributed/transferred across the distributed system as PDX bytes.
	 *
	 * @return a reference to the configured {@link PdxSerializer}.
	 * @see org.apache.geode.cache.GemFireCache#getPdxSerializer()
	 * @see org.apache.geode.pdx.PdxSerializer
	 */
	public @Nullable PdxSerializer getPdxSerializer() {
		return this.pdxSerializer;
	}

	/**
	 * Configures the cache to use PDX serialization.
	 *
	 * @param pdxConfigurer {@link PdxConfigurer} used to configure the cache with PDX serialization.
	 * @return the {@link PdxConfigurer#getTarget()}.
	 */
	protected <T> T configurePdx(PdxConfigurer<T> pdxConfigurer) {

		Optional.ofNullable(getPdxDiskStoreName())
			.filter(StringUtils::hasText)
			.ifPresent(pdxConfigurer::setDiskStoreName);

		Optional.ofNullable(getPdxIgnoreUnreadFields()).ifPresent(pdxConfigurer::setIgnoreUnreadFields);

		Optional.ofNullable(getPdxPersistent()).ifPresent(pdxConfigurer::setPersistent);

		Optional.ofNullable(getPdxReadSerialized()).ifPresent(pdxConfigurer::setReadSerialized);

		Optional.ofNullable(getPdxSerializer()).ifPresent(pdxConfigurer::setSerializer);

		return pdxConfigurer.getTarget();
	}

	public interface PdxConfigurer<T> {

		T getTarget();

		PdxConfigurer<T> setDiskStoreName(String diskStoreName);

		PdxConfigurer<T> setIgnoreUnreadFields(Boolean ignoreUnreadFields);

		PdxConfigurer<T> setPersistent(Boolean persistent);

		PdxConfigurer<T> setReadSerialized(Boolean readSerialized);

		PdxConfigurer<T> setSerializer(PdxSerializer pdxSerializer);

	}
}
