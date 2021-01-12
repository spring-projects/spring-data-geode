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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.geode.cache.Region;

import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.repository.query.support.OqlKeyword;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.Repository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link QueryString} is a base class used to construct and model syntactically valid Apache Geode
 * OQL query statements.
 *
 * {@link QueryString} uses {@link Pattern} based recognition and {@link Matcher matching} to parse and modify
 * the OQL query statement.
 *
 * This is an internal class used by the SDG {@link Repository} infrastructure extension.
 *
 * @author Oliver Gierke
 * @author David Turanski
 * @author John Blum
 * @see java.util.regex.Matcher
 * @see java.util.regex.Pattern
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.domain.Sort
 * @see org.springframework.data.gemfire.repository.query.support.OqlKeyword
 * @see org.springframework.data.repository.Repository
 */
public class QueryString {

	// OQL Query Patterns
	protected static final Pattern HINT_PATTERN = Pattern.compile("<HINT '\\w+'(, '\\w+')*>");
	protected static final Pattern IMPORT_PATTERN = Pattern.compile("IMPORT .+;");
	protected static final Pattern LIMIT_PATTERN = Pattern.compile("LIMIT \\d+");
	protected static final Pattern TRACE_PATTERN = Pattern.compile("<TRACE>");

	// OQL Query Templates
	protected static final String HINTS_OQL_TEMPLATE = "<HINT %1$s> %2$s";
	protected static final String IMPORT_OQL_TEMPLATE = "IMPORT %1$s; %2$s";
	protected static final String LIMIT_OQL_TEMPLATE = "%1$s LIMIT %2$d";
	protected static final String SELECT_OQL_TEMPLATE = "SELECT %1$s FROM /%2$s";
	protected static final String TRACE_OQL_TEMPLATE = "<TRACE> %1$s";

	// OQL Query Regular Expression Patterns
	protected static final String COUNT_PROJECTION = "count(*)";
	protected static final String IN_PATTERN = "(?<=IN (SET|LIST) )\\$\\d";
	protected static final String IN_PARAMETER_PATTERN = "(?<=IN (SET|LIST) \\$)\\d";
	protected static final String REGION_PATTERN = "\\/(\\/?\\w)+";
	protected static final String STAR_PROJECTION = "*";

	/**
	 * @deprecated use {@link #COUNT_PROJECTION}.
	 */
	@Deprecated
	protected static final String COUNT_QUERY = COUNT_PROJECTION;

	/**
	 * @deprecated use {@link #STAR_PROJECTION}.
	 */
	@Deprecated
	protected static final String STAR_QUERY = STAR_PROJECTION;

	/**
	 * Factory method used to construct a new instance of {@link QueryString} initialized with
	 * the given {@link String OQL query statement}.
	 *
	 * @param query {@link String} containing the OQL query.
	 * @return a new {@link QueryString} initialized with the given {@link String query}.
	 * @throws IllegalArgumentException if {@link String query} is {@literal null} or {@literal empty}.
	 * @see #QueryString(String)
	 */
	public static QueryString of(@NonNull String query) {
		return new QueryString(query);
	}

	/**
	 * Factory method used to construct a new instance of {@link QueryString} initialized with the given
	 * {@link Class application domain model type} from which the {@link String OQL query} will be created.
	 *
	 * @param domainType {@link Class application domain model type} for which the {@link String OQL query}
	 * will be created.
	 * @return a new {@link QueryString} for the given {@link Class application domain model type}.
	 * @throws IllegalArgumentException if {@link Class application domain model type} is {@literal null}.
	 * @see #QueryString(Class)
	 */
	public static QueryString from(@NonNull Class<?> domainType) {
		return new QueryString(domainType);
	}

	/**
	 * Factory method used to construct a new instance of {@link QueryString} that creates an {@link String OQL query}
	 * to count the number of objects of the specified {@link Class application domain model type}.
	 *
	 * @param domainType {@link Class application domain model type} for which the OQL query will be created.
	 * @return a new count {@link QueryString}.
	 * @throws IllegalArgumentException if {@link Class application domain model type} is {@literal null}.
	 * @see #QueryString(Class)
	 */
	public static QueryString count(Class<?> domainType) {
		return new QueryString(domainType, true);
	}

