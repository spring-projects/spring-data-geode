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
package org.springframework.data.gemfire.client;

import static java.util.stream.StreamSupport.stream;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeIterable;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.distributed.DistributedSystem;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.config.annotation.PoolConfigurer;
import org.springframework.data.gemfire.support.AbstractFactoryBeanSupport;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.support.ConnectionEndpointList;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.DistributedSystemUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Spring {@link FactoryBean} used to construct, configure and initialize a {@link Pool}.
 *
 * If a new {@link Pool} is created, its lifecycle is bound to that of this declaring {@link FactoryBean}
 * and indirectly, the Spring container.
 *
 * If a {@link Pool} having the configured {@link String name} already exists, then the existing {@link Pool}
 * will be returned as is without any modifications and its lifecycle will be unaffected by this {@link FactoryBean}.
 *
 * @author Costin Leau
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolFactory
 * @see org.apache.geode.cache.client.PoolManager
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
 * @see org.springframework.data.gemfire.support.ConnectionEndpointList
 */
@SuppressWarnings("unused")
public class PoolFactoryBean extends AbstractFactoryBeanSupport<Pool> implements DisposableBean, InitializingBean {

	protected static final int DEFAULT_LOCATOR_PORT = DistributedSystemUtils.DEFAULT_LOCATOR_PORT;
	protected static final int DEFAULT_SERVER_PORT = DistributedSystemUtils.DEFAULT_CACHE_SERVER_PORT;

	protected static final PoolResolver DEFAULT_POOL_RESOLVER = new PoolManagerPoolResolver();

	// Indicates whether the Pool has been created by this FactoryBean, or not
	volatile boolean springManagedPool = true;

	// GemFire Pool Configuration Settings
	private boolean keepAlive = false;
	private boolean multiUserAuthentication = PoolFactory.DEFAULT_MULTIUSER_AUTHENTICATION;
	private boolean prSingleHopEnabled = PoolFactory.DEFAULT_PR_SINGLE_HOP_ENABLED;
	private boolean subscriptionEnabled = PoolFactory.DEFAULT_SUBSCRIPTION_ENABLED;
	private boolean threadLocalConnections = PoolFactory.DEFAULT_THREAD_LOCAL_CONNECTIONS;

	private int freeConnectionTimeout = PoolFactory.DEFAULT_FREE_CONNECTION_TIMEOUT;
	private int loadConditioningInterval = PoolFactory.DEFAULT_LOAD_CONDITIONING_INTERVAL;
	private int maxConnections = PoolFactory.DEFAULT_MAX_CONNECTIONS;
	private int minConnections = PoolFactory.DEFAULT_MIN_CONNECTIONS;
	private int readTimeout = PoolFactory.DEFAULT_READ_TIMEOUT;
	private int retryAttempts = PoolFactory.DEFAULT_RETRY_ATTEMPTS;
	private int serverConnectionTimeout = PoolFactory.DEFAULT_SERVER_CONNECTION_TIMEOUT;
	private int socketBufferSize = PoolFactory.DEFAULT_SOCKET_BUFFER_SIZE;
	private int socketConnectTimeout = PoolFactory.DEFAULT_SOCKET_CONNECT_TIMEOUT;
	private int statisticInterval = PoolFactory.DEFAULT_STATISTIC_INTERVAL;
	private int subscriptionAckInterval = PoolFactory.DEFAULT_SUBSCRIPTION_ACK_INTERVAL;
	private int subscriptionMessageTrackingTimeout = PoolFactory.DEFAULT_SUBSCRIPTION_MESSAGE_TRACKING_TIMEOUT;
	private int subscriptionRedundancy = PoolFactory.DEFAULT_SUBSCRIPTION_REDUNDANCY;
	private int subscriptionTimeoutMultiplier = PoolFactory.DEFAULT_SUBSCRIPTION_TIMEOUT_MULTIPLIER;

	private long idleTimeout = PoolFactory.DEFAULT_IDLE_TIMEOUT;
	private long pingInterval = PoolFactory.DEFAULT_PING_INTERVAL;

	private final ConnectionEndpointList locators = new ConnectionEndpointList();
	private final ConnectionEndpointList servers = new ConnectionEndpointList();
	private final ConnectionEndpointList xmlDeclaredLocators = new ConnectionEndpointList();
	private final ConnectionEndpointList xmlDeclaredServers = new ConnectionEndpointList();

