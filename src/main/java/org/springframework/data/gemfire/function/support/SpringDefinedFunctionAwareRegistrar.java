/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire.function.support;

import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeCollection;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultSender;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.gemfire.config.annotation.support.GemFireComponentClassTypeScanner;
import org.springframework.data.gemfire.function.GemfireFunctionUtils;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.support.DeclarableSupport;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link SpringDefinedFunctionAwareRegistrar} class is an Apache Geode/Pivotal GemFire {@link Function}
 * responsible for searching and identifying classes with {@link GemfireFunction} annotated POJO {@link Method methods},
 * which serve as the {@link Function} implementation.
 *
 * These identified {@link GemfireFunction} annotated POJO {@link Method methods} will be registered with
 * the Apache Geode/Pivotal GemFire {@link FunctionService} and will be executable (invokable) from cache client
 * applications as well as {@literal Gfsh}.
 *
 * @author John Blum
 * @see java.lang.annotation.Annotation
 * @see java.lang.reflect.Method
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionContext
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.springframework.data.gemfire.config.annotation.support.GemFireComponentClassTypeScanner
 * @see org.springframework.data.gemfire.function.GemfireFunctionUtils
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.support.DeclarableSupport
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class SpringDefinedFunctionAwareRegistrar extends DeclarableSupport implements Function<String[]> {

	protected static final Class<? extends Annotation> GEMFIRE_FUNCTION_ANNOTATION = GemfireFunction.class;

	protected static final String BASE_PACKAGES_PROPERTY = "basePackages";

	protected static final TypeFilter[] DEFAULT_EXCLUDES = { };
	protected static final TypeFilter[] DEFAULT_INCLUDES = { GemfireFunctionsPresentTypeFilter.INSTANCE };

	/**
	 * Executes this {@link Function} by searching for and registering any Spring-defined, {@link GemfireFunction}
	 * annotated POJO class methods declared inside the array of configured {@link String base packages}.
	 *
	 * The array of {@link String based packages} are declared by passing arguments to this {@link Function}.
	 *
	 * This method concludes by sending {@link ResultSender#lastResult(Object)} of {@link ResultStatus#SUCCESS}.
	 *
	 * @param functionContext context in which this {@link Function} is executing.
	 * @see org.apache.geode.cache.execute.FunctionContext
	 * @see #registerSpringDefinedFunctions(String[])
	 */
	@Override
	public void execute(FunctionContext<String[]> functionContext) {

		registerSpringDefinedFunctions(functionContext.getArguments());

		functionContext.getResultSender().lastResult(ResultStatus.SUCCESS);
	}

	/**
	 * Initializes this {@link Function} by search for and registering any Spring-defined, {@link GemfireFunction}
	 * annotated POJO class methods declared inside the array of configured {@link String base packages}.
	 *
	 * The array of {@link String base packages} are declared with the {@literal basePackages} property
	 * in the cache configuration meta-data.
	 *
	 * @param cache {@link Cache} instance on which the {@link Function} is registered.
	 * @param properties {@link Properties} used to configure this {@link Function}.
	 * @see #registerSpringDefinedFunctions(String[])
	 * @see org.apache.geode.cache.Cache
	 * @see java.util.Properties
	 */
	@Override
	public void initialize(Cache cache, Properties properties) {

		String[] basePackages = Optional.ofNullable(properties)
			.map(it -> it.getProperty(BASE_PACKAGES_PROPERTY))
			.filter(StringUtils::hasText)
			.map(StringUtils::commaDelimitedListToStringArray)
			.filter(ArrayUtils::isNotEmpty)
			.orElse(null);

		registerSpringDefinedFunctions(basePackages);
	}

	/**
	 * Searches and registers any Spring-defined, {@link GemfireFunction} annotated POJO class methods declared inside
	 * the given array of {@link String base packages}.
	 *
	 * @param basePackages array of {@link String packages} in which to search for {@link GemfireFunction} annotated
	 * POJO class methods.
	 * @see org.springframework.data.gemfire.function.GemfireFunctionUtils#registerFunctionsForPojoType(Class)
	 * @see #newGemFireComponentClassTypeScanner(String[])
	 * @see #resolveBasePackages(String[])
	 */
	protected void registerSpringDefinedFunctions(String[] basePackages) {

		String[] resolvedBasePackages = resolveBasePackages(basePackages);

		GemFireComponentClassTypeScanner scanner = newGemFireComponentClassTypeScanner(resolvedBasePackages);

		Set<Class<?>> pojoClassesWithGemfireFunctionAnnotatedMethods = scanner.scan();

		nullSafeCollection(pojoClassesWithGemfireFunctionAnnotatedMethods).stream()
			.filter(Objects::nonNull)
			.forEach(pojoType -> GemfireFunctionUtils.registerFunctionsForPojoType(pojoType));
	}

	/**
	 * Returns the configured array of {@link String base packages} used to bootstrap the component scan.
	 *
	 * @return an array of {@link String base packages} used to bootstrap the component scan.
	 * @see java.lang.Class#getPackage()
	 * @see java.lang.Package#getName()
	 * @see #getClass()
	 */
	protected String[] getBasePackages() {
		return new String[] { getClass().getPackage().getName() };
	}

	/**
	 * Returns the configured array of {@link TypeFilter TypeFilters} used to exclude {@link Class types}
	 * in the component scan.
	 *
	 * @return an array of {@link TypeFilter TypeFilters} used to exclude {@link Class types} in the component scan.
	 * @see org.springframework.core.type.filter.TypeFilter
	 */
	protected TypeFilter[] getExcludes() {
		return DEFAULT_EXCLUDES;
	}

	/**
	 * Returns the configured array of {@link TypeFilter TypeFilters} used to include {@link Class types}
	 * in the component scan.
	 *
	 * @return an array of {@link TypeFilter TypeFilters} used to incluce {@link Class types} in the component scan.
	 * @see org.springframework.core.type.filter.TypeFilter
	 */
	protected TypeFilter[] getIncludes() {
		return DEFAULT_INCLUDES;
	}

	/**
	 * This {@link Function} returns a {@link ResultStatus} reflecting the completion of search and registration
	 * operation, i.e. POJO {@link Class classes} containing {@link Method methods} annotated with
	 * {@link GemfireFunction}).
	 *
	 * @return {@literal true}, by default.
	 */
	@Override
	public boolean hasResult() {
		return true;
	}

	/**
	 * This {@link Function} should not be re-executed in the case of failures.
	 *
	 * @return {@literal false}, by default.
	 */
	@Override
	public boolean isHA() {
		return false;
	}

	/**
	 * Verifies whether the array of {@link String base package} names are valid.
	 *
	 * @param basePackages array of {@link String base package} names to validate.
	 * @return a boolean value indicating whether the array of {@link String base package} names are valid;
	 * returns {@literal true} iff all {@link String base packages} in the array are valid.
	 * @see java.lang.Package#getPackage(String)
	 */
	protected boolean isValidBasePackages(String[] basePackages) {

		String[] nullSafeBasePackages = nullSafeArray(basePackages, String.class);

		// TODO: reestablish Package validation once GemFire/Geode's ClassLoader issues have been resolved; #ugh!
		long basePackageCount = Arrays.stream(nullSafeBasePackages)
			.filter(StringUtils::hasText)
			//.map(Package::getPackage)
			//.filter(Objects::nonNull)
			.count();

		return basePackageCount > 0 && basePackageCount == nullSafeBasePackages.length;
	}

	/**
	 * Constructs a new instance of {@link GemFireComponentClassTypeScanner} to scan the given array
	 * of {@link String base packages} in search of Apache Geode/Pivotal GemFire components.
	 *
	 * @param basePackages array of {@link String base packages} used to bootstrap the starting point of the scan.
	 * @return the new {@link GemFireComponentClassTypeScanner}.
	 * @see org.springframework.data.gemfire.config.annotation.support.GemFireComponentClassTypeScanner
	 * @see #resolveClassLoader()
	 * @see #getExcludes()
	 * @see #getIncludes()
	 */
	protected GemFireComponentClassTypeScanner newGemFireComponentClassTypeScanner(String[] basePackages) {

		return GemFireComponentClassTypeScanner.from(basePackages)
			.withIncludes(getIncludes())
			.withExcludes(getExcludes())
			.with(resolveClassLoader());
	}

	/**
	 * This {@link Function} performs no write data access operations on any Apache Geode/Pivotal GemFire
	 * defined {@link Region Regions}.
	 *
	 * @return {@literal false}, by default.
	 */
	@Override
	public boolean optimizeForWrite() {
		return false;
	}

	/**
	 * Resolves and validates the given array of {@link String base packages} used to bootstrap
	 * the Apache Geode/Pivotal GemFire component scan.
	 *
	 * @param basePackages array of {@link String base packages} used to bootstrap
	 * the Apache Geode/Pivotal GemFire component scan.
	 * @return the resolved array of {@link String based packages}.
	 * @see #validateBasePackages(String[])
	 * @see #getBasePackages()
	 */
	protected String[] resolveBasePackages(String[] basePackages) {

		return Optional.ofNullable(basePackages)
			.filter(ArrayUtils::isNotEmpty)
			.map(this::validateBasePackages)
			.orElseGet(this::getBasePackages);
	}

	/**
	 * Resolves the {@link ClassLoader} used to resolve POJO {@link Class classes} containing {@link Method methods}
	 * annotated with {@link GemfireFunction}.
	 *
	 * @return the {@link ClassLoader} used to resolve POJO {@link Class classes} containing {@link Method methods}
	 * annotated with {@link GemfireFunction}, packaged in a JAR file deployed to Apache Geode/Pivotal GemFire
	 * using Gfsh {@literal deploy}.
	 * @see java.lang.Class#getClassLoader()
	 * @see java.lang.ClassLoader
	 */
	protected ClassLoader resolveClassLoader() {
		return getClass().getClassLoader();
	}

	/**
	 * Validates the given array of {@link String base packages} used to bootstrap the component scan.
	 *
	 * @param basePackages array of {@link String base packages} to validate.
	 * @return the validated array of {@link String base packages}.
	 * @throws IllegalArgumentException if the array of {@link String base packages} are not valid Java packages.
	 * @see #isValidBasePackages(String[])
	 */
	protected String[] validateBasePackages(String[] basePackages) {

		return Optional.ofNullable(basePackages)
			.filter(this::isValidBasePackages)
			.orElseThrow(() -> newIllegalArgumentException("Base packages [%s] must be specified",
				Arrays.toString(basePackages)));
	}

	/**
	 * Spring {@link TypeFilter} used to filter component types in the classpath during the component scan
	 * that declare the {@link GemfireFunction} annotation.
	 *
	 * @see org.springframework.core.type.filter.TypeFilter
	 */
	protected static class GemfireFunctionsPresentTypeFilter implements TypeFilter {

		protected static final GemfireFunctionsPresentTypeFilter INSTANCE = new GemfireFunctionsPresentTypeFilter();

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
				throws IOException {

			ClassMetadata typeMetadata = metadataReader.getClassMetadata();

			boolean classIsConcreteAndIndependent = typeMetadata.isConcrete() && typeMetadata.isIndependent();

			boolean gemfireFunctionsArePresent = CollectionUtils.nullSafeSize(metadataReader.getAnnotationMetadata()
				.getAnnotatedMethods(GEMFIRE_FUNCTION_ANNOTATION.getName())) > 0;

			return classIsConcreteAndIndependent && gemfireFunctionsArePresent;
		}
	}

	/**
	 * Enumeration defining the result status of this {@link Function}.
	 */
	public enum ResultStatus {

		SUCCESS,
		FAILURE,

	}
}
