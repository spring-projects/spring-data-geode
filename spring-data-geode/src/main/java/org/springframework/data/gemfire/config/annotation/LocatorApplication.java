/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.geode.distributed.Locator;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.support.GemfireBeanFactoryLocator;

/**
 * The {@link LocatorApplication} {@link Annotation} enables a Spring Data for Apache Geode & Pivotal GemFire
 * application to become a {@link Locator} based application.
 *
 * @author John Blum
 * @see java.lang.annotation.Annotation
 * @see java.lang.annotation.Documented
 * @see java.lang.annotation.Inherited
 * @see java.lang.annotation.Retention
 * @see java.lang.annotation.Target
 * @see org.apache.geode.distributed.Locator
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.data.gemfire.config.annotation.LocatorApplicationConfiguration
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @since 2.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Configuration
@Import(LocatorApplicationConfiguration.class)
@SuppressWarnings("unused")
public @interface LocatorApplication {

	/**
	 * Configures the hostname or IP address on which the {@link Locator} will bind to for accepting connections
	 * from clients sending {@link Locator} requests.
	 *
	 * Defaults to {@literal localhost}.
	 *
	 * Use the {@literal spring.data.gemfire.locator.bind-address} property
	 * in Spring Boot {@literal application.properties}.
	 */
	String bindAddress() default "";

	/**
	 * Configures the {@link String hostname} used by clients connecting to this {@link Locator}.
	 *
	 * Defaults to {@literal localhost}.
	 *
	 * Use {@literal spring.data.gemfire.locator.hostname-for-clients}
	 * in Spring Boot {@literal application.properties}.
	 */
	String hostnameForClients() default "";

	/**
	 * Configures the list of {@link Locator Locators} defining the cluster to which this Spring {@link Locator}
	 * application will connect.
	 *
	 * Use {@literal spring.data.gemfire.locators} property in {@literal application.properties}.
	 */
	String locators() default "";

	/**
	 * Configures the log level used to output log messages at Apache Geode / Pivotal GemFire {@link Locator} runtime.
	 *
	 * Defaults to {@literal config}.
	 *
	 * Use {@literal spring.data.gemfire.locator.log-level} property in {@literal application.properties}.
	 */
	String logLevel() default LocatorApplicationConfiguration.DEFAULT_LOG_LEVEL;

	/**
	 * Configures the {@link String name} of the {@link Locator} application.
	 *
	 * Defaults to {@literal SpringBasedLocatorApplication}.
	 *
	 * Use the {@literal spring.data.gemfire.locator.name} property
	 * in Spring Boot {@literal application.properties}.
	 */
	String name() default LocatorApplicationConfiguration.DEFAULT_NAME;

	/**
	 * Configures the port on which the embedded {@link Locator} service will bind to
	 * listening for client connections sending {@link Locator} requests.
	 *
	 * Defaults to {@literal 10334}.
	 *
	 * Use the {@literal spring.data.gemfire.locator.port} property
	 * in Spring Boot {@literal application.properties}.
	 */
	int port() default LocatorApplicationConfiguration.DEFAULT_PORT;

	/**
	 * Configures the {@link Locator} application with the {@link GemfireBeanFactoryLocator} in order to look up
	 * the Spring {@link BeanFactory} used to auto-wire, configure and initialize Apache Geode components
	 * created in a non-Spring managed, Apache Geode context (for example: {@literal cache.xml}).
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use {@literal spring.data.gemfire.use-bean-factory-locator} property in {@literal application.properties}.
	 */
	boolean useBeanFactoryLocator() default LocatorApplicationConfiguration.DEFAULT_USE_BEAN_FACTORY_LOCATOR;

	/**
	 * Configures whether the Spring-based {@link Locator} will pull configuration metadata from the Apache Geode
	 * cluster-based, Cluster Configuration Service.
	 *
	 * Defaults to {@literal false}.
	 *
	 * Use {@literal spring.data.gemfire.locator.use-cluster-configuration} property
	 * in {@literal application.properties}.
	 */
	boolean useClusterConfiguration() default LocatorApplicationConfiguration.DEFAULT_USE_CLUSTER_CONFIGURATTION_SERVICE;

}