	private List<PoolConfigurer> poolConfigurers = Collections.emptyList();

	private volatile Pool pool;

	private final PoolConfigurer xmlDeclaredServersPoolConfigurer = (beanName, bean) ->
		bean.addServers(this.xmlDeclaredServers);

	private final PoolConfigurer xmlDeclaredLocatorsPoolConfigurer = (beanName, bean) ->
		bean.addLocators(this.xmlDeclaredLocators);

	private final PoolConfigurer compositePoolConfigurer = (beanName, bean) -> {

		List<PoolConfigurer> allPoolConfigurers =
			new ArrayList<>(CollectionUtils.nullSafeSize(this.poolConfigurers) + 2);

		allPoolConfigurers.add(this.xmlDeclaredLocatorsPoolConfigurer);
		allPoolConfigurers.add(this.xmlDeclaredServersPoolConfigurer);
		allPoolConfigurers.addAll(CollectionUtils.nullSafeCollection(this.poolConfigurers));

		allPoolConfigurers.forEach(poolConfigurer -> poolConfigurer.configure(beanName, bean));
	};

	private PoolFactoryInitializer poolFactoryInitializer;

	private PoolResolver poolResolver = DEFAULT_POOL_RESOLVER;

	private SocketFactory socketFactory;

	private String name;
	private String serverGroup = PoolFactory.DEFAULT_SERVER_GROUP;

	/**
	 * Prepares the construction, configuration and initialization of a new {@link Pool}.
	 *
	 * @throws Exception if {@link Pool} initialization fails.
	 * @see org.apache.geode.cache.client.PoolManager
	 * @see org.apache.geode.cache.client.PoolFactory
	 * @see org.apache.geode.cache.client.Pool
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		init(resolvePool(resolvePoolName()));
	}

	/**
	 * Initializes the given {@link Pool}
	 *
	 * @param existingPool {@link Pool} to initialize.
	 * @see org.apache.geode.cache.client.Pool
	 */
	private void init(@Nullable Pool existingPool) {

		if (existingPool != null) {

			this.pool = existingPool;
			this.springManagedPool = false;

			logDebug(() -> String.format("Pool [%s] already exists; Using existing Pool; PoolConfigurers will not be applied",
				this.pool.getName()));
		}
		else {
			logDebug("Pool [%s] not found; Lazily creating new Pool...", getName());
			applyPoolConfigurers();
		}
	}

	private Pool resolvePool(String name) {
		return getPoolResolver().resolve(name);
	}

	private String resolvePoolName() {

		if (!StringUtils.hasText(getName())) {
			setName(Optional.ofNullable(getBeanName())
				.filter(StringUtils::hasText)
				.orElseThrow(() -> newIllegalArgumentException("Pool name is required")));
		}

		return getName();
	}

	/**
	 * Applies the composite {@link PoolConfigurer} object to customize the configuration
	 * of this {@link PoolFactoryBean}.
	 *
	 * @see #applyPoolConfigurers(PoolConfigurer...)
	 * @see #getCompositePoolConfigurer()
	 */
	private void applyPoolConfigurers() {
		applyPoolConfigurers(getCompositePoolConfigurer());
	}

	/**
	 * Null-safe operation to apply the given array of {@link PoolConfigurer PoolConfigurers}
	 * to this {@link PoolFactoryBean}.
	 *
	 * @param poolConfigurers array of {@link PoolConfigurer PoolConfigurers} applied to this {@link PoolFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
	 * @see #applyPoolConfigurers(Iterable)
	 */
	protected void applyPoolConfigurers(PoolConfigurer... poolConfigurers) {
		applyPoolConfigurers(Arrays.asList(nullSafeArray(poolConfigurers, PoolConfigurer.class)));
	}

	/**
	 * Null-safe operation to apply the given {@link Iterable} of {@link PoolConfigurer PoolConfigurers}
	 * to this {@link PoolFactoryBean}.
	 *
	 * @param poolConfigurers {@link Iterable} of {@link PoolConfigurer PoolConfigurers}
	 * applied to this {@link PoolFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
	 */
	protected void applyPoolConfigurers(Iterable<PoolConfigurer> poolConfigurers) {
		stream(nullSafeIterable(poolConfigurers).spliterator(), false)
			.forEach(poolConfigurer -> poolConfigurer.configure(getName(), this));
	}

