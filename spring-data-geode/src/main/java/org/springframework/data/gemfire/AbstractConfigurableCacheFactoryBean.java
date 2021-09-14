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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Abstract base class encapsulating functionality for (externally) configuring an Apache Geode {@link ClientCache}
 * or peer {@link Cache} as a bean in the Spring context.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.io.InputStream
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.Resource
 * @see org.springframework.data.gemfire.AbstractBasicCacheFactoryBean
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @since 2.5.0
 */
@SuppressWarnings("unused")
public abstract class AbstractConfigurableCacheFactoryBean extends AbstractBasicCacheFactoryBean {

	private boolean useBeanFactoryLocator = false;

	private GemfireBeanFactoryLocator beanFactoryLocator;

	private Properties properties;

	private Resource cacheXml;

	/**
	 * Gets a reference to the configured {@link GemfireBeanFactoryLocator} used to resolve Spring bean references
	 * in Apache Geode native configuration metadata (e.g. {@literal cache.xml}).
	 *
	 * @param beanFactoryLocator reference to the configured {@link GemfireBeanFactoryLocator}.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 */
	protected void setBeanFactoryLocator(@Nullable GemfireBeanFactoryLocator beanFactoryLocator) {
		this.beanFactoryLocator = beanFactoryLocator;
	}

	/**
	 * Returns a reference to the configured {@link GemfireBeanFactoryLocator} used to resolve Spring bean references
	 * in Apache Geode native configuration metadata (e.g. {@literal cache.xml}).
	 *
	 * @return a reference to the configured {@link GemfireBeanFactoryLocator}.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 */
	public @Nullable GemfireBeanFactoryLocator getBeanFactoryLocator() {
		return this.beanFactoryLocator;
	}

	/**
	 * Returns an {@link Optional} reference to the configured {@link GemfireBeanFactoryLocator} used to
	 * resolve Spring bean references in Apache Geode native configuration metadata (e.g. {@literal cache.xml}).
	 *
	 * @return an {@link Optional} reference to the configured {@link GemfireBeanFactoryLocator}.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 * @see #getBeanFactoryLocator()
	 * @see java.util.Optional
	 */
	public Optional<GemfireBeanFactoryLocator> getOptionalBeanFactoryLocator() {
		return Optional.ofNullable(getBeanFactoryLocator());
	}

	/**
	 * Sets a reference to an (optional) Apache Geode native {@literal cache.xml} {@link Resource}.
	 *
	 * @param cacheXml reference to an (optional) Apache Geode native {@literal cache.xml} {@link Resource}.
	 * @see org.springframework.core.io.Resource
	 */
	public void setCacheXml(@Nullable Resource cacheXml) {
		this.cacheXml = cacheXml;
	}

	/**
	 * Returns a reference to an (optional) Apache Geode native {@literal cache.xml} {@link Resource}.
	 *
	 * @return a reference to an (optional) Apache Geode native {@literal cache.xml} {@link Resource}.
	 * @see org.springframework.core.io.Resource
	 */
	public @Nullable Resource getCacheXml() {
		return this.cacheXml;
	}

	/**
	 * Returns an {@link Optional} reference to an Apache Geode native {@literal cache.xml} {@link Resource}.
	 *
	 * @return an {@link Optional} reference to an Apache Geode native {@literal cache.xml} {@link Resource}.
	 * @see org.springframework.core.io.Resource
	 * @see java.util.Optional
	 * @see #getCacheXml()
	 */
	public Optional<Resource> getOptionalCacheXml() {
		return Optional.ofNullable(getCacheXml());
	}

	/**
	 * Determines whether a {@literal cache.xml} {@link Resource} exists (i.e. is present).
	 *
	 * @return boolean value indicating whether a {@literal cache.xml} exists.
	 * @see org.springframework.core.io.Resource#exists()
	 * @see #getCacheXml()
	 */
	@SuppressWarnings("unused")
	protected boolean isCacheXmlPresent() {
		return getOptionalCacheXml().filter(Resource::exists).isPresent();
	}

	/**
	 * Determines whether the optionally configured {@literal cache.xml} is resolvable as a {@link File}
	 * in the file system.
	 *
	 * @return a boolean value indicating whether the optionally configured {@literal cache.xml}
	 * is resolvable as a {@link File} in the file system.
	 * @see org.springframework.core.io.Resource#isFile()
	 * @see #getCacheXml()
	 * @see java.io.File
	 */
	protected boolean isCacheXmlResolvableAsAFile() {
		return getOptionalCacheXml().filter(Resource::isFile).isPresent();
	}

	/**
	 * Sets and then returns a reference to Apache Geode {@link Properties} used to configure the cache.
	 *
	 * @param properties reference to Apache Geode {@link Properties} used to configure the cache.
	 * @return a reference to Apache Geode {@link Properties} used to configure the cache.
	 * @see #setProperties(Properties)
	 * @see #getProperties()
	 * @see java.util.Properties
	 */
	public @Nullable Properties setAndGetProperties(@Nullable Properties properties) {
		setProperties(properties);
		return getProperties();
	}

