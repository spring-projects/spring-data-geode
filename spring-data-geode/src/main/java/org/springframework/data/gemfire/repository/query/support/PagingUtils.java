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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class used to work with {@link Collection}, {@link Page} and {@link Pageable} objects.
 *
 * @author John Blum
 * @see java.util.Collection
 * @see org.springframework.data.domain.Page
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.repository.query.QueryMethod
 * @since 2.4.0
 */
public abstract class PagingUtils {

	public static final String INVALID_PAGE_NUMBER_MESSAGE = "Page Number [%d] must be greater than equal to 0";
	public static final String INVALID_PAGE_SIZE_MESSAGE = "Page Size [%d] must be greater than equal to 1";
	public static final String NON_NULL_PAGEABLE_MESSAGE = "Pageable must not be null";

	protected static final Function<QueryMethod, Boolean> DEFAULT_IS_PAGE_QUERY_FUNCTION = QueryMethod::isPageQuery;

	static Function<QueryMethod, Boolean> isPageQueryFunction = DEFAULT_IS_PAGE_QUERY_FUNCTION;

	static Function<QueryMethod, Boolean> hasPageableParameterFunction = queryMethod ->
		Optional.ofNullable(queryMethod)
			.map(QueryMethod::getParameters)
			.filter(Parameters::hasPageableParameter)
			.isPresent();

	/**
	 * Asserts that the {@link Pageable} object is valid.
	 *
	 * @param pageable {@link Pageable} object to evaluate.
	 * @throws IllegalArgumentException if {@link Pageable} is {@literal null} or page number is less than {@literal 0}
	 *  or the page size is less than {@literal 1}.
	 * @see org.springframework.data.domain.Pageable
	 */
	public static void assertPageable(@NonNull Pageable pageable) {

		Assert.notNull(pageable, NON_NULL_PAGEABLE_MESSAGE);

		int pageNumber = pageable.getPageNumber();

		Assert.isTrue(pageNumber >= 0, () -> String.format(INVALID_PAGE_NUMBER_MESSAGE, pageNumber));

		int pageSize = pageable.getPageSize();

		Assert.isTrue(pageSize > 0, () -> String.format(INVALID_PAGE_SIZE_MESSAGE, pageSize));
	}

	/**
	 * Null-safe method to determine whether the given {@link Pageable page request} is for page one.
	 *
	 * @param pageable {@link Pageable page request} to evaluate.
	 * @return a boolean value indicating whether the given {@link Pageable page request} is for page one.
	 * @see org.springframework.data.domain.Pageable
	 */
	public static boolean isPageOne(@NonNull Pageable pageable) {
		return pageable != null && pageable.getPageNumber() == 0;
	}

	/**
	 * Null-safe method used to determine whether the given {@link QueryMethod} represents (models) a paged query.
	 *
	 * @param queryMethod {@link QueryMethod} to evaluate for paging.
	 * @return a boolean value indicating whether the given {@link QueryMethod} represents (models) a paged query.
	 * @see org.springframework.data.repository.query.QueryMethod
	 */
	public static boolean isPagingPresent(@Nullable QueryMethod queryMethod) {

		return queryMethod != null
			&& (isPageQueryFunction.apply(queryMethod)
			|| hasPageableParameterFunction.apply(queryMethod));
	}

	/**
	 * Gets a {@literal page} from the given {@link List}.
	 *
	 * This method is {@literal null-safe}, and guards against a {@literal null} {@link List} and {@link Pageable}.
	 *
	 * @param <T> {@link Class type} of the {@link List} elements; must not be {@literal null}.
	 * @param list {@link List} from which to extract a page of elements; must not be {@literal null}.
	 * @param pageable {@link Pageable} object encapsulating the details for the page requested.
	 * @return a {@link List sub-List} containing the contents for the requested page.
	 * @see #getQueryResultSetStartIndexForPage(Pageable)
	 * @see #getQueryResultSetEndIndexForPage(Pageable)
	 * @see org.springframework.data.domain.Pageable
	 * @see java.util.List
	 */
	public static @NonNull <T> List<T> 	getPagedList(@NonNull List<T> list, @NonNull Pageable pageable) {

		list = CollectionUtils.nullSafeList(list);

		int total = list.size();
		int startIndex = getQueryResultSetStartIndexForPage(pageable);
		int endIndex = getQueryResultSetEndIndexForPage(pageable);

		return list.isEmpty() || total <= startIndex
			? Collections.emptyList()
			: list.subList(startIndex, Math.min(total, endIndex));
	}

