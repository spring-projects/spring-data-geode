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
package org.springframework.data.gemfire.repository.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.types.CollectionType;
import org.apache.geode.cache.query.types.ObjectType;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * An abstract base class implementation of Apache Geode's {@link SelectResults} interface and Java {@link Collection}
 * interface, which delegates to, and is backed by a given, required {@link SelectResults} instance.
 *
 * @author John Blum
 * @see java.util.Collection
 * @see org.apache.geode.cache.query.SelectResults
 * @since 2.4.0
 */
public class AbstractSelectResults<T> implements SelectResults<T> {

	private final SelectResults<T> selectResults;

	/**
	 * Constructs a new instance of {@link SelectResults} initialized with the given, required {@link SelectResults}
	 * instance backing this base class.
	 *
	 * @param selectResults {@link SelectResults} delegate backing this implementation; must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link SelectResults} is {@literal null}.
	 * @see org.apache.geode.cache.query.SelectResults
	 */
	public AbstractSelectResults(@NonNull SelectResults<T> selectResults) {

		Assert.notNull(selectResults, "SelectResults must not be null");

		this.selectResults = selectResults;
	}

	/**
	 * Return the configured, underlying {@link SelectResults} used as the delegate
	 * backing this {@link SelectResults} implementation.
	 *
	 * @return the configured, underlying {@link SelectResults}.
	 * @see org.apache.geode.cache.query.SelectResults
 	 */
	protected @NonNull SelectResults<T> getSelectResults() {
		return this.selectResults;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<T> asList() {
		return getSelectResults().asList();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Set<T> asSet() {
		return getSelectResults().asSet();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public CollectionType getCollectionType() {
		return getSelectResults().getCollectionType();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isModifiable() {
		return getSelectResults().isModifiable();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public int occurrences(T result) {
		return getSelectResults().occurrences(result);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void setElementType(ObjectType objectType) {
		getSelectResults().setElementType(objectType);
	}

	// java.util.Collection interface methods

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean add(T result) {
		return getSelectResults().add(result);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean addAll(Collection<? extends T> results) {
		return getSelectResults().addAll(results);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void clear() {
		getSelectResults().clear();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean contains(Object result) {
		return getSelectResults().contains(result);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean containsAll(Collection<?> results) {
		return getSelectResults().containsAll(results);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isEmpty() {
		return getSelectResults().isEmpty();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Iterator<T> iterator() {
		return getSelectResults().iterator();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean remove(Object result) {
		return getSelectResults().remove(result);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean removeAll(Collection<?> results) {
		return getSelectResults().removeAll(results);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean retainAll(Collection<?> results) {
		return getSelectResults().retainAll(results);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public int size() {
		return getSelectResults().size();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Object[] toArray() {
		return getSelectResults().toArray();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@SuppressWarnings("all")
	public <E> E[] toArray(E[] array) {
		return getSelectResults().toArray(array);
	}
}
