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
package org.springframework.data.gemfire.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.domain.Pageable;

/**
 * Unit Tests for {@link PagedSelectResults}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.gemfire.repository.query.PagedSelectResults
 * @since 2.4.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class PagedSelectResultsUnitTests {

	@Mock
	private Pageable mockPageable;

	@Mock
	private SelectResults<Object> mockSelectResults;

	@Test
	public void constructPagedSelectResultsIsCorrect() {

		PagedSelectResults<Object> selectResults = new PagedSelectResults<>(this.mockSelectResults, this.mockPageable);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isSameAs(this.mockSelectResults);
		assertThat(selectResults.getPageRequest()).isSameAs(this.mockPageable);

		verifyNoInteractions(this.mockPageable, mockSelectResults);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructPagedSelectResultsWithNullPageableThrowsIllegalArgumentException() {

		try {
			new PagedSelectResults<>(this.mockSelectResults, null);
		}
		catch(IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pageable must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockSelectResults);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructPagedSelectResultsWithNullSelectResultsThrowsIllegalArgumentException() {

		try {
			new PagedSelectResults<>(null, this.mockPageable);
		}
		catch(IllegalArgumentException expected) {

			assertThat(expected).hasMessage("SelectResults must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verifyNoInteractions(this.mockPageable);
		}
	}

	@Test
	public void asListIsCorrect() {

		List<String> names = Arrays.asList("Jon Doe", "Jane Doe", "Cookie Doe", "Pie Doe", "Sour Doe");

		doReturn(names).when(this.mockSelectResults).asList();
		doReturn(1).when(this.mockPageable).getPageNumber();
		doReturn(2).when(this.mockPageable).getPageSize();

		PagedSelectResults<Object> selectResults = new PagedSelectResults<>(this.mockSelectResults, this.mockPageable);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isEqualTo(this.mockSelectResults);
		assertThat(selectResults.getPageRequest()).isEqualTo(this.mockPageable);

		verifyNoInteractions(this.mockSelectResults, this.mockPageable);

		List<Object> pagedNames = selectResults.asList();

		assertThat(pagedNames).isNotNull();
		assertThat(pagedNames).hasSize(2);
		assertThat(pagedNames).containsExactly("Cookie Doe", "Pie Doe");

		verify(this.mockSelectResults, times(1)).asList();
		verify(this.mockPageable, times(2)).getPageNumber();
		verify(this.mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(this.mockSelectResults, this.mockPageable);
	}

	@Test
	public void asPageListIsCorrect() {

		List<String> names = Arrays.asList("Jon Doe", "Jane Doe", "Cookie Doe", "Pie Doe", "Sour Doe");

		doReturn(names).when(this.mockSelectResults).asList();
		doReturn(0).when(this.mockPageable).getPageNumber();
		doReturn(3).when(this.mockPageable).getPageSize();

		PagedSelectResults<Object> selectResults = new PagedSelectResults<>(this.mockSelectResults, this.mockPageable);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isEqualTo(this.mockSelectResults);
		assertThat(selectResults.getPageRequest()).isEqualTo(this.mockPageable);

		verifyNoInteractions(this.mockSelectResults, this.mockPageable);

		List<Object> pagedNames = selectResults.asList();

		assertThat(pagedNames).isNotNull();
		assertThat(pagedNames).hasSize(3);
		assertThat(pagedNames).containsExactly("Jon Doe", "Jane Doe", "Cookie Doe");

		verify(this.mockSelectResults, times(1)).asList();
		verify(this.mockPageable, times(2)).getPageNumber();
		verify(this.mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(this.mockSelectResults, this.mockPageable);
		reset(this.mockSelectResults, this.mockPageable);

		// Page 2
		doReturn(names).when(this.mockSelectResults).asList();
		doReturn(1).when(this.mockPageable).getPageNumber();
		doReturn(3).when(this.mockPageable).getPageSize();

		selectResults = selectResults.with(this.mockPageable);

		verifyNoInteractions(this.mockSelectResults, this.mockPageable);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isEqualTo(this.mockSelectResults);
		assertThat(selectResults.getPageRequest()).isEqualTo(this.mockPageable);

		pagedNames = selectResults.asList();

		assertThat(pagedNames).isNotNull();
		assertThat(pagedNames).hasSize(2);
		assertThat(pagedNames).containsExactly("Pie Doe", "Sour Doe");

		verify(this.mockSelectResults, times(1)).asList();
		verify(this.mockPageable, times(2)).getPageNumber();
		verify(this.mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(this.mockSelectResults, this.mockPageable);
		reset(this.mockSelectResults, this.mockPageable);

		// Page 3
		doReturn(names).when(this.mockSelectResults).asList();
		doReturn(2).when(this.mockPageable).getPageNumber();
		doReturn(3).when(this.mockPageable).getPageSize();

		selectResults = selectResults.with(this.mockPageable);

		verifyNoInteractions(this.mockSelectResults, this.mockPageable);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isEqualTo(this.mockSelectResults);
		assertThat(selectResults.getPageRequest()).isEqualTo(this.mockPageable);

		pagedNames = selectResults.asList();

		assertThat(pagedNames).isNotNull();
		assertThat(pagedNames).isEmpty();

		verify(this.mockSelectResults, times(1)).asList();
		verify(this.mockPageable, times(2)).getPageNumber();
		verify(this.mockPageable, times(3)).getPageSize();
		verifyNoMoreInteractions(this.mockSelectResults, this.mockPageable);
		reset(this.mockSelectResults, this.mockPageable);
	}

	@Test
	public void asSetCallsAsList() {

		List<Object> list = Collections.singletonList("mock");

		PagedSelectResults<Object> selectResults =
			spy(new PagedSelectResults<>(this.mockSelectResults, this.mockPageable));

		doReturn(list).when(selectResults).asList();

		assertThat(selectResults.asSet()).containsExactly("mock");

		verify(selectResults, times(1)).asList();
		verifyNoInteractions(this.mockSelectResults, this.mockPageable);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void iteratorCallsAsList() {

		List<Object> list = Collections.singletonList("test");

		PagedSelectResults<Object> selectResults =
			spy(new PagedSelectResults<>(this.mockSelectResults, this.mockPageable));

		doReturn(list).when(selectResults).asList();

		Iterator<Object> iterator = selectResults.iterator();

		assertThat(iterator).isNotNull();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("test");
		assertThat(iterator.hasNext()).isFalse();

		try {
			iterator.remove();
		}
		finally {
			verify(selectResults, times(1)).asList();
			verifyNoInteractions(this.mockSelectResults, this.mockPageable);
		}
	}

	@Test
	public void sizeCallsAsList() {

		List<Object> mockList = mock(List.class);

		doReturn(50).when(mockList).size();

		PagedSelectResults<Object> selectResults =
			spy(new PagedSelectResults<>(this.mockSelectResults, this.mockPageable));

		doReturn(mockList).when(selectResults).asList();

		assertThat(selectResults.size()).isEqualTo(50);

		verify(selectResults, times(1)).asList();
		verify(mockList, times(1)).size();
		verifyNoMoreInteractions(mockList);
		verifyNoInteractions(this.mockSelectResults, this.mockPageable);
	}
}
