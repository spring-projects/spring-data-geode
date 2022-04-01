/*
 * Copyright 2012-2022 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.AllConnectionsInUseException;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;
import org.apache.geode.cache.client.SocketFactory;
import org.apache.geode.cache.control.ResourceManager;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;

/**
 * The {@link ClientCacheApplication} annotation enables a Spring Data for Apache Geode based application
 * to become an Apache Geode cache client by creating a {@link ClientCache} instance.
 *
 * @author John Blum
 * @see java.lang.annotation.Documented
 * @see java.lang.annotation.Inherited
 * @see java.lang.annotation.Retention
 * @see java.lang.annotation.Target
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolFactory
 * @see org.apache.geode.cache.client.SocketFactory
 * @see org.apache.geode.cache.control.ResourceManager
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfiguration
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @since 1.9.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Configuration
@Import(ClientCacheConfiguration.class)
@SuppressWarnings("unused")
public @interface ClientCacheApplication {

	/**
	 * Configures cache {@literal copy-on-read} functionality, which copies the value when it is read from the cache.
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use {@literal spring.data.gemfire.cache.copy-on-read} property in {@literal application.properties}.
	 */
	boolean copyOnRead() default false;

	/**
	 * Configures the percentage of heap at or above which the cache is considered to be in danger
	 * of becoming inoperable.
	 *
	 * Defaults to {@link ResourceManager#DEFAULT_CRITICAL_PERCENTAGE}.
	 *
	 * Use {@literal spring.data.gemfire.cache.critical-heap-percentage} property in {@literal application.properties}.
	 */
	float criticalHeapPercentage() default ResourceManager.DEFAULT_CRITICAL_PERCENTAGE;

	/**
	 * Configures the percentage of off-heap at or above which the cache is considered to be in danger
	 * of becoming inoperable.
	 *
	 * Defaults to {@literal 0.0}.
	 *
	 * Use {@literal spring.data.gemfire.cache.critical-off-heap-percentage} property
	 * in {@literal application.properties}.
	 */
	float criticalOffHeapPercentage() default 0.0f;

	/**
	 * Configures the ID for a durable client in a client/server topology.
	 *
	 * If set, the durable client ID indicates a client is durable so messages persist while the client is offline.
	 * The ID is used by servers to identify clients and reestablish any messaging that was interrupted by client
	 * downtime.
	 *
	 * Use {@literal spring.data.gemfire.cache.client.durable-client-id} property in {@literal application.properties}.
	 */
	String durableClientId() default "";

	/**
	 * Configures the number of seconds that a durable client can remain disconnected from a server cluster
	 * and for the servers to continue to accumulate (durable) events for the client while offline.
	 *
	 * Defaults to {@literal 300 seconds}, or {@literal 5 minutes}.
	 *
	 * Use {@literal spring.data.gemfire.cache.client.durable-client-timeout} property
	 * in {@literal application.properties}.
	 */
	int durableClientTimeout() default 300;

	/**
	 * Configures the percentage of heap at or above which eviction should begin on Regions configured
	 * for {@literal Heap LRU eviction}.
	 *
	 * Defaults to {@link ResourceManager#DEFAULT_EVICTION_PERCENTAGE}.
	 *
	 * Use {@literal spring.data.gemfire.cache.eviction-heap-percentage} property in {@literal application.properties}.
	 */
	float evictionHeapPercentage() default ResourceManager.DEFAULT_EVICTION_PERCENTAGE;

	/**
	 * Configures the percentage of off-heap at or above which eviction should begin on Regions configured
	 * for {@literal Heap LRU eviction}.
	 *
	 * Defaults to {@literal 0.0}.
	 *
	 * Use {@literal spring.data.gemfire.cache.eviction-off-heap-percentage} property
	 * in {@literal application.properties}.
	 */
	float evictionOffHeapPercentage() default 0.0f;

	/**
	 * Configures the free connection timeout for the {@literal DEFAULT} {@link Pool}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_FREE_CONNECTION_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.free-connection-timeout} property
	 * or the {@literal spring.data.gemfire.pool.free-connection-timeout} property
	 * in {@literal application.properties}.
	 */
	int freeConnectionTimeout() default PoolFactory.DEFAULT_FREE_CONNECTION_TIMEOUT;

	/**
	 * Configures the amount of time that a connection can remain idle before expiring the connection.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_IDLE_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.idle-timeout} property
	 * or the {@literal spring.data.gemfire.pool.idle-timeout} property
	 * in {@literal application.properties}.
	 */
	long idleTimeout() default PoolFactory.DEFAULT_IDLE_TIMEOUT;

	/**
	 * Configures whether to keep the client event queues alive on the server when the client is disconnected.
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use {@literal spring.data.gemfire.cache.client.keep-alive} property in {@literal application.properties}.
	 */
	boolean keepAlive() default false;

	/**
	 * Configures the load conditioning interval for the {@literal DEFAULT} {@link Pool}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_LOAD_CONDITIONING_INTERVAL}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.load-conditioning-interval} property
	 * or the {@literal spring.data.gemfire.pool.load-conditioning-interval} property
	 * in {@literal application.properties}.
	 */
	int loadConditioningInterval() default PoolFactory.DEFAULT_LOAD_CONDITIONING_INTERVAL;

	/**
	 * Configures the {@link org.apache.geode.distributed.Locator Locators} to which this cache client will connect.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.locators} property
	 * or the {@literal spring.data.gemfire.pool.locators} property
	 * in {@literal application.properties}.
	 */
	Locator[] locators() default {};

	/**
	 * Configures the {@literal log-level} used to output log messages at runtime.
	 *
	 * Defaults to {@literal config}.
	 *
	 * Use {@literal spring.data.gemfire.cache.log-level} property in {@literal application.properties}.
	 * @deprecated Apache Geode cache logging can only be configured using a logging provider (e.g. Log4j).
	 */
	@Deprecated
	String logLevel() default ClientCacheConfiguration.DEFAULT_LOG_LEVEL;

	/**
	 * Configures the maximum number of connections between the client and server that the {@literal DEFAULT}
	 * {@link Pool} will create.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_MAX_CONNECTIONS}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.max-connections} property
	 * or the {@literal spring.data.gemfire.pool.max-connections} property
	 * in {@literal application.properties}.
	 */
	int maxConnections() default PoolFactory.DEFAULT_MAX_CONNECTIONS;

	/**
	 * Configures the minimum number of connections between the client and server to keep alive and available
	 * at all times.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_MIN_CONNECTIONS}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.min-connections} property
	 * or the {@literal spring.data.gemfire.pool.min-connections} property
	 * in {@literal application.properties}.
	 */
	int minConnections() default PoolFactory.DEFAULT_MIN_CONNECTIONS;

	/**
	 * If set to {@literal true} then the {@literal DEFAULT} {@link Pool} can be used by multiple users.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_MULTIUSER_AUTHENTICATION}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.multi-user-authentication} property
	 * or the {@literal spring.data.gemfire.pool.multi-user-authentication} property
	 * in {@literal application.properties}.
	 */
	boolean multiUserAuthentication() default PoolFactory.DEFAULT_MULTIUSER_AUTHENTICATION;

	/**
	 * Configures the name of {@literal this} Apache Geode member in the cluster (distributed system).
	 *
	 * Defaults to {@literal SpringBasedClientCacheApplication}.
	 *
	 * Use either {@literal spring.data.gemfire.name} or the {@literal spring.data.gemfire.cache.name} property
	 * in {@literal application.properties}.
	 */
	String name() default ClientCacheConfiguration.DEFAULT_NAME;

	/**
	 * Configures how often to ping servers and verify that the servers are still alive and responsive.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_PING_INTERVAL}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.ping-interval} property
	 * or the {@literal spring.data.gemfire.pool.ping-interval} property
	 * in {@literal application.properties}.
	 */
	long pingInterval() default PoolFactory.DEFAULT_PING_INTERVAL;

	/**
	 * Configures {@code prSingleHopEnabled} functionality for the {@literal DEFAULT} {@link Pool}.
	 *
	 * When {@literal true} the client will be aware of the location of all partitions on servers
	 * hosting {@link org.apache.geode.cache.DataPolicy#PARTITION} {@link Region Regions}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_PR_SINGLE_HOP_ENABLED}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.pr-single-hop-enabled} property
	 * or the {@literal spring.data.gemfire.pool.pr-single-hop-enabled} property
	 * in {@literal application.properties}.
	 */
	boolean prSingleHopEnabled() default PoolFactory.DEFAULT_PR_SINGLE_HOP_ENABLED;

	/**
	 * Configures the number of milliseconds to wait for a response from a server before timing out the operation
	 * and trying another server (if any are available).
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_READ_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.read-timeout} property
	 * or the {@literal spring.data.gemfire.pool.read-timeout} property
	 * in {@literal application.properties}.
	 */
	int readTimeout() default PoolFactory.DEFAULT_READ_TIMEOUT;

	/**
	 * Configures whether to notify servers (cluster) at runtime after startup that this durable client is ready
	 * to receive updates and events.
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.ready-for-events} property
	 * or the {@literal spring.data.gemfire.pool.ready-for-events} property
	 * in {@literal application.properties}.
	 */
	boolean readyForEvents() default false;

	/**
	 * Configures the number of times to retry a request after timeout or an {@link Exception} occurs while performing
	 * the cache operation between the client and server.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_RETRY_ATTEMPTS}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.retry-attempts} property
	 * or the {@literal spring.data.gemfire.pool.retry-attempts} property
	 * in {@literal application.properties}.
	 */
	int retryAttempts() default PoolFactory.DEFAULT_RETRY_ATTEMPTS;

	/**
	 * Configures the server connection timeout for the {@literal DEFAULT} {@literal Pool}.
	 *
	 * If the {@link Pool} has a max connections setting, operations will block if there are no free connections to
	 * a specific server. The server connection timeout specifies how long those operations will block waiting for
	 * a free connection tro a specific server before receiving an {@link AllConnectionsInUseException}. If max
	 * connections is not set then this setting has no effect. This setting differs from {@link #freeConnectionTimeout()},
	 * which sets the wait time for any server connection in the {@literal DEFAULT} {@link Pool}, whereas this setting
	 * sets the wait time for a free connection to a specific server.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SERVER_CONNECTION_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.server-connection-timeout} property
	 * or the {@literal spring.data.gemfire.pool.server-connection-timeout} property
	 * in {@literal application.properties}.
	 */
	int serverConnectionTimeout() default PoolFactory.DEFAULT_SERVER_CONNECTION_TIMEOUT;

	/**
	 * Configures the {@link String name} of the group that all servers in which the {@literal DEFAULT} {@link Pool}
	 * connects to must belong to.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SERVER_GROUP}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.server-group} property
	 * or the {@literal spring.data.gemfire.pool.server-group} property
	 * in {@literal application.properties}.
	 */
	String serverGroup() default PoolFactory.DEFAULT_SERVER_GROUP;

	/**
	 * Configures the {@link org.apache.geode.cache.server.CacheServer CacheServers} to which this cache client
	 * will connect.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.servers} property
	 * or the {@literal spring.data.gemfire.pool.servers} property
	 * in {@literal application.properties}.
	 */
	Server[] servers() default {};

	/**
	 * Configures the {@link java.net.Socket} buffer size used for each connection made in
	 * the {@literal DEFAULT} {@link Pool}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SOCKET_BUFFER_SIZE}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.socket-buffer-size} property
	 * or the {@literal spring.data.gemfire.pool.socket-buffer-size} property
	 * in {@literal application.properties}.
	 */
	int socketBufferSize() default PoolFactory.DEFAULT_SOCKET_BUFFER_SIZE;

	/**
	 * Configures the {@link java.net.Socket} connect timeout used by the {@literal DEFAULT} {@link Pool}.
	 *
	 * Configures the number of milliseconds used as the {@link java.net.Socket} timeout when the client connects to
	 * the servers and Locators in the cluster. A timeout of zero is interpreted as an infinite timeout. The connection
	 * will then block until established or an error occurs.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SOCKET_CONNECT_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.socket-connect-timeout} property
	 * or the {@literal spring.data.gemfire.pool.socket-connect-timeout} property
	 * in {@literal application.properties}.
	 */
	int socketConnectTimeout() default PoolFactory.DEFAULT_SOCKET_CONNECT_TIMEOUT;

	/**
	 * Configures the {@link SocketFactory} {@link String bean name} used by the {@literal DEFAULT} {@link Pool}
	 * to create {@link java.net.Socket} connections to both Locators (if configured using {@link #locators()})
	 * and Servers in the Apache Geodce cluster.
	 *
	 * Defaults to unset.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.socket-factory-bean-name} property
	 * or the {@literal spring.data.gemfire.pool.socket-factory-bean-name} property
	 * in {@literal application.properties}.
	 */
	String socketFactoryBeanName() default "";

	/**
	 * Configures how often to send client statistics to the server.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_STATISTIC_INTERVAL}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.statistic-interval} property
	 * or the {@literal spring.data.gemfire.pool.statistic-interval} property
	 * in {@literal application.properties}.
	 */
	int statisticInterval() default PoolFactory.DEFAULT_STATISTIC_INTERVAL;

	/**
	 * Configures the interval in milliseconds to wait before sending acknowledgements from the client to the server
	 * for events received from server subscriptions.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SUBSCRIPTION_ACK_INTERVAL}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.subscription-ack-interval} property
	 * or the {@literal spring.data.gemfire.pool.subscription-ack-interval} property
	 * in {@literal application.properties}.
	 */
	int subscriptionAckInterval() default PoolFactory.DEFAULT_SUBSCRIPTION_ACK_INTERVAL;

	/**
	 * Configures server-to-client subscriptions when set to {@literal true} (enabled)
	 * in the {@literal DEFAULT} {@link Pool}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SUBSCRIPTION_ENABLED}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.subscription-enabled} property
	 * or the {@literal spring.data.gemfire.pool.subscription-enabled} property
	 * in {@literal application.properties}.
	 */
	boolean subscriptionEnabled() default PoolFactory.DEFAULT_SUBSCRIPTION_ENABLED;

	/**
	 * Configures the message tracking timeout attribute of the {@literal DEFAULT} {@link Pool}, which is
	 * the time-to-live period in milliseconds for subscription events the client has received from the server.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SUBSCRIPTION_MESSAGE_TRACKING_TIMEOUT}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.subscription-message-tracking-timeout} property
	 * or the {@literal spring.data.gemfire.pool.subscription-message-tracking-timeout} property
	 * in {@literal application.properties}.
	 */
	int subscriptionMessageTrackingTimeout() default PoolFactory.DEFAULT_SUBSCRIPTION_MESSAGE_TRACKING_TIMEOUT;

	/**
	 * Configures the redundancy-level for the {@literal DEFAULT} {@link Pool}'s server-to-client subscriptions.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_SUBSCRIPTION_REDUNDANCY}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.subscription-redundancy} property
	 * or the {@literal spring.data.gemfire.pool.subscription-redundancy} property
	 * in {@literal application.properties}.
	 */
	int subscriptionRedundancy() default PoolFactory.DEFAULT_SUBSCRIPTION_REDUNDANCY;

	/**
	 * Configures the {@link ThreadLocal} connection policy for the {@literal DEFAULT} {@link Pool}.
	 *
	 * Defaults to {@link PoolFactory#DEFAULT_THREAD_LOCAL_CONNECTIONS}.
	 *
	 * Use either the {@literal spring.data.gemfire.pool.default.thread-local-connections} property
	 * or the {@literal spring.data.gemfire.pool.thread-local-connections} property
	 * in {@literal application.properties}.
	 * @deprecated
	 */
	@Deprecated
	boolean threadLocalConnections() default PoolFactory.DEFAULT_THREAD_LOCAL_CONNECTIONS;

	/**
	 * Configures whether the {@link GemfireBeanFactoryLocator} should be enabled to lookup the Spring
	 * {@link BeanFactory} to auto-wire, configure and initialize Apache Geode components created in
	 * a non-Spring managed, Apache Geode context (for example: {@literal cache.xml}).
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use {@literal spring.data.gemfire.use-bean-factory-locator} property in {@literal application.properties}.
	 */
	boolean useBeanFactoryLocator() default false;

	@interface Locator {

		String host() default "localhost";

		int port() default GemfireUtils.DEFAULT_LOCATOR_PORT;

	}

	@interface Server {

		String host() default "localhost";

		int port() default GemfireUtils.DEFAULT_CACHE_SERVER_PORT;

	}
}
