/*
 * Copyright 2010-2022 the original author or authors.
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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.support.AbstractFactoryBeanSupport;
import org.springframework.data.gemfire.support.GemfireFunctions;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring {@link FactoryBean} used to look up or create {@link Region Regions}.
 *
 * For declaring and configuring new {@literal client} {@link Region Regions}, see {@link ClientRegionFactoryBean}.
 * For declaring and configuring new {@literal peer} {@link Region Regions}, see {@link PeerRegionFactoryBean}
 * and {@link Class subclasses}.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 */
@SuppressWarnings("unused")
public abstract class ResolvableRegionFactoryBean<K, V> extends AbstractFactoryBeanSupport<Region<K, V>>
		implements InitializingBean {

	protected static final String CREATING_REGION_LOG_MESSAGE = "Creating Region [%1$s] in Cache [%2$s]";
	protected static final String REGION_FOUND_LOG_MESSAGE = "Found Region [%1$s] in Cache [%2$s]";
	protected static final String REGION_NOT_FOUND_ERROR_MESSAGE = "Region [%1$s] in Cache [%2$s] not found";

	private Boolean lookupEnabled = false;

	private GemFireCache cache;

	private Region<?, ?> parent;

	private Resource snapshot;

	private volatile Region<K, V> region;

	private String name;
	private String regionName;

	/**
	 * Initializes this {@link ResolvableRegionFactoryBean} after properties have been set by the Spring container.
	 *
	 * @throws Exception if initialization fails.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @see #createRegion(GemFireCache, String)
	 */
	@Override
	@SuppressWarnings("all")
	public void afterPropertiesSet() throws Exception {

		GemFireCache cache = requireCache();

		String regionName = requireRegionName();

		synchronized (cache) {

			setRegion(resolveRegion(cache, regionName));

			if (getRegion() != null) {
				logInfo(REGION_FOUND_LOG_MESSAGE, regionName, cache.getName());
			}
			else {
				logInfo(CREATING_REGION_LOG_MESSAGE, regionName, cache.getName());
				setRegion(postProcess(loadSnapshot(createRegion(cache, regionName))));
			}
		}
	}

	private @NonNull GemFireCache requireCache() {

		GemFireCache cache = getCache();

		Assert.notNull(cache, "Cache is required");

		return cache;
	}

	@NonNull String requireRegionName() {

		String regionName = resolveRegionName();

		Assert.hasText(regionName, "regionName, name or the beanName property must be set");

		return regionName;
	}

	private @Nullable Region<K, V> resolveRegion(@NonNull GemFireCache cache, @NonNull String regionName) {

		return isLookupEnabled()
			? Optional.ofNullable(getParent())
				.<Region<K, V>>map(GemfireFunctions.getSubregionFromRegion(regionName))
				.orElseGet(GemfireFunctions.getRegionFromCache(cache, regionName))
			: null;
	}

	/**
	 * Resolves the configured {@link String name} of the {@link Region}.
	 *
	 * @return a {@link String} containing the {@literal name} of the {@link Region}.
	 * @see org.apache.geode.cache.Region#getName()
	 */
	public String resolveRegionName() {

		String name = this.name;
		String regionName = this.regionName;

		return StringUtils.hasText(regionName) ? regionName
			: StringUtils.hasText(name) ? name
			: getBeanName();
	}

	/**
	 * Creates a new {@link Region} with the given {@link String name}.
	 *
	 * This method gets called when a {@link Region} with the specified {@link String name} does not already exist.
	 * By default, this method implementation throws a {@link BeanInitializationException} and it is expected
	 * that {@link Class subclasses} will override this method.
	 *
	 * @param cache reference to the {@link GemFireCache}.
	 * @param regionName {@link String name} of the new {@link Region}.
	 * @return a new {@link Region} with the given {@link String name}.
	 * @throws BeanInitializationException by default unless a {@link Class subclass} overrides this method.
	 * @see org.apache.geode.cache.GemFireCache
	 * @see org.apache.geode.cache.Region
	 */
	protected Region<K, V> createRegion(GemFireCache cache, String regionName) throws Exception {
		throw new BeanInitializationException(String.format(REGION_NOT_FOUND_ERROR_MESSAGE, regionName, cache));
	}

	/**
	 * Loads data from the configured {@link Resource snapshot} into the given {@link Region}.
	 *
	 * @param region {@link Region} to load; must not be {@literal null}.
	 * @return the given {@link Region}.
	 * @throws RuntimeException if loading the snapshot fails.
	 * @see org.apache.geode.cache.Region#loadSnapshot(InputStream)
	 * @see org.apache.geode.cache.Region
	 */
	protected @NonNull Region<K, V> loadSnapshot(@NonNull Region<K, V> region) {

		Resource snapshot = this.snapshot;

		if (snapshot != null) {

			SpringUtils.VoidReturningThrowableOperation operation =
				() -> region.loadSnapshot(snapshot.getInputStream());

			Function<Throwable, RuntimeException> exceptionHandler =
				cause -> newRuntimeException(cause, "Failed to load snapshot [%s]", snapshot);

			SpringUtils.safeRunOperation(operation, exceptionHandler);
		}

		return region;
	}

	/**
	 * Post-process the {@link Region} created by this {@link PeerRegionFactoryBean}.
	 *
	 * @param region {@link Region} to process.
	 * @see org.apache.geode.cache.Region
	 */
	protected Region<K, V> postProcess(Region<K, V> region) {
		return region;
	}

	/**
	 * Returns an object reference to the {@link Region} created by this {@link ResolvableRegionFactoryBean}.
	 *
	 * @return an object reference to the {@link Region} created by this {@link ResolvableRegionFactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 * @see org.apache.geode.cache.Region
	 * @see #getRegion()
	 */
	@Override
	public Region<K, V> getObject() throws Exception {
		return getRegion();
	}

	/**
	 * Returns the {@link Class} type of the {@link Region} produced by this {@link ResolvableRegionFactoryBean}.
	 *
	 * @return the {@link Class} type of the {@link Region} produced by this {@link ResolvableRegionFactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {

		Region<?, ?> region = getRegion();

		return region != null ? region.getClass() : Region.class;
	}

	/**
	 * Returns a reference to the {@link GemFireCache} used to create the {@link Region}.
	 *
	 * @return a reference to the {@link GemFireCache} used to create the {@link Region}.
	 * @see org.apache.geode.cache.GemFireCache
	 */
	public GemFireCache getCache() {
		return this.cache;
	}

	/**
	 * Sets a reference to the {@link GemFireCache} used to create the {@link Region}.
	 *
	 * @param cache reference to the {@link GemFireCache}.
	 * @see org.apache.geode.cache.GemFireCache
	 */
	public void setCache(GemFireCache cache) {
		this.cache = cache;
	}

	public void setLookupEnabled(@Nullable Boolean lookupEnabled) {
		this.lookupEnabled = lookupEnabled;
	}

	public @Nullable Boolean getLookupEnabled() {
		return this.lookupEnabled;
	}

	public boolean isLookupEnabled() {
		return Boolean.TRUE.equals(getLookupEnabled());
	}

	/**
	 * Sets the name of the cache {@link Region} based on the bean 'name' attribute.  If no {@link Region} is found
	 * with the given name, a new one will be created.  If no name is given, the value of the 'beanName' property
	 * will be used.
	 *
	 * @param name {@link Region} name.
	 * @see #setBeanName(String)
	 * @see org.apache.geode.cache.Region#getFullPath()
	 */
	public void setName(@NonNull String name) {
		this.name = name;
	}

	/**
	 * Sets a reference to the parent {@link Region} making this {@link FactoryBean}
	 * represent a cache {@link Region Sub-Region}.
	 *
	 * @param parent reference to the parent {@link Region}.
	 * @see org.apache.geode.cache.Region
	 */
	public void setParent(@Nullable Region<?, ?> parent) {
		this.parent = parent;
	}

	/**
	 * Returns a reference to the parent {@link Region} making this {@link FactoryBean}
	 * represent a cache {@link Region Sub-Region}.
	 *
	 * @return a reference to the parent {@link Region}, or {@literal null} if this {@link Region}
	 * is not a {@link Region Sub-Region}.
	 * @see org.apache.geode.cache.Region
	 */
	protected @Nullable Region<?, ?> getParent() {
		return this.parent;
	}

	/**
	 * Sets a reference to the {@link Region} to be resolved by this Spring {@link FactoryBean}.
	 *
	 * @param region reference to the resolvable {@link Region}.
	 * @see org.apache.geode.cache.Region
	 */
	protected void setRegion(@Nullable Region<K, V> region) {
		this.region = region;
	}

	/**
	 * Returns a reference to the {@link Region} resolved by this Spring {@link FactoryBean}
	 * during the lookup operation; maybe a new {@link Region}.
	 *
	 * @return a reference to the {@link Region} resolved during lookup.
	 * @see org.apache.geode.cache.Region
	 */
	public @Nullable Region<K, V> getRegion() {
		return this.region;
	}

	/**
	 * Sets the name of the cache {@link Region}.  If no {@link Region} is found with the given name,
	 * a new one will be created.  If no name is given, the value of the 'name' property will be used.
	 *
	 * @param regionName name of the {@link Region}.
	 * @see #setName(String)
	 * @see org.apache.geode.cache.Region#getName()
	 */
	public void setRegionName(@Nullable String regionName) {
		this.regionName = regionName;
	}

	/**
	 * Sets the snapshots used for loading a newly <i>created</i> region. That
	 * is, the snapshot will be used <i>only</i> when a new region is created -
	 * if the region already exists, no loading will be performed.
	 *
	 * @see #setName(String)
	 * @param snapshot the snapshot to set
	 */
	public void setSnapshot(@Nullable Resource snapshot) {
		this.snapshot = snapshot;
	}
}
