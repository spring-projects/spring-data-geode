/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation.support;

import static org.springframework.data.gemfire.util.CollectionUtils.asSet;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractAnnotationConfigSupport} class is an abstract base class encapsulating functionality
 * common to all Annotations and configuration classes used to configure Pivotal GemFire/Apache Geode objects
 * with Spring Data GemFire or Spring Data Geode.
 *
 * @author John Blum
 * @author Udo Kohlmeyer
 * @see java.lang.ClassLoader
 * @see org.springframework.beans.factory.BeanClassLoaderAware
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.expression.BeanFactoryAccessor
 * @see org.springframework.context.expression.EnvironmentAccessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.expression.EvaluationContext
 * @since 1.9.0
 */
@SuppressWarnings("unused")
public abstract class AbstractAnnotationConfigSupport
		implements BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware {

	protected static final Set<Integer> INFRASTRUCTURE_ROLES =
		asSet(BeanDefinition.ROLE_INFRASTRUCTURE, BeanDefinition.ROLE_SUPPORT);

	protected static final String ORG_SPRINGFRAMEWORK_DATA_GEMFIRE_PACKAGE = "org.springframework.data.gemfire";
	protected static final String ORG_SPRINGFRAMEWORK_PACKAGE = "org.springframework";
	protected static final String SPRING_DATA_GEMFIRE_PROPERTY_PREFIX = "spring.data.gemfire.";

	private BeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	private Environment environment;

	private final EvaluationContext evaluationContext;

	private final Logger log;

	/**
	 * Determines whether the given {@link Number} has value.  The {@link Number} is valuable
	 * if it is not {@literal null} and is not equal to 0.0d.
	 *
	 * @param value {@link Number} to evaluate.
	 * @return a boolean value indicating whether the given {@link Number} has value.
	 */
	protected static boolean hasValue(Number value) {
		return Optional.ofNullable(value).filter(it -> it.doubleValue() != 0.0d).isPresent();
	}

	/**
	 * Determines whether the given {@link Object} has value.  The {@link Object} is valuable
	 * if it is not {@literal null}.
	 *
	 * @param value {@link Object} to evaluate.
	 * @return a boolean value indicating whether the given {@link Object} has value.
	 */
	protected static boolean hasValue(Object value) {
		return value != null;
	}

	/**
	 * Determines whether the given {@link String} has value.  The {@link String} is valuable
	 * if it is not {@literal null} or empty.
	 *
	 * @param value {@link String} to evaluate.
	 * @return a boolean value indicating whether the given {@link String} is valuable.
	 */
	protected static boolean hasValue(String value) {
		return StringUtils.hasText(value);
	}

	/**
	 * Constructs a new instance of {@link AbstractAnnotationConfigSupport}.
	 *
	 * @see #AbstractAnnotationConfigSupport(BeanFactory)
	 */
	public AbstractAnnotationConfigSupport() {
		this(null);
	}

	/**
	 * Constructs a new instance of {@link AbstractAnnotationConfigSupport}.
	 *
	 * @param beanFactory reference to the Spring {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactory
	 * @see #newEvaluationContext(BeanFactory)
	 */
	public AbstractAnnotationConfigSupport(BeanFactory beanFactory) {

		this.evaluationContext = newEvaluationContext(beanFactory);
		this.log = newLog();
	}

	/**
	 * Constructs, configures and initializes a new instance of an {@link EvaluationContext}.
	 *
	 * @param beanFactory reference to the Spring {@link BeanFactory}.
	 * @return a new {@link EvaluationContext}.
	 * @see org.springframework.beans.factory.BeanFactory
	 * @see org.springframework.expression.EvaluationContext
	 * @see #getBeanFactory()
	 */
	protected EvaluationContext newEvaluationContext(BeanFactory beanFactory) {

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		evaluationContext.addPropertyAccessor(new BeanFactoryAccessor());
		evaluationContext.addPropertyAccessor(new EnvironmentAccessor());
		evaluationContext.addPropertyAccessor(new MapAccessor());
		evaluationContext.setTypeLocator(new StandardTypeLocator(getBeanClassLoader()));

		configureTypeConverter(evaluationContext, beanFactory);

		return evaluationContext;
	}

	private void configureTypeConverter(EvaluationContext evaluationContext, BeanFactory beanFactory) {

		Optional.ofNullable(evaluationContext)
			.filter(evalContext -> evalContext instanceof StandardEvaluationContext)
			.ifPresent(evalContext ->
				Optional.ofNullable(beanFactory)
					.filter(it -> it instanceof ConfigurableBeanFactory)
					.map(it -> ((ConfigurableBeanFactory) it).getConversionService())
					.ifPresent(conversionService ->
						((StandardEvaluationContext) evalContext).setTypeConverter(
							new StandardTypeConverter(conversionService)))
			);
	}

	/**
	 * Constructs a new instance of {@link Logger} to log statements printed by Spring Data GemFire/Geode.
	 *
	 * @return a new instance of {@link Logger}.
	 * @see org.apache.commons.logging.LogFactory#getLog(Class)
	 * @see org.apache.commons.logging.Log
	 */
	protected Logger newLog() {
		return LoggerFactory.getLogger(getClass());
	}

	/**
	 * Determines whether the given {@link AnnotationMetadata type meta-data} for a particular {@link Class}
	 * is annotated with the declared {@link #getAnnotationTypeName()}.
	 *
	 * @param importingClassMetadata {@link AnnotationMetadata type meta-data} for a particular {@link Class}.
	 * @return a boolean indicating whether the particular {@link Class} is annotated with
	 * the declared {@link #getAnnotationTypeName()}.
	 * @see #isAnnotationPresent(AnnotationMetadata, String)
	 * @see #getAnnotationTypeName()
	 * @see org.springframework.core.type.AnnotationMetadata
	 */
	protected boolean isAnnotationPresent(AnnotationMetadata importingClassMetadata) {
		return isAnnotationPresent(importingClassMetadata, getAnnotationTypeName());
	}

	/**
	 * Determines whether the given {@link AnnotationMetadata type meta-data} for a particular {@link Class}
	 * is annotated with the given {@link Annotation} defined by {@link String name}.
	 *
	 * @param importingClassMetadata {@link AnnotationMetadata type meta-data} for a particular {@link Class}.
	 * @param annotationName {@link String name} of the {@link Annotation} of interests.
	 * @return a boolean indicating whether the particular {@link Class} is annotated with
	 * the given {@link Annotation} defined by {@link String name}.
	 * @see org.springframework.core.type.AnnotationMetadata
	 */
	protected boolean isAnnotationPresent(AnnotationMetadata importingClassMetadata, String annotationName) {
		return importingClassMetadata.hasAnnotation(annotationName);
	}

	/**
	 * Returns the {@link AnnotationAttributes} for the given {@link Annotation}.
	 *
	 * @param annotation {@link Annotation} to get the {@link AnnotationAttributes} for.
	 * @return the {@link AnnotationAttributes} for the given {@link Annotation}.
	 * @see org.springframework.core.annotation.AnnotationAttributes
	 * @see java.lang.annotation.Annotation
	 */
	protected AnnotationAttributes getAnnotationAttributes(Annotation annotation) {
		return AnnotationAttributes.fromMap(AnnotationUtils.getAnnotationAttributes(annotation));
	}

	/**
	 * Returns {@link AnnotationAttributes} for the declared {@link #getAnnotationTypeName()}.
	 *
	 * @param importingClassMetadata {@link AnnotationMetadata type meta-data} for a particular {@link Class}.
	 * @return {@link AnnotationAttributes} for the declared {@link #getAnnotationTypeName()}.
	 * @see org.springframework.core.annotation.AnnotationAttributes
	 * @see org.springframework.core.type.AnnotationMetadata
	 * @see #getAnnotationAttributes(AnnotationMetadata, String)
	 * @see #getAnnotationTypeName()
	 */
	protected AnnotationAttributes getAnnotationAttributes(AnnotationMetadata importingClassMetadata) {
		return getAnnotationAttributes(importingClassMetadata, getAnnotationTypeName());
	}

	/**
	 * Returns {@link AnnotationAttributes} for the given {@link String named} {@link Annotation} from the given
	 * {@link AnnotationMetadata type meta-data}.
	 *
	 * @param importingClassMetadata {@link AnnotationMetadata type meta-data} for a particular {@link Class}.
	 * @param annotationName {@link String name} of the {@link Annotation} of interests.
	 * @return {@link AnnotationAttributes} for the given {@link String named} {@link Annotation}.
	 * @see org.springframework.core.annotation.AnnotationAttributes
	 * @see org.springframework.core.type.AnnotationMetadata
	 */
	protected AnnotationAttributes getAnnotationAttributes(AnnotationMetadata importingClassMetadata,
			String annotationName) {

		return AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(annotationName));
	}

	/**
	 * Returns the cache application {@link java.lang.annotation.Annotation} type pertaining to this configuration.
	 *
	 * @return the cache application {@link java.lang.annotation.Annotation} type used by this application.
	 */
	protected abstract Class<? extends Annotation> getAnnotationType();

	/**
	 * Returns the fully-qualified {@link Class#getName() class name} of the cache application
	 * {@link java.lang.annotation.Annotation} type.
	 *
	 * @return the fully-qualified {@link Class#getName() class name} of the cache application
	 * {@link java.lang.annotation.Annotation} type.
	 * @see java.lang.Class#getName()
	 * @see #getAnnotationType()
	 */
	protected String getAnnotationTypeName() {
		return getAnnotationType().getName();
	}

	/**
	 * Returns the simple {@link Class#getName() class name} of the cache application
	 * {@link java.lang.annotation.Annotation} type.
	 *
	 * @return the simple {@link Class#getName() class name} of the cache application
	 * {@link java.lang.annotation.Annotation} type.
	 * @see java.lang.Class#getSimpleName()
	 * @see #getAnnotationType()
	 */
	protected String getAnnotationTypeSimpleName() {
		return getAnnotationType().getSimpleName();
	}

	/**
	 * Null-safe method to determine whether the given {@link Object bean} is a Spring container provided
	 * infrastructure bean.
	 *
	 * @param bean {@link Object} to evaluate.
	 * @return {@literal true} iff the {@link Object bean} is not a Spring container infrastructure bean.
	 * @see #isNotInfrastructureClass(String)
	 */
	protected boolean isNotInfrastructureBean(Object bean) {

		return Optional.ofNullable(bean)
			.map(Object::getClass)
			.map(Class::getName)
			.filter(this::isNotInfrastructureClass).isPresent();
	}

	/**
	 * Null-safe method to determine whether the bean defined in the given {@link BeanDefinition}
	 * is a Spring container provided infrastructure bean.
	 *
	 * @param beanDefinition {@link BeanDefinition} to evaluate.
	 * @return {@literal true} iff the bean defined in the given {@link BeanDefinition} is not a Spring container
	 * infrastructure bean.
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @see #isNotInfrastructureClass(BeanDefinition)
	 * @see #isNotInfrastructureRole(BeanDefinition)
	 */
	protected boolean isNotInfrastructureBean(BeanDefinition beanDefinition) {
		return (isNotInfrastructureRole(beanDefinition) && isNotInfrastructureClass(beanDefinition));
	}

	/**
	 * Null-safe method to determine whether the bean defined in the given {@link BeanDefinition}
	 * is a Spring container infrastructure bean based on the bean's {@link Class#getName() class type}.
	 *
	 * @param beanDefinition {@link BeanDefinition} of the bean to evaluate.
	 * @return {@literal true} iff the bean defined in the given {@link BeanDefinition} is not a Spring container
	 * infrastructure bean.
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @see #resolveBeanClassName(BeanDefinition)
	 * @see #isNotInfrastructureClass(String)
	 */
	protected boolean isNotInfrastructureClass(BeanDefinition beanDefinition) {
		return resolveBeanClassName(beanDefinition).filter(this::isNotInfrastructureClass).isPresent();
	}

	/**
	 * Determines whether the given {@link Class#getName() class type name} is considered a Spring container
	 * infrastructure type.
	 *
	 * The class type name is considered a Spring container infrastructure type if the package name begins with
	 * 'org.springframework', excluding 'org.springframework.data.gemfire'.
	 *
	 * @param className {@link String} containing the name of the class type to evaluate.
	 * @return {@literal true} iff the given {@link Class#getName() class type name} is not considered a
	 * Spring container infrastructure type.
	 */
	protected boolean isNotInfrastructureClass(String className) {
		return (className.startsWith(ORG_SPRINGFRAMEWORK_DATA_GEMFIRE_PACKAGE)
			|| !className.startsWith(ORG_SPRINGFRAMEWORK_PACKAGE));
	}

	/**
	 * Null-safe method to determines whether the bean defined by the given {@link BeanDefinition}
	 * is a Spring container infrastructure bean based on the bean's role.
	 *
	 * @param beanDefinition {@link BeanDefinition} of the bean to evaluate.
	 * @return {@literal true} iff the bean defined in the given {@link BeanDefinition} is not a Spring container
	 * infrastructure bean.
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 */
	protected boolean isNotInfrastructureRole(BeanDefinition beanDefinition) {

		return Optional.ofNullable(beanDefinition)
			.map(BeanDefinition::getRole)
			.filter(role -> !INFRASTRUCTURE_ROLES.contains(role))
			.isPresent();
	}

	/**
	 * Determines whether the given {@link Method} was declared and defined by the user.
	 *
	 * A {@link Method} is considered a user-level {@link Method} if the {@link Method} is not
	 * an {@link Object} class method, is a {@link Method#isBridge() Bridge Method}
	 * or is not {@link Method#isSynthetic()} nor a Groovy method.
	 *
	 * @param method {@link Method} to evaluate.
	 * @return a boolean value indicating whether the {@link Method} was declared/defined by the user.
	 * @see java.lang.reflect.Method
	 */
	protected boolean isUserLevelMethod(Method method) {

		return Optional.ofNullable(method)
			.filter(ClassUtils::isUserLevelMethod)
			.filter(it -> !Object.class.equals(it.getDeclaringClass()))
			.isPresent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
	 * Returns a reference to the {@link ClassLoader} use by the Spring {@link BeanFactory} to load classes
	 * for bean definitions.
	 *
	 * @return the {@link ClassLoader} used by the Spring {@link BeanFactory} to load classes for bean definitions.
	 * @see #setBeanClassLoader(ClassLoader)
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Resolves the {@link ClassLoader bean ClassLoader} to the configured {@link ClassLoader}
	 * or the {@link Thread#getContextClassLoader() Thread Context ClassLoader}.
	 *
	 * @return the configured {@link ClassLoader} or the
	 * {@link Thread#getContextClassLoader() Thread Context ClassLoader}.
	 * @see java.lang.Thread#getContextClassLoader()
	 * @see #getBeanClassLoader()
	 */
	protected ClassLoader resolveBeanClassLoader() {
		return Optional.ofNullable(getBeanClassLoader())
			.orElseGet(() -> Thread.currentThread().getContextClassLoader());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		configureTypeConverter(getEvaluationContext(), beanFactory);
	}

	/**
	 * Returns a reference to the Spring {@link BeanFactory} in the current application context.
	 *
	 * @return a reference to the Spring {@link BeanFactory}.
	 * @throws IllegalStateException if the Spring {@link BeanFactory} was not properly configured.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	protected BeanFactory getBeanFactory() {
		return Optional.ofNullable(this.beanFactory)
			.orElseThrow(() -> newIllegalStateException("BeanFactory is required"));
	}

	/**
	 * Sets a reference to the Spring {@link Environment}.
	 *
	 * @param environment Spring {@link Environment}.
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(Environment)
	 * @see org.springframework.core.env.Environment
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns a reference to the Spring {@link Environment}.
	 *
	 * @return a reference to the Spring {@link Environment}.
	 * @see org.springframework.core.env.Environment
	 */
	protected Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Returns a reference to the {@link EvaluationContext} used to evaluate SpEL expressions.
	 *
	 * @return a reference to the {@link EvaluationContext} used to evaluate SpEL expressions.
	 * @see org.springframework.expression.EvaluationContext
	 */
	protected EvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	/**
	 * Returns a reference to the {@link Logger} used by this class to log {@link String messages}.
	 *
	 * @return a reference to the {@link Logger} used by this class to log {@link String messages}.
	 * @see org.apache.commons.logging.Log
	 */
	protected Logger getLog() {
		return this.log;
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
	 * @see #getLog()
	 */
	protected void logDebug(Supplier<String> message) {
		Optional.ofNullable(getLog())
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
	 * @see #getLog()
	 */
	protected void logInfo(Supplier<String> message) {
		Optional.ofNullable(getLog())
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
	 * Logs the {@link String message} supplied by the given {@link Supplier} at warning level.
	 *
	 * @param message {@link Supplier} containing the {@link String message} and arguments to log.
	 * @see org.apache.commons.logging.Log#isWarnEnabled()
	 * @see org.apache.commons.logging.Log#warn(Object)
	 * @see #getLog()
	 */
	protected void logWarning(Supplier<String> message) {
		Optional.ofNullable(getLog())
			.filter(Logger::isWarnEnabled)
			.ifPresent(log -> log.info(message.get()));
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
	 * @see #getLog()
	 */
	protected void logError(Supplier<String> message) {
		Optional.ofNullable(getLog())
			.filter(Logger::isWarnEnabled)
			.ifPresent(log -> log.info(message.get()));
	}

	/**
	 * Registers the {@link AbstractBeanDefinition} with the {@link BeanDefinitionRegistry} using a generated bean name.
	 *
	 * @param beanDefinition {@link AbstractBeanDefinition} to register.
	 * @return the given {@link AbstractBeanDefinition}.
	 * @see org.springframework.beans.factory.BeanFactory
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerWithGeneratedName(AbstractBeanDefinition, BeanDefinitionRegistry)
	 * @see #getBeanFactory()
	 */
	protected AbstractBeanDefinition register(AbstractBeanDefinition beanDefinition) {

		BeanFactory beanFactory = getBeanFactory();

		return (beanFactory instanceof BeanDefinitionRegistry
			? register(beanDefinition, (BeanDefinitionRegistry) beanFactory)
			: beanDefinition);
	}

	/**
	 * Registers the {@link AbstractBeanDefinition} with the {@link BeanDefinitionRegistry} using a generated bean name.
	 *
	 * @param beanDefinition {@link AbstractBeanDefinition} to register.
	 * @param registry {@link BeanDefinitionRegistry} used to register the {@link AbstractBeanDefinition}.
	 * @return the given {@link AbstractBeanDefinition}.
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerWithGeneratedName(AbstractBeanDefinition, BeanDefinitionRegistry)
	 */
	protected AbstractBeanDefinition register(AbstractBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {

		Optional.ofNullable(registry).ifPresent(it ->
			BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, it)
		);

		return beanDefinition;
	}

	protected List<String> arrayOfPropertyNamesFor(String propertyNamePrefix) {
		return arrayOfPropertyNamesFor(propertyNamePrefix, null);
	}

	protected List<String> arrayOfPropertyNamesFor(String propertyNamePrefix, String propertyNameSuffix) {

		List<String> propertyNames = new ArrayList<>();

		boolean found = true;

		for (int index = 0; (found && index < Integer.MAX_VALUE); index++) {

			String propertyName = asArrayProperty(propertyNamePrefix, index, propertyNameSuffix);

			found = getEnvironment().containsProperty(propertyName);

			if (found) {
				propertyNames.add(propertyName);
			}
		}

		return propertyNames;
	}

	protected String asArrayProperty(String propertyNamePrefix, int index, String propertyNameSuffix) {
		return String.format("%1$s[%2$d]%3$s", propertyNamePrefix, index,
			Optional.ofNullable(propertyNameSuffix).filter(StringUtils::hasText).map("."::concat).orElse(""));
	}

	protected String cacheProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache."), propertyNameSuffix);
	}

	protected String cacheClientProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache.client."), propertyNameSuffix);
	}

	protected String cacheCompressionProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache.compression."), propertyNameSuffix);
	}

	protected String cacheOffHeapProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache.off-heap."), propertyNameSuffix);
	}

	protected String cachePeerProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache.peer."), propertyNameSuffix);
	}

	protected String cacheServerProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cache.server."), propertyNameSuffix);
	}

	protected String namedCacheServerProperty(String name, String propertyNameSuffix) {
		return String.format("%1$s%2$s.%3$s", propertyName("cache.server."), name, propertyNameSuffix);
	}

	protected String clusterProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("cluster."), propertyNameSuffix);
	}

	protected String diskStoreProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("disk.store."), propertyNameSuffix);
	}

	protected String namedDiskStoreProperty(String name, String propertyNameSuffix) {
		return String.format("%1$s%2$s.%3$s", propertyName("disk.store."), name, propertyNameSuffix);
	}

	protected String entitiesProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("entities."), propertyNameSuffix);
	}

	protected String locatorProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("locator."), propertyNameSuffix);
	}

	protected String loggingProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("logging."), propertyNameSuffix);
	}

	protected String managementProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("management."), propertyNameSuffix);
	}

	protected String managerProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("manager."), propertyNameSuffix);
	}

	protected String pdxProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("pdx."), propertyNameSuffix);
	}

	protected String poolProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("pool."), propertyNameSuffix);
	}

	protected String namedPoolProperty(String name, String propertyNameSuffix) {
		return String.format("%1$s%2$s.%3$s", propertyName("pool."), name, propertyNameSuffix);
	}

	protected String securityProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("security."), propertyNameSuffix);
	}

	protected String sslProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", securityProperty("ssl."), propertyNameSuffix);
	}

	protected String statsProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("stats."), propertyNameSuffix);
	}

	protected String serviceProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("service."), propertyNameSuffix);
	}

	protected String redisServiceProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", serviceProperty("redis."), propertyNameSuffix);
	}

	protected String memcachedServiceProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", serviceProperty("memcached."), propertyNameSuffix);
	}

	protected String httpServiceProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", serviceProperty("http."), propertyNameSuffix);
	}

	protected String gatewayReceiverProperty(String propertyNameSuffix) {
		return String.format("%1$s%2$s", propertyName("gateway.receiver."), propertyNameSuffix);
	}

	/**
	 * Returns the fully-qualified {@link String property name}.
	 *
	 * The fully qualified {@link String property name} consists of the {@link String property name}
	 * concatenated with the {@code propertyNameSuffix}.
	 *
	 * @param propertyNameSuffix {@link String} containing the property name suffix
	 * concatenated with the {@link String base property name}.
	 * @return the fully-qualified {@link String property name}.
	 * @see java.lang.String
	 */
	protected String propertyName(String propertyNameSuffix) {
		return String.format("%1$s%2$s", SPRING_DATA_GEMFIRE_PROPERTY_PREFIX, propertyNameSuffix);
	}

	/**
	 * Resolves the value for the given property identified by {@link String name} from the Spring {@link Environment}
	 * as an instance of the specified {@link Class type}.
	 *
	 * @param <T> {@link Class} type of the {@code propertyName property's} assigned value.
	 * @param propertyName {@link String} containing the name of the required property to resolve.
	 * @param type {@link Class} type of the property's assigned value.
	 * @return the assigned value of the {@link String named} property.
	 * @throws IllegalArgumentException if the property has not been assigned a value.
	 * For {@link String} values, this also means the value cannot be {@link String#isEmpty() empty}.
	 * For non-{@link String} values, this means the value must not be {@literal null}.
	 * @see #resolveProperty(String, Class, Object)
	 */
	@SuppressWarnings("all")
	protected <T> T requireProperty(String propertyName, Class<T> type) {

		return Optional.of(propertyName)
			.map(it -> resolveProperty(propertyName, type, null))
			.filter(Objects::nonNull)
			.filter(value -> !(value instanceof String) || StringUtils.hasText((String) value))
			.orElseThrow(() -> newIllegalArgumentException("Property [%s] is required", propertyName));
	}

	/**
	 * Resolves the {@link Annotation} with the given {@link Class type} from the {@link AnnotatedElement}.
	 *
	 * @param <A> {@link Class Subclass type} of the resolved {@link Annotation}.
	 * @param annotatedElement {@link AnnotatedElement} from which to resolve the {@link Annotation}.
	 * @param annotationType {@link Class type} of the {@link Annotation} to resolve from the {@link AnnotatedElement}.
	 * @return the resolved {@link Annotation}.
	 * @see java.lang.annotation.Annotation
	 * @see java.lang.reflect.AnnotatedElement
	 * @see java.lang.Class
	 */
	protected <A extends Annotation> A resolveAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {

		return (annotatedElement instanceof Class
			? AnnotatedElementUtils.findMergedAnnotation(annotatedElement, annotationType)
			: AnnotationUtils.findAnnotation(annotatedElement, annotationType));
	}

	/**
	 * Resolves the {@link Class type} of the bean defined by the given {@link BeanDefinition}.
	 *
	 * @param beanDefinition {@link BeanDefinition} defining the bean from which the {@link Class type} is resolved.
	 * @param registry {@link BeanDefinitionRegistry} used to resolve the {@link ClassLoader} used to resolve
	 * the bean's {@link Class type}.
	 * @return an {@link Optional} {@link Class} specifying the resolved type of the bean.
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see #resolveBeanClassLoader(BeanDefinitionRegistry)
	 * @see #resolveBeanClass(BeanDefinition, ClassLoader)
	 */
	protected Optional<Class<?>> resolveBeanClass(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
		return resolveBeanClass(beanDefinition, resolveBeanClassLoader(registry));
	}

	/**
	 * Resolves the {@link Class type} of the bean defined by the given {@link BeanDefinition}.
	 *
	 * @param beanDefinition {@link BeanDefinition} defining the bean from which the {@link Class type} is resolved.
	 * @param classLoader {@link ClassLoader} used to resolve the bean's {@link Class type}.
	 * @return an {@link Optional} resolved {@link Class type} of the bean.
	 * @see java.lang.ClassLoader
	 * @see org.springframework.beans.factory.config.BeanDefinition
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#resolveBeanClass(ClassLoader)
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 * @see #resolveBeanClassName(BeanDefinition)
	 */
	protected Optional<Class<?>> resolveBeanClass(BeanDefinition beanDefinition, ClassLoader classLoader) {

		Class<?> beanClass = beanDefinition instanceof AbstractBeanDefinition
			? safeResolveType(() -> ((AbstractBeanDefinition) beanDefinition).resolveBeanClass(classLoader))
			: null;

		if (beanClass == null) {
			beanClass = resolveBeanClassName(beanDefinition)
				.map(beanClassName ->
					safeResolveType(() ->
						ClassUtils.forName(beanClassName, classLoader))).orElse(null);
		}

		return Optional.ofNullable(beanClass);
	}

	/**
	 * Attempts to resolve the {@link ClassLoader} used by the {@link BeanDefinitionRegistry}
	 * to load {@link Class} definitions of the beans defined in the registry.
	 *
	 * @param registry {@link BeanDefinitionRegistry} from which to resolve the {@link ClassLoader}.
	 * @return the resolved {@link ClassLoader} from the {@link BeanDefinitionRegistry}
	 * or the {@link Thread#currentThread() current Thread's} {@link Thread#getContextClassLoader() context ClassLoader}.
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see java.lang.Thread#currentThread()
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	protected ClassLoader resolveBeanClassLoader(BeanDefinitionRegistry registry) {

		return Optional.ofNullable(registry)
			.filter(it -> it instanceof ConfigurableBeanFactory)
			.map(it -> ((ConfigurableBeanFactory) it).getBeanClassLoader())
			.orElseGet(() -> Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Resolves the class type name of the bean defined by the given {@link BeanDefinition}.
	 *
	 * @param beanDefinition {@link BeanDefinition} defining the bean from which to resolve the class type name.
	 * @return an {@link Optional} {@link String} containing the resolved class type name of the bean defined
	 * by the given {@link BeanDefinition}.
	 * @see org.springframework.beans.factory.config.BeanDefinition#getBeanClassName()
	 */
	protected Optional<String> resolveBeanClassName(BeanDefinition beanDefinition) {

		Optional<String> beanClassName = Optional.ofNullable(beanDefinition)
			.map(BeanDefinition::getBeanClassName)
			.filter(StringUtils::hasText);

		if (!beanClassName.isPresent()) {
			beanClassName = Optional.ofNullable(beanDefinition)
				.filter(it -> it instanceof AnnotatedBeanDefinition)
				.filter(it -> StringUtils.hasText(it.getFactoryMethodName()))
				.map(it -> ((AnnotatedBeanDefinition) it).getFactoryMethodMetadata())
				.map(MethodMetadata::getReturnTypeName);
		}

		return beanClassName;
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as a {@link Boolean}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected Boolean resolveProperty(String propertyName, Boolean defaultValue) {
		return resolveProperty(propertyName, Boolean.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as an {@link Double}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected Double resolveProperty(String propertyName, Double defaultValue) {
		return resolveProperty(propertyName, Double.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as an {@link Float}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected Float resolveProperty(String propertyName, Float defaultValue) {
		return resolveProperty(propertyName, Float.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as an {@link Integer}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected Integer resolveProperty(String propertyName, Integer defaultValue) {
		return resolveProperty(propertyName, Integer.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as a {@link Long}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected Long resolveProperty(String propertyName, Long defaultValue) {
		return resolveProperty(propertyName, Long.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}
	 * as a {@link String}.
	 *
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected String resolveProperty(String propertyName, String defaultValue) {
		return resolveProperty(propertyName, String.class, defaultValue);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}.
	 *
	 * @param <T> {@link Class} type of the property value.
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param targetType {@link Class} type of the property's value.
	 * @return the value of the property identified by {@link String name} or {@literal null} if the property
	 * is not defined or not set.
	 * @see #resolveProperty(String, Class, Object)
	 */
	protected <T> T resolveProperty(String propertyName, Class<T> targetType) {
		return resolveProperty(propertyName, targetType, null);
	}

	/**
	 * Attempts to resolve the property with the given {@link String name} from the Spring {@link Environment}.
	 *
	 * @param <T> {@link Class} type of the property value.
	 * @param propertyName {@link String name} of the property to resolve.
	 * @param targetType {@link Class} type of the property's value.
	 * @param defaultValue default value to return if the property is not defined or not set.
	 * @return the value of the property identified by {@link String name} or default value if the property
	 * is not defined or not set.
	 * @see #getEnvironment()
	 */
	protected <T> T resolveProperty(String propertyName, Class<T> targetType, T defaultValue) {

		return Optional.ofNullable(getEnvironment())
			.filter(environment -> environment.containsProperty(propertyName))
			.map(environment -> {

				String resolvedPropertyName = environment.resolveRequiredPlaceholders(propertyName);

				return environment.getProperty(resolvedPropertyName, targetType, defaultValue);
			})
			.orElse(defaultValue);
	}

	/**
	 * Safely resolves a {@link Class type} returned by the supplied {@link TypeResolver} where the {@link Class type}
	 * resolution might result in a {@link ClassNotFoundException}, or possibly, {@link NoClassDefFoundError}.
	 *
	 * @param <T> {@link Class} of the type being resolved.
	 * @param typeResolver {@link TypeResolver} used to resolve a specific {@link Class type}.
	 * @return the resolved {@link Class type} or {@literal null} if the {@link Class type} returned by
	 * the {@link TypeResolver} could not be resolved.
	 * @see org.springframework.data.gemfire.config.annotation.support.AbstractAnnotationConfigSupport.TypeResolver
	 * @see java.lang.Class
	 */
	protected <T> Class<T> safeResolveType(TypeResolver<T> typeResolver) {

		try {
			return typeResolver.resolve();
		}
		catch (ClassNotFoundException | NoClassDefFoundError cause) {
			return null;
		}
	}

	/**
	 * {@link TypeResolver} is a {@link FunctionalInterface} defining a contract to encapsulated the logic
	 * to resolve a particular {@link Class type}.
	 *
	 * Implementations are free to decide on how a {@link Class type} gets resolved, such as
	 * with {@link Class#forName(String)} or perhaps using {@link ClassLoader#defineClass(String, byte[], int, int)}.
	 *
	 * @param <T> {@link Class} of the type to resolve.
	 */
	@FunctionalInterface
	protected interface TypeResolver<T> {
		Class<T> resolve() throws ClassNotFoundException;
	}
}
