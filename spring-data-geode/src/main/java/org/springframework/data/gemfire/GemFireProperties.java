/*
 * Copyright 2020-2021 the original author or authors.
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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.io.File;
import java.util.Arrays;

import org.apache.geode.distributed.ConfigurationProperties;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An Enum (enumeration) of Apache Geode {@literal gemfire.properties}.
 *
 * @author John Blum
 * @see org.apache.geode.distributed.ConfigurationProperties
 * @since 2.3.0
 */
@SuppressWarnings("unused")
public enum GemFireProperties {

	ACK_SEVERE_ALERT_THRESHOLD(ConfigurationProperties.ACK_SEVERE_ALERT_THRESHOLD, Long.class, 0),
	ACK_WAIT_THRESHOLD(ConfigurationProperties.ACK_WAIT_THRESHOLD, Long.class, 15),
	ARCHIVE_DISK_SPACE_LIMIT(ConfigurationProperties.ARCHIVE_DISK_SPACE_LIMIT, Integer.class, 0),
	ARCHIVE_FILE_SIZE_LIMIT(ConfigurationProperties.ARCHIVE_FILE_SIZE_LIMIT, Integer.class, 0),
	ASYNC_DISTRIBUTION_TIMEOUT(ConfigurationProperties.ASYNC_DISTRIBUTION_TIMEOUT, Long.class, 0),
	ASYNC_MAX_QUEUE_SIZE(ConfigurationProperties.ASYNC_MAX_QUEUE_SIZE, Integer.class, 8),
	ASYNC_QUEUE_TIMEOUT(ConfigurationProperties.ASYNC_QUEUE_TIMEOUT, Long.class, 60000),
	BIND_ADDRESS(ConfigurationProperties.BIND_ADDRESS, String.class),
	CACHE_XML_FILE(ConfigurationProperties.CACHE_XML_FILE, String.class),
	CONFLATE_EVENTS(ConfigurationProperties.CONFLATE_EVENTS, String.class, "server"),
	CONSERVE_SOCKETS(ConfigurationProperties.CONSERVE_SOCKETS, Boolean.class, true),
	DELTA_PROPAGATION(ConfigurationProperties.DELTA_PROPAGATION, Boolean.class, true),
	DEPLOY_WORKING_DIRECTORY(ConfigurationProperties.DEPLOY_WORKING_DIR, File.class, new File(".")),
	DISABLE_AUTO_RECONNECT(ConfigurationProperties.DISABLE_AUTO_RECONNECT, Boolean.class, false),
	DISABLE_JMX(ConfigurationProperties.DISABLE_JMX, Boolean.class, false),
	DISABLE_TCP(ConfigurationProperties.DISABLE_TCP, Boolean.class, false),
	DISTRIBUTED_SYSTEM_ID(ConfigurationProperties.DISTRIBUTED_SYSTEM_ID, Integer.class, -1),
	DISTRIBUTED_TRANSACTIONS(ConfigurationProperties.DISTRIBUTED_TRANSACTIONS, Boolean.class, false),
	DURABLE_CLIENT_ID(ConfigurationProperties.DURABLE_CLIENT_ID, String.class),
	DURABLE_CLIENT_TIMEOUT(ConfigurationProperties.DURABLE_CLIENT_TIMEOUT, Long.class, 300L),
	ENABLE_CLUSTER_CONFIGURATION(ConfigurationProperties.ENABLE_CLUSTER_CONFIGURATION, Boolean.class, true),
	ENABLE_MANAGEMENT_REST_SERVICE(ConfigurationProperties.ENABLE_MANAGEMENT_REST_SERVICE, Boolean.class,true),
	ENABLE_NETWORK_PARTITION_DETECTION(ConfigurationProperties.ENABLE_NETWORK_PARTITION_DETECTION, Boolean.class, true),
	ENABLE_TIME_STATISTICS(ConfigurationProperties.ENABLE_TIME_STATISTICS, Boolean.class, false),
	ENFORCE_UNIQUE_HOST(ConfigurationProperties.ENFORCE_UNIQUE_HOST, Boolean.class, false),
	//GEODE_DISALLOW_INTERNAL_MESSAGES_WITHOUT_CREDENTIALS("geode.disallow-internal-messages-without-credentials", Boolean.class, false),
	GROUPS(ConfigurationProperties.GROUPS, String.class),
	HTTP_SERVICE_BIND_ADDRESS(ConfigurationProperties.HTTP_SERVICE_BIND_ADDRESS, String.class),
	HTTP_SERVICE_PORT(ConfigurationProperties.HTTP_SERVICE_PORT, Integer.class, 7070),
	JMX_MANAGER(ConfigurationProperties.JMX_MANAGER, Boolean.class, false),
	JMX_MANAGER_ACCESS_FILE(ConfigurationProperties.JMX_MANAGER_ACCESS_FILE, File.class),
	JMX_MANAGER_BIND_ADDRESS(ConfigurationProperties.JMX_MANAGER_BIND_ADDRESS, String.class),
	JMX_MANAGER_HOSTNAME_FOR_CLIENTS(ConfigurationProperties.JMX_MANAGER_HOSTNAME_FOR_CLIENTS, String.class),
	JMX_MANAGER_PASSWORD_FILE(ConfigurationProperties.JMX_MANAGER_PASSWORD_FILE, File.class),
	JMX_MANAGER_PORT(ConfigurationProperties.JMX_MANAGER_PORT, Integer.class, 1099),
	JMX_MANAGER_START(ConfigurationProperties.JMX_MANAGER_START, Boolean.class, false),
	JMX_MANAGER_UPDATE_RATE(ConfigurationProperties.JMX_MANAGER_UPDATE_RATE, Long.class, 2000L),
	LOAD_CLUSTER_CONFIGURATION_FROM_DIR(ConfigurationProperties.LOAD_CLUSTER_CONFIGURATION_FROM_DIR, Boolean.class, false),
	LOCATOR_WAIT_TIME(ConfigurationProperties.LOCATOR_WAIT_TIME, Long.class, 0),
	LOCATORS(ConfigurationProperties.LOCATORS, String.class),
	LOCK_MEMORY(ConfigurationProperties.LOCK_MEMORY, Boolean.class, false),
	LOG_DISK_SPACE_LIMIT(ConfigurationProperties.LOG_DISK_SPACE_LIMIT, Integer.class, 0),
	LOG_FILE(ConfigurationProperties.LOG_FILE, String.class),
	LOG_FILE_SIZE_LIMIT(ConfigurationProperties.LOG_FILE_SIZE_LIMIT, Integer.class, 0),
	LOG_LEVEL(ConfigurationProperties.LOG_LEVEL, String.class, "config"),
	MAX_NUM_RECONNECT_TRIES(ConfigurationProperties.MAX_NUM_RECONNECT_TRIES, Integer.class, 3),
	MAX_WAIT_TIME_RECONNECT(ConfigurationProperties.MAX_WAIT_TIME_RECONNECT, Long.class, 60000),
	MCAST_ADDRESS(ConfigurationProperties.MCAST_ADDRESS, String.class, "239.192.81.1"),
	MCAST_FLOW_CONTROL(ConfigurationProperties.MCAST_FLOW_CONTROL, String.class, "1048576,0.25, 5000"),
	MCAST_PORT(ConfigurationProperties.MCAST_PORT, Integer.class, 10334),
	MCAST_RECV_BUFFER_SIZE(ConfigurationProperties.MCAST_RECV_BUFFER_SIZE, Integer.class, 1048576),
	MCAST_SEND_BUFFER_SIZE(ConfigurationProperties.MCAST_SEND_BUFFER_SIZE, Integer.class, 65535),
	MCAST_TTL(ConfigurationProperties.MCAST_TTL, Integer.class, 32),
	MEMBER_TIMEOUT(ConfigurationProperties.MEMBER_TIMEOUT, Long.class, 5000L),
	MEMBERSHIP_PORT_RANGE(ConfigurationProperties.MEMBERSHIP_PORT_RANGE, String.class, "41000-61000"),
	MEMCACHED_BIND_ADDRESS(ConfigurationProperties.MEMCACHED_BIND_ADDRESS, String.class),
	MEMCACHED_PORT(ConfigurationProperties.MEMCACHED_PORT, Integer.class, 0),
	MEMCACHED_PROTOCOL(ConfigurationProperties.MEMCACHED_PROTOCOL, String.class, "ASCII"),
	NAME(ConfigurationProperties.NAME, String.class),
	OFF_HEAP_MEMORY_SIZE(ConfigurationProperties.OFF_HEAP_MEMORY_SIZE, Integer.class),
	REDUNDANCY_ZONE(ConfigurationProperties.REDUNDANCY_ZONE, String.class),
	REDIS_BIND_ADDRESS(ConfigurationProperties.REDIS_BIND_ADDRESS, String.class),
	REDIS_PASSWORD(ConfigurationProperties.REDIS_PASSWORD, String.class),
	REDIS_PORT(ConfigurationProperties.REDIS_PORT, Integer.class, 0),
	REMOTE_LOCATORS(ConfigurationProperties.REMOTE_LOCATORS, String.class),
	REMOVE_UNRESPONSIVE_CLIENT(ConfigurationProperties.REMOVE_UNRESPONSIVE_CLIENT, Boolean.class, false),
	SECURITY_AUTH_TOKEN_ENABLED_COMPONENTS(ConfigurationProperties.SECURITY_AUTH_TOKEN_ENABLED_COMPONENTS, String[].class),
	SECURITY_CLIENT_AUTH_INIT(ConfigurationProperties.SECURITY_CLIENT_AUTH_INIT, String.class),
	SECURITY_LOG_FILE(ConfigurationProperties.SECURITY_LOG_FILE, File.class),
	SECURITY_LOG_LEVEL(ConfigurationProperties.SECURITY_LOG_LEVEL, String.class, "config"),
	SECURITY_MANAGER(ConfigurationProperties.SECURITY_MANAGER, String.class),
	SECURITY_PEER_AUTH_INIT(ConfigurationProperties.SECURITY_PEER_AUTH_INIT, String.class),
	SECURITY_PEER_VERIFY_MEMBER_TIMEOUT(ConfigurationProperties.SECURITY_PEER_VERIFY_MEMBER_TIMEOUT, Long.class, 1000L),
	SECURITY_POST_PROCESSOR(ConfigurationProperties.SECURITY_POST_PROCESSOR, String.class),
	SECURITY_SHIRO_INIT(ConfigurationProperties.SECURITY_SHIRO_INIT, String.class),
	SECURITY_UDP_DHALO(ConfigurationProperties.SECURITY_UDP_DHALGO, String.class),
	SERIALIZABLE_OBJECT_FILTER(ConfigurationProperties.SERIALIZABLE_OBJECT_FILTER, String.class, "!*"),
	SERVER_BIND_ADDRESS(ConfigurationProperties.SERVER_BIND_ADDRESS, String.class),
	SOCKET_BUFFER_SIZE(ConfigurationProperties.SOCKET_BUFFER_SIZE, Integer.class, 32768),
	SOCKET_LEASE_TIME(ConfigurationProperties.SOCKET_LEASE_TIME, Long.class, 60000L),
	SSL_ENABLED_COMPONENTS(ConfigurationProperties.SSL_ENABLED_COMPONENTS, String.class, "all"),
	SSL_ENDPOINT_IDENTIFICATION_ENABLED(ConfigurationProperties.SSL_ENDPOINT_IDENTIFICATION_ENABLED, Boolean.class, false),
	SSL_REQUIRE_AUTHENTICATION(ConfigurationProperties.SSL_REQUIRE_AUTHENTICATION, Boolean.class, true),
	SSL_CIPHERS(ConfigurationProperties.SSL_CIPHERS, String.class, "any"),
	SSL_CLUSTER_ALIAS(ConfigurationProperties.SSL_CLUSTER_ALIAS, String.class),
	SSL_DEFAULT_ALIAS(ConfigurationProperties.SSL_DEFAULT_ALIAS, String.class),
	SSL_GATEWAY_ALIAS(ConfigurationProperties.SSL_GATEWAY_ALIAS, String.class),
	SSL_JMX_ALIAS(ConfigurationProperties.SSL_JMX_ALIAS, String.class),
	SSL_LOCATOR_ALIAS(ConfigurationProperties.SSL_LOCATOR_ALIAS, String.class),
	SSL_PARAMETER_EXTENSION(ConfigurationProperties.SSL_PARAMETER_EXTENSION, String.class),
	SSL_SERVER_ALIAS(ConfigurationProperties.SSL_SERVER_ALIAS, String.class),
	SSL_WEB_ALIAS(ConfigurationProperties.SSL_WEB_ALIAS, String.class),
	SSL_WEB_SERVICE_REQUIRE_AUTHENTICATION(ConfigurationProperties.SSL_WEB_SERVICE_REQUIRE_AUTHENTICATION, Boolean.class, false),
	SSL_KEYSTORE(ConfigurationProperties.SSL_KEYSTORE, String.class),
	SSL_KEYSTORE_PASSWORD(ConfigurationProperties.SSL_KEYSTORE_PASSWORD, String.class),
	SSL_KEYSTORE_TYPE(ConfigurationProperties.SSL_KEYSTORE_TYPE, String.class, "JKS"),
	SSL_PROTOCOLS(ConfigurationProperties.SSL_PROTOCOLS, String.class, "any"),
	SSL_TRUSTSTORE(ConfigurationProperties.SSL_TRUSTSTORE, String.class),
	SSL_TRUSTSTORE_PASSWORD(ConfigurationProperties.SSL_TRUSTSTORE_PASSWORD, String.class),
	SSL_TRUSTSTORE_TYPE(ConfigurationProperties.SSL_TRUSTSTORE_TYPE, String.class, "JKS"),
	SSL_USE_DEFAULT_CONTEXT(ConfigurationProperties.SSL_USE_DEFAULT_CONTEXT, Boolean.class, false),
	START_DEV_REST_API(ConfigurationProperties.START_DEV_REST_API, Boolean.class, false),
	START_LOCATOR(ConfigurationProperties.START_LOCATOR, Boolean.class),
	STATISTIC_ARCHIVE_FILE(ConfigurationProperties.STATISTIC_ARCHIVE_FILE, File.class),
	STATISTIC_SAMPLE_RATE(ConfigurationProperties.STATISTIC_SAMPLE_RATE, Long.class, 1000),
	STATISTIC_SAMPLING_ENABLED(ConfigurationProperties.STATISTIC_SAMPLING_ENABLED, Boolean.class, false),
	TCP_PORT(ConfigurationProperties.TCP_PORT, Integer.class, 0),
	THREAD_MONITOR_ENABLED(ConfigurationProperties.THREAD_MONITOR_ENABLED, Boolean.class, true),
	THREAD_MONITOR_INTERVAL_MS(ConfigurationProperties.THREAD_MONITOR_INTERVAL, Long.class, 0),
	THREAD_MONITOR_TIME_LIMIT(ConfigurationProperties.THREAD_MONITOR_TIME_LIMIT, Long.class, 30000),
	//TOMBSTONE_GC_THRESHOLD("tombstone-gc-threshold", Integer.class, 100000),
	UDP_FRAGMENT_SIZE(ConfigurationProperties.UDP_FRAGMENT_SIZE, Integer.class, 60000),
	UDP_RECV_BUFFER_SIZE(ConfigurationProperties.UDP_RECV_BUFFER_SIZE, Integer.class, 1048576),
	UPD_SEND_BUFFER_SIZE(ConfigurationProperties.UDP_SEND_BUFFER_SIZE, Integer.class, 65535),
	USE_CLUSTER_CONFIGURATION(ConfigurationProperties.USE_CLUSTER_CONFIGURATION, Boolean.class, true),
	USER_COMMAND_PACKAGES(ConfigurationProperties.USER_COMMAND_PACKAGES, String.class),
	VALIDATE_SERIALIZABLE_OBJECTS(ConfigurationProperties.VALIDATE_SERIALIZABLE_OBJECTS, Boolean.class, false);

