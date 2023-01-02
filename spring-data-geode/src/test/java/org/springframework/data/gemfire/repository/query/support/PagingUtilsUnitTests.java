/*
 * Copyright 2020-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.repository.query.GemfireQueryMethod;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.test.context.event.annotation.AfterTestClass;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Unit Tests for {@link PagingUtils}.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see java.util.List
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.domain.Page
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.gemfire.repository.query.support.PagingUtils
 * @since 2.4.0
 */
public class PagingUtilsUnitTests {

	@AfterTestClass
	public static void tearDown() {
		PagingUtils.isPageQueryFunction = PagingUtils.DEFAULT_IS_PAGE_QUERY_FUNCTION;
	}

	@Test
	public void assertPageableIsCorrect() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(10).when(mockPageable).getPageSize();

		PagingUtils.assertPageable(mockPageable);

		verify(mockPageable, times(1)).getPageNumber();
		verify(mockPageable, times(1)).getPageSize();
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertPageableWithInvalidPageNumberThrowsIllegalArgumentException() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(-1).when(mockPageable).getPageNumber();

		try {
			PagingUtils.assertPageable(mockPageable);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage(PagingUtils.INVALID_PAGE_NUMBER_MESSAGE, -1);
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockPageable, times(1)).getPageNumber();
			verify(mockPageable, never()).getPageSize();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertPageableWithInvalidPageSizeThrowsIllegalArgumentException() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(1).when(mockPageable).getPageNumber();
		doReturn(0).when(mockPageable).getPageSize();

