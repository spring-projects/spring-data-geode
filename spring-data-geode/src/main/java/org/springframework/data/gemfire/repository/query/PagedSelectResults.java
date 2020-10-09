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
package org.springframework.data.gemfire.repository.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.domain.Pageable;
import org.springframework.data.gemfire.repository.query.support.PagingUtils;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * An Apache Geode {@link SelectResults} implementation with support for {@literal Paging}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.gemfire.repository.query.AbstractSelectResults
 * @see org.springframework.data.gemfire.repository.query.support.PagingUtils
 * @see org.springframework.data.util.Lazy
 * @since 2.4.0
 */
public class PagedSelectResults<T> extends AbstractSelectResults<T> {

	protected static final String NON_NULL_PAGEABLE_MESSAGE = "Pageable must not be null";

	private Lazy<List<T>> pagedList;

	private Pageable pageRequest;

	/**
	 * Constructs a new instance of {@link PagedSelectResults} initialized with the given, required
	 * {@link SelectResults} and {@link Pageable} object encapsulating the details of the requested page.
	 *
	 * @param selectResults {@link SelectResults} to wrap; must not be {@literal null}.
	 * @param pageable {@link Pageable} object encapsulating the details of the requested page;
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if the {@link SelectResults} or the {@link Pageable} object is {@literal null}.
	 * @see org.apache.geode.cache.query.SelectResults
	 * @see org.springframework.data.domain.Pageable
	 */
	public PagedSelectResults(@NonNull SelectResults<T> selectResults, @NonNull Pageable pageable) {

		super(selectResults);

		Assert.notNull(pageable, NON_NULL_PAGEABLE_MESSAGE);

		this.pageRequest = pageable;
		this.pagedList = newLazyPagedList();
	}

	// WARNING newLazyPagedList() allows the `this` reference to escape when called inside the constructor,
	// but this class makes no Thread-safety guarantees either.
	private Lazy<List<T>> newLazyPagedList() {
		return Lazy.of(() -> PagingUtils.getPagedList(getSelectResults().asList(), getPageRequest()));
	}

	/**
	 * Returns the {@link Pageable} object encapsulating the details of the requested page.
	 *
	 * @return the {@link Pageable} object encapsulating the details of the requested page.
	 * @see org.springframework.data.domain.Pageable
	 */
	protected @NonNull Pageable getPageRequest() {
		return this.pageRequest;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Set<T> asSet() {
		return new HashSet<>(asList());
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<T> asList() {
		return this.pagedList.get();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Iterator<T> iterator() {
		return Collections.unmodifiableList(asList()).iterator();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public int size() {
		return asList().size();
	}

	/**
	 * Builder method used to allow a new {@link Pageable page request} in order to get a different page of results
	 * from the underlying {@link SelectResults}.
	 *
	 * @param pageRequest {@link Pageable} object encapsulating the details of the requested page;
	 * must not be {@literal null}.
	 * @return this {@link PagedSelectResults}.
	 * @throws IllegalArgumentException if {@link Pageable} is {@literal null}.
	 * @see org.springframework.data.domain.Pageable
	 */
	public PagedSelectResults<T> with(@NonNull Pageable pageRequest) {

		Assert.notNull(pageRequest, NON_NULL_PAGEABLE_MESSAGE);

		this.pageRequest = pageRequest;
		this.pagedList = newLazyPagedList();

		return this;
	}
}
