/*
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.function.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.ResultCollector;

/**
 * The AbstractFunctionExecutionTest class is a test suite of test cases testing the contract and functionality
 * of the AbstractFunctionExecution class.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionExecution
 * @see org.apache.geode.cache.execute.Execution
 * @since 1.7.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class AbstractFunctionExecutionTest {

	@Mock
	private Execution mockExecution;

	@Test
	@SuppressWarnings("unchecked")
	public void executeWithResults() throws Exception {

		Object[] args = { "one", "two", "three" };

		List<Object> results = Arrays.asList(args);

		Function mockFunction = mock(Function.class, "MockFunction");

		ResultCollector mockResultCollector = mock(ResultCollector.class, "MockResultCollector");

		when(mockExecution.setArguments(eq(args))).thenReturn(mockExecution);
		when(mockExecution.execute(eq(mockFunction))).thenReturn(mockResultCollector);
		when(mockFunction.hasResult()).thenReturn(true);
		when(mockResultCollector.getResult(500, TimeUnit.MILLISECONDS)).thenReturn(results);

		AbstractFunctionExecution functionExecution = new AbstractFunctionExecution() {
			@Override protected Execution getExecution() {
				return mockExecution;
			}
		};

		Iterable<Object> actualResults = functionExecution.setFunction(mockFunction)
			.setArguments(args).setTimeout(500).execute();

		assertThat(actualResults).isNotNull();
		assertThat(actualResults).isEqualTo(results);

		verify(mockExecution, times(1)).setArguments(eq(args));
		verify(mockExecution, never()).withCollector(any(ResultCollector.class));
		verify(mockExecution, never()).withFilter(any(Set.class));
		verify(mockExecution, times(1)).execute(eq(mockFunction));
		verify(mockExecution, never()).execute(any(String.class));
		verify(mockResultCollector, times(1)).getResult(500, TimeUnit.MILLISECONDS);
		verify(mockResultCollector, never()).getResult();
	}

	@Test
	public void executeAndExtractWithSingleResult() {

		List<String> results = Collections.singletonList("test");

		AbstractFunctionExecution functionExecution = new AbstractFunctionExecution() {

			@Override
			protected Execution getExecution() {
				return mockExecution;
			}

			@SuppressWarnings("unchecked")
			@Override <T> Iterable<T> execute() {
				return (Iterable<T>) results;
			}
		};

		assertThat(functionExecution.<String>executeAndExtract()).isEqualTo("test");
	}

	@Test
	public void executeAndExtractWithMultipleResults() {

		List<String> results = Arrays.asList("one", "two", "three");

		AbstractFunctionExecution functionExecution = new AbstractFunctionExecution() {

			@Override
			protected Execution getExecution() {
				return mockExecution;
			}

			@SuppressWarnings("unchecked")
			@Override <T> Iterable<T> execute() {
				return (Iterable<T>) results;
			}
		};

		assertThat(functionExecution.<String>executeAndExtract()).isEqualTo("one");
	}

	@Test
	public void executeAndExtractWithNullResults() {

		AbstractFunctionExecution functionExecution = new AbstractFunctionExecution() {

			@Override
			protected Execution getExecution() {
				return mockExecution;
			}

			@Override <T> Iterable<T> execute() {
				return null;
			}
		};

		assertThat((Object) functionExecution.executeAndExtract()).isNull();
	}

	@Test
	public void executeAndExtractWithNoResults() {

		AbstractFunctionExecution functionExecution = new AbstractFunctionExecution() {

			@Override
			protected Execution getExecution() {
				return mockExecution;
			}

			@Override
			<T> Iterable<T> execute() {
				return Collections.emptyList();
			}
		};

		assertThat((Object) functionExecution.executeAndExtract()).isNull();
	}

	@Test(expected = FunctionException.class)
	public void executeAndExtractWithThrowsException() {

		AbstractFunctionExecution functionExecution = mock(AbstractFunctionExecution.class);

		doReturn(Collections.singleton(new IllegalArgumentException("test"))).when(functionExecution).execute();
		doCallRealMethod().when(functionExecution).setFunctionId(anyString());
		doCallRealMethod().when(functionExecution).getFunctionId();
		doCallRealMethod().when(functionExecution).executeAndExtract();

		try {
			functionExecution.setFunctionId("TestFunction").executeAndExtract();
		}
		catch (Exception expected) {

			assertThat(expected).isInstanceOf(FunctionException.class);
			assertThat(expected).hasMessage("Execution of Function [with ID [TestFunction]] failed");
			assertThat(expected).hasCauseInstanceOf(IllegalArgumentException.class);
			assertThat(expected.getCause()).hasMessage("test");
			assertThat(expected.getCause()).hasNoCause();

			throw expected;
		}
	}
}
