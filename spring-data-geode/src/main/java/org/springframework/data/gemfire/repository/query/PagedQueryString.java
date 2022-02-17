/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * {@link QueryString} implementation handling {@literal paging} functionality and behavior.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.repository.query.QueryString
 * @since 2.4.0
 */
public class PagedQueryString extends QueryString {

	/**
	 * Factory method used to construct a new instance of {@link PagedQueryString} from an existing, {@literal non-null}
	 * {@link QueryString}.
	 *
	 * @param queryString {@link QueryString} on which the {@link PagedQueryString} will be based.
	 * @return a new instance of {@link PagedQueryString} initialized with the {@literal OQL query}
	 * from the given {@link QueryString}.
	 * @throws IllegalArgumentException if {@link QueryString} is {@literal null}.
	 * @see org.springframework.data.gemfire.repository.query.QueryString
	 * @see #of(String)
	 */
	public static PagedQueryString of(@NonNull QueryString queryString) {

		Assert.notNull(queryString, "QueryString must not be null");

		return of(queryString.getQuery());
	}

	/**
	 * Factory method used to construct a new instance of {@link PagedQueryString} initialized with
	 * the given {@literal OQL-based query}.
	 *
	 * @param query {@link String} containing the OQL query statement.
	 * @return a new instance of {@link PagedQueryString} initialized with the given {@literal OQL-based query}.
	 * @throws IllegalArgumentException if the {@link String OQL query} is {@literal null} or {@literal empty}.
	 * @see #PagedQueryString(String)
	 */
	public static PagedQueryString of(@NonNull String query) {
		return new PagedQueryString(query);
	}

	private GemfireQueryMethod queryMethod;

	/**
	 * Constructs a new instance of {@link PagedQueryString} initialized with the given {@literal OQL-based query}.
	 *
	 * @param query {@link String} containing the OQL query statement.
	 * @throws IllegalArgumentException if the {@link String OQL query} is {@literal null} or {@literal empty}.
	 */
	public PagedQueryString(@NonNull String query) {
		super(query);
	}

	protected Optional<GemfireQueryMethod> getQueryMethod() {
		return Optional.ofNullable(this.queryMethod);
	}

	public PagedQueryString withQueryMethod(GemfireQueryMethod queryMethod) {
		this.queryMethod = queryMethod;
		return this;
	}
}
