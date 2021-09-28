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
package org.springframework.data.gemfire.repository.query;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.repository.Query;
import org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor;
import org.springframework.data.gemfire.repository.query.support.PagingUtils;
import org.springframework.data.gemfire.repository.query.support.TemplateBasedOqlQueryExecutor;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link GemfireRepositoryQuery} using plain {@link String} based OQL queries.
 *
 * @author Oliver Gierke
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.domain.Page
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.domain.Sort
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.Query
 * @see org.springframework.data.gemfire.repository.query.GemfireRepositoryQuery
 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.query.QueryMethod
 * @see org.springframework.data.repository.query.RepositoryQuery
 */
@SuppressWarnings("unused")
public class StringBasedGemfireRepositoryQuery extends GemfireRepositoryQuery {

	private static final String INVALID_QUERY = "Modifying queries are not supported";

	private volatile boolean userDefinedQuery = false;

	private final GemfireTemplate template;

	private final OqlQueryExecutor nonPagedQueryExecutor;
	private final OqlQueryExecutor pagedQueryExecutor;

	private final QueryString query;

	/**
	 * Constructor used for testing purposes only!
	 */
	StringBasedGemfireRepositoryQuery() {

		this.query = null;
		this.nonPagedQueryExecutor = (queryMethod, query, arguments) -> null;
		this.pagedQueryExecutor = (queryMethod, query, arguments) -> null;
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

		this.nonPagedQueryExecutor = new TemplateBasedOqlQueryExecutor(template);

		this.pagedQueryExecutor = new SmartPagedOqlQueryExecutor(template)
			.thenExecuteWith(new TwoPhasePagedOqlQueryExecutor(template)
				.thenExecuteWith(new TemplateBasedOqlQueryExecutor(template)));

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
	 * Returns the configured {@link OqlQueryExecutor} (strategy) used to execute Apache Geode
	 * {@link Page non-paged} {@link String OQL queries}.
	 *
	 * @return the configured {@link OqlQueryExecutor} (strategy) used to execute Apache Geode
	 * {@link Page non-paged} {@link String OQL queries}.
	 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
	 */
	protected @NonNull OqlQueryExecutor getNonPagedQueryExecutor() {
		return this.nonPagedQueryExecutor;
	}

	/**
	 * Returns the configured {@link OqlQueryExecutor} (strategy) used to execute Apache Geode
	 * {@link Page paged} {@link String OQL queries}.
	 *
	 * @return the configured {@link OqlQueryExecutor} (strategy) used to execute Apache Geode
	 * {@link Page paged} {@link String OQL queries}.
	 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
	 */
	protected @NonNull OqlQueryExecutor getPagedQueryExecutor() {
		return this.pagedQueryExecutor;
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

		QueryString query = getQuery();

		String preparedQuery = prepareQuery(queryMethod, query, arguments);

		SelectResults<?> selectResults =
			resolveOqlQueryExecutor(queryMethod).execute(queryMethod, preparedQuery, arguments);

		return processQueryResults(queryMethod, selectResults, arguments);
	}

	/**
	 * Prepares the OQL query statement to execute.
	 *
	 * @param queryMethod {@link QueryMethod} modeling the OQL query.
	 * @param query {@link QueryString} containing the OQL query statement.
	 * @param arguments array of {@link Object} values containing the arguments for the OQL query bind in parameters.
	 * @return the {@literal prepared} OQL query to execute.
	 * @see org.springframework.data.gemfire.repository.query.QueryPostProcessor
	 * @see org.springframework.data.gemfire.repository.query.QueryString
	 * @see org.springframework.data.repository.query.QueryMethod
	 * @see #bindInParameters(QueryMethod, QueryString, Object[])
	 * @see #resolveFromClause(QueryMethod, QueryString)
	 * @see #getQueryPostProcessor()
	 */
	protected @NonNull String prepareQuery(@NonNull QueryMethod queryMethod, @NonNull QueryString query,
			@NonNull Object[] arguments) {

		query = bindInParameters(queryMethod, resolveFromClause(queryMethod, query), arguments);

		String queryString = query.toString();
		String processedQueryString = getQueryPostProcessor().postProcess(queryMethod, queryString, arguments);

		return processedQueryString;
	}

	private QueryString bindInParameters(QueryMethod queryMethod, QueryString query, Object[] arguments) {

		Parameters<?, ?> queryMethodParameters = queryMethod.getParameters();

		ParametersParameterAccessor parameterAccessor =
			new ParametersParameterAccessor(queryMethodParameters, arguments);

		for (Integer index : query.getInParameterIndexes()) {
			query = query.bindIn(toCollection(parameterAccessor.getBindableValue(index - 1)));
		}

		return query;
	}

	private QueryString resolveFromClause(QueryMethod queryMethod, QueryString query) {

		return isUserDefinedQuery() ? query
			: query.fromRegion(getTemplate().getRegion(), queryMethod.getEntityInformation().getJavaType());
	}

	/**
	 * Resolves the {@link OqlQueryExecutor} used to execute the {@link String OQL query statement} modeled by
	 * the given {@link QueryMethod}.
	 *
	 * @param queryMethod {@link QueryMethod} used to resolve the {@link OqlQueryExecutor}; must not be {@literal null}.
	 * @return the resolve {@link OqlQueryExecutor} appropriate for executing the {@link String OQL query statement}
	 * modeled by the give {@link QueryMethod}.
	 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
	 * @see org.springframework.data.repository.query.QueryMethod
	 */
	protected @NonNull OqlQueryExecutor resolveOqlQueryExecutor(@NonNull QueryMethod queryMethod) {

		return PagingUtils.isPagingPresent(queryMethod)
			? getPagedQueryExecutor()
			: getNonPagedQueryExecutor();
	}

	/**
	 * Processes the OQL query {@link SelectResults result set}.
	 *
	 * @param queryMethod {@link QueryMethod} modeling the OQL query.
	 * @param selectResults {@link SelectResults} from the execution of the OQL query.
	 * @return the OQL query results.
	 * @throws IncorrectResultSizeDataAccessException if the query result does not match
	 * the {@link QueryMethod} {@link Class return type}.
	 * @throws IllegalStateException if the OQL query is not supported based on the return value.
	 * @see org.springframework.data.repository.query.QueryMethod
	 * @see org.apache.geode.cache.query.SelectResults
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected @Nullable Object processQueryResults(@NonNull QueryMethod queryMethod,
			@NonNull SelectResults<?> selectResults, @NonNull Object... arguments) {

		Collection collection = toCollection(selectResults);

		if (queryMethod.isCollectionQuery()) {
			return collection;
		}
		else if (queryMethod.isPageQuery()) {
			return new PageImpl<Object>(new ArrayList<>(collection), PagingUtils.getPageRequest(queryMethod, arguments), Integer.MAX_VALUE);
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

	private boolean isSingleNonEntityResult(QueryMethod method, Collection<?> result) {

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
	@SuppressWarnings("rawtypes")
	@NonNull Collection toCollection(@Nullable Object source) {

		return source == null ? Collections.emptyList()
			: source instanceof SelectResults ? ((SelectResults) source).asList()
			: source instanceof Collection ? (Collection<?>) source
			: source.getClass().isArray() ? CollectionUtils.arrayToList(source)
			: Collections.singletonList(source);
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

	/**
	 * A {@link SimplePagedOqlQueryExecutor} implementation that implements a paged OQL query statement
	 * using a 2-phase approach.
	 *
	 * The first phase executes a {@literal keys query} (or OQL query for keys) satisfying the user's defined
	 * OQL query predicate(s) specified in the {@literal WHERE} clause, {@link Sort sorted} according to
	 * the user-defined {@literal ORDER BY} clause.
	 *
	 * The returned keys are then filtered by the {@link Pageable requested page}.
	 *
	 * Then, in the second (and final) phase, the keys returned by the {@literal keys query} are used as a predicate
	 * in the user's original OQL query statement to limit the results returned to exactly those keys satisfying
	 * the {@link Pageable requested page}.
	 *
	 * @see SimplePagedOqlQueryExecutor
	 */
	static class TwoPhasePagedOqlQueryExecutor extends SimplePagedOqlQueryExecutor {

		/**
		 * Constructs a new instance of {@link TwoPhasePagedOqlQueryExecutor} initialized with the given, required
		 * {@link GemfireTemplate}.
		 *
		 * @param template {@link GemfireTemplate} used to execute Apache Geode OQL queries; must not be {@literal null}.
		 * @throws IllegalArgumentException if {@link GemfireTemplate} is {@literal null}.
		 * @see org.springframework.data.gemfire.GemfireTemplate
		 */
		TwoPhasePagedOqlQueryExecutor(@NonNull GemfireTemplate template) {
			super(template);
		}

		/**
		 * Simply return the {@link SelectResults} as is.
		 *
		 * The {@link SelectResults} were already limited to the {@link Pageable requested page} in this 2-phased
		 * paged query implementation.
		 *
		 * @param selectResults {@link SelectResults} to process; must not be {@literal null}.
		 * @param pageRequest {@link Pageable} object encapsulating the details of the {@link Page requested page};
		 * must not be {@literal null}.
		 * @return the {@link SelectResults} as is.
		 * @see org.apache.geode.cache.query.SelectResults
		 * @see org.springframework.data.domain.Pageable
		 */
		@Override
		@SuppressWarnings("rawtypes")
		protected SelectResults processPagedQueryResults(SelectResults selectResults, Pageable pageRequest) {
			//return selectResults;
			return super.processPagedQueryResults(selectResults, pageRequest);
		}
	}

	/**
	 * A {@literal smart} {@link PageLimitingOqlQueryExecutor} implementation that looks ahead at
	 * the {@link Pageable requested page}, and if the user requested page on or the number of results needed
	 * to satisfy the contents of the page are withing a pre-defined, configurable/tunable threshold, then the limited
	 * {@link SelectResults OQL query result set} is returned.
	 *
	 * Alternatively, a 2-phased paged query can be used when the page number or page size is relatively large. Another
	 * consideration is the size of the objects returned in the {@link SelectResults OQL query result set}.
	 *
	 * @see PageLimitingOqlQueryExecutor
	 */
	class SmartPagedOqlQueryExecutor extends PageLimitingOqlQueryExecutor {

		// TODO enable this parameter to be configurable/tunable using the Spring Environment
		final int PAGED_QUERY_RESULT_SET_LIMIT_THRESHOLD =
			Integer.getInteger("spring.data.gemfire.query.limit.threshold", 101);

		/**
		 * Constructs a new instance of {@link SmartPagedOqlQueryExecutor} initialized with the given, required
		 * {@link GemfireTemplate}.
		 *
		 * @param template {@link GemfireTemplate} used to execute Apache Geode OQL queries; must not be {@literal null}.
		 * @throws IllegalArgumentException if {@link GemfireTemplate} is {@literal null}.
		 * @see org.springframework.data.gemfire.GemfireTemplate
		 */
		SmartPagedOqlQueryExecutor(@NonNull GemfireTemplate template) {
			super(template);
		}

		/**
		 * @inheritDoc
		 */
		@Override
		@SuppressWarnings("rawtypes")
		protected SelectResults doExecute(Pageable pageRequest, QueryMethod queryMethod, String query,
			Object... arguments) {

			if (isExecutable(pageRequest)) {
				return super.doExecute(pageRequest, queryMethod, query, arguments);
			}

			throw newUnsupportedQueryExecutionException(query);
		}

		/**
		 * Determines whether this {@link OqlQueryExecutor} can execute the {@link String OQL query} directly (as is).
		 *
		 * Just execute the OQL query as is, bypassing the 2-phase implementation, if the requested page
		 * is the first page or the query result set size would be less than the threshold (e.g. 101 results).
		 * Dynamically supports 10 pages if page size were 10, 5 pages if page size were 20, 4 pages if page size
		 * were 25, 2 pages if page size were 50 and so on.
		 *
		 * @param pageRequest {@link Pageable} object encapsulating the details of the requested page.
		 * @return a boolean value indicating whether the original {@link String OQL query} should be executed directly,
		 * bypassing the 2-phase implementation.
		 * @see org.springframework.data.domain.Pageable
		 */
		protected boolean isExecutable(@NonNull Pageable pageRequest) {

			return PagingUtils.isPageOne(pageRequest)
				|| PagingUtils.getQueryResultSetLimitForPage(pageRequest) < PAGED_QUERY_RESULT_SET_LIMIT_THRESHOLD;
		}
	}

	/**
	 * A {@link SimplePagedOqlQueryExecutor} implementation that applies a {@literal LIMIT} to
	 * the {@link String OQL query statement} based on the {@link Pageable requested page} in order to limit
	 * the {@link SelectResults query result set}, or number of {@link Object results}, returned by
	 * the {@link String OQL query}.
	 *
	 * @see SimplePagedOqlQueryExecutor
	 */
	class PageLimitingOqlQueryExecutor extends SimplePagedOqlQueryExecutor {

		/**
		 * Constructs a new instance of {@link PageLimitingOqlQueryExecutor} initialized with the given, required
		 * {@link GemfireTemplate}.
		 *
		 * @param template {@link GemfireTemplate} used to execute Apache Geode OQL queries; must not be {@literal null}.
		 * @throws IllegalArgumentException if {@link GemfireTemplate} is {@literal null}.
		 * @see org.springframework.data.gemfire.GemfireTemplate
		 */
		PageLimitingOqlQueryExecutor(@NonNull GemfireTemplate template) {
			super(template);
		}

		/**
		 * @inheritDoc
		 */
		@Override
		protected String preparePagedQuery(String query, Pageable pageRequest) {

			String pagedQuery = super.preparePagedQuery(query, pageRequest);

			QueryString pagedQueryString = QueryString.of(pagedQuery);

			int pagedQueryResultSetLimit = PagingUtils.getQueryResultSetLimitForPage(pageRequest);

			if (pagedQueryString.isLimited()) {

				int queryLimit = pagedQueryString.getLimit();

				if (pagedQueryResultSetLimit < queryLimit) {
					pagedQueryString = pagedQueryString.adjustLimit(pagedQueryResultSetLimit);
				}
				else {

					int startIndex = PagingUtils.getQueryResultSetStartIndexForPage(pageRequest);

					Assert.state(queryLimit > startIndex,
						() -> String.format("The user-defined OQL query result set LIMIT [%d] must be greater than the requested page offset [%d]",
							queryLimit, startIndex));

					int endIndex = PagingUtils.getQueryResultSetEndIndexForPage(pageRequest);

					if (queryLimit < endIndex) {
						getLogger().warn(String.format("The requested page ending at index [%d] may be truncated by the user-defined OQL query result set LIMIT [%d]",
							endIndex, queryLimit));
					}
				}
			}
			else {
				pagedQueryString = pagedQueryString.withLimit(pagedQueryResultSetLimit);
			}

			return pagedQueryString.toString();
		}
	}

	/**
	 * Abstract base class for {@link Page paged} OQL queries and {@link OqlQueryExecutor} implementations.
	 *
	 * This base class implementation simply returns the entire/full (i.e. non-limited) {@link SelectResults OQL query result set}
	 * and then performs the paging logic to extract subsets of the results based on the {@link Page requested page}.
	 *
	 * @see org.springframework.data.gemfire.repository.query.support.TemplateBasedOqlQueryExecutor
	 */
	static abstract class SimplePagedOqlQueryExecutor extends TemplateBasedOqlQueryExecutor {

		/**
		 * Constructs a new instance of {@link SimplePagedOqlQueryExecutor} initialized with the given, required
		 * {@link GemfireTemplate}.
		 *
		 * @param template {@link GemfireTemplate} used to execute Apache Geode OQL queries; must not be {@literal null}.
		 * @throws IllegalArgumentException if {@link GemfireTemplate} is {@literal null}.
		 * @see org.springframework.data.gemfire.GemfireTemplate
		 */
		SimplePagedOqlQueryExecutor(@NonNull GemfireTemplate template) {
			super(template);
		}

		/**
		 * @inheritDoc
		 */
		@Override
		@SuppressWarnings("rawtypes")
		public @NonNull SelectResults execute(@NonNull QueryMethod queryMethod, @NonNull String query,
				@NonNull Object... arguments) {

			if (PagingUtils.isPagingPresent(queryMethod)) {

				Pageable pageRequest = PagingUtils.getPageRequest(queryMethod, arguments);

				return doExecute(pageRequest, queryMethod, query, arguments);
			}

			throw newUnsupportedQueryExecutionException(query);
		}

		/**
		 * Executes the {@link String OQL query statement}.
		 *
		 * @param pageRequest {@link Pageable} object encapsulating the details of the {@link Page requested paged};
		 * must not be {@literal null}.
		 * @param queryMethod {@link QueryMethod} modeling the {@link String OQL query statement} to be executed;
		 * must not be {@literal null}.
		 * @param query {@link String} containing the OQL query statement;
		 * must not be {@literal null} or {@literal empty}.
		 * @param arguments array of {@link Object arguments} passed to the placeholders in
		 * the {@link String OQL query statement}.
		 * @return the {@link SelectResults} from executing the {@link String OQL query statement}.
		 * @see org.apache.geode.cache.query.SelectResults
		 * @see org.springframework.data.domain.Pageable
		 * @see org.springframework.data.repository.query.QueryMethod
		 */
		@SuppressWarnings("rawtypes")
		protected SelectResults doExecute(@NonNull Pageable pageRequest, @NonNull QueryMethod queryMethod,
				@NonNull String query, @NonNull Object... arguments) {

			String preparedQuery = preparePagedQuery(query, pageRequest);

			SelectResults selectResults = super.execute(queryMethod, preparedQuery, arguments);

			return processPagedQueryResults(selectResults, pageRequest);
		}

		/**
		 * Prepares the required {@link String OQL query statement} as a paged query.
		 *
		 * @param query {@link String} containing the OQL query statement to prepare.
		 * @param pageRequest {@link Pageable} object containing the details of the {@link Page requested page}.
		 * @return the prepared {@link String OQL query}.
		 * @see org.springframework.data.domain.Pageable
		 */
		protected @NonNull String preparePagedQuery(String query, Pageable pageRequest) {
			return query;
		}

		/**
		 * Processes the {@link SelectResults} as a {@link Page paged} query result set.
		 *
		 * @param selectResults {@link SelectResults} to process; must not be {@literal null}.
		 * @param pageRequest {@link Pageable} object encapsulating the details of the {@link Page requested page};
		 * must not be {@literal null}.
		 * @return the processed {@link SelectResults}.
		 * @see org.apache.geode.cache.query.SelectResults
		 * @see org.springframework.data.domain.Pageable
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected @NonNull SelectResults processPagedQueryResults(@NonNull SelectResults selectResults,
				@NonNull Pageable pageRequest) {

			return new PagedSelectResults(selectResults, pageRequest);
		}
	}
}
