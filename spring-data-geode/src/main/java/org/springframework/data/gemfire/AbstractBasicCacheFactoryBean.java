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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.geode.GemFireCheckedException;
import org.apache.geode.GemFireException;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Phased;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer;
import org.springframework.data.gemfire.support.AbstractFactoryBeanSupport;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link CacheFactoryBean} and {@link ClientCacheFactoryBean} classes
 * used to create Apache Geode peer {@link Cache} and {@link ClientCache} instances, respectively.
 *
 * This class implements Spring's {@link PersistenceExceptionTranslator} interface and is auto-detected by Spring's
 * {@link PersistenceExceptionTranslationPostProcessor} to enable AOP-based translation of native Apache Geode
 * {@link RuntimeException RuntimeExceptions} to Spring's {@link DataAccessException} hierarchy. Therefore,
 * the presence of this class automatically enables a {@link PersistenceExceptionTranslationPostProcessor}
 * to translate Apache Geode {@link RuntimeException RuntimeExceptions} appropriately.
 *
 * Importantly, this class encapsulates configure applicable to tuning Apache Geode in response to JVM Heap memory.
 * Since Apache Geode stores data in-memory, on the JVM Heap, it is important that Aapche Geode be tuned to monitor
 * the JVM Heap and respond accordingly to memory pressure, by evicting data and issuing warnings when the JVM Heap
 * reaches critical mass.
 *
 * @author John Blum
 * @see java.io.File
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.context.Phased
 * @see org.springframework.core.io.Resource
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @since 2.5.0
 */
