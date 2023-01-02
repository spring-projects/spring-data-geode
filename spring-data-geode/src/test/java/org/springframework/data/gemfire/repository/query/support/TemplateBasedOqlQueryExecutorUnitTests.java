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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Test;

import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit Tests for {@link TemplateBasedOqlQueryExecutor}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.query.support.TemplateBasedOqlQueryExecutor
 * @since 2.4.0
 */
public class TemplateBasedOqlQueryExecutorUnitTests {

	@Test
	public void constructTemplateBasedOqlQueryExecutorSuccessfully() {

		GemfireTemplate mockTemplate = mock(GemfireTemplate.class);

		TemplateBasedOqlQueryExecutor queryExecutor = new TemplateBasedOqlQueryExecutor(mockTemplate);

		assertThat(queryExecutor).isNotNull();
		assertThat(queryExecutor.getTemplate()).isEqualTo(mockTemplate);

		verifyNoInteractions(mockTemplate);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructTemplateBasedOqlQueryExecutorWithNullTemplateThrowsIllegalArgumentException() {

		try {
			new TemplateBasedOqlQueryExecutor(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("GemfireTemplate must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void executeCallsTemplateFind() {

		GemfireTemplate mockTemplate = mock(GemfireTemplate.class);

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		SelectResults<?> mockSelectResults = mock(SelectResults.class);

		String query = "SELECT * FROM /TestRegion WHERE id = $1";

		doReturn(mockSelectResults).when(mockTemplate).find(eq(query), eq(1));

		TemplateBasedOqlQueryExecutor queryExecutor = new TemplateBasedOqlQueryExecutor(mockTemplate);

		assertThat(queryExecutor.execute(mockQueryMethod, query, 1)).isEqualTo(mockSelectResults);

		verify(mockTemplate, times(1)).find(eq(query), eq(1));
		verifyNoInteractions(mockQueryMethod, mockSelectResults);
	}
}
