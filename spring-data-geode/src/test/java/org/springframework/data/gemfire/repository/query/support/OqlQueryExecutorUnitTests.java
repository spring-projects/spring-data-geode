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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import org.junit.Test;

import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit Tests for {@link OqlQueryExecutor}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.repository.query.support.OqlQueryExecutor
 * @since 2.4.0
 */
public class OqlQueryExecutorUnitTests {

	@Test
	@SuppressWarnings("all")
	public void newUnsupportedQueryExecutionExceptionIsCorrect() {

		OqlQueryExecutor mockQueryExecutor = mock(OqlQueryExecutor.class);

		doCallRealMethod().when(mockQueryExecutor).newUnsupportedQueryExecutionException(anyString());

		UnsupportedQueryExecutionException exception =
			mockQueryExecutor.newUnsupportedQueryExecutionException("SELECT * FROM /TestRegion");

		assertThat(exception).isNotNull();
		assertThat(exception).hasMessage(OqlQueryExecutor.NON_EXECUTABLE_QUERY_MESSAGE,
			"SELECT * FROM /TestRegion", mockQueryExecutor.getClass().getName());
		assertThat(exception).hasNoCause();
	}

	@Test
	public void thenExecuteWithComposesOqlQueryExecutorsCorrectly() {

		OqlQueryExecutor one = mock(OqlQueryExecutor.class);
		OqlQueryExecutor two = mock(OqlQueryExecutor.class);

		doCallRealMethod().when(one).thenExecuteWith(any());

		assertThat(one.thenExecuteWith(null)).isSameAs(one);

		OqlQueryExecutor composed = one.thenExecuteWith(two);

		assertThat(composed).isNotNull();
		assertThat(composed).isNotSameAs(one);
		assertThat(composed).isNotSameAs(two);
	}

	@Test
	public void composedOqlQueryExecutorExecutesOne() {

		String query = "SELECT * FROM /TestRegion";

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		OqlQueryExecutor one = mock(OqlQueryExecutor.class);
		OqlQueryExecutor two = mock(OqlQueryExecutor.class);

		doCallRealMethod().when(one).thenExecuteWith(any());

		OqlQueryExecutor composed = one.thenExecuteWith(two);

		assertThat(composed).isNotNull();

		composed.execute(mockQueryMethod, query, "test");

		verify(one, times(1)).execute(eq(mockQueryMethod), eq(query), eq("test"));
		verifyNoInteractions(two);
	}

	@Test(expected = IllegalArgumentException.class)
	public void composedOqlQueryExecutorExecutesOneThenShortCircuitsWhenExceptionIsThrown() {

		String query = "SELECT * FROM /TestRegion";

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		OqlQueryExecutor one = mock(OqlQueryExecutor.class);
		OqlQueryExecutor two = mock(OqlQueryExecutor.class);

		doCallRealMethod().when(one).thenExecuteWith(any());
		doThrow(newIllegalArgumentException("test"))
			.when(one).execute(any(QueryMethod.class), anyString(), any());

		OqlQueryExecutor composed = one.thenExecuteWith(two);

		assertThat(composed).isNotNull();

		try {
			composed.execute(mockQueryMethod, query, "junk");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("test");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(one, times(1)).execute(eq(mockQueryMethod), eq(query), eq("junk"));
			verifyNoInteractions(two);
		}
	}

	@Test
	public void composedOqlQueryExecutorExecutesTwoWhenOneThrowsUnsupportedQueryExecutionException() {

		String query = "SELECT * FROM /TestRegion";

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		OqlQueryExecutor one = mock(OqlQueryExecutor.class);
		OqlQueryExecutor two = mock(OqlQueryExecutor.class);

		doCallRealMethod().when(one).thenExecuteWith(any());
		doThrow(new UnsupportedQueryExecutionException("test"))
			.when(one).execute(any(QueryMethod.class), anyString(), any());

		OqlQueryExecutor composed = one.thenExecuteWith(two);

		assertThat(composed).isNotNull();

		composed.execute(mockQueryMethod, query, "mock");

		verify(one, times(1)).execute(eq(mockQueryMethod), eq(query), eq("mock"));
		verify(two, times(1)).execute(eq(mockQueryMethod), eq(query), eq("mock"));
	}
}
