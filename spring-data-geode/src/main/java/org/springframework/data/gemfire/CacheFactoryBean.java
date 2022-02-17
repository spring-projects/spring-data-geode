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

import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.util.GatewayConflictResolver;
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
import org.springframework.lang.Nullable;
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
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.data.gemfire.AbstractResolvableCacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
 */
@SuppressWarnings("unused")
public class CacheFactoryBean extends AbstractResolvableCacheFactoryBean {

	private Boolean enableAutoReconnect;
	private Boolean useClusterConfiguration;

	private GatewayConflictResolver gatewayConflictResolver;

	private Integer lockLease;
	private Integer lockTimeout;
	private Integer messageSyncInterval;
	private Integer searchTimeout;

	private List<JndiDataSource> jndiDataSources;

	private final List<PeerCacheConfigurer> peerCacheConfigurers = new ArrayList<>();

	private final PeerCacheConfigurer compositePeerCacheConfigurer = (beanName, bean) ->
		nullSafeList(peerCacheConfigurers).forEach(peerCacheConfigurer ->
			peerCacheConfigurer.configure(beanName, bean));

	private org.apache.geode.security.SecurityManager securityManager;

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
	@SuppressWarnings("unchecked")
	protected <T extends GemFireCache> T doFetchCache() {
		return (T) CacheFactory.getAnyInstance();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected Class<? extends GemFireCache> doGetObjectType() {
		return Cache.class;
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
	@Override
	protected @NonNull Object createFactory(@NonNull Properties gemfireProperties) {
		return new CacheFactory(gemfireProperties);
	}

	/**
	 * Configures the {@link CacheFactory} used to create the {@link Cache}.
	 *
	 * @param factory {@link CacheFactory} used to create the {@link Cache}.
	 * @return the configured {@link CacheFactory}.
	 * @see #configurePdx(CacheFactory)
	 * @see #configureSecurity(CacheFactory)
	 * @see org.apache.geode.cache.CacheFactory
	 */
	@Override
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
	protected @NonNull CacheFactory configureSecurity(@NonNull CacheFactory cacheFactory) {

		org.apache.geode.security.SecurityManager securityManager = getSecurityManager();

		return securityManager != null
			? cacheFactory.setSecurityManager(securityManager)
			: cacheFactory;
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
	@Override
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
	@Override
	protected @NonNull <T extends GemFireCache> T postProcess(@NonNull T cache) {

		super.postProcess(cache);

		if (cache instanceof Cache) {

			Cache peerCache = (Cache) cache;

			Optional.ofNullable(getGatewayConflictResolver()).ifPresent(peerCache::setGatewayConflictResolver);
			Optional.ofNullable(getLockLease()).ifPresent(peerCache::setLockLease);
			Optional.ofNullable(getLockTimeout()).ifPresent(peerCache::setLockTimeout);
			Optional.ofNullable(getMessageSyncInterval()).ifPresent(peerCache::setMessageSyncInterval);
			Optional.ofNullable(getSearchTimeout()).ifPresent(peerCache::setSearchTimeout);
		}

		registerJndiDataSources(cache);

		return cache;
	}

	private GemFireCache registerJndiDataSources(GemFireCache cache) {

		CollectionUtils.nullSafeCollection(getJndiDataSources()).forEach(jndiDataSource -> {

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

	/**
	 * Returns a reference to the {@literal Composite} {@link PeerCacheConfigurer} used to
	 * apply additional configuration to this {@link CacheFactoryBean} during Spring container initialization.
	 *
	 * @return the {@literal Composite} {@link PeerCacheConfigurer}.
	 * @see org.springframework.data.gemfire.config.annotation.PeerCacheConfigurer
	 */
	public @NonNull PeerCacheConfigurer getCompositePeerCacheConfigurer() {
		return this.compositePeerCacheConfigurer;
	}

	/**
	 * Controls whether auto-reconnect functionality introduced in GemFire 8 is enabled or not.
	 *
	 * @param enableAutoReconnect a boolean value to enable/disable auto-reconnect functionality.
	 * @since GemFire 8.0
	 */
	public void setEnableAutoReconnect(@Nullable Boolean enableAutoReconnect) {
		this.enableAutoReconnect = enableAutoReconnect;
	}

	/**
	 * Gets the value for the auto-reconnect setting.
	 *
	 * @return a boolean value indicating whether auto-reconnect was specified (non-null) and whether it was enabled
	 * or not.
	 */
	public @Nullable Boolean getEnableAutoReconnect() {
		return this.enableAutoReconnect;
	}

	/**
	 * Requires GemFire 7.0 or higher
	 * @param gatewayConflictResolver defined as Object in the signature for backward
	 * compatibility with Gemfire 6 compatibility. This must be an instance of
	 * {@link org.apache.geode.cache.util.GatewayConflictResolver}
	 */
	public void setGatewayConflictResolver(@Nullable GatewayConflictResolver gatewayConflictResolver) {
		this.gatewayConflictResolver = gatewayConflictResolver;
	}

	/**
	 * @return the gatewayConflictResolver
	 */
	public @Nullable GatewayConflictResolver getGatewayConflictResolver() {
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
	public void setLockLease(@Nullable Integer lockLease) {
		this.lockLease = lockLease;
	}

	/**
	 * @return the lockLease
	 */
	public @Nullable Integer getLockLease() {
		return this.lockLease;
	}

	/**
	 * Sets the number of seconds in which the implicit object lock request will timeout.
	 *
	 * @param lockTimeout an integer value specifying the object lock request timeout.
	 */
	public void setLockTimeout(@Nullable Integer lockTimeout) {
		this.lockTimeout = lockTimeout;
	}

	/**
	 * @return the lockTimeout
	 */
	public @Nullable Integer getLockTimeout() {
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
	public void setMessageSyncInterval(@Nullable Integer messageSyncInterval) {
		this.messageSyncInterval = messageSyncInterval;
	}

	/**
	 * @return the messageSyncInterval
	 */
	public @Nullable Integer getMessageSyncInterval() {
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
		setPeerCacheConfigurers(Arrays.asList(ArrayUtils.nullSafeArray(peerCacheConfigurers, PeerCacheConfigurer.class)));
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
	public void setSearchTimeout(@Nullable Integer searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	/**
	 * @return the searchTimeout
	 */
	public @Nullable Integer getSearchTimeout() {
		return this.searchTimeout;
	}

	/**
	 * Configures the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 *
	 * @param securityManager {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 * @see org.apache.geode.security.SecurityManager
	 */
	public void setSecurityManager(@Nullable SecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	/**
	 * Returns the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 *
	 * @return the {@link org.apache.geode.security.SecurityManager} used to secure this cache.
	 * @see org.apache.geode.security.SecurityManager
	 */
	public @Nullable SecurityManager getSecurityManager() {
		return this.securityManager;
	}

	/**
	 * Sets the state of the {@literal use-shared-configuration} Pivotal GemFire/Apache Geode
	 * distribution configuration setting.
	 *
	 * @param useSharedConfiguration boolean value to set the {@literal use-shared-configuration}
	 * Pivotal GemFire/Apache Geode distribution configuration setting.
	 */
	public void setUseClusterConfiguration(@Nullable Boolean useSharedConfiguration) {
		this.useClusterConfiguration = useSharedConfiguration;
	}

	/**
	 * Return the state of the {@literal use-shared-configuration} Pivotal GemFire/Apache Geode
	 * distribution configuration setting.
	 *
	 * @return the current boolean value for the {@literal use-shared-configuration}
	 * Pivotal GemFire/Apache Geode distribution configuration setting.
	 */
	public @Nullable Boolean getUseClusterConfiguration() {
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