	/**
	 * Null-safe method used to extract {@literal digits} from the given {@link String} value as a whole number.
	 *
	 * @param value {@link String} to evaluate.
	 * @return the {@literal digits} extracted from the give {@link String} value as a whole number
	 * or an {@link String#isEmpty() empty String} if the given {@link String} is {@literal null}, {@literal empty}
	 * or contains no {@literal digits}.
	 * @see java.lang.String
	 */
	protected static String getDigitsOnly(@Nullable String value) {

		StringBuilder builder = new StringBuilder();

		if (StringUtils.hasText(value)) {
			for (char c : value.toCharArray()) {
				if (Character.isDigit(c)) {
					builder.append(c);
				}
			}
		}

		return builder.toString();
	}

	static String asQuery(Class<?> domainType, boolean isCountQuery) {
		return String.format(SELECT_OQL_TEMPLATE, resolveProjection(isCountQuery), resolveFrom(domainType));
	}

	static String resolveFrom(@NonNull Class<?> domainType) {

		return Optional.of(validateDomainType(domainType))
			.filter(it -> it.isAnnotationPresent(org.springframework.data.gemfire.mapping.annotation.Region.class))
			.map(it -> it.getAnnotation(org.springframework.data.gemfire.mapping.annotation.Region.class))
			.map(it -> it.value())
			.filter(StringUtils::hasText)
			.orElseGet(() -> domainType.getSimpleName());
	}

	static @NonNull String resolveProjection(boolean isCountQuery) {
		return isCountQuery ? COUNT_PROJECTION : STAR_PROJECTION;
	}

	static @NonNull <T> Class<T> validateDomainType(@NonNull Class<T> domainType) {
		Assert.notNull(domainType, "Domain type is required");
		return domainType;
	}

	static @NonNull String validateQuery(@NonNull String query) {
		Assert.hasText(query, String.format("Query [%s] is required", query));
		return query;
	}

	private final String query;

	/**
	 * Constructs a new instance of {@link QueryString} initialized with the given {@link String OQL query}.
	 *
	 * @param query {@link String} containing the OQL query.
	 * @throws IllegalArgumentException if {@link String query} is {@literal null} or {@literal empty}.
	 * @see #validateQuery(String)
	 * @see java.lang.String
	 */
	public QueryString(@NonNull String query) {
		this.query = validateQuery(query);
	}

	/**
	 * Constructs a new instance of {@link QueryString} initialized with the given
	 * {@link Class application domain model type} used to construct an OQL {@literal SELECT} query statement.
	 *
	 * @param domainType {@link Class application domain model type} to query; must not be {@literal null}.
	 * @throws IllegalArgumentException if the {@link Class application domain model type} is {@literal null}.
	 * @see #QueryString(Class, boolean)
	 */
	@SuppressWarnings("unused")
	public QueryString(@NonNull Class<?> domainType) {
		this(domainType, false);
	}

	/**
	 * Constructs a new instance of {@link QueryString} initialized with the given
	 * {@link Class application domain model type}, which is used to construct an OQL {@literal SELECT} query statement.
	 *
	 * {@code asCountQuery} is a {@link Boolean} flag indicating whether to select a count or select the contents
	 * of the objects for the given {@link Class applicatlion domain model type}.
	 *
	 * @param domainType {@link Class application domain model type} to query; must not be {@literal null}.
	 * @param asCountQuery boolean value to indicate if this is a select count query.
	 * @throws IllegalArgumentException if the {@link Class application domain model type} is {@literal null}.
	 * @see #asQuery(Class, boolean)
	 * @see #QueryString(String)
	 */
	public QueryString(@NonNull Class<?> domainType, boolean asCountQuery) {
		this(asQuery(domainType, asCountQuery));
	}

	/**
	 * Determines whether a {@literal LIMIT} is present in the OQL query.
	 *
	 * @return a boolean value determining whether a {@literal LIMIT} is present in the OQL query.
	 * @see #getLimit()
	 */
	public boolean isLimited() {
		return LIMIT_PATTERN.matcher(getQuery()).find();
	}

	/**
	 * Returns the parameter indexes used in this query.
	 *
	 * @return the parameter indexes used in this query or an empty {@link Iterable} if no parameter indexes are used.
	 * @see java.lang.Iterable
	 */
	public Iterable<Integer> getInParameterIndexes() {

		Pattern pattern = Pattern.compile(IN_PARAMETER_PATTERN);

		Matcher matcher = pattern.matcher(getQuery());

		List<Integer> indexes = new ArrayList<>();

		while (matcher.find()) {
			indexes.add(Integer.parseInt(matcher.group()));
		}

		return indexes;
	}