	/**
	 * Finds the {@link Pageable page request} argument from an array of arguments passed to
	 * the given {@link QueryMethod}.
	 *
	 * @param queryMethod invoked {@link QueryMethod}; must not be {@literal null}.
	 * @param arguments array of {@link Object arguments} passed to the {@link QueryMethod};
	 * must not be {@literal null}.
	 * @return the {@link Pageable page request} argument in the array of {@link Object arguments}
	 * passed to the {@link QueryMethod}.
	 * @throws IllegalArgumentException if {@link QueryMethod} is {@literal null}, or the {@link QueryMethod} parameter
	 * count is not equal to the argument count, or the indexed {@link QueryMethod} argument is not an instance of
	 * {@link Pageable}.
	 * @throws IllegalStateException if the {@link QueryMethod} does not have a {@link Pageable} parameter.
	 * @see org.springframework.data.repository.query.QueryMethod
	 * @see org.springframework.data.domain.Pageable
	 */
	public static @NonNull Pageable getPageRequest(@NonNull QueryMethod queryMethod, @NonNull Object... arguments) {

		Assert.notNull(queryMethod, "QueryMethod must not be null");

		Parameters<?, ?> queryMethodParameters = queryMethod.getParameters();

		Assert.state(queryMethodParameters.hasPageableParameter(),
			() -> String.format("QueryMethod [%s] does not have a Pageable parameter", queryMethod));

		arguments = ArrayUtils.nullSafeArray(arguments, Object.class);

		long queryMethodArgumentCount = arguments.length;
		long queryMethodParameterCount = queryMethodParameters.stream().count();

		Assert.isTrue(queryMethodArgumentCount == queryMethodParameterCount,
			() -> String.format("The number of arguments [%d] must match the number of QueryMethod [%s] parameters [%d]",
				queryMethodArgumentCount, queryMethod, queryMethodParameterCount));

		int pageableIndex = queryMethodParameters.getPageableIndex();

		Object pageableArgument = arguments[pageableIndex];

		Assert.isInstanceOf(Pageable.class, pageableArgument,
			() -> String.format("Argument [%1$s] must be of type [%2$s]", pageableArgument, Pageable.class.getName()));

		return (Pageable) pageableArgument;
	}

	/**
	 * Null-safe method used to determine the starting index in the query result set for populating the content
	 * of the {@link Page}.
	 *
	 * @param pageable {@link Pageable} object encapsulating the details of the requested {@link Page}.
	 * @return the start index in the query result set to populate the content of the {@link Page}.
	 * @see org.springframework.data.domain.Pageable
	 * @see #getQueryResultSetEndIndexForPage(Pageable)
	 */
	public static int getQueryResultSetStartIndexForPage(@Nullable Pageable pageable) {
		return pageable != null ? pageable.getPageNumber() * pageable.getPageSize() : 0;
	}

	/**
	 * Null-safe method used to determine the end index in the query result set for populating the content
	 * of the {@link Page}.
	 *
	 * @param pageable {@link Pageable} object encapsulating the details of the requested {@link Page}.
	 * @return the end index in the query result set to populate the content of the {@link Page}.
	 * @see org.springframework.data.domain.Pageable
	 * @see #getQueryResultSetStartIndexForPage(Pageable)
	 */
	public static int getQueryResultSetEndIndexForPage(@Nullable Pageable pageable) {
		return pageable != null ? getQueryResultSetStartIndexForPage(pageable) + pageable.getPageSize() : 0;
	}

