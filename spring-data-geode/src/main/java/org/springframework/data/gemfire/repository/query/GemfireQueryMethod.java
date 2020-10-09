/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.gemfire.repository.query;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.gemfire.mapping.GemfirePersistentProperty;
import org.springframework.data.gemfire.repository.Query;
import org.springframework.data.gemfire.repository.query.annotation.Hint;
import org.springframework.data.gemfire.repository.query.annotation.Import;
import org.springframework.data.gemfire.repository.query.annotation.Limit;
import org.springframework.data.gemfire.repository.query.annotation.Trace;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link QueryMethod} implementation for Apache Geode.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see java.lang.reflect.Method
 * @see org.springframework.data.gemfire.repository.Query
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.query.QueryMethod
 */
public class GemfireQueryMethod extends QueryMethod {

	protected static final String[] EMPTY_STRING_ARRAY = new String[0];

	private final GemfirePersistentEntity<?> entity;

	private final Method method;

	@SuppressWarnings("unused")
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	/**
	 * Constructs a new instance of {@link GemfireQueryMethod} from the given {@link Method}
	 * and {@link RepositoryMetadata}.
	 *
	 * @param method {@link Method} object backing the actual {@literal query} for this {@link QueryMethod};
	 * must not be {@literal null}.
	 * @param metadata {@link RepositoryMetadata} containing metadata about the {@link Repository}
	 * to which this {@link QueryMethod} belongs; must not be {@literal null}.
	 * @param projectionFactory {@link ProjectionFactory} used to handle the {@literal query} {@literal projection};
	 * must not be {@literal null}.
	 * @param mappingContext {@link MappingContext} used to map {@link Object entities} to Apache Geode and back to
	 * {@link Object entities}; must not be {@literal null}.
	 * @see #GemfireQueryMethod(Method, RepositoryMetadata, ProjectionFactory, MappingContext, QueryMethodEvaluationContextProvider)
	 * @see org.springframework.data.repository.core.RepositoryMetadata
	 * @see org.springframework.data.projection.ProjectionFactory
	 * @see org.springframework.data.mapping.context.MappingContext
	 * @see java.lang.reflect.Method
	 */
	public GemfireQueryMethod(@NonNull Method method,
			@NonNull RepositoryMetadata metadata,
			@NonNull ProjectionFactory projectionFactory,
			@NonNull MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> mappingContext) {

		this(method, metadata, projectionFactory, mappingContext, null);

	}

