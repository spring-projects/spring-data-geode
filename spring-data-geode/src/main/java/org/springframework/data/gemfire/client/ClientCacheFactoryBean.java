/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.gemfire.client;

import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeCollection;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.pdx.PdxSerializer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.support.DefaultableDelegatingPoolAdapter;
import org.springframework.data.gemfire.client.support.DelegatingPoolAdapter;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.support.ConnectionEndpointList;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.SpringExtensions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring {@link FactoryBean} used to construct, configure and initialize a {@link ClientCache}.
 *
 * @author Costin Leau
 * @author Lyndon Adams
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see java.util.Properties
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.SocketFactory
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.ApplicationContextEvent
 * @see org.springframework.context.event.ContextRefreshedEvent
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
 * @since 1.0.0
 */
@SuppressWarnings("unused")
// TODO: Refactor this class to no longer extend CacheFactoryBean
public class ClientCacheFactoryBean extends CacheFactoryBean implements ApplicationListener<ContextRefreshedEvent> {

	protected static final PoolResolver DEFAULT_POOL_RESOLVER = new PoolManagerPoolResolver();

	private Boolean keepAlive = false;
	private Boolean multiUserAuthentication;
	private Boolean prSingleHopEnabled;
	private Boolean readyForEvents;
	private Boolean subscriptionEnabled;
	private Boolean threadLocalConnections;

	private final ConnectionEndpointList locators = new ConnectionEndpointList();
	private final ConnectionEndpointList servers = new ConnectionEndpointList();

	private Integer durableClientTimeout;
	private Integer freeConnectionTimeout;
	private Integer loadConditioningInterval;
	private Integer maxConnections;
	private Integer minConnections;
	private Integer readTimeout;
	private Integer retryAttempts;
	private Integer serverConnectionTimeout;
	private Integer socketBufferSize;
	private Integer socketConnectTimeout;
	private Integer statisticsInterval;
	private Integer subscriptionAckInterval;
	private Integer subscriptionMessageTrackingTimeout;
	private Integer subscriptionRedundancy;

	private List<ClientCacheConfigurer> clientCacheConfigurers = Collections.emptyList();

	private Long idleTimeout;
	private Long pingInterval;

	private Pool pool;

	private PoolResolver poolResolver = DEFAULT_POOL_RESOLVER;

	private SocketFactory socketFactory;

	private String durableClientId;
	private String poolName;
	private String serverGroup;

	private final ClientCacheConfigurer compositeClientCacheConfigurer = (beanName, bean) ->
		nullSafeCollection(this.clientCacheConfigurers).forEach(clientCacheConfigurer ->
			clientCacheConfigurer.configure(beanName, bean));

	/**
	 * Applies the composite {@link ClientCacheConfigurer ClientCacheConfigurers} to this {@link ClientCacheFactoryBean}
	 * before the {@link ClientCache} is created.
	 *
	 * @see #getCompositeClientCacheConfigurer()
	 * @see #applyClientCacheConfigurers(ClientCacheConfigurer...)
	 */
	@Override
	protected void applyCacheConfigurers() {
		applyClientCacheConfigurers(getCompositeClientCacheConfigurer());
	}

	/**
	 * Applies the array of {@link ClientCacheConfigurer ClientCacheConfigurers} to this {@link ClientCacheFactoryBean}
	 * before the {@link ClientCache} is created.
	 *
	 * @param clientCacheConfigurers array of {@link ClientCacheConfigurer ClientCacheConfigurers}
	 * applied to this {@link ClientCacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
	 * @see #applyClientCacheConfigurers(Iterable)
	 */
	protected void applyClientCacheConfigurers(ClientCacheConfigurer... clientCacheConfigurers) {
		applyClientCacheConfigurers(Arrays.asList(ArrayUtils.nullSafeArray(clientCacheConfigurers,
			ClientCacheConfigurer.class)));
	}

