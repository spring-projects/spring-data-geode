/*
 * Copyright 2010-2021 the original author or authors.
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

import static org.springframework.data.gemfire.GemfireUtils.apacheGeodeProductName;
import static org.springframework.data.gemfire.GemfireUtils.apacheGeodeVersion;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeCollection;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeList;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.TransactionListener;
import org.apache.geode.cache.TransactionWriter;
import org.apache.geode.cache.util.GatewayConflictResolver;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.datasource.ConfigProperty;
import org.apache.geode.internal.jndi.JNDIInvoker;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.security.SecurityManager;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean} used to construct, configure and initialize a {@literal peer} {@link Cache) instance.
 *
 * Allows either the retrieval of an existing, open {@link Cache} or the creation of a new {@link Cache}.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @author Patrick Johnson
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.apache.geode.security.SecurityManager
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
 */
@SuppressWarnings("unused")
public class CacheFactoryBean extends AbstractPdxConfigurableCacheFactoryBean {

	private Boolean enableAutoReconnect;
	private Boolean useClusterConfiguration;

	private GatewayConflictResolver gatewayConflictResolver;

	private Integer lockLease;
	private Integer lockTimeout;
	private Integer messageSyncInterval;
	private Integer searchTimeout;

	private final List<PeerCacheConfigurer> peerCacheConfigurers = new ArrayList<>();

	private List<JndiDataSource> jndiDataSources;

	private List<TransactionListener> transactionListeners;

	private final PeerCacheConfigurer compositePeerCacheConfigurer = (beanName, bean) ->
		nullSafeList(peerCacheConfigurers).forEach(peerCacheConfigurer ->
			peerCacheConfigurer.configure(beanName, bean));

	private String cacheResolutionMessagePrefix;

	private org.apache.geode.security.SecurityManager securityManager;

	private TransactionWriter transactionWriter;

	/**
	 * Applies the composite {@link PeerCacheConfigurer PeerCacheConfigurers} to this {@link CacheFactoryBean}
	 * before the {@link Cache peer cache} is created.
	 *
	 * @see #getCompositePeerCacheConfigurer()
	 * @see #applyPeerCacheConfigurers(PeerCacheConfigurer...)
	 */
	protected void applyCacheConfigurers() {

		PeerCacheConfigurer autoReconnectClusterConfigurationConfigurer = (beanName, cacheFactoryBean) -> {

			Properties gemfireProperties = resolveProperties();

			gemfireProperties.setProperty(GemFireProperties.DISABLE_AUTO_RECONNECT.getName(),
				String.valueOf(!Boolean.TRUE.equals(getEnableAutoReconnect())));

			gemfireProperties.setProperty(GemFireProperties.USE_CLUSTER_CONFIGURATION.getName(),
				String.valueOf(Boolean.TRUE.equals(getUseClusterConfiguration())));

		};

		this.peerCacheConfigurers.add(autoReconnectClusterConfigurationConfigurer);

		applyPeerCacheConfigurers(getCompositePeerCacheConfigurer());
	}

	/**
	 * Applies the array of {@link PeerCacheConfigurer PeerCacheConfigurers} to this {@link CacheFactoryBean}
	 * before the {@link Cache peer cache} is created.
	 *
	 * @param peerCacheConfigurers array of {@link PeerCacheConfigurer PeerCacheConfigurers}
	 * applied to this {@link CacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 * @see #applyPeerCacheConfigurers(Iterable)
	 */
	protected void applyPeerCacheConfigurers(PeerCacheConfigurer... peerCacheConfigurers) {
		applyPeerCacheConfigurers(Arrays.asList(ArrayUtils.nullSafeArray(peerCacheConfigurers, PeerCacheConfigurer.class)));
	}

