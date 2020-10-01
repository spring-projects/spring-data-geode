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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.util.Collection;
import java.util.Collections;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.repository.Query;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * {@link GemfireRepositoryQuery} using plain {@link String} based OQL queries.
 * <p>
 * @author Oliver Gierke
 * @author David Turanski
 * @author John Blum
 */
@SuppressWarnings("unused")
public class StringBasedGemfireRepositoryQuery extends GemfireRepositoryQuery {

	private static final String INVALID_QUERY = "Modifying queries are not supported";

	private volatile boolean userDefinedQuery = false;

	private final GemfireTemplate template;

	private final QueryString query;

	/**
	 * Constructor used for testing purposes only!
	 */
	StringBasedGemfireRepositoryQuery() {

		this.query = null;
		this.template = null;

		register(ProvidedQueryPostProcessors.LIMIT
			.processBefore(ProvidedQueryPostProcessors.IMPORT)
			.processBefore(ProvidedQueryPostProcessors.HINT)
			.processBefore(ProvidedQueryPostProcessors.TRACE));
	}

	/**
	 * Constructs a new instance of {@link StringBasedGemfireRepositoryQuery} initialized with
	 * the given {@link String query}, {@link GemfireQueryMethod} and {@link GemfireTemplate}.
	 *
	 * @param query {@link String} containing the {@literal OQL query} to execute;
	 * must not be {@literal null} or empty.
	 * @param queryMethod {@link GemfireQueryMethod} implementing the {@link RepositoryQuery};
	 * must not be {@literal null}.
	 * @param template {@link GemfireTemplate} used to execute {@literal QOL queries};
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link GemfireQueryMethod} or {@link GemfireTemplate} are {@literal null}.
	 * @throws IllegalStateException if the {@link GemfireQueryMethod} represents a modifying query.
	 * @see org.springframework.data.gemfire.repository.query.GemfireQueryMethod
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	public StringBasedGemfireRepositoryQuery(String query, GemfireQueryMethod queryMethod, GemfireTemplate template) {

		super(queryMethod);

		Assert.hasText(query, "Query must not be null or empty");
		Assert.notNull(template, "GemfireTemplate must not be null");
		Assert.state(!queryMethod.isModifyingQuery(), INVALID_QUERY);

		this.query = QueryString.of(query);
		this.template = template;

		register(ProvidedQueryPostProcessors.LIMIT
			.processBefore(ProvidedQueryPostProcessors.IMPORT)
			.processBefore(ProvidedQueryPostProcessors.HINT)
			.processBefore(ProvidedQueryPostProcessors.TRACE));
	}

	/**
	 * Builder method used to set this {@link RepositoryQuery} as derived.
	 *
	 * @return a boolean value indicating whether the OQL query was derived from the {@link Repository} infrastructure
	 * {@link QueryMethod} name/signature.
	 * @see #asUserDefinedQuery()
	 */
	public @NonNull StringBasedGemfireRepositoryQuery asDerivedQuery() {
		this.userDefinedQuery = false;
		return this;
	}

	/**
	 * Builder method used to set this {@link RepositoryQuery} as user-defined.
	 *
	 * @return this {@link RepositoryQuery}.
	 * @see #isUserDefinedQuery()
	 */
	public @NonNull StringBasedGemfireRepositoryQuery asUserDefinedQuery() {
		this.userDefinedQuery = true;
		return this;
	}

	/**
	 * Determines whether the OQL query represented by this {@link RepositoryQuery} is derived from
	 * the {@link Repository} infrastructure {@link QueryMethod} name/signature conventions.
	 *
	 * @return a boolean value indicating if the OQL query represented by this {@link RepositoryQuery} is derived.
	 * @see #asDerivedQuery()
	 * @see #isUserDefinedQuery()
	 */
	public boolean isDerivedQuery() {
		return !isUserDefinedQuery();
	}

	/**
	 * Determines whether the OQL query represented by this {@link RepositoryQuery} is user-defined
	 * or was generated by the Spring Data {@link Repository} infrastructure.
	 *
	 * An {@literal OQL query} is user-defined if the query was specified using the {@link Query} annotation on
	 * the {@link Repository} {@link QueryMethod} or was specified in the {@literal <module>-named-queries.properties}
	 * file.
	 *
	 * Derived queries are not user-defined.
	 *
	 * @return a boolean value indicating whether the OQL query represented this {@link RepositoryQuery} is user-defined.
	 * @see #asUserDefinedQuery()
	 * @see #isDerivedQuery()
	 */
	public boolean isUserDefinedQuery() {
		return this.userDefinedQuery;
	}

	/**
	 * Returns a reference to the {@link QueryString managed query}.
	 *
	 * @return a reference to the {@link QueryString managed query}.
	 * @see org.springframework.data.gemfire.repository.query.QueryString
	 */
	protected @NonNull QueryString getQuery() {
		return this.query;
	}