	/**
	 * Releases all system resources and destroys the {@link Pool} when created by this {@link PoolFactoryBean}.
	 *
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() {

		Optional.ofNullable(this.pool)
			.filter(this::isSpringManagedPool)
			.filter(pool -> !pool.isDestroyed())
			.ifPresent(pool -> {
				pool.releaseThreadLocalConnection();
				pool.destroy(this.keepAlive);
				setPool(null);
				logDebug("Destroyed Pool [%s]", pool.getName());
			});
	}

	private boolean isSpringManagedPool(Pool pool) {
		return this.springManagedPool;
	}

	/**
	 * Returns an object reference to the {@link Pool} created by this {@link PoolFactoryBean}.
	 *
	 * @return an object reference to the {@link Pool} created by this {@link PoolFactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 * @see org.apache.geode.cache.client.Pool
	 */
	@Override
	public Pool getObject() throws Exception {

		return Optional.ofNullable(this.pool).orElseGet(() -> {

			eagerlyInitializeClientCache();

			String poolName = getName();

			Pool namedPool = resolvePool(poolName);

			this.pool = namedPool != null ? namedPool
				: postProcess(create(postProcess(configure(initialize(createPoolFactory()))), poolName));

			return this.pool;
		});
	}

	/**
	 * Attempts to eagerly initialize the {@link ClientCache} if not already present so that a single
	 * {@link DistributedSystem} will exist, which is required to create a {@link Pool} instance.
	 *
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.apache.geode.distributed.DistributedSystem
	 * @see #isClientCachePresent()
	 */
	private void eagerlyInitializeClientCache() {

		if (!isClientCachePresent()) {
			getBeanFactory().getBean(ClientCache.class);
		}
	}

	/**
	 * Determines whether the {@link ClientCache} exists yet or not.
	 *
	 * @return a boolean value indicating whether the single {@link ClientCache} instance
	 * has been created yet.
	 * @see org.springframework.data.gemfire.GemfireUtils#getClientCache()
	 * @see org.apache.geode.distributed.DistributedSystem
	 * @see org.apache.geode.cache.client.ClientCache
	 */
	boolean isClientCachePresent() {

		return Optional.ofNullable(GemfireUtils.getClientCache())
			.filter(clientCache -> !clientCache.isClosed())
			.map(ClientCache::getDistributedSystem)
			.filter(GemfireUtils::isConnected)
			.isPresent();
	}

	/**
	 * Creates an instance of the {@link PoolFactory} interface to construct, configure and initialize a {@link Pool}.
	 *
	 * @return a {@link PoolFactory} implementation to create a {@link Pool}.
	 * @see org.apache.geode.cache.client.PoolManager#createFactory()
	 * @see org.apache.geode.cache.client.PoolFactory
	 */
	protected PoolFactory createPoolFactory() {
		return PoolManager.createFactory();
	}

	/**
	 * Configures the given {@link PoolFactory} from this {@link PoolFactoryBean}.
	 *
	 * @param poolFactory {@link PoolFactory} to configure.
	 * @return the given {@link PoolFactory}.
	 * @see org.apache.geode.cache.client.PoolFactory
	 */
	protected PoolFactory configure(PoolFactory poolFactory) {

		Optional.ofNullable(poolFactory).ifPresent(it -> {

			it.setFreeConnectionTimeout(this.freeConnectionTimeout);
			it.setIdleTimeout(this.idleTimeout);
			it.setLoadConditioningInterval(this.loadConditioningInterval);
			it.setMaxConnections(this.maxConnections);
			it.setMinConnections(this.minConnections);
			it.setMultiuserAuthentication(this.multiUserAuthentication);
			it.setPingInterval(this.pingInterval);
			it.setPRSingleHopEnabled(this.prSingleHopEnabled);
			it.setReadTimeout(this.readTimeout);
			it.setRetryAttempts(this.retryAttempts);
			it.setServerConnectionTimeout(this.serverConnectionTimeout);
			it.setServerGroup(this.serverGroup);
			it.setSocketBufferSize(this.socketBufferSize);
			it.setSocketConnectTimeout(this.socketConnectTimeout);
			it.setSocketFactory(getSocketFactory());
			it.setStatisticInterval(this.statisticInterval);
			it.setSubscriptionAckInterval(this.subscriptionAckInterval);
			it.setSubscriptionEnabled(this.subscriptionEnabled);
			it.setSubscriptionMessageTrackingTimeout(this.subscriptionMessageTrackingTimeout);
			it.setSubscriptionRedundancy(this.subscriptionRedundancy);
			it.setSubscriptionTimeoutMultiplier(this.subscriptionTimeoutMultiplier);
			it.setThreadLocalConnections(this.threadLocalConnections);

			CollectionUtils.nullSafeCollection(this.locators).forEach(locator ->
				it.addLocator(locator.getHost(), locator.getPort()));

			CollectionUtils.nullSafeCollection(this.servers).forEach(server ->
				it.addServer(server.getHost(), server.getPort()));
		});

		return poolFactory;
	}