	/**
	 * Sets the Apache Geode {@link Properties} used to configure the cache.
	 *
	 * @param properties reference to Apache Geode {@link Properties} used to configure the cache.
	 * @see java.util.Properties
	 */
	public void setProperties(@Nullable Properties properties) {
		this.properties = properties;
	}

	/**
	 * Returns a reference to the Apache Geode {@link Properties} used to configure the cache.
	 *
	 * @return a reference to Apache Geode {@link Properties}.
	 * @see java.util.Properties
	 */
	public @Nullable Properties getProperties() {
		return this.properties;
	}

	/**
	 * Sets a boolean value used to determine whether to enable the {@link GemfireBeanFactoryLocator}.
	 *
	 * @param use boolean value used to determine whether to enable the {@link GemfireBeanFactoryLocator}.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 */
	public void setUseBeanFactoryLocator(boolean use) {
		this.useBeanFactoryLocator = use;
	}

	/**
	 * Determines whether the {@link GemfireBeanFactoryLocator} has been enabled.
	 *
	 * @return a boolean value indicating whether the {@link GemfireBeanFactoryLocator} has been enabled.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 */
	public boolean isUseBeanFactoryLocator() {
		return this.useBeanFactoryLocator;
	}

	/**
	 * Initializes the {@link GemfireBeanFactoryLocator} if {@link #isUseBeanFactoryLocator()} returns {@literal true}
	 * and an existing {@link #getBeanFactoryLocator() GemfireBeanFactoryLocator} is not already present.
	 *
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator#newBeanFactoryLocator(BeanFactory, String)
	 * @see #getOptionalBeanFactoryLocator()
	 * @see #isUseBeanFactoryLocator()
	 * @see #getBeanFactory()
	 * @see #getBeanName()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();

		if (useBeanFactoryLocator()) {
			setBeanFactoryLocator(GemfireBeanFactoryLocator.newBeanFactoryLocator(getBeanFactory(), getBeanName()));
		}
	}

	/**
	 * Determine whether to use the {@link GemfireBeanFactoryLocator}.
	 *
	 * This method really determines whether the {@link GemfireBeanFactoryLocator} is enabled and required to configure
	 * native Apache Geode configuration metadata ({@literal cache.xml}).
	 *
	 * @return a boolean value indicating whether to use the {@link GemfireBeanFactoryLocator}.
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
	 * @see #getOptionalBeanFactoryLocator()
	 * @see #isUseBeanFactoryLocator()
	 */
	private boolean useBeanFactoryLocator() {
		return isUseBeanFactoryLocator() && !getOptionalBeanFactoryLocator().isPresent();
	}

	/**
	 * Destroys and releases resources used by the {@link GemfireBeanFactoryLocator}, if present.
	 *
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator#destroy()
	 * @see #getOptionalBeanFactoryLocator()
	 */
	@Override
	public void destroy() {

		super.destroy();

		getOptionalBeanFactoryLocator().ifPresent(GemfireBeanFactoryLocator::destroy);

		setBeanFactoryLocator(null);
	}

	/**
	 * Loads the configured {@literal cache.xml} to initialize the {@link GemFireCache}.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @param cache {@link GemFireCache} instance to initialize with {@literal cache.xml}; must not be {@literal null}.
	 * @return the given {@link GemFireCache}.
	 * @throws RuntimeException if the configured {@literal cache.xml} file could not be loaded
	 * into the {@link GemFireCache}.
	 * @see org.apache.geode.cache.GemFireCache#loadCacheXml(InputStream)
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #getOptionalCacheXml()
	 */
	protected @NonNull <T extends GemFireCache> T loadCacheXml(@NonNull T cache) {

		// Load the cache.xml file (Resource) and initialize the cache
		getOptionalCacheXml().ifPresent(cacheXml -> {
			try {
				logDebug("Initializing cache with [%s]", cacheXml);
				cache.loadCacheXml(cacheXml.getInputStream());
			}
			catch (IOException cause) {
				throw newRuntimeException(cause, "Failed to load cache.xml [%s]", cacheXml);
			}
		});

		return cache;
	}

	/**
	 * Resolves the Apache Geode {@link Properties} used to configure the {@link GemFireCache}.
	 *
	 * @return the resolved Apache Geode {@link Properties} used to configure the {@link GemFireCache}.
	 * @see #setAndGetProperties(Properties)
	 * @see #getProperties()
	 * @see java.util.Properties
	 */
	protected @NonNull Properties resolveProperties() {

		Properties properties = getProperties();

		return properties != null ? properties : setAndGetProperties(new Properties());
	}
}
