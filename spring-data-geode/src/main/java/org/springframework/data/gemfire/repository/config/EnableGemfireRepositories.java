/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.gemfire.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.gemfire.repository.support.SimpleGemfireRepository;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Annotation to enable Apache Geode, Spring Data {@link Repository Repositories}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.repository.Repository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(GemfireRepositoriesRegistrar.class)
public @interface EnableGemfireRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute.
	 *
	 * Allows for more concise annotation declarations, e.g. {@code @EnableGemfireRepositories("org.my.pkg")}
	 * instead of {@code @EnableGemfireRepositories(basePackages="org.my.pkg")}.
	 *
	 * @return a {@link String} array specifying the packages to search for Apache Geode Repositories.
	 * @see #basePackages()
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with)
	 * this attribute.
	 *
	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 *
	 * @return a {@link String} array specifying the packages to search for Apache Geode Repositories.
	 * @see #value()
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} to specify the packages to scan for annotated components.
	 *
	 * The package of each class specified will be scanned. Consider creating a special no-op marker class or interface
	 * in each package that serves no other purpose than being referenced by this attribute.
	 *
	 * @return an array of {@link Class classes} used to determine the packages to scan for Apache Geode Repositories.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 *
	 * @return an array of Filters used to specify Repositories to be included during the component scan.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 *
	 * @return an array of Filters used to specify Repositories to be excluded during the component scan.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Configures the name of the {@link GemfireMappingContext} bean definition to use when creating Repositories
	 * discovered through this annotation. If not configured a default {@link GemfireMappingContext} will be created.
	 *
	 * @return the {@link String bean name} of the {@link MappingContext} used by a Repository to map entities to
	 * the underlying data store (i.e. Apache Geode).
	 */
	String mappingContextRef() default "";

	/**
	 * Configures the {@link String location} of where to find the Spring Data named queries properties file.
	 *
	 * Defaults to {@code META-INFO/gemfire-named-queries.properties}.
	 *
	 * @return a {@link String} indicating the location of the named queries properties file.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the {@link Key} of the {@link QueryLookupStrategy} used to lookup queries for query methods.
	 *
	 * Defaults to {@link Key#CREATE_IF_NOT_FOUND}.
	 *
	 * @return the {@link Key} used to determine the query lookup and creation strategy.
	 * @see org.springframework.data.repository.query.QueryLookupStrategy.Key
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	/**
	 * Configure the {@link Repository} {@link Class base class} used to create {@link Repository} proxies
	 * for this particular configuration.
	 *
	 * @return the {@link Repository} {@link Class base class} used to create {@link Repository} proxies.
	 * @see org.springframework.data.gemfire.repository.support.SimpleGemfireRepository
	 * @since 1.7
	 */
	Class<?> repositoryBaseClass() default SimpleGemfireRepository.class;

	/**
	 * Configures the {@link FactoryBean} {@link Class} used to create each {@link Repository} instance.
	 *
	 * Defaults to {@link GemfireRepositoryFactoryBean}.
	 *
	 * @return the {@link FactoryBean} {@link Class} used to create each {@link Repository} instance.
	 * @see org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean
	 */
	Class<?> repositoryFactoryBeanClass() default GemfireRepositoryFactoryBean.class;

	/**
	 * Returns the {@link String postfix} used when looking up custom {@link Repository} implementations.
	 *
	 * Defaults to {@literal Impl}.
	 *
	 * For example, for a {@link Repository} named {@code PersonRepository}, the corresponding implementation class
	 * will be looked up scanning for {@code PersonRepositoryImpl}.
	 *
	 * @return a {@link String} indicating the postfix to append to the {@link Repository} interface name
	 * when looking up the custom {@link Repository} implementing class.
	 */
	String repositoryImplementationPostfix() default "Impl";

}