	/**
	 * Initializes the given {@link PoolFactory} with any configured {@link PoolFactoryInitializer}.
	 *
	 * @param poolFactory {@link PoolFactory} to initialize.
	 * @return the initialized {@link PoolFactory}.
	 * @see org.apache.geode.cache.client.PoolFactory
	 */
	protected PoolFactory initialize(PoolFactory poolFactory) {

		return Optional.ofNullable(this.poolFactoryInitializer)
			.map(initializer -> initializer.initialize(poolFactory))
			.orElse(poolFactory);
	}

	/**
	 * Post processes the fully configured {@link PoolFactory}.
	 *
	 * @param poolFactory {@link PoolFactory} to post process.
	 * @return the post processed {@link PoolFactory}.
	 * @see org.apache.geode.cache.client.PoolFactory
	 */
	protected PoolFactory postProcess(PoolFactory poolFactory) {
		return poolFactory;
	}

	/**
	 * @deprecated Use {@link #createPool(PoolFactory, String)} instead.
	 */
	@Deprecated
	protected Pool create(PoolFactory poolFactory, String poolName) {
		return createPool(poolFactory, poolName);
	}

	/**
	 * Creates a {@link Pool} with the given {@link String name} using the provided {@link PoolFactory}.
	 *
	 * @param poolFactory {@link PoolFactory} used to create the {@link Pool}.
	 * @param poolName {@link String name} of the new {@link Pool}.
	 * @return a new instance of {@link Pool} with the given {@link String name}.
	 * @see org.apache.geode.cache.client.PoolFactory#create(String)
	 * @see org.apache.geode.cache.client.Pool
	 */
	protected Pool createPool(PoolFactory poolFactory, String poolName) {
		return poolFactory.create(poolName);
	}

	/**
	 * Post processes the {@link Pool} created by this {@link PoolFactoryBean}.
	 *
	 * @param pool {@link Pool} to post process.
	 * @return the post processed {@link Pool}.
	 * @see org.apache.geode.cache.client.Pool
	 */
	protected Pool postProcess(Pool pool) {
		return pool;
	}

