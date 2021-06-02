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
package org.springframework.data.gemfire.repository.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.Wrapper;
import org.springframework.data.gemfire.repository.query.QueryString;
import org.springframework.data.gemfire.repository.query.support.PagingUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple, basic {@link PagingAndSortingRepository} / {@link CrudRepository} implementation for Apache Geode.
 *
 * @author Oliver Gierke
 * @author David Turanski
 * @author John Blum
 * @author Jens Schauder
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheTransactionManager
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.GemfireRepository
 * @see org.springframework.data.repository.CrudRepository
 * @see org.springframework.data.repository.PagingAndSortingRepository
 * @see org.springframework.data.repository.core.EntityInformation
 */
public class SimpleGemfireRepository<T, ID> implements GemfireRepository<T, ID> {

	protected static final String SELECT_COUNT_OQL_QUERY = "SELECT count(*) FROM %s";

	private final EntityInformation<T, ID> entityInformation;

	private final GemfireTemplate template;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Constructs a new instance of {@link SimpleGemfireRepository} initialized with the {@link GemfireTemplate}
	 * and {@link EntityInformation}.
	 *
	 * @param template {@link GemfireTemplate} used to perform basic data access operations and simple OQL queries;
	 * must not be {@literal null}.
	 * @param entityInformation {@link EntityInformation} that describes the entity; must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link GemfireTemplate} or {@link EntityInformation} is {@literal null}.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 * @see org.springframework.data.repository.core.EntityInformation
	 */
	public SimpleGemfireRepository(@NonNull GemfireTemplate template,
			@NonNull EntityInformation<T, ID> entityInformation) {

		Assert.notNull(template, "GemfireTemplate must not be null");
		Assert.notNull(entityInformation, "EntityInformation must not be null");

		this.template = template;
		this.entityInformation = entityInformation;
	}

	/**
	 * Returns a reference to the {@link EntityInformation} type describing the entity.
	 *
	 * @return a reference to the {@link EntityInformation} type describing the entity.
	 * @see org.springframework.data.repository.core.EntityInformation
	 */
	public @NonNull EntityInformation<T, ID> getEntityInformation() {
		return this.entityInformation;
	}

	/**
	 * Returns a reference to the SLF4J {@link Logger} used to log the operations of this {@link GemfireRepository}.
	 *
	 * @return a reference to the SLF4J {@link Logger} used to log the operations of this {@link GemfireRepository}.
	 * @see org.slf4j.Logger
	 */
	public @NonNull Logger getLogger() {
		return this.logger;
	}

	/**
	 * Gets the {@link Region} to which this {@link GemfireRepository} performs all data access operations.
	 *
	 * @return a reference to the {@link Region} on which this {@link GemfireRepository} operates.
	 * @see org.apache.geode.cache.Region
	 * @see #getTemplate()
	 */
	public @NonNull Region<ID, T> getRegion() {
		return getTemplate().getRegion();
	}

	/**
	 * Returns a reference to the {@link GemfireTemplate} used by this {@link GemfireRepository} to perform basic
	 * CRUD and simple OQL queries data access operations
	 *
	 * @return a reference to the {@link GemfireTemplate} used by this {@link GemfireRepository}.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	public @NonNull GemfireTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public <U extends T> U save(@NonNull U entity) {

		ID id = getEntityInformation().getRequiredId(entity);

		// CREATE/UPDATE entity in Region
		T existingValue = getTemplate().put(id, entity);

		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Overwrote existing value [{}] for ID [{}]", existingValue, id);
		}

		return entity;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public T save(@NonNull Wrapper<T, ID> wrapper) {

		T entity = wrapper.getEntity();

		// CREATE/UPDATE entity in Region
		T existingValue = getTemplate().put(wrapper.getKey(), entity);

		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Overwrote existing value [{}] for ID [{}]", existingValue, wrapper.getKey());
		}

		return entity;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public <U extends T> Iterable<U> saveAll(@NonNull Iterable<U> entities) {

		EntityInformation<T, ID> entityInformation = getEntityInformation();

		Map<ID, U> entitiesToSave = new HashMap<>();

		Streamable.of(CollectionUtils.nullSafeIterable(entities)).stream()
			.filter(Objects::nonNull)
			.forEach(entity -> entitiesToSave.put(entityInformation.getRequiredId(entity), entity));

		if (!entitiesToSave.isEmpty()) {
			getTemplate().putAll(entitiesToSave);
		}

		return entitiesToSave.values();
	}

	/**
	 * Counts the number of entities stored in the {@link Region}.
	 *
	 * This method executes a {@literal SELECT count(*) FROM /Region} OQL query.
	 *
	 * @return a count of the number of entities stored in the {@link Region}.
	 */
	@Override
	public long count() {

		String regionPath = getRegion().getFullPath();
		String countQuery = String.format(SELECT_COUNT_OQL_QUERY, regionPath);

		SelectResults<Integer> results = getTemplate().find(countQuery);

		return Optional.ofNullable(results)
			.map(SelectResults::iterator)
			.filter(Iterator::hasNext)
			.map(Iterator::next)
			.map(Long::valueOf)
			.orElse(0L);
	}

	/**
	 * Determines whether an entity with the given ID is stored in the {@link Region}.
	 *
	 * @param id {@link Long} value identifying the entity.
	 * @return a boolean value indicating whether an entity with the given ID is stored in the {@link Region}.
	 * @see #findById(Object)
	 */
	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public @NonNull Iterable<T> findAll() {

		String regionPath = getRegion().getFullPath();
		String query = String.format("SELECT * FROM %s", regionPath);

		SelectResults<T> selectResults = getTemplate().find(query);

		return toList(selectResults);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Page<T> findAll(@NonNull Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null");

		Iterable<T> results = findAll(pageable.getSort());

		return toPage(results, pageable);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public @NonNull Iterable<T> findAll(@NonNull Sort sort) {

		QueryString query = QueryString.of("SELECT * FROM /RegionPlaceholder")
			.fromRegion(getRegion(), getEntityInformation().getJavaType())
			.orderBy(sort);

		SelectResults<T> selectResults = getTemplate().find(query.toString());

		return toList(selectResults);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public @NonNull Iterable<T> findAllById(@NonNull Iterable<ID> ids) {

		List<ID> keys = Streamable.of(CollectionUtils.nullSafeIterable(ids)).stream()
			.filter(Objects::nonNull)
			.collect(StreamUtils.toUnmodifiableList());

		Map<ID, T> keysValues = !keys.isEmpty()
			? getTemplate().getAll(keys)
			: Collections.emptyMap();

		List<T> values = CollectionUtils.nullSafeMap(keysValues).values().stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		return values;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Optional<T> findById(@NonNull ID id) {

		T value = id != null
			? getTemplate().get(id)
			: null;

		return Optional.ofNullable(value);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void delete(@NonNull T entity) {
		deleteById(getEntityInformation().getRequiredId(entity));
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteAll() {

		getTemplate().execute((GemfireCallback<Void>) region -> {

			if (isPartitioned(region) || isTransactionPresent(region)) {
				doRegionClear(region);
			}
			else {
				SpringUtils.safeDoOperation(() -> region.clear(), () -> doRegionClear(region));
			}

			return null;
		});
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteAll(@NonNull Iterable<? extends T> entities) {
		CollectionUtils.nullSafeIterable(entities).forEach(this::delete);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteAllById(@NonNull Iterable<? extends ID> ids) {

		Set<? extends ID> keys =
			StreamSupport.stream(CollectionUtils.nullSafeIterable(ids).spliterator(), false)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (!keys.isEmpty()) {
			getTemplate().removeAll(keys);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void deleteById(@NonNull ID id) {
		getTemplate().remove(id);
	}

	boolean isPartitioned(@Nullable Region<?, ?> region) {

		return region != null
			&& region.getAttributes() != null
			&& isPartitioned(region.getAttributes().getDataPolicy());
	}

	boolean isPartitioned(@Nullable DataPolicy dataPolicy) {
		return dataPolicy != null && dataPolicy.withPartitioning();
	}

	boolean isTransactionPresent(@Nullable Region<?, ?> region) {

		return region != null
			&& region.getRegionService() instanceof GemFireCache
			&& isTransactionPresent(((GemFireCache) region.getRegionService()).getCacheTransactionManager());
	}

	boolean isTransactionPresent(@Nullable CacheTransactionManager cacheTransactionManager) {
		return cacheTransactionManager != null && cacheTransactionManager.exists();
	}

	<K> void  doRegionClear(@NonNull Region<K, ?> region) {
		region.removeAll(resolveRegionKeys(region));
	}

	@NonNull <K> Set<K> resolveRegionKeys(@NonNull Region<K, ?> region) {

		return RegionUtils.isClient(region) ? region.keySetOnServer()
			: RegionUtils.isServer(region) ? region.keySet()
			: Collections.emptySet();
	}

	@NonNull List<T> toList(@Nullable Iterable<T> iterable) {

		return iterable instanceof List ? (List<T>) iterable
			: StreamSupport.stream(CollectionUtils.nullSafeIterable(iterable).spliterator(), false)
				.collect(Collectors.toList());
	}

	@NonNull List<T> toList(@Nullable SelectResults<T> selectResults) {

		return selectResults != null
			? CollectionUtils.nullSafeList(selectResults.asList())
			: Collections.emptyList();
	}

	@NonNull Page<T> toPage(@Nullable Iterable<T> iterable, @NonNull Pageable pageable) {

		PagingUtils.assertPageable(pageable);

		List<T> results = toList(iterable);

		return PagingUtils.toPage(results, pageable);
	}
}
