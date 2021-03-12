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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.geode.GemFireCheckedException;
import org.apache.geode.GemFireException;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.TransactionListener;
import org.apache.geode.cache.TransactionWriter;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Phased;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer;
import org.springframework.data.gemfire.support.AbstractFactoryBeanSupport;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link CacheFactoryBean} and {@link ClientCacheFactoryBean} classes,
 * used to create Apache Geode peer {@link Cache} and {@link ClientCache} instances, respectively.
 *
 * This class implements Spring's {@link PersistenceExceptionTranslator} interface and is auto-detected by Spring's
 * {@link PersistenceExceptionTranslationPostProcessor} to enable AOP-based translation of native Apache Geode
 * {@link RuntimeException RuntimeExceptions} to Spring's {@link DataAccessException} hierarchy. Therefore,
 * the presence of this class automatically enables Spring's {@link PersistenceExceptionTranslationPostProcessor}
 * to translate Apache Geode thrown {@link GemFireException} and {@link GemFireCheckedException} types
 * as Spring {@link DataAccessException DataAccessExceptions}.
 *
 * More importantly, this abstract class encapsulates configuration applicable to tuning Apache Geode in order to
 * efficiently use JVM Heap memory. Since Apache Geode stores data in-memory, on the JVM Heap, it is important that
 * Apache Geode be tuned to monitor the JVM Heap and respond to memory pressure accordingly, by evicting data
 * and issuing warnings when the JVM Heap reaches critical mass.
 *
 * This abstract class is also concerned with the configuration of PDX and transaction event handling along with
 * whether the contents (entries) of the cache should be made effectively immutable on reads (i.e. get(key)).
 *
 * @author John Blum
 * @see org.apache.geode.GemFireCheckedException
 * @see org.apache.geode.GemFireException
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.TransactionListener
 * @see org.apache.geode.cache.TransactionWriter
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.context.Phased
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 * @since 2.5.0
 */