	/**
	 * Returns the {@link Class type} of {@link Pool} produced by this {@link PoolFactoryBean}.
	 *
	 * @return the {@link Class type} of {@link Pool} produced by this {@link PoolFactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see org.apache.geode.cache.client.Pool
	 * @see java.lang.Class
	 */
	@Override
	public Class<?> getObjectType() {
		return this.pool != null ? this.pool.getClass() : Pool.class;
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
	 * Returns a reference to the {@literal Composite} {@link PoolConfigurer} used to apply additional configuration
	 * to this {@link PoolFactoryBean} on Spring container initialization.
	 *
	 * @return the {@literal Composite} {@link PoolConfigurer}.
	 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
	 */
	protected PoolConfigurer getCompositePoolConfigurer() {
		return this.compositePoolConfigurer;
	}

	/**
	 * Configures the {@link String name} of the {@link Pool} bean.
	 *
	 * @param name {@link String} containing the name for the {@link Pool} bean.
	 * @see #getName()
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the configured {@link String name} of the {@link Pool} bean.
	 *
	 * @return the configured {@link String name} of the {@link Pool} bean.
	 * @see #getBeanName()
	 * @see #setName(String)
	 */
	protected String getName() {
		return this.name;
	}

	/**
	 * Configures the {@link Pool} to be returned by this {@link PoolFactoryBean}.
	 *
	 * @param pool the {@link Pool} to be returned by this {@link PoolFactoryBean}.
	 * @see org.apache.geode.cache.client.Pool
	 */
	public void setPool(@Nullable Pool pool) {
		this.pool = pool;
	}

	/**
	 * Gets the {@link Pool} configured and built by this {@link PoolFactoryBean}.
	 *
	 * May return a proxy {@link Pool} if the actual {@link Pool} has not yet been configured and built by
	 * this {@link PoolFactoryBean}. In this case, the proxy {@link Pool} object will have the same configuration
	 * as the final {@link Pool} built by this {@link PoolFactoryBean}.
	 *
	 * @return the {@link Pool} configured and built by this {@link PoolFactoryBean}.
	 * @see org.apache.geode.cache.client.Pool
	 * @see #setPool(Pool)
	 */
	public @NonNull Pool getPool() {

		return Optional.ofNullable(this.pool).orElseGet(() -> new PoolAdapter() {

			@Override
			public boolean isDestroyed() {

				Pool pool = PoolFactoryBean.this.pool;

				return pool != null && pool.isDestroyed();
			}

			@Override
			public int getFreeConnectionTimeout() {
				return PoolFactoryBean.this.freeConnectionTimeout;
			}

			@Override
			public long getIdleTimeout() {
				return PoolFactoryBean.this.idleTimeout;
			}

			@Override
			public int getLoadConditioningInterval() {
				return PoolFactoryBean.this.loadConditioningInterval;
			}

			@Override
			public List<InetSocketAddress> getLocators() {
				return PoolFactoryBean.this.locators.toInetSocketAddresses();
			}

			@Override
			public List<InetSocketAddress> getOnlineLocators() {

				return Optional.ofNullable(PoolFactoryBean.this.pool)
					.map(Pool::getOnlineLocators)
					.orElseThrow(() -> newIllegalStateException("Pool [%s] has not been initialized", getName()));
			}

			@Override
			public int getMaxConnections() {
				return PoolFactoryBean.this.maxConnections;
			}

			@Override
			public int getMinConnections() {
				return PoolFactoryBean.this.minConnections;
			}

			@Override
			public boolean getMultiuserAuthentication() {
				return PoolFactoryBean.this.multiUserAuthentication;
			}

			@Override
			public String getName() {

				return Optional.ofNullable(PoolFactoryBean.this.getName())
					.filter(StringUtils::hasText)
					.orElseGet(PoolFactoryBean.this::getBeanName);
			}

			@Override
			public int getPendingEventCount() {

				return Optional.ofNullable(PoolFactoryBean.this.pool)
					.map(Pool::getPendingEventCount)
					.orElseThrow(() -> newIllegalStateException("Pool [%s] has not been initialized", getName()));
			}

			@Override
			public long getPingInterval() {
				return PoolFactoryBean.this.pingInterval;
			}

			@Override
			public boolean getPRSingleHopEnabled() {
				return PoolFactoryBean.this.prSingleHopEnabled;
			}

			@Override
			public QueryService getQueryService() {

				return Optional.ofNullable(PoolFactoryBean.this.pool)
					.map(Pool::getQueryService)
					.orElseThrow(() -> newIllegalStateException("Pool [%s] has not been initialized", getName()));
			}

			@Override
			public int getReadTimeout() {
				return PoolFactoryBean.this.readTimeout;
			}

			@Override
			public int getRetryAttempts() {
				return PoolFactoryBean.this.retryAttempts;
			}

			@Override
			public int getServerConnectionTimeout() {
				return PoolFactoryBean.this.serverConnectionTimeout;
			}

			@Override
			public String getServerGroup() {
				return PoolFactoryBean.this.serverGroup;
			}

			@Override
			public List<InetSocketAddress> getServers() {
				return PoolFactoryBean.this.servers.toInetSocketAddresses();
			}

			@Override
			public int getSocketBufferSize() {
				return PoolFactoryBean.this.socketBufferSize;
			}

			@Override
			public int getSocketConnectTimeout() {
				return PoolFactoryBean.this.socketConnectTimeout;
			}

			@Override
			public SocketFactory getSocketFactory() {
				return PoolFactoryBean.this.getSocketFactory();
			}

			@Override
			public int getStatisticInterval() {
				return PoolFactoryBean.this.statisticInterval;
			}

			@Override
			public int getSubscriptionAckInterval() {
				return PoolFactoryBean.this.subscriptionAckInterval;
			}

			@Override
			public boolean getSubscriptionEnabled() {
				return PoolFactoryBean.this.subscriptionEnabled;
			}

			@Override
			public int getSubscriptionMessageTrackingTimeout() {
				return PoolFactoryBean.this.subscriptionMessageTrackingTimeout;
			}

			@Override
			public int getSubscriptionRedundancy() {
				return PoolFactoryBean.this.subscriptionRedundancy;
			}

			@Override
			public int getSubscriptionTimeoutMultiplier() {
				return PoolFactoryBean.this.subscriptionTimeoutMultiplier;
			}

			@Override
			public boolean getThreadLocalConnections() {
				return PoolFactoryBean.this.threadLocalConnections;
			}

			@Override
			public void destroy() {
				destroy(false);
			}

			@Override
			public void destroy(boolean keepAlive) {

				try {
					PoolFactoryBean.this.destroy();
				}
				catch (Exception ignore) {
					Optional.ofNullable(PoolFactoryBean.this.pool).ifPresent(pool -> pool.destroy(keepAlive));
				}
			}

			@Override
			public void releaseThreadLocalConnection() {

				Optional.ofNullable(PoolFactoryBean.this.pool)
					.map(it -> {
						it.releaseThreadLocalConnection();
						return it;
					})
					.orElseThrow(() -> newIllegalStateException("Pool [%s] has not been initialized", getName()));
			}
		});
	}

	public void setFreeConnectionTimeout(int freeConnectionTimeout) {
		this.freeConnectionTimeout = freeConnectionTimeout;
	}

	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public void setLoadConditioningInterval(int loadConditioningInterval) {
		this.loadConditioningInterval = loadConditioningInterval;
	}

	public void setLocators(ConnectionEndpoint[] locators) {
		setLocators(ConnectionEndpointList.from(locators));
	}

	public void setLocators(Iterable<ConnectionEndpoint> locators) {
		getLocators().clear();
		getLocators().add(locators);
	}

	ConnectionEndpointList getLocators() {
		return this.locators;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	public void setMultiUserAuthentication(boolean multiUserAuthentication) {
		this.multiUserAuthentication = multiUserAuthentication;
	}

	public void setPingInterval(long pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * Null-safe operation to set an array of {@link PoolConfigurer PoolConfigurers} used to apply
	 * additional configuration to this {@link PoolFactoryBean} when using Annotation-based configuration.
	 *
	 * @param poolConfigurers array of {@link PoolConfigurer PoolConfigurers} used to apply
	 * additional configuration to this {@link PoolFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
	 * @see #setPoolConfigurers(List)
	 */
	public void setPoolConfigurers(PoolConfigurer... poolConfigurers) {
		setPoolConfigurers(Arrays.asList(nullSafeArray(poolConfigurers, PoolConfigurer.class)));
	}

	/**
	 * Null-safe operation to set an {@link Iterable} of {@link PoolConfigurer PoolConfigurers} used to apply
	 * additional configuration to this {@link PoolFactoryBean} when using Annotation-based configuration.
	 *
	 * @param poolConfigurers {@link Iterable} of {@link PoolConfigurer PoolConfigurers} used to apply
	 * additional configuration to this {@link PoolFactoryBean}.
	 * @see org.springframework.data.gemfire.config.annotation.PoolConfigurer
	 */
	public void setPoolConfigurers(List<PoolConfigurer> poolConfigurers) {
		this.poolConfigurers = Optional.ofNullable(poolConfigurers).orElseGet(Collections::emptyList);
	}

	/**
	 * Sets the {@link PoolFactoryInitializer} to initialize the {@link PoolFactory} used by
	 * this {@link PoolFactoryBean} to create a {@link Pool}.
	 *
	 * @param poolFactoryInitializer {@link PoolFactoryInitializer} user provided callback interface invoked
	 * by this {@link PoolFactoryBean} to initialize the {@link PoolFactory} constructed to create the {@link Pool}.
	 * @see org.springframework.data.gemfire.client.PoolFactoryBean.PoolFactoryInitializer
	 */
	public void setPoolFactoryInitializer(PoolFactoryInitializer poolFactoryInitializer) {
		this.poolFactoryInitializer = poolFactoryInitializer;
	}

	/**
	 * Configures the {@link PoolResolver} to resolve {@link Pool} objects by {@link String name}
	 * from the Apache Geode cache.
	 *
	 * @param poolResolver the configured {@link PoolResolver} used to resolve {@link Pool} objects
	 * by {@link String name}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	public void setPoolResolver(PoolResolver poolResolver) {
		this.poolResolver = poolResolver;
	}

	/**
	 * Returns the configured {@link PoolResolver} used to resolve {@link Pool} object by {@link String name}.
	 *
	 * @return the configured {@link PoolResolver}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	public PoolResolver getPoolResolver() {

		PoolResolver poolResolver = this.poolResolver;

		return poolResolver != null ? poolResolver : DEFAULT_POOL_RESOLVER;
	}

	public void setPrSingleHopEnabled(boolean prSingleHopEnabled) {
		this.prSingleHopEnabled = prSingleHopEnabled;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public void setRetryAttempts(int retryAttempts) {
		this.retryAttempts = retryAttempts;
	}

	public void setServerConnectionTimeout(int serverConnectionTimeout) {
		this.serverConnectionTimeout = serverConnectionTimeout;
	}

	public void setServerGroup(String serverGroup) {
		this.serverGroup = serverGroup;
	}

	public void setServers(ConnectionEndpoint[] servers) {
		setServers(ConnectionEndpointList.from(servers));
	}

	public void setServers(Iterable<ConnectionEndpoint> servers) {
		getServers().clear();
		getServers().add(servers);
	}

	ConnectionEndpointList getServers() {
		return this.servers;
	}

	public void setSocketBufferSize(int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public void setSocketConnectTimeout(int socketConnectTimeout) {
		this.socketConnectTimeout = socketConnectTimeout;
	}

	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	protected SocketFactory getSocketFactory() {
		return this.socketFactory != null ? this.socketFactory : PoolFactory.DEFAULT_SOCKET_FACTORY;
	}

	public void setStatisticInterval(int statisticInterval) {
		this.statisticInterval = statisticInterval;
	}

	public void setSubscriptionAckInterval(int subscriptionAckInterval) {
		this.subscriptionAckInterval = subscriptionAckInterval;
	}

	public void setSubscriptionEnabled(boolean subscriptionEnabled) {
		this.subscriptionEnabled = subscriptionEnabled;
	}

	public void setSubscriptionMessageTrackingTimeout(int subscriptionMessageTrackingTimeout) {
		this.subscriptionMessageTrackingTimeout = subscriptionMessageTrackingTimeout;
	}

	public void setSubscriptionRedundancy(int subscriptionRedundancy) {
		this.subscriptionRedundancy = subscriptionRedundancy;
	}

	public void setSubscriptionTimeoutMultiplier(int subscriptionTimeoutMultiplier) {
		this.subscriptionTimeoutMultiplier = subscriptionTimeoutMultiplier;
	}

	@Deprecated
	public void setThreadLocalConnections(boolean threadLocalConnections) {
		this.threadLocalConnections = threadLocalConnections;
	}

	public void setXmlDeclaredLocators(ConnectionEndpointList xmlDeclaredLocators) {
		this.xmlDeclaredLocators.add(xmlDeclaredLocators);
	}

	public void setXmlDeclaredServers(ConnectionEndpointList xmlDeclaredServers) {
		this.xmlDeclaredServers.add(xmlDeclaredServers);
	}

	/**
	 * Callback interface to initialize the {@link PoolFactory} used by this {@link PoolFactoryBean}
	 * to create a {@link Pool} by providing additional or alternative configuration for the factory.
	 *
	 * @see org.apache.geode.cache.client.PoolFactory
	 */
	public interface PoolFactoryInitializer {

		/**
		 * Initializes the given {@link PoolFactory}.
		 *
		 * @param poolFactory {@link PoolFactory} to initialize.
		 * @return the given {@link PoolFactory}.
		 * @see org.apache.geode.cache.client.PoolFactory
		 */
		PoolFactory initialize(PoolFactory poolFactory);

	}
}
