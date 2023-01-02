/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.gemfire.support;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Spring {@link FactoryBean} base class implementation encapsulating operations common to all
 * Spring Data for Apache Geode (SDG) {@link FactoryBean} implementations.
 *
 * @author John Blum
 * @see org.slf4j.Logger
 * @see org.slf4j.LoggerFactory
 * @see org.springframework.beans.factory.BeanClassLoaderAware
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.springframework.beans.factory.FactoryBean
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public abstract class AbstractFactoryBeanSupport<T>
		implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, BeanNameAware {

	protected static final boolean DEFAULT_SINGLETON = true;

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	private final Logger logger;

	private String beanName;

	/**
	 * Constructs a new instance of {@link AbstractFactoryBeanSupport} initializing a {@link Logger} to log operations
	 * performed by {@literal this} {@link FactoryBean}.
	 *
	 * @see #newLogger()
	 */
	protected AbstractFactoryBeanSupport() {
		this.logger = newLogger();
	}

	/**
	 * Constructs a new instance of {@link Logger} to log statements printed by Spring Data for Apache Geode.
	 *
	 * @return a new instance of SLF4J {@link Logger}.
	 * @see org.apache.commons.logging.LogFactory#getLog(Class)
	 * @see org.apache.commons.logging.Log
	 */
	protected @NonNull Logger newLogger() {
		return LoggerFactory.getLogger(getClass());
	}

	/**
	 * Sets a reference to the {@link ClassLoader} used by the Spring container to load bean {@link Class classes}.
	 *
	 * @param classLoader {@link ClassLoader} used by the Spring container to load bean {@link Class classes}.
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(ClassLoader)
	 * @see java.lang.ClassLoader
	 * @see java.lang.Class
	 */
	@Override
	public void setBeanClassLoader(@Nullable ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Returns a reference to the {@link ClassLoader} used by the Spring container to load bean {@link Class classes}.
	 *
	 * @return the {@link ClassLoader} used by the Spring container to load bean {@link Class classes}.
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(ClassLoader)
	 * @see java.lang.ClassLoader
	 * @see java.lang.Class
	 */
	public @Nullable ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Sets a reference to the Spring {@link BeanFactory} in which this {@link FactoryBean} was declared.
	 *
	 * @param beanFactory reference to the declaring Spring {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(BeanFactory)
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Returns a reference to the Spring {@link BeanFactory} in which this {@link FactoryBean} was declared.
	 *
	 * @return a reference to the declaring Spring {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(BeanFactory)
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	public @Nullable BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Sets the {@link String bean name} assigned to this {@link FactoryBean} as declared in the Spring container.
	 *
	 * @param name {@link String bean name} assigned to this {@link FactoryBean} as declared in the Spring container.
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(String)
	 * @see java.lang.String
	 */
	@Override
	public void setBeanName(@Nullable String name) {
		this.beanName = name;
	}

	/**
	 * Returns the {@link String bean name} assigned to this {@link FactoryBean} as declared in the Spring container.
	 *
	 * @return the {@link String bean name} assigned to this {@link FactoryBean} as declared in the Spring container.
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(String)
	 * @see java.lang.String
	 */
	public @Nullable String getBeanName() {
		return this.beanName;
	}

	/**
	 * Returns a reference to the {@link Logger} used by {@literal this} {@link FactoryBean}
	 * to log {@link String messages}.
	 *
	 * @return a reference to the {@link Logger} used by {@literal this} {@link FactoryBean}
	 * to log {@link String messages}.
	 * @see org.apache.commons.logging.Log
	 */
	protected @NonNull Logger getLogger() {
		return this.logger;
	}

	/**
	 * Returns an {@link Optional} reference to the {@link Logger} used by {@literal this} {@link FactoryBean}
	 * to log {@link String messages}.
	 *
	 * @return an {@link Optional} reference to the {@link Logger} used by {@literal this} {@link FactoryBean}
	 * to log {@link String messages}.
	 * @see java.util.Optional
	 * @see org.slf4j.Logger
	 * @see #getLogger()
	 */
	protected Optional<Logger> getOptionalLogger() {
		return Optional.ofNullable(getLogger());
	}

	/**
	 * Determines whether {@literal DEBUG} logging is enabled.
	 *
	 * @return a boolean value indicating whether {@literal DEBUG} logging is enabled.
	 * @see org.slf4j.Logger#isDebugEnabled()
	 * @see #getOptionalLogger()
	 */
	public boolean isDebugLoggingEnabled() {
		return getOptionalLogger().filter(Logger::isDebugEnabled).isPresent();
	}

	/**
	 * Determines whether {@literal INFO} logging is enabled.
	 *
	 * @return a boolean value indicating whether {@literal INFO} logging is enabled.
	 * @see org.slf4j.Logger#isInfoEnabled()
	 * @see #getOptionalLogger()
	 */
	public boolean isInfoLoggingEnabled() {
		return getOptionalLogger().filter(Logger::isInfoEnabled).isPresent();
	}

	/**
	 * Determines whether {@literal WARN} logging is enabled.
	 *
	 * @return a boolean value indicating whether {@literal WARN} logging is enabled.
	 * @see org.slf4j.Logger#isWarnEnabled()
	 * @see #getOptionalLogger()
	 */
	public boolean isWarnLoggingEnabled() {
		return getOptionalLogger().filter(Logger::isWarnEnabled).isPresent();
	}

	/**
	 * Determines whether {@literal ERROR} logging is enabled.
	 *
	 * @return a boolean value indicating whether {@literal ERROR} logging is enabled.
	 * @see org.slf4j.Logger#isErrorEnabled()
	 * @see #getOptionalLogger()
	 */
	public boolean isErrorLoggingEnabled() {
		return getOptionalLogger().filter(Logger::isErrorEnabled).isPresent();
	}

	/**
	 * Indicates that {@literal this} {@link FactoryBean} produces a single bean instance.
	 *
	 * @return {@literal true} by default.
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return DEFAULT_SINGLETON;
	}

	/**
	 * Logs the {@link String message} formatted with the array of {@link Object arguments} at debug level.
	 *
	 * @param message {@link String} containing the message to log.
	 * @param args array of {@link Object arguments} used to format the {@code message}.
	 * @see #logDebug(Supplier)
	 */
	protected void logDebug(String message, Object... args) {
		logDebug(() -> String.format(message, args));
	}

	/**
	 * Logs the {@link String message} supplied by the given {@link Supplier} at debug level.
	 *
	 * @param message {@link Supplier} containing the {@link String message} and arguments to log.
	 * @see org.apache.commons.logging.Log#isDebugEnabled()
	 * @see org.apache.commons.logging.Log#debug(Object)
	 * @see #getLogger()
	 */
	protected void logDebug(Supplier<String> message) {
		getOptionalLogger()
			.filter(Logger::isDebugEnabled)
			.ifPresent(log -> log.debug(message.get()));
	}

	/**
	 * Logs the {@link String message} formatted with the array of {@link Object arguments} at info level.
	 *
	 * @param message {@link String} containing the message to log.
	 * @param args array of {@link Object arguments} used to format the {@code message}.
	 * @see #logInfo(Supplier)
	 */
	protected void logInfo(String message, Object... args) {
		logInfo(() -> String.format(message, args));
	}

	/**
	 * Logs the {@link String message} supplied by the given {@link Supplier} at info level.
	 *
	 * @param message {@link Supplier} containing the {@link String message} and arguments to log.
	 * @see org.apache.commons.logging.Log#isInfoEnabled()
	 * @see org.apache.commons.logging.Log#info(Object)
	 * @see #getLogger()
	 */
	protected void logInfo(Supplier<String> message) {
		getOptionalLogger()
			.filter(Logger::isInfoEnabled)
			.ifPresent(log -> log.info(message.get()));
	}

	/**
	 * Logs the {@link String message} formatted with the array of {@link Object arguments} at warn level.
	 *
	 * @param message {@link String} containing the message to log.
	 * @param args array of {@link Object arguments} used to format the {@code message}.
	 * @see #logWarning(Supplier)
	 */
	protected void logWarning(String message, Object... args) {
		logWarning(() -> String.format(message, args));
	}

	/**
	 * Logs the {@link String message} supplied by the given {@link Supplier} at warn level.
	 *
	 * @param message {@link Supplier} containing the {@link String message} and arguments to log.
	 * @see org.apache.commons.logging.Log#isWarnEnabled()
	 * @see org.apache.commons.logging.Log#warn(Object)
	 * @see #getLogger()
	 */
	protected void logWarning(Supplier<String> message) {
		getOptionalLogger()
			.filter(Logger::isWarnEnabled)
			.ifPresent(log -> log.warn(message.get()));
	}

	/**
	 * Logs the {@link String message} formatted with the array of {@link Object arguments} at error level.
	 *
	 * @param message {@link String} containing the message to log.
	 * @param args array of {@link Object arguments} used to format the {@code message}.
	 * @see #logError(Supplier)
	 */
	protected void logError(String message, Object... args) {
		logError(() -> String.format(message, args));
	}

	/**
	 * Logs the {@link String message} supplied by the given {@link Supplier} at error level.
	 *
	 * @param message {@link Supplier} containing the {@link String message} and arguments to log.
	 * @see org.apache.commons.logging.Log#isErrorEnabled()
	 * @see org.apache.commons.logging.Log#error(Object)
	 * @see #getLogger()
	 */
	protected void logError(Supplier<String> message) {
		getOptionalLogger()
			.filter(Logger::isErrorEnabled)
			.ifPresent(log -> log.error(message.get()));
	}
}