	/**
	 * Apples the {@link Iterable} of {@link ClientCacheConfigurer ClientCacheConfigurers}
	 * to this {@link ClientCacheFactoryBean} before the {@link ClientCache} is created.
	 *
	 * @param clientCacheConfigurers {@link Iterable} of {@link ClientCacheConfigurer ClientCacheConfigurers}
	 * applied to this {@link ClientCacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
	 * @see java.lang.Iterable
	 */
	protected void applyClientCacheConfigurers(Iterable<ClientCacheConfigurer> clientCacheConfigurers) {
		StreamSupport.stream(CollectionUtils.nullSafeIterable(clientCacheConfigurers).spliterator(), false)
			.forEach(clientCacheConfigurer -> clientCacheConfigurer.configure(getBeanName(), this));
	}

	/**
	 * Fetches an existing {@link ClientCache} instance from the {@link ClientCacheFactory}.
	 *
	 * @param <T> parameterized {@link Class} type extension of {@link GemFireCache}.
	 * @return an existing {@link ClientCache} instance if available.
	 * @throws org.apache.geode.cache.CacheClosedException if an existing {@link ClientCache} instance does not exist.
	 * @see org.apache.geode.cache.client.ClientCacheFactory#getAnyInstance()
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #getCache()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected <T extends GemFireCache> T doFetchCache() {
		return (T) ClientCacheFactory.getAnyInstance();
	}

	/**
	 * Returns the {@link Class type} of {@link GemFireCache} constructed by this {@link ClientCacheFactoryBean}.
	 *
	 * Returns {@link ClientCache} {@link Class}.
	 *
	 * @return the {@link Class type} of {@link GemFireCache} constructed by this {@link ClientCacheFactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	protected Class<? extends GemFireCache> doGetObjectType() {
		return ClientCache.class;
	}

	/**
	 * Resolves the Apache Geode {@link Properties} used to configure the {@link ClientCache}.
	 *
	 * @return the resolved Apache Geode {@link Properties} used to configure the {@link ClientCache}.
	 * @see org.apache.geode.distributed.DistributedSystem#getProperties()
	 */
	@Override
	protected @NonNull Properties resolveProperties() {
		return resolveProperties(GemfireUtils::getDistributedSystem);
	}

	@NonNull Properties resolveProperties(@NonNull Supplier<DistributedSystem> distributedSystemSupplier) {

		Properties gemfireProperties = super.resolveProperties();

		DistributedSystem distributedSystem = distributedSystemSupplier.get();

		if (GemfireUtils.isConnected(distributedSystem)) {
			Properties distributedSystemProperties = (Properties) distributedSystem.getProperties().clone();
			distributedSystemProperties.putAll(gemfireProperties);
			gemfireProperties = distributedSystemProperties;
		}

		GemfireUtils.configureDurableClient(gemfireProperties, getDurableClientId(), getDurableClientTimeout());

		return gemfireProperties;
	}

	/**
	 * Constructs a new instance of {@link ClientCacheFactory} initialized with the given Apache Geode {@link Properties}
	 * used to construct, configure and initialize a new {@link ClientCache} instance.
	 *
	 * @param gemfireProperties {@link Properties} used by the {@link ClientCacheFactory}
	 * to configure the {@link ClientCache}.
	 * @return a new instance of {@link ClientCacheFactory} initialized with the given Apache Geode {@link Properties}.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see java.util.Properties
	 */
	@Override
	protected @NonNull Object createFactory(@NonNull Properties gemfireProperties) {
		return new ClientCacheFactory(gemfireProperties);
	}

	/**
	 * Configures the {@link ClientCacheFactory} used to create the {@link ClientCache}.
	 *
	 * @param factory {@link ClientCacheFactory} used to create the {@link ClientCache}.
	 * @return the configured {@link ClientCacheFactory}.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see #configurePool(ClientCacheFactory)
	 * @see #configurePdx(PdxConfigurer)
	 */
	@Override
	protected @NonNull Object configureFactory(@NonNull Object factory) {
		return configurePool(configurePdx((ClientCacheFactory) factory));
	}

