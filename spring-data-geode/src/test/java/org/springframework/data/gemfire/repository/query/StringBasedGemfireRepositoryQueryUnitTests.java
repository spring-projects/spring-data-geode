/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.data.gemfire.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.apache.geode.cache.query.SelectResults;

/**
 * Unit Tests for {@link StringBasedGemfireRepositoryQuery}
 *
 * @author John Blum
 * @see org.mockito.Mockito
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.repository.query.StringBasedGemfireRepositoryQuery
 * @since 1.4.0
 */
public class StringBasedGemfireRepositoryQueryUnitTests {

	private final StringBasedGemfireRepositoryQuery repositoryQuery = new StringBasedGemfireRepositoryQuery();

	@Test
	@SuppressWarnings("unchecked")
	public void testToCollectionWithSelectResults() {

		SelectResults<String> mockSelectResults = mock(SelectResults.class);

		List<String> expectedList = Arrays.asList("one", "two", "three");

		when(mockSelectResults.asList()).thenReturn(expectedList);

		Collection<?> actualList = this.repositoryQuery.toCollection(mockSelectResults);

		assertThat(actualList).isSameAs(expectedList);
	}

	@Test
	public void testToCollectionWithCollection() {

		List<String> expectedList = Arrays.asList("x", "y", "z");

		Collection<?> actualList = this.repositoryQuery.toCollection(expectedList);

		assertThat(actualList).isSameAs(expectedList);
	}

	@Test
	@SuppressWarnings("all")
	public void testToCollectionWithArray() {

		Object[] array = { 1, 2, 3 };

		Collection<?> list = repositoryQuery.toCollection(array);

		assertThat(list).isNotNull();
		assertThat(list).isNotSameAs(array);
		assertThat(list).isInstanceOf(List.class);
		assertThat(list.size()).isEqualTo(array.length);
		assertThat(list.containsAll(Arrays.asList(array))).isTrue();
	}

	@Test
	public void testToCollectionWithSingleObject() {

		Collection<?> list = repositoryQuery.toCollection("test");

		assertThat(list instanceof List).isTrue();
		assertThat(list.isEmpty()).isFalse();
		assertThat(list.size()).isEqualTo(1);
		assertThat(((List<?>) list).get(0)).isEqualTo("test");
	}

	@Test
	public void testToCollectionWithNull() {

		Collection<?> list = repositoryQuery.toCollection(null);

		assertThat(list).isNotNull();
		assertThat(list.isEmpty()).isTrue();
	}

	@Test
	public void applyAllQueryAnnotationExtensions() {

		GemfireQueryMethod mockQueryMethod = mock(GemfireQueryMethod.class, "MockGemfireQueryMethod");

		when(mockQueryMethod.hasHint()).thenReturn(true);
		when(mockQueryMethod.getHints()).thenReturn(Arrays.asList("IdIdx", "NameIdx").toArray(new String[2]));
		when(mockQueryMethod.hasImport()).thenReturn(true);
		when(mockQueryMethod.getImport()).thenReturn("org.example.domain.Type");
		when(mockQueryMethod.hasLimit()).thenReturn(true);
		when(mockQueryMethod.getLimit()).thenReturn(10);
		when(mockQueryMethod.hasTrace()).thenReturn(true);

		QueryString queryString = QueryString.of("SELECT * FROM /Example");

		assertThat(queryString.toString()).isEqualTo("SELECT * FROM /Example");

		StringBasedGemfireRepositoryQuery repositoryQuery = new StringBasedGemfireRepositoryQuery();

		String postProcessedQueryString = repositoryQuery.getQueryPostProcessor()
			.postProcess(mockQueryMethod, queryString.toString());

		assertThat(postProcessedQueryString).isNotNull();
		assertThat(postProcessedQueryString).isEqualTo(
			"<TRACE> <HINT 'IdIdx', 'NameIdx'> IMPORT org.example.domain.Type; SELECT * FROM /Example LIMIT 10");

		verify(mockQueryMethod, times(1)).hasHint();
		verify(mockQueryMethod, times(1)).getHints();
		verify(mockQueryMethod, times(1)).hasImport();
		verify(mockQueryMethod, times(1)).getImport();
		verify(mockQueryMethod, times(1)).hasLimit();
		verify(mockQueryMethod, times(1)).getLimit();
		verify(mockQueryMethod, times(1)).hasTrace();
	}