	/**
	 * Gets the {@literal LIMIT} number.
	 *
	 * Use {@link #isLimited()} to determine whether the {@link String OQL query statement} has a {@literal LIMIT}.
	 *
	 * @return an {@link Integer} value containing the {@literal LIMIT} number or {@link Integer#MAX_VALUE}
	 * if the {@link String OQL query statement} is not {@link #isLimited() limited}.
	 * @see #isLimited()
	 */
	public int getLimit() {

		String query = getQuery();

		Matcher matcher = LIMIT_PATTERN.matcher(query);

		if (matcher.find()) {

			int startIndex = matcher.start();
			int endIndex = matcher.end();

			String limit = query.substring(startIndex, endIndex);

			return Integer.parseInt(getDigitsOnly(limit));
		}

		return Integer.MAX_VALUE;
	}

	/**
	 * Returns the {@link String OQL query statement} from which this {@link QueryString} was constructed.
	 *
	 * @return the {@link String OQL query}; never {@literal null} or {@literal empty}.
	 */
	protected @NonNull String getQuery() {
		return this.query;
	}

	/**
	 * Null-safe method to adjust the {@literal LIMIT} of the {@link String OQL query} to use the new,
	 * given {@link Integer LIMIT}.
	 *
	 * @param limit {@link Integer} value specifying the new query {@literal LIMIT}.
	 * @return a new {@link QueryString} with the adjusted query {@literal LIMIT}.
	 * @see #withLimit(Integer)
	 */
	public QueryString adjustLimit(@Nullable Integer limit) {

		return limit != null
			? QueryString.of(LIMIT_PATTERN.matcher(getQuery()).replaceAll("").trim()).withLimit(limit)
			: this;
	}

	/**
	 * Replaces an OQL {@literal SELECT} query with an OQL {@literal SELECT DISTINCT} query if the {@link String query}
	 * is not already {@literal distinct}; i.e. does not contain the {@literal DISTINCT} OQL keyword.
	 *
	 * @return a {@literal SELECT DISTINCT} {@link QueryString query} if the {@link String query} does not contain
	 * the {@literal DISTINCT} OQL keyword.
	 * @see #asDistinct(String)
	 */
	public QueryString asDistinct() {
		return QueryString.of(asDistinct(getQuery()));
	}

	/**
	 * Replaces an OQL {@literal SELECT} query with an OQL {@literal SELECT DISTINCT} query if the {@link String query}
	 * is not already {@literal distinct}; i.e. does not contain the {@literal DISTINCT} OQL keyword.
	 *
	 * @param query {@link String} containing the query to evaluate.
	 * @return a {@literal SELECT DISTINCT} {@link String query} if the {@link String query} does not contain
	 * the {@literal DISTINCT} OQL keyword.
	 * @see java.lang.String#replaceFirst(String, String)
	 */
	String asDistinct(String query) {

		return query.contains(OqlKeyword.DISTINCT.getKeyword()) ? query
			: query.replaceFirst(OqlKeyword.SELECT.getKeyword(),
				String.format("%1$s %2$s", OqlKeyword.SELECT.getKeyword(), OqlKeyword.DISTINCT.getKeyword()));
	}

	/**
	 * Binds the given {@link Collection} of values into the {@literal IN} parameters of the OQL Query by expanding
	 * the given values into a comma-separated {@link String}.
	 *
	 * @param values the values to bind, returns the {@link QueryString} as is if {@literal null} is given.
	 * @return a Query String having "in" parameters bound with values.
	 */
	public QueryString bindIn(Collection<?> values) {

		if (!CollectionUtils.nullSafeIsEmpty(values)) {
			return QueryString.of(getQuery().replaceFirst(IN_PATTERN, String.format("(%s)",
				StringUtils.collectionToDelimitedString(values, ", ", "'", "'"))));
		}

		return this;
	}