	/**
	 * Configures the {@link ClientCache} to use PDX serialization.
	 *
	 * @param clientCacheFactory {@link ClientCacheFactory} to configure with PDX.
	 * @return the given {@link ClientCacheFactory}.
	 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean.ClientCacheFactoryToPdxConfigurerAdapter
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see #configurePdx(PdxConfigurer)
	 */
	protected @NonNull ClientCacheFactory configurePdx(@NonNull ClientCacheFactory clientCacheFactory) {

		PdxConfigurer<ClientCacheFactory> pdxConfigurer =
			ClientCacheFactoryToPdxConfigurerAdapter.from(clientCacheFactory);

		return configurePdx(pdxConfigurer);
	}

	/**
	 * Configure the {@literal DEFAULT} {@link Pool} of the {@link ClientCacheFactory} using a given {@link Pool}
	 * instance or a named {@link Pool} instance.
	 *
	 * @param clientCacheFactory {@link ClientCacheFactory} used to configure the {@literal DEFAULT} {@link Pool}.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.client.Pool
	 */
	protected @NonNull ClientCacheFactory configurePool(@NonNull ClientCacheFactory clientCacheFactory) {

		DefaultableDelegatingPoolAdapter pool =
			DefaultableDelegatingPoolAdapter.from(DelegatingPoolAdapter.from(resolvePool())).preferDefault();

		clientCacheFactory.setPoolFreeConnectionTimeout(pool.getFreeConnectionTimeout(getFreeConnectionTimeout()));
		clientCacheFactory.setPoolIdleTimeout(pool.getIdleTimeout(getIdleTimeout()));
		clientCacheFactory.setPoolLoadConditioningInterval(pool.getLoadConditioningInterval(getLoadConditioningInterval()));
		clientCacheFactory.setPoolMaxConnections(pool.getMaxConnections(getMaxConnections()));
		clientCacheFactory.setPoolMinConnections(pool.getMinConnections(getMinConnections()));
		clientCacheFactory.setPoolMultiuserAuthentication(pool.getMultiuserAuthentication(getMultiUserAuthentication()));
		clientCacheFactory.setPoolPingInterval(pool.getPingInterval(getPingInterval()));
		clientCacheFactory.setPoolPRSingleHopEnabled(pool.getPRSingleHopEnabled(getPrSingleHopEnabled()));
		clientCacheFactory.setPoolReadTimeout(pool.getReadTimeout(getReadTimeout()));
		clientCacheFactory.setPoolRetryAttempts(pool.getRetryAttempts(getRetryAttempts()));
		clientCacheFactory.setPoolServerConnectionTimeout(pool.getServerConnectionTimeout(getServerConnectionTimeout()));
		clientCacheFactory.setPoolServerGroup(pool.getServerGroup(getServerGroup()));
		clientCacheFactory.setPoolSocketBufferSize(pool.getSocketBufferSize(getSocketBufferSize()));
		clientCacheFactory.setPoolSocketConnectTimeout(pool.getSocketConnectTimeout(getSocketConnectTimeout()));
		clientCacheFactory.setPoolSocketFactory(pool.getSocketFactory(getSocketFactory()));
		clientCacheFactory.setPoolStatisticInterval(pool.getStatisticInterval(getStatisticsInterval()));
		clientCacheFactory.setPoolSubscriptionAckInterval(pool.getSubscriptionAckInterval(getSubscriptionAckInterval()));
		clientCacheFactory.setPoolSubscriptionEnabled(pool.getSubscriptionEnabled(getSubscriptionEnabled()));
		clientCacheFactory.setPoolSubscriptionMessageTrackingTimeout(pool.getSubscriptionMessageTrackingTimeout(getSubscriptionMessageTrackingTimeout()));
		clientCacheFactory.setPoolSubscriptionRedundancy(pool.getSubscriptionRedundancy(getSubscriptionRedundancy()));
		clientCacheFactory.setPoolThreadLocalConnections(pool.getThreadLocalConnections(getThreadLocalConnections()));

		AtomicBoolean noServers = new AtomicBoolean(getServers().isEmpty());

		boolean noLocators = getLocators().isEmpty();
		boolean hasLocators = !noLocators;
		boolean hasServers = !noServers.get();

		if (hasServers || noLocators) {

			Iterable<InetSocketAddress> servers = pool.getServers(getServers().toInetSocketAddresses());

			StreamSupport.stream(servers.spliterator(), false).forEach(server -> {
				clientCacheFactory.addPoolServer(server.getHostName(), server.getPort());
				noServers.set(false);
			});
		}

		if (hasLocators || noServers.get()) {

			Iterable<InetSocketAddress> locators = pool.getLocators(getLocators().toInetSocketAddresses());

			StreamSupport.stream(locators.spliterator(), false).forEach(locator ->
				clientCacheFactory.addPoolLocator(locator.getHostName(), locator.getPort()));
		}

		return clientCacheFactory;
	}

