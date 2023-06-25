/*
 * Copyright 2017-2023 the original author or authors.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit Tests for {@link QueryPostProcessor}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.repository.query.QueryPostProcessor
 * @since 1.0.0
 */
public class QueryPostProcessorUnitTests {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void processAfterReturnsCompositeQueryPostProcessorAndPostProcessesInOrder() {

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		String query = "SELECT * FROM /Test";

		QueryPostProcessor<Repository, String> mockQueryPostProcessorOne = mock(QueryPostProcessor.class);
		QueryPostProcessor<Repository, String> mockQueryPostProcessorTwo = mock(QueryPostProcessor.class);

		doCallRealMethod().when(mockQueryPostProcessorOne).processAfter(any());
		doReturn(query).when(mockQueryPostProcessorOne).postProcess(any(QueryMethod.class), anyString(), anyString());
		doReturn(query).when(mockQueryPostProcessorTwo).postProcess(any(QueryMethod.class), anyString(), anyString());

		QueryPostProcessor<?, String> composite = mockQueryPostProcessorOne.processAfter(mockQueryPostProcessorTwo);

		assertThat(composite).isNotNull();
		assertThat(composite).isNotSameAs(mockQueryPostProcessorOne);
		assertThat(composite).isNotSameAs(mockQueryPostProcessorTwo);
		assertThat(composite.postProcess(mockQueryMethod, query, "arg")).isEqualTo(query);

		InOrder inOrder = inOrder(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.processAfter(eq(mockQueryPostProcessorTwo));

		inOrder.verify(mockQueryPostProcessorTwo, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), eq("arg"));

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), eq("arg"));

		verifyNoMoreInteractions(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);
	}

	@Test
	public void processAfterReturnsThis() {

		QueryPostProcessor<?, ?> mockQueryPostProcessor = mock(QueryPostProcessor.class);

		doCallRealMethod().when(mockQueryPostProcessor).processAfter(any());

		assertThat(mockQueryPostProcessor.processAfter(null)).isSameAs(mockQueryPostProcessor);

		verify(mockQueryPostProcessor, times(1)).processAfter(isNull());
		verifyNoMoreInteractions(mockQueryPostProcessor);
	}
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void processBeforeReturnsCompositeQueryPostProcessorAndPostProcessesInOrder() {

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		String query = "SELECT * FROM /Test";

		QueryPostProcessor<Repository, String> mockQueryPostProcessorOne = mock(QueryPostProcessor.class, "ONE");
		QueryPostProcessor<Repository, String> mockQueryPostProcessorTwo = mock(QueryPostProcessor.class, "TWO");

		doCallRealMethod().when(mockQueryPostProcessorOne).processBefore(any());
		doReturn(query).when(mockQueryPostProcessorOne).postProcess(any(QueryMethod.class), anyString(), anyString());
		doReturn(query).when(mockQueryPostProcessorTwo).postProcess(any(QueryMethod.class), anyString(), anyString());

		QueryPostProcessor<?, String> composite = mockQueryPostProcessorOne.processBefore(mockQueryPostProcessorTwo);

		assertThat(composite).isNotNull();
		assertThat(composite).isNotSameAs(mockQueryPostProcessorOne);
		assertThat(composite).isNotSameAs(mockQueryPostProcessorTwo);
		assertThat(composite.postProcess(mockQueryMethod, query, "arg")).isEqualTo(query);

		InOrder inOrder = inOrder(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.processBefore(eq(mockQueryPostProcessorTwo));

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), eq("arg"));

		inOrder.verify(mockQueryPostProcessorTwo, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), eq("arg"));

		verifyNoMoreInteractions(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);
	}

	@Test
	public void processBeforeReturnsThis() {

		QueryPostProcessor<?, ?> mockQueryPostProcessor = mock(QueryPostProcessor.class);

		doCallRealMethod().when(mockQueryPostProcessor).processBefore(any());

		assertThat(mockQueryPostProcessor.processBefore(null)).isSameAs(mockQueryPostProcessor);

		verify(mockQueryPostProcessor, times(1)).processBefore(isNull());
		verifyNoMoreInteractions(mockQueryPostProcessor);
	}
}