	/**
	 * Replaces the {@link Class domain classes} referenced inside the current {@link String query}
	 * with the given {@link Region}.
	 *
	 * @param region {@link Region} to query; must not be {@literal null}.
	 * @param domainType {@link Class type} of the persistent entity to query; must not be {@literal null}.
	 * @return a new {@link QueryString} with an OQL {@literal SELECT statement} having a {@literal FROM clause}
	 * based on the selected {@link Region}.
	 * @see org.apache.geode.cache.Region
	 * @see java.lang.Class
	 */
	@SuppressWarnings("unused")
	public QueryString fromRegion(Region<?, ?> region, Class<?> domainType) {
		return QueryString.of(getQuery().replaceAll(REGION_PATTERN, region.getFullPath()));
	}

	/**
	 * @deprecated use {@link #fromRegion(Region, Class)}.
	 */
	@Deprecated
	public QueryString fromRegion(Class<?> domainType, Region<?, ?> region) {
		return fromRegion(region, domainType);
	}

	/**
	 * Appends the {@link Sort} order to this GemFire OQL Query string.
	 *
	 * @param sort {@link Sort} indicating the order of the query results.
	 * @return a new {@link QueryString} with an ORDER BY clause if {@link Sort} is not {@literal null},
	 * or this {@link QueryString} as-is if {@link Sort} is {@literal null}.
	 * @see org.springframework.data.domain.Sort
	 * @see org.springframework.data.gemfire.repository.query.QueryString
	 */
	public @NonNull QueryString orderBy(@Nullable Sort sort) {

		if (hasSort(sort)) {

			StringBuilder orderByClause = new StringBuilder("ORDER BY ");

			int count = 0;

			for (Sort.Order order : sort) {
				orderByClause.append(count++ > 0 ? ", " : "");
				orderByClause.append(String.format("%1$s %2$s", order.getProperty(), order.getDirection()));
			}

			return new QueryString(String.format("%1$s %2$s", asDistinct(getQuery()), orderByClause.toString()));
		}

		return this;
	}

	/**
	 * Null-safe method to determine whether the {@link Sort} is valid (i.e. has been specified by the caller).
	 *
	 * @param sort {@link Sort} to evaluate.
	 * @return a boolean value indicating whether the {@link Sort} is valid.
	 * @see org.springframework.data.domain.Sort
	 */
	private boolean hasSort(@Nullable Sort sort) {
		return sort != null && sort.iterator().hasNext();
	}

	/**
	 * Applies HINTS to the OQL Query.
	 *
	 * @param hints array of {@link String Strings} containing query hints.
	 * @return a new {@link QueryString} if hints are not null or empty, or return this {@link QueryString}.
	 */
	public @NonNull QueryString withHints(@NonNull String... hints) {

		if (!ObjectUtils.isEmpty(hints)) {

			StringBuilder builder = new StringBuilder();

			for (String hint : hints) {
				builder.append(builder.length() > 0 ? ", " : "");
				builder.append(String.format("'%s'", hint));
			}

			return QueryString.of(String.format(HINTS_OQL_TEMPLATE, builder.toString(), getQuery()));
		}

		return this;
	}

	/**
	 * Applies an IMPORT to the OQL Query.
	 *
	 * @param importExpression {@link String} containing the import clause.
	 * @return a new {@link QueryString} if an import was declared, or return this {@link QueryString}.
	 */
	public @NonNull QueryString withImport(@NonNull String importExpression) {

		return StringUtils.hasText(importExpression)
			? QueryString.of(String.format(IMPORT_OQL_TEMPLATE, importExpression, getQuery()))
			: this;
	}

	/**
	 * Applies a LIMIT to the OQL Query.
	 *
	 * @param limit {@link Integer} indicating the number of results to return from the query.
	 * @return a new {@link QueryString} if a limit was specified, or return this {@link QueryString}.
	 */
	public @NonNull QueryString withLimit(@NonNull Integer limit) {

		return limit != null
			? QueryString.of(String.format(LIMIT_OQL_TEMPLATE, getQuery(), limit))
			: this;
	}

	/**
	 * Applies TRACE logging to the OQL Query.
	 *
	 * @return a new {@link QueryString} with tracing enabled.
	 */
	public @NonNull QueryString withTrace() {
		return QueryString.of(String.format(TRACE_OQL_TEMPLATE, getQuery()));
	}

	/**
	 * Returns a {@link String} representation of this {@link QueryString}.
	 *
	 * Returns the complete {@link String OQL query statement}.
	 *
	 * @return a {@link String} representation of this {@link QueryString}.
	 * @see java.lang.Object#toString()
	 * @see java.lang.String
	 */
	@Override
	public String toString() {
		return getQuery();
	}
}