	/**
	 * Resolves the {@link Pool} used to configure the {@link ClientCache}, {@literal DEFAULT} {@link Pool}.
	 *
	 * @return the resolved {@link Pool} used to configure the {@link ClientCache}, {@literal DEFAULT} {@link Pool}.
	 * @see org.apache.geode.cache.client.PoolManager#find(String)
	 * @see org.apache.geode.cache.client.Pool
	 * @see #getPoolName()
	 * @see #getPool()
	 * @see #findPool(String)
	 * @see #isPoolNameResolvable(String)
	 */
	protected @Nullable Pool resolvePool() {

		Pool pool = getPool();

		if (pool == null) {

			String poolName = resolvePoolName();

			pool = findPool(poolName);

			if (pool == null && isPoolNameResolvable(poolName)) {

				String dereferencedPoolName = SpringExtensions.dereferenceBean(poolName);

				PoolFactoryBean poolFactoryBean =
					getBeanFactory().getBean(dereferencedPoolName, PoolFactoryBean.class);

				return poolFactoryBean.getPool();
			}
		}

		return pool;
	}

	private boolean isPoolNameResolvable(@Nullable String poolName) {

		return Optional.ofNullable(poolName)
			.filter(StringUtils::hasText)
			.filter(getBeanFactory()::containsBean)
			.isPresent();
	}

	@NonNull String resolvePoolName() {

		return Optional.ofNullable(getPoolName())
			.filter(StringUtils::hasText)
			.orElseGet(this::getDefaultPoolName);
	}

	@NonNull String getDefaultPoolName() {
		return GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME;
	}

	Pool findPool(String name) {
		return getPoolResolver().resolve(name);
	}

	/**
	 * Creates a new {@link ClientCache} instance using the provided {@link ClientCacheFactory factory}.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @param factory instance of {@link ClientCacheFactory}.
	 * @return a new instance of {@link ClientCache} created by the provided factory.
	 * @see org.apache.geode.cache.client.ClientCacheFactory#create()
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.apache.geode.cache.GemFireCache
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected @NonNull <T extends GemFireCache> T createCache(@NonNull Object factory) {
		return (T) ((ClientCacheFactory) factory).create();
	}

	/**
	 * Inform the Apache Geode cluster that this {@link ClientCache} is ready to receive events
	 * iff the client is non-durable.
	 *
	 * @param event {@link ApplicationContextEvent} fired when the {@link ApplicationContext} is refreshed.
	 * @see org.apache.geode.cache.client.ClientCache#readyForEvents()
	 * @see #isReadyForEvents()
	 * @see #fetchCache()
	 */
	@Override
	public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {

		if (isReadyForEvents()) {
			try {
				this.<ClientCache>fetchCache().readyForEvents();
			}
			catch (IllegalStateException | CacheClosedException ignore) {
				// Exceptions are thrown when ClientCache.readyForEvents() is called on a non-durable client
				// or the ClientCache is closing.
			}
		}
	}