	/**
	 * Returns a reference to the {@link GemfireTemplate} used to perform all data access and query operations.
	 *
	 * @return a reference to the {@link GemfireTemplate} used to perform all data access and query operations.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	protected @NonNull GemfireTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Object execute(Object[] arguments) {

		QueryMethod queryMethod = getQueryMethod();

		QueryString query = preProcess(queryMethod, getQuery(), arguments);

		String queryString = query.toString();
		String processedQueryString = getQueryPostProcessor().postProcess(queryMethod, queryString, arguments);

		SelectResults<?> selectResults = getTemplate().find(processedQueryString, arguments);

		return postProcess(queryMethod, selectResults);
	}

	QueryString preProcess(QueryMethod queryMethod, QueryString query, Object[] arguments) {

		query = isUserDefinedQuery() ? query
			: query.fromRegion(queryMethod.getEntityInformation().getJavaType(), getTemplate().getRegion());

		ParametersParameterAccessor parameterAccessor =
			new ParametersParameterAccessor(queryMethod.getParameters(), arguments);

		for (Integer index : query.getInParameterIndexes()) {
			query = query.bindIn(toCollection(parameterAccessor.getBindableValue(index - 1)));
		}

		return query;
	}

	Object postProcess(QueryMethod queryMethod, SelectResults<?> selectResults) {

		Collection<?> collection = toCollection(selectResults);

		if (queryMethod.isCollectionQuery()) {
			return collection;
		}
		else if (queryMethod.isQueryForEntity()) {
			if (collection.isEmpty()) {
				return null;
			}
			else if (collection.size() == 1) {
				return collection.iterator().next();
			}
			else {
				throw new IncorrectResultSizeDataAccessException(1, collection.size());
			}
		}
		else if (isSingleNonEntityResult(queryMethod, collection)) {
			return collection.iterator().next();
		}
		else {
			throw newIllegalStateException("Unsupported query: %s", query.toString());
		}
	}

	@SuppressWarnings("all")
	boolean isSingleNonEntityResult(QueryMethod method, Collection<?> result) {

		Class<?> methodReturnType = method.getReturnedObjectType();

		methodReturnType = methodReturnType != null ? methodReturnType : Void.class;

		return CollectionUtils.nullSafeSize(result) == 1
			&& !Void.TYPE.equals(methodReturnType)
			&& !method.isCollectionQuery();
	}

	/**
	 * Returns the given object as a Collection. Collections will be returned as is, Arrays will be converted into a
	 * Collection and all other objects will be wrapped into a single-element Collection.
	 *
	 * @param source the resulting object from the GemFire Query.
	 * @return the querying resulting object as a Collection.
	 * @see java.util.Arrays#asList(Object[])
	 * @see java.util.Collection
	 * @see org.springframework.util.CollectionUtils#arrayToList(Object)
	 * @see org.apache.geode.cache.query.SelectResults
	 */
	Collection<?> toCollection(Object source) {

		if (source instanceof SelectResults) {
			return ((SelectResults<?>) source).asList();
		}

		if (source instanceof Collection) {
			return (Collection<?>) source;
		}

		if (source == null) {
			return Collections.emptyList();
		}

		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singletonList(source);
	}

	@SuppressWarnings("rawtypes")
	enum ProvidedQueryPostProcessors implements QueryPostProcessor<Repository, String> {

		HINT {

			@Override
			public String postProcess(QueryMethod queryMethod, String query, Object... arguments) {

				if (queryMethod instanceof GemfireQueryMethod) {

					GemfireQueryMethod gemfireQueryMethod = (GemfireQueryMethod) queryMethod;

					if (gemfireQueryMethod.hasHint() && !QueryString.HINT_PATTERN.matcher(query).find()) {
						query = QueryString.of(query).withHints(gemfireQueryMethod.getHints()).toString();
					}
				}

				return query;
			}
		},

		IMPORT {

			@Override
			public String postProcess(QueryMethod queryMethod, String query, Object... arguments) {

				if (queryMethod instanceof GemfireQueryMethod) {

					GemfireQueryMethod gemfireQueryMethod = (GemfireQueryMethod) queryMethod;

					if (gemfireQueryMethod.hasImport() && !QueryString.IMPORT_PATTERN.matcher(query).find()) {
						query = QueryString.of(query).withImport(gemfireQueryMethod.getImport()).toString();
					}
				}

				return query;
			}
		},

		LIMIT {

			@Override
			public String postProcess(QueryMethod queryMethod, String query, Object... arguments) {

				if (queryMethod instanceof GemfireQueryMethod) {

					GemfireQueryMethod gemfireQueryMethod = (GemfireQueryMethod) queryMethod;

					if (gemfireQueryMethod.hasLimit() && !QueryString.LIMIT_PATTERN.matcher(query).find()) {
						query = QueryString.of(query).withLimit(gemfireQueryMethod.getLimit()).toString();
					}
				}

				return query;
			}
		},

		TRACE {

			@Override
			public String postProcess(QueryMethod queryMethod, String query, Object... arguments) {

				if (queryMethod instanceof GemfireQueryMethod) {

					GemfireQueryMethod gemfireQueryMethod = (GemfireQueryMethod) queryMethod;

					if (gemfireQueryMethod.hasTrace() && !QueryString.TRACE_PATTERN.matcher(query).find()) {
						query = QueryString.of(query).withTrace().toString();
					}
				}

				return query;
			}
		}
	}
}