public abstract class AbstractBasicCacheFactoryBean extends AbstractFactoryBeanSupport<GemFireCache>
		implements DisposableBean, InitializingBean, PersistenceExceptionTranslator, Phased {

	private boolean close = true;
	private boolean useBeanFactoryLocator = false;

	private int phase = -1;

	private Boolean copyOnRead;

	private CacheFactoryInitializer<?> cacheFactoryInitializer;

	private Float criticalHeapPercentage;
	private Float criticalOffHeapPercentage;
	private Float evictionHeapPercentage;
	private Float evictionOffHeapPercentage;

	private GemfireBeanFactoryLocator beanFactoryLocator;

	private GemFireCache cache;

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
	 * Sets a reference to the constructed, configured an initialized {@link GemFireCache} instance created by
	 * this cache {@link FactoryBean}.
	 *
	 * @param cache {@link GemFireCache} created by this {@link FactoryBean}.
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected void setCache(@Nullable GemFireCache cache) {
		this.cache = cache;
	}

	/**
	 * Returns a reference to the constructed, configured an initialized {@link GemFireCache} instance created by
	 * this cache {@link FactoryBean}.
	 *
	 * @return a reference to the {@link GemFireCache} created by this {@link FactoryBean}.
	 * @see org.apache.geode.cache.GemFireCache
	 */
	@SuppressWarnings("unchecked")
	public @Nullable <T extends GemFireCache> T getCache() {
		return (T) this.cache;
	}

	/**
	 * Set the {@link CacheFactoryInitializer} called by this {@link FactoryBean} to initialize the Apache Geode
	 * cache factory used to create the cache constructed by this {@link FactoryBean}.
	 *
	 * @param cacheFactoryInitializer {@link CacheFactoryInitializer} called to initialize the cache factory.
	 * @see org.springframework.data.gemfire.CacheFactoryBean.CacheFactoryInitializer
	 */
	@SuppressWarnings("rawtypes")
	public void setCacheFactoryInitializer(@Nullable CacheFactoryInitializer cacheFactoryInitializer) {
		this.cacheFactoryInitializer = cacheFactoryInitializer;
	}

	/**
	 * Return the {@link CacheFactoryInitializer} called by this {@link FactoryBean} to initialize the Apache Geode
	 * cache factory used to create the cache constructed by this {@link FactoryBean}.
	 *
	 * @return the {@link CacheFactoryInitializer} called to initialize the cache factory.
	 * @see org.springframework.data.gemfire.CacheFactoryBean.CacheFactoryInitializer
	 */
	@SuppressWarnings("rawtypes")
	public @Nullable CacheFactoryInitializer getCacheFactoryInitializer() {
		return this.cacheFactoryInitializer;
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
	 * Determines whether the {@literal cache.xml} {@link File} is present.
	 *
	 * @return boolean value indicating whether a {@literal cache.xml} {@link File} is present.
	 * @see org.springframework.core.io.Resource#isFile()
	 * @see #getCacheXml()
	 */
	@SuppressWarnings("unused")
	protected boolean isCacheXmlAvailable() {

		Resource cacheXml = getCacheXml();

		return cacheXml != null && cacheXml.isFile();
	}

	/**
	 * Returns the Apache Geode native {@literal cache.xml} {@link Resource} as a {@link File}.
	 *
	 * @return the Apache Geode native {@literal cache.xml} {@link Resource} as a {@link File}.
	 * @throws IllegalStateException if the {@link Resource} is not a valid {@link File} in the file system
	 * or a general problem exists accessing or reading the {@link File}.
	 * @see org.springframework.core.io.Resource
	 * @see java.io.File
	 * @see #getCacheXml()
	 */
	@SuppressWarnings("unused")
	protected File getCacheXmlFile() {

		try {
			return getCacheXml().getFile();
		}
		catch (Throwable cause) {
			throw newIllegalStateException(cause, "Resource [%s] is not resolvable as a file", getCacheXml());
		}
	}

	/**
	 * Sets a boolean value used to determine whether the cache should be closed on shutdown of the Spring application.
	 *
	 * @param close boolean value used to determine whether the cache will be closed on shutdown
	 * of the Spring application.
	 */
	public void setClose(boolean close) {
		this.close = close;
	}

	/**
	 * Returns a boolean value used to determine whether the cache will be closed on shutdown of the Spring application.
	 *
	 * @return a boolean value used to determine whether the cache will be closed on shutdown of the Spring application.
	 */
	public boolean isClose() {
		return this.close;
	}

	/**
	 * Sets the {@link GemFireCache#getCopyOnRead()} property of the {@link GemFireCache cache}.
	 *
	 * @param copyOnRead a {@link Boolean value} indicating whether {@link Object objects}
	 * stored in the {@link GemFireCache cache} are copied on read (i.e. {@link Region#get(Object)}.
	 */
	public void setCopyOnRead(@Nullable Boolean copyOnRead) {
		this.copyOnRead = copyOnRead;
	}

	/**
	 * Returns the configuration of the {@link GemFireCache#getCopyOnRead()} property on the {@link GemFireCache cache}.
	 *
	 * @return a {@link Boolean value} indicating whether {@link Object objects}
	 * stored in the {@link GemFireCache cache} are copied on read (i.e. {@link Region#get(Object)}.
	 */
	public @Nullable Boolean getCopyOnRead() {
		return this.copyOnRead;
	}

	/**
	 * Determines whether {@link Object objects} stored in the {@link GemFireCache cache} are copied
	 * when read (i.e. {@link Region#get(Object)}.
	 *
	 * @return a boolean value indicating whether {@link Object objects} stored in the {@link GemFireCache cache}
	 * are copied on read (i.e. {@link Region#get(Object)}.
	 */
	public boolean isCopyOnRead() {
		return Boolean.TRUE.equals(this.copyOnRead);
	}

	/**
	 * Set the Cache's critical heap percentage attribute.
	 *
	 * @param criticalHeapPercentage floating point value indicating the critical heap percentage.
	 */
	public void setCriticalHeapPercentage(@Nullable Float criticalHeapPercentage) {
		this.criticalHeapPercentage = criticalHeapPercentage;
	}

	/**
	 * @return the criticalHeapPercentage
	 */
	public Float getCriticalHeapPercentage() {
		return this.criticalHeapPercentage;
	}

	/**
	 * Set the cache's critical off-heap percentage property.
	 *
	 * @param criticalOffHeapPercentage floating point value indicating the critical off-heap percentage.
	 */
	public void setCriticalOffHeapPercentage(@Nullable Float criticalOffHeapPercentage) {
		this.criticalOffHeapPercentage = criticalOffHeapPercentage;
	}

	/**
	 * @return the criticalOffHeapPercentage
	 */
	public Float getCriticalOffHeapPercentage() {
		return this.criticalOffHeapPercentage;
	}

	/**
	 * Set the Cache's eviction heap percentage attribute.
	 *
	 * @param evictionHeapPercentage float-point value indicating the Cache's heap use percentage to trigger eviction.
	 */
	public void setEvictionHeapPercentage(Float evictionHeapPercentage) {
		this.evictionHeapPercentage = evictionHeapPercentage;
	}

	/**
	 * @return the evictionHeapPercentage
	 */
	public Float getEvictionHeapPercentage() {
		return this.evictionHeapPercentage;
	}

	/**
	 * Set the cache's eviction off-heap percentage property.
	 *
	 * @param evictionOffHeapPercentage float-point value indicating the percentage of off-heap use triggering eviction.
	 */
	public void setEvictionOffHeapPercentage(Float evictionOffHeapPercentage) {
		this.evictionOffHeapPercentage = evictionOffHeapPercentage;
	}

	/**
	 * @return the evictionOffHeapPercentage
	 */
	public Float getEvictionOffHeapPercentage() {
		return this.evictionOffHeapPercentage;
	}

	/**
	 * Returns the cache object reference created by this cache {@link FactoryBean}.
	 *
	 * @return the cache object reference created by this cache {@link FactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #doGetObject()
	 * @see #getCache()
	 */
	@Override
	public GemFireCache getObject() throws Exception {
		return Optional.<GemFireCache>ofNullable(getCache()).orElseGet(this::doGetObject);
	}

	protected abstract GemFireCache doGetObject();

	/**
	 * Returns the {@link Class type} of {@link GemFireCache} produced by this cache {@link FactoryBean}.
	 *
	 * @return the {@link Class type} type of {@link GemFireCache} produced by this cache {@link FactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<? extends GemFireCache> getObjectType() {

		GemFireCache cache = getCache();

		return cache != null ? cache.getClass() : doGetObjectType();
	}

	protected Class<? extends GemFireCache> doGetObjectType() {
		return GemFireCache.class;
	}

	/**
	 * Set the lifecycle phase for this cache bean in the Spring container.
	 *
	 * @param phase {@link Integer#TYPE} value used as the lifecycle phase for this cache bean in the Spring container.
	 * @see org.springframework.context.Phased#getPhase()
	 */
	protected void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Returns the configured lifecycle phase for this cache bean in the Spring container.
	 *
	 * @return an {@link Integer#TYPE} used as the lifecycle phase for this cache bean in the Spring container.
	 * @see org.springframework.context.Phased#getPhase()
	 */
	@Override
	public int getPhase() {
		return this.phase;
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
	public Properties setAndGetProperties(@Nullable Properties properties) {
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
	 * Initializes this cache {@link FactoryBean} after all properties for this cache bean have been set
	 * by the Spring container.
	 *
	 * @throws Exception if initialization fails.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @see #applyCacheConfigurers()
	 * @see #initBeanFactoryLocator()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		applyCacheConfigurers();
		initBeanFactoryLocator();
	}

	/**
	 * Applies any user-defined cache configurers (e.g. {@link ClientCacheConfigurer} or {@link PeerCacheConfigurer})
	 * to this cache {@link FactoryBean} before cache construction, configuration and initialization.
 	 */
	protected abstract void applyCacheConfigurers();

	/**
	 * Null-safe method used to close the {@link GemFireCache} by calling {@link GemFireCache#close()}
	 * iff the cache is not already closed.
	 *
	 * @param cache {@link GemFireCache} to close.
	 * @see org.apache.geode.cache.GemFireCache#isClosed()
	 * @see org.apache.geode.cache.GemFireCache#close()
	 */
	protected void close(@Nullable GemFireCache cache) {

		Optional.ofNullable(cache)
			.filter(it -> !it.isClosed())
			.ifPresent(GemFireCache::close);

		setCache(null);
	}

	/**
	 * Destroys the cache bean on Spring Container shutdown.
	 *
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 * @see #destroyBeanFactoryLocator()
	 * @see #close(GemFireCache)
	 * @see #isClose()
	 */
	@Override
	public void destroy() {

		if (isClose()) {
			close(fetchCache());
			destroyBeanFactoryLocator();
		}
	}

	/**
	 * Destroys the {@link GemfireBeanFactoryLocator}.
	 *
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator#destroy()
	 */
	protected void destroyBeanFactoryLocator() {

		Optional.ofNullable(getBeanFactoryLocator())
			.ifPresent(GemfireBeanFactoryLocator::destroy);

		setBeanFactoryLocator(null);
	}

	private boolean isHeapPercentageValid(@NonNull Float heapPercentage) {
		return heapPercentage >= 0.0f && heapPercentage <= 100.0f;
	}

	protected GemFireCache configureHeapPercentages(GemFireCache cache) {

		Optional.ofNullable(getCriticalHeapPercentage()).ifPresent(criticalHeapPercentage -> {

			Assert.isTrue(isHeapPercentageValid(criticalHeapPercentage),
				() -> String.format("criticalHeapPercentage [%s] is not valid; must be >= 0.0 and <= 100.0",
					criticalHeapPercentage));

			cache.getResourceManager().setCriticalHeapPercentage(criticalHeapPercentage);
		});

		Optional.ofNullable(getEvictionHeapPercentage()).ifPresent(evictionHeapPercentage -> {

			Assert.isTrue(isHeapPercentageValid(evictionHeapPercentage),
				() -> String.format("evictionHeapPercentage [%s] is not valid; must be >= 0.0 and <= 100.0",
					evictionHeapPercentage));

			cache.getResourceManager().setEvictionHeapPercentage(evictionHeapPercentage);
		});

		return cache;
	}

	protected GemFireCache configureOffHeapPercentages(GemFireCache cache) {

		Optional.ofNullable(getCriticalOffHeapPercentage()).ifPresent(criticalOffHeapPercentage -> {

			Assert.isTrue(isHeapPercentageValid(criticalOffHeapPercentage),
				() -> String.format("criticalOffHeapPercentage [%s] is not valid; must be >= 0.0 and <= 100.0",
					criticalOffHeapPercentage));

			cache.getResourceManager().setCriticalOffHeapPercentage(criticalOffHeapPercentage);
		});

		Optional.ofNullable(getEvictionOffHeapPercentage()).ifPresent(evictionOffHeapPercentage -> {

			Assert.isTrue(isHeapPercentageValid(evictionOffHeapPercentage),
				() -> String.format("evictionOffHeapPercentage [%s] is not valid; must be >= 0.0 and <= 100.0",
					evictionOffHeapPercentage));

			cache.getResourceManager().setEvictionOffHeapPercentage(evictionOffHeapPercentage);
		});

		return cache;
	}

	/**
	 * Fetches an existing cache instance from the Apache Geode cache factory.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @return an existing cache instance if available.
	 * @throws org.apache.geode.cache.CacheClosedException if an existing cache instance does not exist.
	 * @see org.apache.geode.cache.client.ClientCacheFactory#getAnyInstance()
	 * @see org.apache.geode.cache.CacheFactory#getAnyInstance()
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #doFetchCache()
	 * @see #getCache()
	 */
	protected <T extends GemFireCache> T fetchCache() {

		T cache = getCache();

		return cache != null ? cache : doFetchCache();
	}

	protected abstract <T extends GemFireCache> T doFetchCache();

	/**
	 * Initializes the {@link GemfireBeanFactoryLocator} if {@link #isUseBeanFactoryLocator()} returns {@literal true}
	 * and an existing {@link #getBeanFactoryLocator()} is not already present.
	 *
	 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator#newBeanFactoryLocator(BeanFactory, String)
	 * @see #isUseBeanFactoryLocator()
	 * @see #getBeanFactoryLocator()
	 * @see #getBeanFactory()
	 * @see #getBeanName()
	 */
	protected void initBeanFactoryLocator() {

		if (isUseBeanFactoryLocator() && getBeanFactoryLocator() == null) {
			setBeanFactoryLocator(GemfireBeanFactoryLocator.newBeanFactoryLocator(getBeanFactory(), getBeanName()));
		}
	}

	/**
	 * Initializes the given {@link CacheFactory} or {@link ClientCacheFactory}
	 * with the configured {@link CacheFactoryInitializer}.
	 *
	 * @param factory {@link CacheFactory} or {@link ClientCacheFactory} to initialize.
	 * @return the initialized {@link CacheFactory} or {@link ClientCacheFactory}.
	 * @see org.springframework.data.gemfire.CacheFactoryBean.CacheFactoryInitializer#initialize(Object)
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.CacheFactory
	 * @see #getCacheFactoryInitializer()
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	protected Object initializeFactory(Object factory) {

		return Optional.ofNullable(getCacheFactoryInitializer())
			.map(cacheFactoryInitializer -> cacheFactoryInitializer.initialize(factory))
			.orElse(factory);
	}

	/**
	 * Loads the configured {@literal cache.xml} to initialize the cache.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @param cache cache instance to initialized with {@literal cache.xml}.
	 * @return the given cache instance.
	 * @throws RuntimeException if the configured {@literal cache.xml} file could not be loaded.
	 * @see org.apache.geode.cache.GemFireCache#loadCacheXml(InputStream)
	 */
	protected <T extends GemFireCache> T loadCacheXml(T cache) {

		// Load the cache.xml file (Resource) and initialize the cache
		Optional.ofNullable(getCacheXml()).ifPresent(cacheXml -> {
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
	 * Resolves the Apache Geode {@link Properties} used to configure the {@link Cache}.
	 *
	 * @return the resolved Apache Geode {@link Properties} used to configure the {@link Cache}.
	 * @see #setAndGetProperties(Properties)
	 * @see #getProperties()
	 */
	protected Properties resolveProperties() {

		return Optional.ofNullable(getProperties())
			.orElseGet(() -> setAndGetProperties(new Properties()));
	}

	/**
	 * Translates the thrown Apache Geode {@link RuntimeException} to a corresponding {@link Exception} from Spring's
	 * generic {@link DataAccessException} hierarchy if possible.
	 *
	 * @param exception the Apache Geode {@link RuntimeException} to translate.
	 * @return the translated Spring {@link DataAccessException} or {@literal null}
	 * if the Apache Geode {@link RuntimeException} could not be translated.
	 * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(RuntimeException)
	 * @see org.springframework.dao.DataAccessException
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(@Nullable RuntimeException exception) {

		if (exception instanceof IllegalArgumentException) {

			DataAccessException wrapped = GemfireCacheUtils.convertQueryExceptions(exception);

			// ignore conversion if generic exception is returned
			if (!(wrapped instanceof GemfireSystemException)) {
				return wrapped;
			}
		}

		if (exception instanceof GemFireException) {
			return GemfireCacheUtils.convertGemfireAccessException((GemFireException) exception);
		}

		if (exception.getCause() instanceof GemFireException) {
			return GemfireCacheUtils.convertGemfireAccessException((GemFireException) exception.getCause());
		}

		if (exception.getCause() instanceof GemFireCheckedException) {
			return GemfireCacheUtils.convertGemfireAccessException((GemFireCheckedException) exception.getCause());
		}

		return null;
	}

	/**
	 * Callback interface for initializing either a {@link CacheFactory} or a {@link ClientCacheFactory} instance,
	 * which is used to create an instance of {@link GemFireCache}.
	 *
	 * @see org.apache.geode.cache.CacheFactory
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 */
	@FunctionalInterface
	public interface CacheFactoryInitializer<T> {

		/**
		 * Initialize the given cache factory.
		 *
		 * @param cacheFactory cache factory to initialize.
		 * @return the given cache factory.
		 * @see org.apache.geode.cache.CacheFactory
		 * @see org.apache.geode.cache.client.ClientCacheFactory
		 */
		T initialize(T cacheFactory);

	}
}
