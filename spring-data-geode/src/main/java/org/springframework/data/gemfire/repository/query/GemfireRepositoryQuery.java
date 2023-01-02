/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Apache Geode specific {@link RepositoryQuery} implementations.
 *
 * @author Oliver Gierke
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.query.RepositoryQuery
 * @see org.springframework.data.gemfire.repository.query.GemfireQueryMethod
 */
@SuppressWarnings("rawtypes")
public abstract class GemfireRepositoryQuery implements RepositoryQuery {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final GemfireQueryMethod queryMethod;

	private QueryPostProcessor<Repository, String> queryPostProcessor = ProvidedQueryPostProcessor.IDENTITY;

	/**
	 * Constructor used for testing purposes only!
	 */
	GemfireRepositoryQuery() {
		this.queryMethod = null;
	}

	/**
	 * Constructs a new instance of {@link GemfireRepositoryQuery} initialized with
	 * the given {@link GemfireQueryMethod} implementing the {@link Repository} {@link QueryMethod}.
	 *
	 * @param queryMethod {@link GemfireQueryMethod} capturing the metadata and implementation of the {@link Repository}
	 * {@link QueryMethod}; must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link GemfireQueryMethod} is {@literal null}.
	 * @see org.springframework.data.gemfire.repository.query.GemfireQueryMethod
	 */
	public GemfireRepositoryQuery(GemfireQueryMethod queryMethod) {

		Assert.notNull(queryMethod, "QueryMethod must not be null");

		this.queryMethod = queryMethod;
	}

	/**
	 * Returns a reference to the {@link Repository} {@link GemfireQueryMethod} modeling the Apache Geode OQL query.
	 *
	 * @return a reference to the {@link Repository} {@link GemfireQueryMethod} modeling the Apache Geode OQL query.
	 * @see org.springframework.data.gemfire.repository.query.GemfireQueryMethod
	 * @see #getQueryMethod()
	 */
	protected @NonNull GemfireQueryMethod getGemfireQueryMethod() {
		return (GemfireQueryMethod) getQueryMethod();
	}

	/**
	 * Returns the configured SLF4J {@link Logger} used log statements.
	 *
	 * @return the configured SLF4J {@link Logger}.
	 * @see org.slf4j.Logger
	 */
	protected @NonNull Logger getLogger() {
		return this.logger;
	}

	/**
	 * Returns a reference to the {@link Repository} {@link QueryMethod} modeling the data store query.
	 *
	 * @return a reference to the {@link Repository} {@link QueryMethod} modeling the data store query.
	 * @see org.springframework.data.repository.query.QueryMethod
	 */
	@Override
	public @NonNull QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	/**
	 * Returns a reference to the composed {@link QueryPostProcessor QueryPostProcessors} that are applied to
	 * {@literal OQL queries} prior to execution.
	 *
	 * @return a reference to the composed {@link QueryPostProcessor QueryPostProcessors}.
	 * @see org.springframework.data.gemfire.repository.query.QueryPostProcessor
	 */
	protected @NonNull QueryPostProcessor<Repository, String> getQueryPostProcessor() {
		return this.queryPostProcessor;
	}

	/**
	 * Registers the given {@link QueryPostProcessor} to use for processing {@literal OQL queries}
	 * generated from {@link Repository} {@link QueryMethod query methods}.
	 *
	 * Registration always links the given {@link QueryPostProcessor} to the end of the processing chain
	 * of previously registered {@link QueryPostProcessor QueryPostProcessors}.  In other words, the given
	 * {@link QueryPostProcessor} argument will process {@literal OQL queries} only after all
	 * {@link QueryPostProcessor QueryPostProcessor} registered before it.
	 *
	 * @param queryPostProcessor {@link QueryPostProcessor} to register.
	 * @return this {@link GemfireRepositoryQuery}.
	 * @see org.springframework.data.gemfire.repository.query.QueryPostProcessor#processBefore(QueryPostProcessor)
	 */
	public GemfireRepositoryQuery register(@Nullable QueryPostProcessor<Repository, String> queryPostProcessor) {
		this.queryPostProcessor = this.queryPostProcessor.processBefore(queryPostProcessor);
		return this;
	}

	@SuppressWarnings("rawtypes")
	enum ProvidedQueryPostProcessor implements QueryPostProcessor<Repository, String> {

		IDENTITY {

			@Override
			public String postProcess(QueryMethod queryMethod, String query, Object... arguments) {
				return query;
			}
		}
	}
}