	/**
	 * Factory method used to get a {@link GemFireProperties} enumerated value for the given {@link String property name}.
	 *
	 * @param propertyName {@link String name} of the {@link GemFireProperties} enumerated value to return.
	 * @return a {@link GemFireProperties} enumerated value for the given {@link String property name}.
	 * @throws IllegalArgumentException if a {@link GemFireProperties} enumerated value cannot be found
	 * for the given {@link String property name}.
	 * @see #values()
	 */
	public static @NonNull GemFireProperties from(@Nullable String propertyName) {

		return Arrays.stream(values())
			.filter(it -> equals(it, propertyName))
			.findFirst()
			.orElseThrow(() -> newIllegalArgumentException("[%s] is not a valid Apache Geode property", propertyName));
	}

	private static boolean equals(@Nullable GemFireProperties property, @Nullable String propertyName) {
		return property != null && property.getName().equals(normalizePropertyName(propertyName));
	}

	private static @Nullable String normalizePropertyName(@Nullable String propertyName) {

		String safePropertyName = String.valueOf(propertyName).trim();

		boolean gemfireDotPrefixed = safePropertyName.startsWith(PROPERTY_NAME_PREFIX);

		int index = safePropertyName.lastIndexOf(".");

		return gemfireDotPrefixed && index > -1 ? safePropertyName.substring(index + 1) : propertyName;
	}