	@Test
	public void applyHintLimitAndTraceQueryAnnotationExtensionsWithExistingHintAndLimit() {

		GemfireQueryMethod mockQueryMethod = mock(GemfireQueryMethod.class, "MockGemfireQueryMethod");

		when(mockQueryMethod.hasHint()).thenReturn(true);
		when(mockQueryMethod.getHints()).thenReturn(Collections.singletonList("FirstNameIdx").toArray(new String[1]));
		when(mockQueryMethod.hasImport()).thenReturn(false);
		when(mockQueryMethod.hasLimit()).thenReturn(true);
		when(mockQueryMethod.getLimit()).thenReturn(50);
		when(mockQueryMethod.hasTrace()).thenReturn(true);

		QueryString queryString = new QueryString("<HINT 'LastNameIdx'> SELECT * FROM /Example LIMIT 25");

		assertThat(queryString.toString()).isEqualTo("<HINT 'LastNameIdx'> SELECT * FROM /Example LIMIT 25");

		StringBasedGemfireRepositoryQuery repositoryQuery = new StringBasedGemfireRepositoryQuery();

		String postProcessedQueryString = repositoryQuery.getQueryPostProcessor().postProcess(mockQueryMethod, queryString.toString());

		assertThat(postProcessedQueryString).isNotNull();
		assertThat(postProcessedQueryString).isEqualTo("<TRACE> <HINT 'LastNameIdx'> SELECT * FROM /Example LIMIT 25");

		verify(mockQueryMethod, times(1)).hasHint();
		verify(mockQueryMethod, never()).getHints();
		verify(mockQueryMethod, times(1)).hasImport();
		verify(mockQueryMethod, never()).getImport();
		verify(mockQueryMethod, times(1)).hasLimit();
		verify(mockQueryMethod, never()).getLimit();
		verify(mockQueryMethod, times(1)).hasTrace();
	}

	@Test
	public void applyImportAndTraceQueryAnnotationExtensionsWithExistingTrace() {

		GemfireQueryMethod mockQueryMethod = mock(GemfireQueryMethod.class, "MockGemfireQueryMethod");

		when(mockQueryMethod.hasHint()).thenReturn(false);
		when(mockQueryMethod.hasImport()).thenReturn(true);
		when(mockQueryMethod.getImport()).thenReturn("org.example.domain.Type");
		when(mockQueryMethod.hasLimit()).thenReturn(false);
		when(mockQueryMethod.hasTrace()).thenReturn(true);

		QueryString queryString = new QueryString("<TRACE> SELECT * FROM /Example");

		assertThat(queryString.toString()).isEqualTo("<TRACE> SELECT * FROM /Example");

		StringBasedGemfireRepositoryQuery repositoryQuery = new StringBasedGemfireRepositoryQuery();

		String postProcessedQueryString = repositoryQuery.getQueryPostProcessor().postProcess(mockQueryMethod, queryString.toString());

		assertThat(postProcessedQueryString).isNotNull();
		assertThat(postProcessedQueryString).isEqualTo("IMPORT org.example.domain.Type; <TRACE> SELECT * FROM /Example");

		verify(mockQueryMethod, times(1)).hasHint();
		verify(mockQueryMethod, never()).getHints();
		verify(mockQueryMethod, times(1)).hasImport();
		verify(mockQueryMethod, times(1)).getImport();
		verify(mockQueryMethod, times(1)).hasLimit();
		verify(mockQueryMethod, never()).getLimit();
		verify(mockQueryMethod, times(1)).hasTrace();
	}
}