	/**
	 * Constructs a new instance of {@link GemfireQueryMethod} from the given {@link Method}
	 * and {@link RepositoryMetadata}.
	 *
	 * @param method {@link Method} object backing the actual {@literal query} for this {@link QueryMethod};
	 * must not be {@literal null}.
	 * @param metadata {@link RepositoryMetadata} containing metadata about the {@link Repository}
	 * to which this {@link QueryMethod} belongs; must not be {@literal null}.
	 * @param projectionFactory {@link ProjectionFactory} used to handle the {@literal query} {@literal projection};
	 * must not be {@literal null}.
	 * @param mappingContext {@link MappingContext} used to map {@link Object entities} to Apache Geode and back to
	 * {@link Object entities}; must not be {@literal null}.
	 * @param evaluationContextProvider {@link QueryMethodEvaluationContextProvider} used to process {@literal SpEL}
	 * expressions.
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider
	 * @see org.springframework.data.repository.core.RepositoryMetadata
	 * @see org.springframework.data.projection.ProjectionFactory
	 * @see org.springframework.data.mapping.context.MappingContext
	 * @see java.lang.reflect.Method
	 */
	public GemfireQueryMethod(@NonNull Method method,
			@NonNull RepositoryMetadata metadata,
			@NonNull ProjectionFactory projectionFactory,
			@NonNull MappingContext<? extends GemfirePersistentEntity<?>, GemfirePersistentProperty> mappingContext,
			@Nullable QueryMethodEvaluationContextProvider evaluationContextProvider) {

		super(method, metadata, projectionFactory);

		Assert.notNull(mappingContext, "MappingContext must not be null");

		this.method = method;
		this.entity = mappingContext.getPersistentEntity(getDomainClass());
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/**
	 * Returns the {@link Method} reference on which this {@link QueryMethod} is based.
	 *
	 * @return the {@link Method} reference on which this {@link QueryMethod} is based.
	 * @see java.lang.reflect.Method
	 */
	protected @NonNull Method getMethod() {
		return this.method;
	}

	/**
	 * Returns the {@link GemfirePersistentEntity} handled by this {@link QueryMethod}.
	 *
	 * @return the {@link GemfirePersistentEntity} handled by this {@link QueryMethod}.
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
	 */
	public @NonNull GemfirePersistentEntity<?> getPersistentEntity() {
		return this.entity;
	}

	/**
	 * Determines whether this query method specifies an annotated, non-empty query.
	 *
	 * @return a boolean value indicating whether the query method specifies an annotated, non-empty query.
	 * @see org.springframework.util.StringUtils#hasText(String)
	 * @see #getAnnotatedQuery()
	 */
	public boolean hasAnnotatedQuery() {
		return StringUtils.hasText(getAnnotatedQuery());
	}

	/**
	 * Returns the {@link Query} annotated OQL query value for this {@link QueryMethod} if present.
	 *
	 * @return the {@link Query} annotated OQL query value or {@literal null} in case it's {@literal null}, empty
	 * or not present.
	 * @see org.springframework.data.gemfire.repository.Query
	 * @see java.lang.reflect.Method#getAnnotation(Class)
	 */
	public @Nullable String getAnnotatedQuery() {

		Query query = getMethod().getAnnotation(Query.class);

		String queryString = query != null ? (String) AnnotationUtils.getValue(query) : null;

		return StringUtils.hasText(queryString) ? queryString : null;
	}

	/**
	 * Determines whether this query method uses a query HINT to tell the GemFire OQL query engine which indexes
	 * to apply to the query execution.
	 *
	 * @return a boolean value to indicate whether this query method uses a query HINT.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Hint
	 * @see java.lang.reflect.Method#isAnnotationPresent(Class)
	 */
	public boolean hasHint() {
		return getMethod().isAnnotationPresent(Hint.class);
	}

	/**
	 * Gets the query HINTs for this query method.
	 *
	 * @return the query HINTs for this query method or an empty array if this query method has no query HINTs.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Hint
	 * @see java.lang.reflect.Method#getAnnotation(Class)
	 */
	public String[] getHints() {

		Hint hint = getMethod().getAnnotation(Hint.class);

		return hint != null ? hint.value() : EMPTY_STRING_ARRAY;
	}

	/**
	 * Determine whether this query method declares an IMPORT statement to qualify application domain object types
	 * referenced in the query.
	 *
	 * @return a boolean value to indicate whether this query method declares an IMPORT statement.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Import
	 * @see java.lang.reflect.Method#isAnnotationPresent(Class)
	 */
	public boolean hasImport() {
		return getMethod().isAnnotationPresent(Import.class);
	}

	/**
	 * Gets the IMPORT statement for this query method.
	 *
	 * @return the IMPORT statement for this query method or null if this query method does not have an IMPORT statement.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Import
	 * @see java.lang.reflect.Method#getAnnotation(Class)
	 */
	public String getImport() {

		Import importStatement = getMethod().getAnnotation(Import.class);

		return importStatement != null ? importStatement.value() : null;
	}

	/**
	 * Determines whether this query method defines a LIMIT on the number of results returned by the query.
	 *
	 * @return a boolean value indicating whether this query method defines a LIMIT on the result set
	 * returned by the query.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Limit
	 * @see java.lang.reflect.Method#isAnnotationPresent(Class)
	 */
	public boolean hasLimit() {
		return getMethod().isAnnotationPresent(Limit.class);
	}

	/**
	 * Gets the LIMIT for this query method on the result set returned by the query.
	 *
	 * @return the LIMIT for this query method limiting the number of results returned by the query.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Limit
	 * @see java.lang.reflect.Method#getAnnotation(Class)
	 */
	public int getLimit() {

		Limit limit = getMethod().getAnnotation(Limit.class);

		return limit != null ? limit.value() : Integer.MAX_VALUE;
	}

	/**
	 * Determines whether this query method has TRACE (i.e. logging) enabled.
	 *
	 * @return a boolean value to indicate whether this query method has TRACE enabled.
	 * @see org.springframework.data.gemfire.repository.query.annotation.Limit
	 * @see java.lang.reflect.Method#isAnnotationPresent(Class)
	 */
	public boolean hasTrace() {
		return getMethod().isAnnotationPresent(Trace.class);
	}
}