	private @Nullable Class<?> nullSafeType(@Nullable Object target, @Nullable Class<?> defaultType) {
		return target != null ? target.getClass() : defaultType;
	}

	private static final Class<?> DEFAULT_PROPERTY_TYPE = Object.class;

	private static final Object DEFAULT_PROPERTY_VALUE = null;

	public static final String PROPERTY_NAME_PREFIX = "gemfire.";

	private final Class<?> propertyType;

	private final ConversionService conversionService;

	/** NOTE: a {@literal null} value represents an unset value */
	private final Object defaultValue;

	private final String propertyName;

	GemFireProperties(@NonNull String propertyName, @Nullable Class<?> propertyType) {
		this(propertyName, propertyType, null);
	}

	GemFireProperties(@NonNull String propertyName, @Nullable Class<?> propertyType, @Nullable Object defaultValue) {

		Assert.hasText(propertyName, "Property name is required");

		this.conversionService = DefaultConversionService.getSharedInstance();
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.defaultValue = defaultValue;
	}

	/**
	 * Gets this property's {@link Object default value}.
	 *
	 * @return this property's {@link Object default value}.
	 * @see java.lang.Object
	 */
	public @Nullable Object getDefaultValue() {
		return this.defaultValue != null ? this.defaultValue : DEFAULT_PROPERTY_VALUE;
	}

