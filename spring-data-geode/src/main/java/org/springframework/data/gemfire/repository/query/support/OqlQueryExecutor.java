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
package org.springframework.data.gemfire.repository.query.support;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.NonNull;

/**
 * A Strategy interface for executing Apache Geode OQL queries (e.g. {@literal SELECT} statements).
 *
 * @author John Blum
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.repository.query.QueryMethod
 * @since 2.4.0
 */
@FunctionalInterface
public interface OqlQueryExecutor {

	String NON_EXECUTABLE_QUERY_MESSAGE = "OQL query [%1$s] is not executable by this executor [%2$s]";

	/**
	 * Executes the given {@link String OQL query}.
	 *
	 * @param queryMethod {@link QueryMethod} modeling the OQl query.
	 * @param query {@link String} containing the Apache Geode OQL query.
	 * @param arguments array of {@link Object arguments} used for the bind in OQL query parameters.
	 * @return the {@link SelectResults OQL query result set}.
	 * @throws UnsupportedQueryExecutionException if this {@link OqlQueryExecutor} cannot execute (i.e. handle)
	 * the OQL query.
	 * @see org.springframework.data.repository.query.QueryMethod
	 * @see org.apache.geode.cache.query.SelectResults
	 */
	@SuppressWarnings("rawtypes")
	SelectResults execute(QueryMethod queryMethod, String query, Object... arguments);

	/**
	 * Constructs a new instance of {@link UnsupportedQueryExecutionException} initialized with a canned message
	 * containing the given OQL query that could not be executed by the {@link OqlQueryExecutor} implementation.
	 *
	 * @param query {@link String OQL query} that could not be executed.
	 * @return a new {@link UnsupportedQueryExecutionException}.
	 * @see org.springframework.data.gemfire.repository.query.support.UnsupportedQueryExecutionException
	 */
	default UnsupportedQueryExecutionException newUnsupportedQueryExecutionException(String query) {
		return new UnsupportedQueryExecutionException(String.format(NON_EXECUTABLE_QUERY_MESSAGE, query,
			this.getClass().getName()));
	}

	/**
	 * Null-safe composition method to {@literal compose} {@literal this} {@link OqlQueryExecutor} with
	 * the given {@link OqlQueryExecutor}.
	 *
	 * {@link OqlQueryExecutor} implementations should be {@literal composed} in an order that is most suitable to
	 * the execution of the OQL query first.  Meaning, the outer most {@link OqlQueryExecutor} should be the most
	 * suitable {@link OqlQueryExecutor} to execute the given OQL query followed by the next most suitable
	 * {@link OqlQueryExecutor} in the composition (i.e. chain) and so on until the OQL query is either successfully
	 * executed (handled) or the composition is exhausted, in which case, an {@link Exception} could be thrown.
	 *
	 * If an {@link OqlQueryExecutor is unable to execute, or handle, the given OQL query, then it must throw
	 * an {@link UnsupportedQueryExecutionException } to triggger the next {@link OqlQueryExecutor} in the composition.
	 *
	 * @param queryExecutor {@link OqlQueryExecutor} to compose with this {@link OqlQueryExecutor};
	 * must not be {@literal null}.
	 * @return a composed {@link OqlQueryExecutor} consisting of this {@link OqlQueryExecutor} composed with
	 * the given {@link OqlQueryExecutor}. If the {@link OqlQueryExecutor} is {@literal null}, then this method
	 * returns this {@link OqlQueryExecutor}.
	 * @see <a href="https://en.wikipedia.org/wiki/Composite_pattern">Composite Software Design Pattern</a>
	 */
	default OqlQueryExecutor thenExecuteWith(@NonNull OqlQueryExecutor queryExecutor) {

		return queryExecutor == null ? this
			: (queryMethod, query, arguments) -> {
				try {
					return this.execute(queryMethod, query, arguments);
				}
				catch (UnsupportedQueryExecutionException cause) {
					return queryExecutor.execute(queryMethod, query, arguments);
				}
			};
	}
}