	/**
	 * Null-safe internal method used to close the {@link ClientCache} and preserve durability.
	 *
	 * @param cache {@link GemFireCache} to close.
	 * @see org.apache.geode.cache.client.ClientCache#close(boolean)
	 * @see #isKeepAlive()
	 */
	@Override
	protected void close(@NonNull GemFireCache cache) {
		((ClientCache) cache).close(isKeepAlive());
	}

	public void addLocators(ConnectionEndpoint... locators) {
		this.locators.add(locators);
	}

	public void addLocators(Iterable<ConnectionEndpoint> locators) {
		this.locators.add(locators);
	}

	public void addServers(ConnectionEndpoint... servers) {
		this.servers.add(servers);
	}

	public void addServers(Iterable<ConnectionEndpoint> servers) {
		this.servers.add(servers);
	}

	/**
	 * Null-safe operation to set an array of {@link ClientCacheConfigurer ClientCacheConfigurers} used to apply
	 * additional configuration to this {@link ClientCacheFactoryBean} when using Annotation-based configuration.
	 *
	 * @param clientCacheConfigurers array of {@link ClientCacheConfigurer ClientCacheConfigurers} used to
	 * apply additional configuration to this {@link ClientCacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
	 * @see #setClientCacheConfigurers(List)
	 */
	public void setClientCacheConfigurers(ClientCacheConfigurer... clientCacheConfigurers) {
		setClientCacheConfigurers(Arrays.asList(ArrayUtils.nullSafeArray(clientCacheConfigurers, ClientCacheConfigurer.class)));
	}

	/**
	 * Null-safe operation to set an {@link List} of {@link ClientCacheConfigurer ClientCacheConfigurers} to apply
	 * additional configuration to this {@link ClientCacheFactoryBean} when using Annotation-based configuration.
	 *
	 * @param clientCacheConfigurers {@link List} of {@link ClientCacheConfigurer ClientCacheConfigurers} used to
	 * apply additional configuration to this {@link ClientCacheFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
	 * @see #setClientCacheConfigurers(ClientCacheConfigurer...)
	 */
	public void setClientCacheConfigurers(List<ClientCacheConfigurer> clientCacheConfigurers) {
		this.clientCacheConfigurers = clientCacheConfigurers != null ? clientCacheConfigurers : Collections.emptyList();
	}

	/**
	 * Returns a reference to the {@literal Composite} {@link ClientCacheConfigurer} used to apply additional
	 * configuration to this {@link ClientCacheFactoryBean} on Spring container initialization.
	 *
	 * @return the {@literal Composite} {@link ClientCacheConfigurer}.
	 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
	 */
	public @NonNull ClientCacheConfigurer getCompositeClientCacheConfigurer() {
		return this.compositeClientCacheConfigurer;
	}

	/**
	 * Set the GemFire System property 'durable-client-id' to indicate to the server that this client is durable.
	 *
	 * @param durableClientId a String value indicating the durable client id.
	 */
	public void setDurableClientId(String durableClientId) {
		this.durableClientId = durableClientId;
	}

	/**
	 * Gets the value of the GemFire System property 'durable-client-id' indicating to the server whether
	 * this client is durable.
	 *
	 * @return a String value indicating the durable client id.
	 */
	public String getDurableClientId() {
		return this.durableClientId;
	}

	/**
	 * Set the GemFire System property 'durable-client-timeout' indicating to the server how long to track events
	 * for the durable client when disconnected.
	 *
	 * @param durableClientTimeout an Integer value indicating the timeout in seconds for the server to keep
	 * the durable client's queue around.
	 */
	public void setDurableClientTimeout(Integer durableClientTimeout) {
		this.durableClientTimeout = durableClientTimeout;
	}

	/**
	 * Get the value of the GemFire System property 'durable-client-timeout' indicating to the server how long
	 * to track events for the durable client when disconnected.
	 *
	 * @return an Integer value indicating the timeout in seconds for the server to keep
	 * the durable client's queue around.
	 */
	public Integer getDurableClientTimeout() {
		return this.durableClientTimeout;
	}

	@Override
	public final void setEnableAutoReconnect(Boolean enableAutoReconnect) {
		throw new UnsupportedOperationException("Auto-reconnect does not apply to clients");
	}