	/**
	 * Null-safe method used to determine the maximum results that would be returned by a query
	 * given the {@link Pageable} object specifying the requested {@link Page}.
	 *
	 * @param pageable {@link Pageable} object encapsulating the details of the requested {@link Page}.
	 * @return the maximum results that would be returned by a query  given the {@link Pageable} object
	 * specifying the requested {@link Page}.
	 * @see org.springframework.data.domain.Pageable
	 * @see #normalizePageNumber(Pageable)
	 */
	public static int getQueryResultSetLimitForPage(@Nullable Pageable pageable) {
		return pageable != null ? normalizePageNumber(pageable) * pageable.getPageSize() : 0;
	}

	/**
	 * Null-safe method used to normalize 0 index based page numbers (i.e. 0, 1, 2, ...) to natural page numbers
	 * (i.e. 1, 2, 3, ...) using the given {@link Page}.
	 *
	 * @param page {@link Page} used to determine the page number to normalize.
	 * @return the normalized page number from the 0 index based page number.
	 * @see org.springframework.data.domain.Page
	 * @see #normalize(int)
	 */
	public static int normalizePageNumber(@Nullable Page<?> page) {
		return page != null ? normalize(page.getNumber()) : 0;
	}

	/**
	 * Null-safe method used to normalize 0 index based page numbers (i.e. 0, 1, 2, ...) to natural page numbers
	 * (i.e. 1, 2, 3, ...) using the given {@link Pageable}.
	 *
	 * @param pageable {@link Pageable} used to determine the page number to normalize.
	 * @return the normalized page number from the 0 index based page number.
	 * @see org.springframework.data.domain.Pageable
	 * @see #normalize(int)
	 */
	public static int normalizePageNumber(@Nullable Pageable pageable) {
		return pageable != null ? normalize(pageable.getPageNumber()) : 0;
	}

	/**
	 * Normalizes 0 index based page numbers (i.e. 0, 1, 2, ...) to natural page numbers (i.e. 1, 2, 3, ...).
	 *
	 * @param pageNumber The {@link Integer#TYPE page number} to normalize.
	 * @return the normalized page number from the 0 index based page number.
	 */
	protected static int normalize(int pageNumber) {
		return Math.max(pageNumber, -1) + 1;
	}

	/**
	 * Null-safe method to determine the size (number of elements) of the {@link Iterable}.
	 *
	 * The {@link Iterable} object may be an array, a {@link Collection} or simply a stream backing,
	 * pure {@link Iterable} object.
	 *
	 * @param iterable {@link Iterable} object to evaluate.
	 * @return the size (number of elements) contained by the {@link Iterable} object.  If the {@link Iterable} object
	 * is {@literal null}, then this method will return {@literal 0}.
	 * @see java.lang.Iterable
	 */
	protected static long nullSafeSize(@Nullable Iterable<?> iterable) {

		return iterable == null ? 0
			: iterable instanceof Collection ? ((Collection<?>) iterable).size()
			: StreamSupport.stream(iterable.spliterator(), false).count();
	}

	/**
	 * Gets a {@link Page} view from the given {@link List} based on the {@link Pageable} object (page request).
	 *
	 * @param <T> {@link Class type} of the {@link List} elements.
	 * @param list {@link List} of content from which to extract a {@link Page}; must not be {@literal null}.
	 * @param pageable {@link Pageable} object encapsulating the details of the {@link Page} requested;
	 * must not be {@literal null}.
	 * @return a {@literal non-null} {@link Page} view from the given {@link List} based on the {@link Pageable} object
	 * (page request).
	 * @see org.springframework.data.domain.Pageable
	 * @see org.springframework.data.domain.Page
	 * @see #getPagedList(List, Pageable)
	 * @see java.util.List
	 */
	public static @NonNull <T> Page<T> toPage(@NonNull List<T> list, @NonNull Pageable pageable) {

		List<T> pagedList = getPagedList(list, pageable);

		return pagedList.isEmpty()
			? Page.empty()
			: new PageImpl<>(pagedList, pageable, nullSafeSize(list));
	}
}