	/**
	 * Applies the {@link Iterable} of {@link PeerCacheConfigurer PeerCacheConfigurers} to this {@link CacheFactoryBean}
	 * before the {@link Cache peer cache} is created.
	 *
	 * @param peerCacheConfigurers {@link Iterable} of {@link PeerCacheConfigurer PeerCacheConfigurers}
	 * applied to this {@link CacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 * @see java.lang.Iterable
	 */
	protected void applyPeerCacheConfigurers(Iterable<PeerCacheConfigurer> peerCacheConfigurers) {
		StreamSupport.stream(CollectionUtils.nullSafeIterable(peerCacheConfigurers).spliterator(), false)
			.forEach(peerCacheConfigurer -> peerCacheConfigurer.configure(getBeanName(), this));
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected GemFireCache doGetObject() {
		return init();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected Class<? extends GemFireCache> doGetObjectType() {
		return Cache.class;
	}

	/**
	 * Initializes the {@link Cache}.
	 *
	 * @return a reference to the initialized {@link Cache}.
	 * @see org.apache.geode.cache.Cache
	 * @see #resolveCache()
	 * @see #postProcess(GemFireCache)
	 * @see #setCache(GemFireCache)
	 */
	@SuppressWarnings("deprecation")
	GemFireCache init() {

		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			// Use Spring Bean ClassLoader to load Spring configured Apache Geode classes
			Thread.currentThread().setContextClassLoader(getBeanClassLoader());

			setCache(postProcess(resolveCache()));

			Optional.<GemFireCache>ofNullable(getCache()).ifPresent(cache -> {

				Optional.ofNullable(cache.getDistributedSystem())
					.map(DistributedSystem::getDistributedMember)
					.ifPresent(member ->
						logInfo(() -> String.format("Connected to Distributed System [%1$s] as Member [%2$s]"
								.concat(" in Group(s) [%3$s] with Role(s) [%4$s] on Host [%5$s] having PID [%6$d]"),
							cache.getDistributedSystem().getName(), member.getId(), member.getGroups(),
							member.getRoles(), member.getHost(), member.getProcessId())));

				logInfo(() -> String.format("%1$s %2$s version [%3$s] Cache [%4$s]", this.cacheResolutionMessagePrefix,
					apacheGeodeProductName(), apacheGeodeVersion(), cache.getName()));

			});

			return getCache();
		}
		catch (Exception cause) {
			throw newRuntimeException(cause, "An error occurred while initializing the cache");
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}
	}

	/**
	 * Resolves the {@link Cache} by first attempting to lookup an existing {@link Cache} instance in the JVM.
	 * If an existing {@link Cache} could not be found, then this method proceeds in attempting to create
	 * a new {@link Cache} instance.
	 *
	 * @param <T> parameterized {@link Class} type extension of {@link GemFireCache}.
	 * @return the resolved {@link Cache} instance.
	 * @see org.apache.geode.cache.Cache
	 * @see #fetchCache()
	 * @see #resolveProperties()
	 * @see #createFactory(java.util.Properties)
	 * @see #configureFactory(Object)
	 * @see #createCache(Object)
	 */
	protected <T extends GemFireCache> T resolveCache() {

		try {

			this.cacheResolutionMessagePrefix = "Found existing";

			return fetchCache();
		}
		catch (CacheClosedException cause) {

			this.cacheResolutionMessagePrefix = "Created new";

			return createCache(postProcess(configureFactory(initializeFactory(createFactory(resolveProperties())))));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends GemFireCache> T doFetchCache() {
		return (T) CacheFactory.getAnyInstance();
	}

	/**
	 * Constructs a new instance of {@link CacheFactory} initialized with the given Apache Geode {@link Properties}
	 * used to construct, configure and initialize a new peer {@link Cache} instance.
	 *
	 * @param gemfireProperties {@link Properties} used by the {@link CacheFactory} to configure the peer {@link Cache}.
	 * @return a new instance of {@link CacheFactory} initialized with the given Apache Geode {@link Properties}.
	 * @see org.apache.geode.cache.CacheFactory
	 * @see java.util.Properties
	 */
	protected @NonNull Object createFactory(@NonNull Properties gemfireProperties) {
		return new CacheFactory(gemfireProperties);
	}

	/**
	 * Configures the {@link CacheFactory} used to create the {@link Cache}.
	 *
	 * @param factory {@link CacheFactory} used to create the {@link Cache}.
	 * @return the configured {@link CacheFactory}.
	 * @see #configurePdx(org.springframework.data.gemfire.AbstractPdxConfigurableCacheFactoryBean.PdxConfigurer)
	 * @see #configureSecurity(CacheFactory)
	 * @see org.apache.geode.cache.CacheFactory
	 */
	protected @NonNull Object configureFactory(@NonNull Object factory) {
		return configureSecurity(configurePdx((CacheFactory) factory));
	}

	/**
	 * Configures the {@link Cache} to use PDX serialization.
	 *
	 * @param cacheFactory {@link CacheFactory} to configure with PDX.
	 * @return the given {@link CacheFactory}.
	 * @see org.springframework.data.gemfire.CacheFactoryBean.CacheFactoryToPdxConfigurerAdapter
	 * @see org.apache.geode.cache.CacheFactory
	 * @see #configurePdx(PdxConfigurer)
	 */
	protected @NonNull CacheFactory configurePdx(@NonNull CacheFactory cacheFactory) {
		return configurePdx(CacheFactoryToPdxConfigurerAdapter.from(cacheFactory));
	}

	/**
	 * Configures the {@link Cache} with security.
	 *
	 * @param cacheFactory {@link CacheFactory} used to configure the peer {@link Cache} instance with security.
	 * @return the given {@link CacheFactory}.
	 * @see org.apache.geode.cache.CacheFactory
	 */
	private @NonNull CacheFactory configureSecurity(@NonNull CacheFactory cacheFactory) {

		org.apache.geode.security.SecurityManager securityManager = getSecurityManager();

		return securityManager != null
			? cacheFactory.setSecurityManager(securityManager)
			: cacheFactory;
	}

	/**
	 * Post process the {@link CacheFactory} used to create the {@link Cache}.
	 *
	 * @param factory {@link CacheFactory} used to create the {@link Cache}.
	 * @return the post processed {@link CacheFactory}.
	 * @see org.apache.geode.cache.CacheFactory
	 */
	protected @NonNull Object postProcess(@NonNull Object factory) {
		return factory;
	}

	/**
	 * Creates a new {@link Cache} instance using the provided {@link Object factory}.
	 *
	 * @param <T> {@link Class sub-type} of {@link GemFireCache}.
	 * @param factory instance of {@link CacheFactory}.
	 * @return a new instance of {@link Cache} created by the provided {@link Object factory}.
	 * @see org.apache.geode.cache.CacheFactory#create()
	 * @see org.apache.geode.cache.GemFireCache
	 */
	@SuppressWarnings("unchecked")
	protected @NonNull <T extends GemFireCache> T createCache(@NonNull Object factory) {
		return (T) ((CacheFactory) factory).create();
	}

	/**
	 * Post process the {@link GemFireCache} by loading any {@literal cache.xml} file, applying custom settings
	 * specified in SDG XML configuration meta-data, and registering appropriate Transaction Listeners, Writer
	 * and JNDI settings along with JVM Heap configuration.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @param cache {@link GemFireCache} to post process.
	 * @return the given {@link GemFireCache}.
	 * @see #loadCacheXml(GemFireCache)
	 * @see org.apache.geode.cache.Cache#loadCacheXml(java.io.InputStream)
	 * @see #configureHeapPercentages(org.apache.geode.cache.GemFireCache)
	 * @see #configureOffHeapPercentages(GemFireCache)
	 * @see #registerJndiDataSources(GemFireCache)
	 * @see #registerTransactionListeners(org.apache.geode.cache.GemFireCache)
	 * @see #registerTransactionWriter(org.apache.geode.cache.GemFireCache)
	 */
	protected @NonNull <T extends GemFireCache> T postProcess(@NonNull T cache) {

		loadCacheXml(cache);

		Optional.ofNullable(getCopyOnRead()).ifPresent(cache::setCopyOnRead);

		if (cache instanceof Cache) {
			Optional.ofNullable(getGatewayConflictResolver()).ifPresent(((Cache) cache)::setGatewayConflictResolver);
			Optional.ofNullable(getLockLease()).ifPresent(((Cache) cache)::setLockLease);
			Optional.ofNullable(getLockTimeout()).ifPresent(((Cache) cache)::setLockTimeout);
			Optional.ofNullable(getMessageSyncInterval()).ifPresent(((Cache) cache)::setMessageSyncInterval);
			Optional.ofNullable(getSearchTimeout()).ifPresent(((Cache) cache)::setSearchTimeout);
		}

		configureHeapPercentages(cache);
		configureOffHeapPercentages(cache);
		registerJndiDataSources(cache);
		registerTransactionListeners(cache);
		registerTransactionWriter(cache);

		return cache;
	}

	private GemFireCache registerJndiDataSources(GemFireCache cache) {

		nullSafeCollection(getJndiDataSources()).forEach(jndiDataSource -> {

			String type = jndiDataSource.getAttributes().get("type");

			JndiDataSourceType jndiDataSourceType = JndiDataSourceType.valueOfIgnoreCase(type);

			Assert.notNull(jndiDataSourceType,
				String.format("'jndi-binding' 'type' [%1$s] is invalid; 'type' must be one of %2$s",
					type, Arrays.toString(JndiDataSourceType.values())));

			jndiDataSource.getAttributes().put("type", jndiDataSourceType.getName());

			SpringUtils.safeRunOperation(() ->
				JNDIInvoker.mapDatasource(jndiDataSource.getAttributes(), jndiDataSource.getProps()));
		});

		return cache;
	}

	private GemFireCache registerTransactionListeners(GemFireCache cache) {

		nullSafeCollection(getTransactionListeners())
			.forEach(transactionListener -> cache.getCacheTransactionManager().addListener(transactionListener));

		return cache;
	}

	private GemFireCache registerTransactionWriter(GemFireCache cache) {

		Optional.ofNullable(getTransactionWriter()).ifPresent(it -> cache.getCacheTransactionManager().setWriter(it));

		return cache;
	}

	/**
	 * Returns a reference to the Composite {@link PeerCacheConfigurer} used to apply additional configuration
	 * to this {@link CacheFactoryBean} on Spring container initialization.
	 *
	 * @return the Composite {@link PeerCacheConfigurer}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 */
	public PeerCacheConfigurer getCompositePeerCacheConfigurer() {
		return this.compositePeerCacheConfigurer;
	}

	/**
	 * Controls whether auto-reconnect functionality introduced in GemFire 8 is enabled or not.
	 *
	 * @param enableAutoReconnect a boolean value to enable/disable auto-reconnect functionality.
	 * @since GemFire 8.0
	 */
	public void setEnableAutoReconnect(Boolean enableAutoReconnect) {
		this.enableAutoReconnect = enableAutoReconnect;
	}

	/**
	 * Gets the value for the auto-reconnect setting.
	 *
	 * @return a boolean value indicating whether auto-reconnect was specified (non-null) and whether it was enabled
	 * or not.
	 */
	public Boolean getEnableAutoReconnect() {
		return this.enableAutoReconnect;
	}

	/**
	 * Requires GemFire 7.0 or higher
	 * @param gatewayConflictResolver defined as Object in the signature for backward
	 * compatibility with Gemfire 6 compatibility. This must be an instance of
	 * {@link org.apache.geode.cache.util.GatewayConflictResolver}
	 */
	public void setGatewayConflictResolver(GatewayConflictResolver gatewayConflictResolver) {
		this.gatewayConflictResolver = gatewayConflictResolver;
	}

	/**
	 * @return the gatewayConflictResolver
	 */
	public GatewayConflictResolver getGatewayConflictResolver() {
		return this.gatewayConflictResolver;
	}

	/**
	 * @param jndiDataSources the list of configured JndiDataSources to use with this Cache.
	 */
	public void setJndiDataSources(List<JndiDataSource> jndiDataSources) {
		this.jndiDataSources = jndiDataSources;
	}

	/**
	 * @return the list of configured JndiDataSources.
	 */
	public List<JndiDataSource> getJndiDataSources() {
		return this.jndiDataSources;
	}

	/**
	 * Sets the number of seconds for implicit and explicit object lock leases to timeout.
	 *
	 * @param lockLease an integer value indicating the object lock lease timeout.
	 */
	public void setLockLease(Integer lockLease) {
		this.lockLease = lockLease;
	}

	/**
	 * @return the lockLease
	 */
	public Integer getLockLease() {
		return this.lockLease;
	}

	/**
	 * Sets the number of seconds in which the implicit object lock request will timeout.
	 *
	 * @param lockTimeout an integer value specifying the object lock request timeout.
	 */
	public void setLockTimeout(Integer lockTimeout) {
		this.lockTimeout = lockTimeout;
	}

	/**
	 * @return the lockTimeout
	 */
	public Integer getLockTimeout() {
		return this.lockTimeout;
	}

	/**
	 * Set for client subscription queue synchronization when this member acts as a server to clients
	 * and server redundancy is used. Sets the frequency (in seconds) at which the primary server sends messages
	 * to its secondary servers to remove queued events that have already been processed by the clients.
	 *
	 * @param messageSyncInterval an integer value specifying the number of seconds in which the primary server
	 * sends messages to secondary servers.
	 */
	public void setMessageSyncInterval(Integer messageSyncInterval) {
		this.messageSyncInterval = messageSyncInterval;
	}

	/**
	 * @return the messageSyncInterval
	 */
	public Integer getMessageSyncInterval() {
		return this.messageSyncInterval;
	}

	/**
	 * Null-safe operation to set an array of {@link PeerCacheConfigurer PeerCacheConfigurers} used to apply
	 * additional configuration to this {@link CacheFactoryBean} when using Annotation-based configuration.
	 *
	 * @param peerCacheConfigurers array of {@link PeerCacheConfigurer PeerCacheConfigurers} used to apply
	 * additional configuration to this {@link CacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 * @see #setPeerCacheConfigurers(List)
	 */
	public void setPeerCacheConfigurers(PeerCacheConfigurer... peerCacheConfigurers) {
		setPeerCacheConfigurers(Arrays.asList(nullSafeArray(peerCacheConfigurers, PeerCacheConfigurer.class)));
	}

	/**
	 * Null-safe operation to set an {@link Iterable} of {@link PeerCacheConfigurer PeerCacheConfigurers} to apply
	 * additional configuration to this {@link CacheFactoryBean} when using Annotation-based configuration.
	 *
	 * @param peerCacheConfigurers {@link Iterable} of {@link PeerCacheConfigurer PeerCacheConfigurers} used to apply
	 * additional configuration to this {@link CacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 */
	public void setPeerCacheConfigurers(List<PeerCacheConfigurer> peerCacheConfigurers) {
		Optional.ofNullable(peerCacheConfigurers).ifPresent(this.peerCacheConfigurers::addAll);
	}

	/**
	 * Set the number of seconds a netSearch operation can wait for data before timing out.
	 *
	 * @param searchTimeout an integer value indicating the netSearch timeout value.
	 */
	public void setSearchTimeout(Integer searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	/**
	 * @return the searchTimeout
	 */
	public Integer getSearchTimeout() {
		return this.searchTimeout;
	}

	/**
	 * Configures the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 *
	 * @param securityManager {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 * @see org.apache.geode.security.SecurityManager
	 */
	public void setSecurityManager(SecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	/**
	 * Returns the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 *
	 * @return the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 * @see org.apache.geode.security.SecurityManager
	 */
	public SecurityManager getSecurityManager() {
		return this.securityManager;
	}

	/**
	 * Sets the list of TransactionListeners used to configure the Cache to receive transaction events after
	 * the transaction is processed (committed, rolled back).
	 *
	 * @param transactionListeners the list of GemFire TransactionListeners listening for transaction events.
	 * @see org.apache.geode.cache.TransactionListener
	 */
	public void setTransactionListeners(List<TransactionListener> transactionListeners) {
		this.transactionListeners = transactionListeners;
	}

	/**
	 * @return the transactionListeners
	 */
	public List<TransactionListener> getTransactionListeners() {
		return this.transactionListeners;
	}

	/**
	 * Sets the {@link TransactionWriter} used to configure the cache for handling transaction events, such as to veto
	 * the transaction or update an external DB before the commit.
	 *
	 * @param transactionWriter configured {@link TransactionWriter} callback receiving transaction events.
	 * @see org.apache.geode.cache.TransactionWriter
	 */
	public void setTransactionWriter(TransactionWriter transactionWriter) {
		this.transactionWriter = transactionWriter;
	}

	/**
	 * Return the configured {@link TransactionWriter} used to process and handle transaction events.
	 *
	 * @return the configured {@link TransactionWriter}.
	 * @see org.apache.geode.cache.TransactionWriter
	 */
	public TransactionWriter getTransactionWriter() {
		return this.transactionWriter;
	}

	/**
	 * Sets the state of the {@literal use-shared-configuration} Pivotal GemFire/Apache Geode
	 * distribution configuration setting.
	 *
	 * @param useSharedConfiguration boolean value to set the {@literal use-shared-configuration}
	 * Pivotal GemFire/Apache Geode distribution configuration setting.
	 */
	public void setUseClusterConfiguration(Boolean useSharedConfiguration) {
		this.useClusterConfiguration = useSharedConfiguration;
	}

	/**
	 * Return the state of the {@literal use-shared-configuration} Pivotal GemFire/Apache Geode
	 * distribution configuration setting.
	 *
	 * @return the current boolean value for the {@literal use-shared-configuration}
	 * Pivotal GemFire/Apache Geode distribution configuration setting.
	 */
	public Boolean getUseClusterConfiguration() {
		return this.useClusterConfiguration;
	}

	public static class CacheFactoryToPdxConfigurerAdapter implements PdxConfigurer<CacheFactory> {

		public static CacheFactoryToPdxConfigurerAdapter from(@NonNull CacheFactory cacheFactory) {
			return new CacheFactoryToPdxConfigurerAdapter(cacheFactory);
		}

		private final CacheFactory cacheFactory;

		protected CacheFactoryToPdxConfigurerAdapter(@NonNull CacheFactory cacheFactory) {
			Assert.notNull(cacheFactory, "CacheFactory must not be null");
			this.cacheFactory = cacheFactory;
		}

		@Override
		public @NonNull CacheFactory getTarget() {
			return this.cacheFactory;
		}

		@Override
		public @NonNull PdxConfigurer<CacheFactory> setDiskStoreName(String diskStoreName) {
			getTarget().setPdxDiskStore(diskStoreName);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<CacheFactory> setIgnoreUnreadFields(Boolean ignoreUnreadFields) {
			getTarget().setPdxIgnoreUnreadFields(ignoreUnreadFields);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<CacheFactory> setPersistent(Boolean persistent) {
			getTarget().setPdxPersistent(persistent);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<CacheFactory> setReadSerialized(Boolean readSerialized) {
			getTarget().setPdxReadSerialized(readSerialized);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<CacheFactory> setSerializer(PdxSerializer pdxSerializer) {
			getTarget().setPdxSerializer(pdxSerializer);
			return this;
		}
	}

	public static class JndiDataSource {

		private List<ConfigProperty> configProperties;

		private Map<String, String> attributes;

		public Map<String, String> getAttributes() {
			return this.attributes;
		}

		public void setAttributes(Map<String, String> attributes) {
			this.attributes = attributes;
		}

		public List<ConfigProperty> getProps() {
			return this.configProperties;
		}

		public void setProps(List<ConfigProperty> props) {
			this.configProperties = props;
		}
	}
}