	@Override
	public final Boolean getEnableAutoReconnect() {
		return Boolean.FALSE;
	}

	public void setFreeConnectionTimeout(Integer freeConnectionTimeout) {
		this.freeConnectionTimeout = freeConnectionTimeout;
	}

	public Integer getFreeConnectionTimeout() {
		return this.freeConnectionTimeout;
	}

	public void setIdleTimeout(Long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public Long getIdleTimeout() {
		return this.idleTimeout;
	}

	/**
	 * Sets whether the server(s) should keep the durable client's queue alive for the duration of the timeout
	 * when the client voluntarily disconnects.
	 *
	 * @param keepAlive a boolean value indicating to the server to keep the durable client's queues alive.
	 */
	public void setKeepAlive(Boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * Gets the user specified value for whether the server(s) should keep the durable client's queue alive
	 * for the duration of the timeout when the client voluntarily disconnects.
	 *
	 * @return a boolean value indicating whether the server should keep the durable client's queues alive.
	 */
	public Boolean getKeepAlive() {
		return this.keepAlive;
	}

	/**
	 * Determines whether the server(s) should keep the durable client's queue alive for the duration of the timeout
	 * when the client voluntarily disconnects.
	 *
	 * @return a boolean value indicating whether the server should keep the durable client's queues alive.
	 */
	public boolean isKeepAlive() {
		return Boolean.TRUE.equals(getKeepAlive());
	}

	public void setLoadConditioningInterval(Integer loadConditioningInterval) {
		this.loadConditioningInterval = loadConditioningInterval;
	}

	public Integer getLoadConditioningInterval() {
		return this.loadConditioningInterval;
	}

	public void setLocators(ConnectionEndpoint[] locators) {
		setLocators(ConnectionEndpointList.from(locators));
	}

	public void setLocators(Iterable<ConnectionEndpoint> locators) {
		getLocators().clear();
		addLocators(locators);
	}

	protected ConnectionEndpointList getLocators() {
		return this.locators;
	}

	public void setMaxConnections(Integer maxConnections) {
		this.maxConnections = maxConnections;
	}

	public Integer getMaxConnections() {
		return this.maxConnections;
	}

	public void setMinConnections(Integer minConnections) {
		this.minConnections = minConnections;
	}

	public Integer getMinConnections() {
		return this.minConnections;
	}

	public void setMultiUserAuthentication(Boolean multiUserAuthentication) {
		this.multiUserAuthentication = multiUserAuthentication;
	}

	public Boolean getMultiUserAuthentication() {
		return this.multiUserAuthentication;
	}

	/**
	 * Sets the {@link Pool} used by this {@link ClientCache} to obtain connections to the Apache Geode cluster.
	 *
	 * @param pool {@link Pool} used by this {@link ClientCache} to obtain connections to the Apache Geode cluster.
	 * @see org.apache.geode.cache.client.Pool
	 */
	public void setPool(@Nullable Pool pool) {
		this.pool = pool;
	}

	/**
	 * Gets the {@link Pool} used by this {@link ClientCache} to obtain connections to the Apache Geode cluster.
	 *
	 * @return {@link Pool} used by this {@link ClientCache} to obtain connections to the Apache Geode cluster.
	 * @see org.apache.geode.cache.client.Pool
	 */
	public @Nullable Pool getPool() {
		return this.pool;
	}

	/**
	 * Sets the {@link String name} of the {@link Pool} used by this {@link ClientCache} to obtain connections to
	 * the Apache Geode cluster.
	 *
	 * @param poolName {@link String name} of the {@link Pool} used by this {@link ClientCache} to obtain connections to
	 * the Apache Geode cluster.
	 */
	public void setPoolName(@Nullable String poolName) {
		this.poolName = poolName;
	}

	/**
	 * Gets the {@link String name} of the {@link Pool} used by this {@link ClientCache} to obtain connections to
	 * the Apache Geode cluster.
	 *
	 * @return {@link String name} of the {@link Pool} used by this {@link ClientCache} to obtain connections to
	 * the Apache Geode cluster.
	 */
	public @Nullable String getPoolName() {
		return this.poolName;
	}

	/**
	 * Sets (configures) the {@link PoolResolver} used by this {@link ClientCache} to resolve {@link Pool} objects.
	 *
	 * The {@link Pool} objects may be managed or un-managed depending on the {@link PoolResolver} implementation.
	 *
	 * @param poolResolver {@link PoolResolver} used to resolve the configured {@link Pool}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	public void setPoolResolver(@Nullable PoolResolver poolResolver) {
		this.poolResolver = poolResolver;
	}

	/**
	 * Gets the configured {@link PoolResolver} used by this {@link ClientCache} to resolve {@link Pool} objects.
	 *
	 * @return the configured {@link PoolResolver}.  If no {@link PoolResolver} was configured, then return the default,
	 * {@link PoolManagerPoolResolver}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 * @see org.springframework.data.gemfire.client.support.PoolManagerPoolResolver
	 */
	public @NonNull PoolResolver getPoolResolver() {

		PoolResolver poolResolver = this.poolResolver;

		return poolResolver != null ? poolResolver : DEFAULT_POOL_RESOLVER;
	}

	public void setPingInterval(Long pingInterval) {
		this.pingInterval = pingInterval;
	}

	public Long getPingInterval() {
		return this.pingInterval;
	}

	public void setPrSingleHopEnabled(Boolean prSingleHopEnabled) {
		this.prSingleHopEnabled = prSingleHopEnabled;
	}

	public Boolean getPrSingleHopEnabled() {
		return this.prSingleHopEnabled;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Integer getReadTimeout() {
		return this.readTimeout;
	}

	/**
	 * Sets the readyForEvents property to indicate whether the cache client should notify the server
	 * that it is ready to receive updates.
	 *
	 * @param readyForEvents sets a boolean flag to notify the server that this durable client
	 * is ready to receive updates.
	 * @see #getReadyForEvents()
	 */
	public void setReadyForEvents(Boolean readyForEvents){
		this.readyForEvents = readyForEvents;
	}

	/**
	 * Gets the user-configured value for deciding that this client is ready to receive events from the server(s).
	 *
	 * @return a {@link Boolean} indicating whether this client is ready to receive events from the server(s).
	 */
	public Boolean getReadyForEvents(){
		return this.readyForEvents;
	}

	/**
	 * Determines whether this GemFire cache client is ready for events.  If 'readyForEvents' was explicitly set,
	 * then it takes precedence over all other considerations (e.g. durability).
	 *
	 * @return a boolean value indicating whether this GemFire cache client is ready for events.
	 * @see org.springframework.data.gemfire.GemfireUtils#isDurable(ClientCache)
	 * @see #getReadyForEvents()
	 */
	public boolean isReadyForEvents() {

		Boolean readyForEvents = getReadyForEvents();

		return readyForEvents != null ? Boolean.TRUE.equals(readyForEvents)
			: SpringExtensions.safeGetValue(() -> GemfireUtils.isDurable(fetchCache()), false);
	}

	public void setRetryAttempts(Integer retryAttempts) {
		this.retryAttempts = retryAttempts;
	}

	public Integer getRetryAttempts() {
		return this.retryAttempts;
	}

	public void setServerConnectionTimeout(Integer serverConnectionTimeout) {
		this.serverConnectionTimeout = serverConnectionTimeout;
	}

	public Integer getServerConnectionTimeout() {
		return this.serverConnectionTimeout;
	}

	public void setServerGroup(String serverGroup) {
		this.serverGroup = serverGroup;
	}

	public String getServerGroup() {
		return this.serverGroup;
	}

	public void setServers(ConnectionEndpoint[] servers) {
		setServers(ConnectionEndpointList.from(servers));
	}

	public void setServers(Iterable<ConnectionEndpoint> servers) {
		getServers().clear();
		addServers(servers);
	}

	protected ConnectionEndpointList getServers() {
		return this.servers;
	}

	public void setSocketBufferSize(Integer socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public Integer getSocketBufferSize() {
		return this.socketBufferSize;
	}

	public void setSocketConnectTimeout(Integer socketConnectTimeout) {
		this.socketConnectTimeout = socketConnectTimeout;
	}

	public Integer getSocketConnectTimeout() {
		return this.socketConnectTimeout;
	}

	public void setSocketFactory(@Nullable SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public @NonNull SocketFactory getSocketFactory() {
		return this.socketFactory;
	}

	public void setStatisticsInterval(Integer statisticsInterval) {
		this.statisticsInterval = statisticsInterval;
	}

	public Integer getStatisticsInterval() {
		return this.statisticsInterval;
	}

	public void setSubscriptionAckInterval(Integer subscriptionAckInterval) {
		this.subscriptionAckInterval = subscriptionAckInterval;
	}

	public Integer getSubscriptionAckInterval() {
		return this.subscriptionAckInterval;
	}

	public void setSubscriptionEnabled(Boolean subscriptionEnabled) {
		this.subscriptionEnabled = subscriptionEnabled;
	}

	public Boolean getSubscriptionEnabled() {
		return this.subscriptionEnabled;
	}

	public void setSubscriptionMessageTrackingTimeout(Integer subscriptionMessageTrackingTimeout) {
		this.subscriptionMessageTrackingTimeout = subscriptionMessageTrackingTimeout;
	}

	public Integer getSubscriptionMessageTrackingTimeout() {
		return this.subscriptionMessageTrackingTimeout;
	}

	public void setSubscriptionRedundancy(Integer subscriptionRedundancy) {
		this.subscriptionRedundancy = subscriptionRedundancy;
	}

	public Integer getSubscriptionRedundancy() {
		return this.subscriptionRedundancy;
	}

	public void setThreadLocalConnections(Boolean threadLocalConnections) {
		this.threadLocalConnections = threadLocalConnections;
	}

	public Boolean getThreadLocalConnections() {
		return this.threadLocalConnections;
	}

	@Override
	public final void setUseClusterConfiguration(Boolean useClusterConfiguration) {
		throw new UnsupportedOperationException("Cluster-based Configuration does not apply to clients");
	}

	@Override
	public final Boolean getUseClusterConfiguration() {
		return Boolean.FALSE;
	}

	public static class ClientCacheFactoryToPdxConfigurerAdapter implements PdxConfigurer<ClientCacheFactory> {

		public static ClientCacheFactoryToPdxConfigurerAdapter from(@NonNull ClientCacheFactory clientCacheFactory) {
			return new ClientCacheFactoryToPdxConfigurerAdapter(clientCacheFactory);
		}

		private final ClientCacheFactory cacheFactory;

		protected ClientCacheFactoryToPdxConfigurerAdapter(@NonNull ClientCacheFactory cacheFactory) {
			Assert.notNull(cacheFactory, "ClientCacheFactory must not be null");
			this.cacheFactory = cacheFactory;
		}

		@Override
		public @NonNull ClientCacheFactory getTarget() {
			return this.cacheFactory;
		}

		@Override
		public @NonNull PdxConfigurer<ClientCacheFactory> setDiskStoreName(String diskStoreName) {
			getTarget().setPdxDiskStore(diskStoreName);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<ClientCacheFactory> setIgnoreUnreadFields(Boolean ignoreUnreadFields) {
			getTarget().setPdxIgnoreUnreadFields(ignoreUnreadFields);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<ClientCacheFactory> setPersistent(Boolean persistent) {
			getTarget().setPdxPersistent(persistent);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<ClientCacheFactory> setReadSerialized(Boolean readSerialized) {
			getTarget().setPdxReadSerialized(readSerialized);
			return this;
		}

		@Override
		public @NonNull PdxConfigurer<ClientCacheFactory> setSerializer(PdxSerializer pdxSerializer) {
			getTarget().setPdxSerializer(pdxSerializer);
			return this;
		}
	}
}
