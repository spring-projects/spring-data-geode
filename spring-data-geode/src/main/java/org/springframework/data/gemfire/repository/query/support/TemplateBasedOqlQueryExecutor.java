/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * {@link OqlQueryExecutor} implementation using SDG's {@link GemfireTemplate} to execute Apache Geode
 * {@link String OQL queries}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
 * @see org.springframework.data.repository.query.QueryMethod
 * @since 2.4.0
 */
public class TemplateBasedOqlQueryExecutor implements OqlQueryExecutor {

	private final GemfireTemplate template;

	/**
	 * Constructs a new instance of {@link TemplateBasedOqlQueryExecutor} initialized with the given,
	 * required {@link GemfireTemplate} used to execute Apache Geode {@link String OQL queries}.
	 *
	 * @param template {@link GemfireTemplate} used to execute Apache Geode {@link String OQL queries};
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link GemfireTemplate} is {@literal null}.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	public TemplateBasedOqlQueryExecutor(@NonNull GemfireTemplate template) {

		Assert.notNull(template, "GemfireTemplate must not be null");

		this.template = template;
	}

	/**
	 * Gets the configured {@link GemfireTemplate} used to execute Apache Geode {@link String OQL queries}.
	 *
	 * @return the configured {@link GemfireTemplate} used to execute Apache Geode {@link String OQL queries}.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	protected @NonNull GemfireTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public SelectResults execute(QueryMethod queryMethod, @NonNull String query, @NonNull Object... arguments) {
		return getTemplate().find(query, arguments);
	}
}