public abstract class AbstractBasicCacheFactoryBean extends AbstractFactoryBeanSupport<GemFireCache>
		implements DisposableBean, InitializingBean, PersistenceExceptionTranslator, Phased {

	private boolean close = true;

	private int phase = -1;

	private Boolean copyOnRead;
	private Boolean pdxIgnoreUnreadFields;
	private Boolean pdxPersistent;
	private Boolean pdxReadSerialized;

	private CacheFactoryInitializer<?> cacheFactoryInitializer;

	private Float criticalHeapPercentage;
	private Float criticalOffHeapPercentage;
	private Float evictionHeapPercentage;
	private Float evictionOffHeapPercentage;

	private GemFireCache cache;

	private List<TransactionListener> transactionListeners;

	private PdxSerializer pdxSerializer;

	private String pdxDiskStoreName;

	private TransactionWriter transactionWriter;

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
	 * Returns an {@link Optional} reference to the constructed, configured and initialized {@link GemFireCache}
	 * instance created by this cache {@link FactoryBean}.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @return an {@link Optional} reference to the {@link GemFireCache} created by this {@link FactoryBean}.
	 * @see org.apache.geode.cache.GemFireCache
	 * @see java.util.Optional
	 * @see #getCache()
	 */
	public <T extends GemFireCache> Optional<T> getOptionalCache() {
		return Optional.ofNullable(getCache());
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
	 * Returns the {@link Class type} of {@link GemFireCache} created by this cache {@link FactoryBean}.
	 *
	 * @return the {@link Class type} type of {@link GemFireCache} created by this cache {@link FactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<? extends GemFireCache> getObjectType() {

		GemFireCache cache = getCache();

		return cache != null ? cache.getClass() : doGetObjectType();
	}

	/**
	 * By default, returns {@link GemFireCache} {@link Class}.
	 *
	 * @return {@link GemFireCache} {@link Class} by default.
	 * @see org.apache.geode.cache.GemFireCache#getClass()
	 * @see java.lang.Class
	 */
	protected Class<? extends GemFireCache> doGetObjectType() {
		return GemFireCache.class;
	}

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
	 * Configures the cache (transaction manager) with a {@link List} of {@link TransactionListener TransactionListeners}
	 * implemented by applications to listen for and receive transaction events after a transaction is processed
	 * (i.e. committed or rolled back).
	 *
	 * @param transactionListeners {@link List} of application-defined {@link TransactionListener TransactionListeners}
	 * registered with the cache to listen for and receive transaction events.
	 * @see org.apache.geode.cache.TransactionListener
	 */
	public void setTransactionListeners(List<TransactionListener> transactionListeners) {
		this.transactionListeners = transactionListeners;
	}

	/**
	 * Returns the {@link List} of configured, application-defined {@link TransactionListener TransactionListeners}
	 * registered with the cache (transaction manager) to enable applications to receive transaction events after a
	 * transaction is processed (i.e. committed or rolled back).
	 *
	 * @return a {@link List} of application-defined {@link TransactionListener TransactionListeners} registered with
	 * the cache (transaction manager) to listen for and receive transaction events.
	 * @see org.apache.geode.cache.TransactionListener
	 */
	public List<TransactionListener> getTransactionListeners() {
		return CollectionUtils.nullSafeList(this.transactionListeners);
	}

	/**
	 * Configures a {@link TransactionWriter} implemented by the application to receive transaction events and perform
	 * a action, like a veto.
	 *
	 * @param transactionWriter {@link TransactionWriter} receiving transaction events.
	 * @see org.apache.geode.cache.TransactionWriter
	 */
	public void setTransactionWriter(@Nullable TransactionWriter transactionWriter) {
		this.transactionWriter = transactionWriter;
	}

	/**
	 * Return the configured {@link TransactionWriter} used to process and handle transaction events.
	 *
	 * @return the configured {@link TransactionWriter}.
	 * @see org.apache.geode.cache.TransactionWriter
	 */
	public @Nullable TransactionWriter getTransactionWriter() {
		return this.transactionWriter;
	}

	/**
	 * Initializes this cache {@link FactoryBean} after all properties for this cache bean have been set
	 * by the Spring container.
	 *
	 * @throws Exception if initialization fails.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @see #applyCacheConfigurers()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		applyCacheConfigurers();
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
	 * @see #close(GemFireCache)
	 * @see #fetchCache()
	 * @see #isClose()
	 */
	@Override
	public void destroy() {

		if (isClose()) {
			close(fetchCache());
		}
	}

	private boolean isHeapPercentageValid(@NonNull Float heapPercentage) {
		return heapPercentage >= 0.0f && heapPercentage <= 100.0f;
	}

	/**
	 * Configures the {@link GemFireCache} critical and eviction heap thresholds as percentages.
	 *
	 * @param cache {@link GemFireCache} to configure the critical and eviction heap thresholds;
	 * must not be {@literal null}.
	 * @return the given {@link GemFireCache}.
	 * @throws IllegalArgumentException if the critical or eviction heap thresholds are not valid percentages.
	 * @see org.apache.geode.cache.control.ResourceManager#setCriticalHeapPercentage(float)
	 * @see org.apache.geode.cache.control.ResourceManager#setEvictionHeapPercentage(float)
	 * @see org.apache.geode.cache.control.ResourceManager
	 * @see org.apache.geode.cache.GemFireCache#getResourceManager()
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected @NonNull GemFireCache configureHeapPercentages(@NonNull GemFireCache cache) {

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

	/**
	 * Configures the {@link GemFireCache} critical and eviction off-heap thresholds as percentages.
	 *
	 * @param cache {@link GemFireCache} to configure the critical and eviction off-heap thresholds;
	 * must not be {@literal null}.
	 * @return the given {@link GemFireCache}.
	 * @throws IllegalArgumentException if the critical or eviction off-heap thresholds are not valid percentages.
	 * @see org.apache.geode.cache.control.ResourceManager#setCriticalOffHeapPercentage(float)
	 * @see org.apache.geode.cache.control.ResourceManager#setEvictionOffHeapPercentage(float)
	 * @see org.apache.geode.cache.control.ResourceManager
	 * @see org.apache.geode.cache.GemFireCache#getResourceManager()
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected @NonNull GemFireCache configureOffHeapPercentages(@NonNull GemFireCache cache) {

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
	 * Registers configured, application-defined {@link TransactionListener TransactionListeners} with the cache
	 * (transaction manager) to listen for and receive transaction events when a (cache) transaction is processed
	 * (e.g. committed or rolled back).
	 *
	 * @param cache {@link GemFireCache} used to register the configured, application-defined
	 * {@link TransactionListener TransactionListeners}; must not be {@literal null}.
	 * @return the given {@link GemFireCache}.
	 * @see org.apache.geode.cache.GemFireCache#getCacheTransactionManager()
	 * @see org.apache.geode.cache.CacheTransactionManager#addListener(TransactionListener)
	 * @see org.apache.geode.cache.CacheTransactionManager
	 * @see org.apache.geode.cache.TransactionListener
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected @NonNull GemFireCache registerTransactionListeners(@NonNull GemFireCache cache) {

		CollectionUtils.nullSafeCollection(getTransactionListeners()).stream()
			.filter(Objects::nonNull)
			.forEach(transactionListener -> cache.getCacheTransactionManager().addListener(transactionListener));

		return cache;
	}

	/**
	 * Registers the configured, application-defined {@link TransactionWriter} with the cache (transaction manager)
	 * to receive transaction events with the intent to alter the transaction outcome (e.g. veto).
	 *
	 * @param cache {@link GemFireCache} used to register the configured, application-defined {@link TransactionWriter},
	 * must not be {@literal null}.
	 * @return the given {@link GemFireCache}.
	 * @see org.apache.geode.cache.GemFireCache#getCacheTransactionManager()
	 * @see org.apache.geode.cache.CacheTransactionManager#setWriter(TransactionWriter)
	 * @see org.apache.geode.cache.CacheTransactionManager
	 * @see org.apache.geode.cache.TransactionWriter
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected @NonNull GemFireCache registerTransactionWriter(@NonNull GemFireCache cache) {

		Optional.ofNullable(getTransactionWriter())
			.ifPresent(transactionWriter -> cache.getCacheTransactionManager().setWriter(transactionWriter));

		return cache;
	}

	/**
	 * Translates the thrown Apache Geode {@link RuntimeException} into a corresponding {@link Exception} from Spring's
	 * generic {@link DataAccessException} hierarchy if possible.
	 *
	 * @param exception the Apache Geode {@link RuntimeException} to translate.
	 * @return the translated Spring {@link DataAccessException} or {@literal null}
	 * if the Apache Geode {@link RuntimeException} could not be translated.
	 * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(RuntimeException)
	 * @see org.springframework.dao.DataAccessException
	 */
	@Override
	public @Nullable DataAccessException translateExceptionIfPossible(@Nullable RuntimeException exception) {

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
	 * Callback interface for initializing a {@link CacheFactory} or a {@link ClientCacheFactory} instance,
	 * which is used to create an instance of {@link GemFireCache}.
	 *
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.CacheFactory
	 * @see java.util.function.Function
	 */
	@FunctionalInterface
	public interface CacheFactoryInitializer<T> extends Function<T, T> {

		/**
		 * Alias for {@link #initialize(Object)}.
		 *
		 * @param t cache factory to initialize.
		 * @return the initialized cache factory.
		 * @see #initialize(Object)
		 */
		@Override
		default T apply(T t) {
			return initialize(t);
		}

		/**
		 * Initialize the given cache factory.
		 *
		 * @param cacheFactory cache factory to initialize.
		 * @return the given cache factory.
		 * @see org.apache.geode.cache.client.ClientCacheFactory
		 * @see org.apache.geode.cache.CacheFactory
		 */
		T initialize(T cacheFactory);

	}

	/**
	 * Callback interface to configure PDX.
	 *
	 * @param <T> parameterized {@link Class} type capable of configuring Apache Geode PDX functionality.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.CacheFactory
	 * @see org.apache.geode.cache.GemFireCache
	 */
	public interface PdxConfigurer<T> {

		T getTarget();

		PdxConfigurer<T> setDiskStoreName(String diskStoreName);

		PdxConfigurer<T> setIgnoreUnreadFields(Boolean ignoreUnreadFields);

		PdxConfigurer<T> setPersistent(Boolean persistent);

		PdxConfigurer<T> setReadSerialized(Boolean readSerialized);

		PdxConfigurer<T> setSerializer(PdxSerializer pdxSerializer);

	}
}