		try {
			PagingUtils.assertPageable(mockPageable);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage(PagingUtils.INVALID_PAGE_SIZE_MESSAGE, 0);
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockPageable, times(1)).getPageNumber();
			verify(mockPageable, times(1)).getPageSize();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertPageableWithNull() {

		try {
			PagingUtils.assertPageable(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage(PagingUtils.NON_NULL_PAGEABLE_MESSAGE);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void isPageOneForPageOneReturnsTrue() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).when(mockPageable).getPageNumber();

		assertThat(PagingUtils.isPageOne(mockPageable)).isTrue();

		verify(mockPageable, times(1)).getPageNumber();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void isPageOneForPageTwoReturnsFalse() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(1).when(mockPageable).getPageNumber();

		assertThat(PagingUtils.isPageOne(mockPageable)).isFalse();

		verify(mockPageable, times(1)).getPageNumber();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void isPageOneWithNullReturnsFalse() {
		assertThat(PagingUtils.isPageOne(null)).isFalse();
	}

	@Test
	public void isPagingPresentForQueryMethodReturningPageIsTrue() {

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		PagingUtils.isPageQueryFunction = queryMethod -> true;

		assertThat(PagingUtils.isPagingPresent(mockQueryMethod)).isTrue();
	}

	@Test
	public void isPagingPresentForQueryMethodWithPageableParameterIsTrue() {

		Parameters<?, ?> mockParameters = mock(Parameters.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		doReturn(mockParameters).when(mockQueryMethod).getParameters();
		doReturn(true).when(mockParameters).hasPageableParameter();

		PagingUtils.isPageQueryFunction = queryMethod -> false;

		assertThat(PagingUtils.isPagingPresent(mockQueryMethod)).isTrue();

		verify(mockQueryMethod, times(1)).getParameters();
		verify(mockParameters, times(1)).hasPageableParameter();
		verifyNoMoreInteractions(mockQueryMethod, mockParameters);
	}

	@Test
	public void isPagingPresentForNonQueryMethodIsFalse() {

		GemfireQueryMethod mockQueryMethod = mock(GemfireQueryMethod.class);

		PagingUtils.isPageQueryFunction = queryMethod -> false;

		doReturn(null).when(mockQueryMethod).getParameters();

		assertThat(PagingUtils.isPagingPresent(mockQueryMethod)).isFalse();

		verify(mockQueryMethod, times(1)).getParameters();
		verifyNoMoreInteractions(mockQueryMethod);
	}

	@Test
	public void isPagingPresentForNullQueryMethodIsFalse() {
		assertThat(PagingUtils.isPagingPresent(null)).isFalse();
	}

	@Test
	public void toPageIsCorrect() {

		List<User> users = Arrays.asList(
			User.newUser("Jon Doe"),
			User.newUser("Jane Doe"),
			User.newUser("Cookie Doe"),
			User.newUser("Fro Doe"),
			User.newUser("Joe Doe"),
			User.newUser("Lan Doe"),
			User.newUser("Pie Doe"),
			User.newUser("Play Doe"),
			User.newUser("Sour Doe")
		);

		Pageable mockPageable = mock(Pageable.class);

		Sort orderBy = Sort.by("name").ascending();

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();
		doReturn(orderBy).when(mockPageable).getSort();

		Page<User> pageOne = PagingUtils.toPage(users, mockPageable);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isNotEmpty();
		assertThat(pageOne.getNumber()).isEqualTo(0);
		assertThat(pageOne.getNumberOfElements()).isEqualTo(5);
		assertThat(pageOne.getSize()).isEqualTo(5);
		assertThat(pageOne.getSort()).isEqualTo(orderBy);
		assertThat(pageOne.getTotalElements()).isEqualTo(users.size());
		assertThat(pageOne.getTotalPages()).isEqualTo(2);
		assertThat(pageOne.getContent()).containsExactly(users.subList(0, 5).toArray(new User[0]));

		doReturn(1).when(mockPageable).getPageNumber();

		Page<User> pageTwo = PagingUtils.toPage(users, mockPageable);

		assertThat(pageTwo).isNotNull();
		assertThat(pageTwo).isNotEmpty();
		assertThat(pageTwo.getNumber()).isEqualTo(1);
		assertThat(pageTwo.getNumberOfElements()).isEqualTo(4);
		assertThat(pageTwo.getSize()).isEqualTo(5);
		assertThat(pageTwo.getSort()).isEqualTo(orderBy);
		assertThat(pageTwo.getTotalElements()).isEqualTo(users.size());
		assertThat(pageTwo.getTotalPages()).isEqualTo(2);
		assertThat(pageTwo.getContent()).containsExactly(users.subList(5, users.size()).toArray(new User[0]));

		doReturn(2).when(mockPageable).getPageNumber();

		Page<User> pageThree = PagingUtils.toPage(users, mockPageable);

		assertThat(pageThree).isNotNull();
		assertThat(pageThree).isEmpty();
	}

	@Test
	public void getPagedListFromListWithSizeLessThanPageSize() {

		List<User> users = Arrays.asList(
			User.newUser("Jon Doe"),
			User.newUser("Jane Doe"),
			User.newUser("Pie Doe")
		);

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		List<User> pageOne = PagingUtils.getPagedList(users, mockPageable);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isNotEmpty();
		assertThat(pageOne).containsExactly(users.toArray(new User[0]));
	}

	@Test
	public void getPagedListFromListResultingInEmptyPage() {

		List<User> users = Arrays.asList(
			User.newUser("Jon Doe"),
			User.newUser("Jane Doe"),
			User.newUser("Pie Doe")
		);

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(1).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		List<User> pageOne = PagingUtils.getPagedList(users, mockPageable);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isEmpty();
	}

	@Test
	public void getPagedListFromEmptyList() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		List<User> pageOne = PagingUtils.getPagedList(Collections.emptyList(), mockPageable);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isEmpty();
	}

	@Test
	public void getPagedListFromNullList() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		List<User> pageOne = PagingUtils.getPagedList(null, mockPageable);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isEmpty();
	}

	@Test
	public void getPagedListFromNullPageable() {

		List<User> pageOne = PagingUtils.getPagedList(Collections.singletonList(User.newUser("Jon Doe")), null);

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isEmpty();
	}

	@Test
	public void getPageRequestIsCorrect() {

		Pageable mockPageable = mock(Pageable.class);

		Object[] arguments = { "test", mockPageable, "mock" };

		Parameters<?, ?> mockParameters = mock(Parameters.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		Stream<?> mockStream = mock(Stream.class);

		doReturn(mockParameters).when(mockQueryMethod).getParameters();
		doReturn(1).when(mockParameters).getPageableIndex();
		doReturn(true).when(mockParameters).hasPageableParameter();
		doReturn(mockStream).when(mockParameters).stream();
		doReturn(3L).when(mockStream).count();

		Pageable pageable = PagingUtils.getPageRequest(mockQueryMethod, arguments);

		assertThat(pageable).isSameAs(mockPageable);

		InOrder order = inOrder(mockQueryMethod, mockParameters, mockStream);

		order.verify(mockQueryMethod, times(1)).getParameters();
		order.verify(mockParameters, times(1)).hasPageableParameter();
		order.verify(mockParameters, times(1)).stream();
		order.verify(mockStream, times(1)).count();
		order.verify(mockParameters, times(1)).getPageableIndex();
		verifyNoMoreInteractions(mockQueryMethod, mockParameters, mockStream);
		verifyNoInteractions(mockPageable);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPageRequestWithNullQueryMethodThrowsIllegalArgumentException() {

		try {
			PagingUtils.getPageRequest(null, "test", "mock");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("QueryMethod must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void getPageRequestFromQueryMethodHavingNoPageableParameter() {

		Parameters<?, ?> mockParameters = mock(Parameters.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		doReturn(mockParameters).when(mockQueryMethod).getParameters();
		doReturn(false).when(mockParameters).hasPageableParameter();

		try {
			PagingUtils.getPageRequest(mockQueryMethod, "test", "mock");
		}
		catch (IllegalStateException expected) {

			assertThat(expected)
				.hasMessage("QueryMethod [%s] does not have a Pageable parameter", mockQueryMethod);

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockQueryMethod, times(1)).getParameters();
			verify(mockParameters, times(1)).hasPageableParameter();
			verifyNoMoreInteractions(mockQueryMethod, mockParameters);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPageRequestFromQueryMethodHavingLessParametersThanArgumentsThrowsIllegalArgumentException() {

		Object[] arguments = { 1, 2, 3 };

		Parameters<?, ?> mockParameters = mock(Parameters.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		Stream<?> mockStream = mock(Stream.class);

		doReturn(mockParameters).when(mockQueryMethod).getParameters();
		doReturn(true).when(mockParameters).hasPageableParameter();
		doReturn(mockStream).when(mockParameters).stream();
		doReturn(2L).when(mockStream).count();

		try {
			PagingUtils.getPageRequest(mockQueryMethod, arguments);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected)
				.hasMessage("The number of arguments [%d] must match the number of QueryMethod [%s] parameters [2]",
					arguments.length, mockQueryMethod);

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockQueryMethod, times(1)).getParameters();
			verify(mockParameters, times(1)).hasPageableParameter();
			verify(mockParameters, times(1)).stream();
			verify(mockStream, times(1)).count();
			verifyNoMoreInteractions(mockQueryMethod, mockParameters, mockStream);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPageRequestFromQueryMethodWherePageableParameterIndexDoesMatchPageableArgument() {

		Pageable mockPageable = mock(Pageable.class);

		Object[] arguments = { mockPageable, "test", -1 };

		Parameters<?, ?> mockParameters = mock(Parameters.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		Stream<?> mockStream = mock(Stream.class);

		doReturn(mockParameters).when(mockQueryMethod).getParameters();
		doReturn(1).when(mockParameters).getPageableIndex();
		doReturn(true).when(mockParameters).hasPageableParameter();
		doReturn(mockStream).when(mockParameters).stream();
		doReturn(3L).when(mockStream).count();

		try {
			PagingUtils.getPageRequest(mockQueryMethod, arguments);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Argument [test] must be of type [%2$s]", Pageable.class.getName());
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockQueryMethod, times(1)).getParameters();
			verify(mockParameters, times(1)).hasPageableParameter();
			verify(mockParameters, times(1)).stream();
			verify(mockStream, times(1)).count();
			verify(mockParameters, times(1)).getPageableIndex();
			verifyNoMoreInteractions(mockQueryMethod, mockParameters, mockStream);
		}
	}

	@Test
	public void getQueryResultSetStartIndexForPageIsCorrect() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).doReturn(1).doReturn(2).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		assertThat(PagingUtils.getQueryResultSetStartIndexForPage(mockPageable)).isZero();
		assertThat(PagingUtils.getQueryResultSetStartIndexForPage(mockPageable)).isEqualTo(5);
		assertThat(PagingUtils.getQueryResultSetStartIndexForPage(mockPageable)).isEqualTo(10);

		verify(mockPageable, times(3)).getPageNumber();
		verify(mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void getQueryResultSetStartIndexForPageWithNullPageableIsCorrect() {
		assertThat(PagingUtils.getQueryResultSetStartIndexForPage(null)).isZero();
	}

	@Test
	public void getQueryResultSetEndIndexForPageIsCorrect() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).doReturn(1).doReturn(2).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		assertThat(PagingUtils.getQueryResultSetEndIndexForPage(mockPageable)).isEqualTo(5);
		assertThat(PagingUtils.getQueryResultSetEndIndexForPage(mockPageable)).isEqualTo(10);
		assertThat(PagingUtils.getQueryResultSetEndIndexForPage(mockPageable)).isEqualTo(15);

		verify(mockPageable, times(3)).getPageNumber();
		verify(mockPageable, times(6)).getPageSize();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void getQueryResultSetEndIndexForPageWithNullPageableIsCorrect() {
		assertThat(PagingUtils.getQueryResultSetEndIndexForPage(null)).isZero();
	}

	@Test
	public void getQueryResultSetLimitForPageIsCorrect() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).doReturn(1).doReturn(2).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		assertThat(PagingUtils.getQueryResultSetLimitForPage(mockPageable)).isEqualTo(5);
		assertThat(PagingUtils.getQueryResultSetLimitForPage(mockPageable)).isEqualTo(10);
		assertThat(PagingUtils.getQueryResultSetLimitForPage(mockPageable)).isEqualTo(15);

		verify(mockPageable, times(3)).getPageNumber();
		verify(mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void getQueryResultSetLimitForPageWithNullPageableIsCorrect() {
		assertThat(PagingUtils.getQueryResultSetLimitForPage(null)).isZero();
	}

	@Test
	public void normalizePageNumberFromPage() {

		Page<?> mockPage = mock(Page.class);

		doReturn(0).when(mockPage).getNumber();

		assertThat(PagingUtils.normalizePageNumber(mockPage)).isOne();

		verify(mockPage, times(1)).getNumber();
		verifyNoMoreInteractions(mockPage);
	}

	@Test
	public void normalizePageNumberFromNullPage() {
		assertThat(PagingUtils.normalizePageNumber((Page<?>) null)).isZero();
	}

	@Test
	public void normalizePageNumberFromPageable() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).when(mockPageable).getPageNumber();

		assertThat(PagingUtils.normalizePageNumber(mockPageable)).isOne();

		verify(mockPageable, times(1)).getPageNumber();
		verifyNoMoreInteractions(mockPageable);
	}

	@Test
	public void normalizePageNumberFromNullPageable() {
		assertThat(PagingUtils.normalizePageNumber((Pageable) null)).isZero();
	}

	@Test
	public void normalizesZeroIndexBasedPageNumbers() {

		assertThat(PagingUtils.normalize(-2)).isEqualTo(0);
		assertThat(PagingUtils.normalize(-1)).isEqualTo(0);
		assertThat(PagingUtils.normalize(0)).isEqualTo(1);
		assertThat(PagingUtils.normalize(1)).isEqualTo(2);
		assertThat(PagingUtils.normalize(2)).isEqualTo(3);
	}

	@Test
	public void nullSafeSizeFromCollection() {

		assertThat(PagingUtils.nullSafeSize(Collections.singleton(User.newUser("Jon Doe")))).isOne();
		assertThat(PagingUtils.nullSafeSize(Arrays.asList(User.newUser("Jon Doe"), User.newUser("Jane Doe"), User.newUser("Pie Doe")))).isEqualTo(3);
	}

	@Test
	public void nullSafeSizeFromEmptyCollection() {
		assertThat(PagingUtils.nullSafeSize(Collections.emptyList())).isZero();
	}

	@Test
	public void nullSafeSizeFromIterable() {
		assertThat(PagingUtils.nullSafeSize(ArrayUtils.toIterable(User.newUser("Jon Doe"), User.newUser("Jane Doe")))).isEqualTo(2);
	}

	@Test
	public void nullSafeSizeFromEmptyIterable() {
		assertThat(PagingUtils.nullSafeSize(CollectionUtils.emptyIterable())).isZero();
	}

	@Test
	public void nullSafeSizeFromNull() {
		assertThat(PagingUtils.nullSafeSize(null)).isZero();
	}

	@Getter
	@Region("Users")
	@ToString(of = "name")
	@EqualsAndHashCode(of = "name")
	@RequiredArgsConstructor(staticName = "newUser")
	static class User {

		@Id
		Long id;

		@NonNull
		final String name;
	}
}