	/**
	 * Gets this property's {@link Object default value} as a {@link String}.
	 *
	 * @return this property's {@link Object default value} as a {@link String}. If this property's
	 * {@link Object default value} is {@literal null}, then this method return the {@literal "null"} {@link String}.
	 * @see #getDefaultValue()
	 * @see java.lang.String
	 */
	public @NonNull String getDefaultValueAsString() {
		return String.valueOf(getDefaultValue());
	}

	/**
	 * Gets this property's {@link Object default value} converted to the property's declared {@link Class type}.
	 *
	 * @param <T> declared {@link Class type} of this property.
	 * @return this property's {@link Object default value} converted to the property's declared {@link Class type}.
	 * @throws IllegalArgumentException if this property's {@link Object default value} cannot be converted to
	 * the property's declared {@link Class type}.
	 * @see #getDefaultValueAsType(Class)
	 * @see #getType()
	 */
	public @NonNull <T> T getDefaultValueAsType() {
		return getDefaultValueAsType(getType());
	}

	/**
	 * Gets this property's {@link Object default value} converted to the given {@link Class type}.
	 *
	 * @param <T> desired {@link Class type} for this property's {@link Object default value}.
	 * @param type {@link Class type} to convert the property's {@link Object default value} to.
	 * @return this property's {@link Object default value} converted to an instance of the given {@link Class type}.
	 * @throws IllegalArgumentException if this property's {@link Object default value} cannot be converted to
	 * an instance of the given {@link Class type}.
	 * @see #getDefaultValue()
	 * @see #getType()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDefaultValueAsType(Class<?> type) {

		Object defaultValue = getDefaultValue();

		Class<?> defaultValueType = nullSafeType(defaultValue, getType());

		if (this.conversionService.canConvert(defaultValueType, type)) {
			return (T) this.conversionService.convert(defaultValue, type);
		}

		throw newIllegalArgumentException("Cannot convert value [%s] from type [%s] to type [%s]",
			defaultValue, defaultValueType, type);
	}

	/**
	 * Gets the {@link String name} of this property.
	 *
	 * @return the {@link String name} of this property.
	 * @see java.lang.String
	 */
	public @NonNull String getName() {
		return this.propertyName;
	}

	/**
	 * Gets the declared {@link Class type} of this property.
	 *
	 * @return the declared {@link Class type} of this property.
	 * @see java.lang.Class
	 */
	public @NonNull Class<?> getType() {
		return this.propertyType != null ? this.propertyType : DEFAULT_PROPERTY_TYPE;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String toString() {
		return getName();
	}
}
