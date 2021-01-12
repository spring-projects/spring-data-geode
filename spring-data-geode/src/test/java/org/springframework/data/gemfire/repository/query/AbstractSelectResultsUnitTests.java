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
package org.springframework.data.gemfire.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.types.CollectionType;
import org.apache.geode.cache.query.types.ObjectType;

import org.springframework.lang.NonNull;

/**
 * Unit Tests for {@link AbstractSelectResults}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.gemfire.repository.query.AbstractSelectResults
 * @since 2.4.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class AbstractSelectResultsUnitTests {

	@Mock
	private SelectResults<Object> mockSelectResults;

	@Test
	public void constructsAbstractSelectResultsSuccessfully() {

		AbstractSelectResults<?> selectResults = new TestSelectResults(this.mockSelectResults);

		assertThat(selectResults).isNotNull();
		assertThat(selectResults.getSelectResults()).isSameAs(this.mockSelectResults);

		verifyNoInteractions(this.mockSelectResults);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructAbstractSelectResultsWithNullThrowsIllegalArgumentException() {

		try {
			new TestSelectResults(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("SelectResults must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void asListCallsSelectResultsAsList() {

		List<Object> mockList = mock(List.class);

		doReturn(mockList).when(this.mockSelectResults).asList();

		assertThat(new TestSelectResults(this.mockSelectResults).asList()).isEqualTo(mockList);

		verify(this.mockSelectResults, times(1)).asList();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void asSetCallsSelectResultsAsSet() {

		Set<Object> mockSet = mock(Set.class);

		doReturn(mockSet).when(this.mockSelectResults).asSet();

		assertThat(new TestSelectResults(this.mockSelectResults).asSet()).isEqualTo(mockSet);

		verify(this.mockSelectResults, times(1)).asSet();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void getCollectionTypeCallsSelectResultsGetCollectionType() {

		CollectionType mockCollectionType = mock(CollectionType.class);

		doReturn(mockCollectionType).when(this.mockSelectResults).getCollectionType();

		assertThat(new TestSelectResults(this.mockSelectResults).getCollectionType()).isEqualTo(mockCollectionType);

		verify(this.mockSelectResults, times(1)).getCollectionType();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void isModifiableCallsSelectResultsIsModifiable() {

		doReturn(true).when(this.mockSelectResults).isModifiable();

		assertThat(new TestSelectResults(this.mockSelectResults).isModifiable()).isTrue();

		verify(this.mockSelectResults, times(1)).isModifiable();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void occurrencesCallsSelectResultsOccurrences() {

		doReturn(5).when(this.mockSelectResults).occurrences(eq("test"));

		assertThat(new TestSelectResults(this.mockSelectResults).occurrences("test")).isEqualTo(5);

		verify(this.mockSelectResults, times(1)).occurrences(eq("test"));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void setObjectTypeCallsSelectResultsSetObjectType() {

		ObjectType mockObjectType = mock(ObjectType.class);

		new TestSelectResults(this.mockSelectResults).setElementType(mockObjectType);

		verify(this.mockSelectResults, times(1)).setElementType(eq(mockObjectType));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void addCallsSelectResultsAdd() {

		doReturn(true).when(this.mockSelectResults).add(any());

		assertThat(new TestSelectResults(this.mockSelectResults).add("test")).isTrue();

		verify(this.mockSelectResults, times(1)).add(eq("test"));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void addAllCallsSelectResultsAddAll() {

		Collection<Object> mockCollection = mock(Collection.class);

		doReturn(true).when(this.mockSelectResults).addAll(anyCollection());

		assertThat(new TestSelectResults(this.mockSelectResults).addAll(mockCollection)).isTrue();

		verify(this.mockSelectResults, times(1)).addAll(eq(mockCollection));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void clearCallsSelectResultsClear() {

		new TestSelectResults(this.mockSelectResults).clear();

		verify(this.mockSelectResults, times(1)).clear();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void containsCallsSelectResultsContains() {

		doReturn(false).when(this.mockSelectResults).contains(any());

		assertThat(new TestSelectResults(this.mockSelectResults).contains("test")).isFalse();

		verify(this.mockSelectResults, times(1)).contains(eq("test"));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void containsAllCallsSelectResultsContainsAll() {

		Collection<Object> mockCollection = mock(Collection.class);

		doReturn(true).when(this.mockSelectResults).containsAll(anyCollection());

		assertThat(new TestSelectResults(this.mockSelectResults).containsAll(mockCollection)).isTrue();

		verify(this.mockSelectResults, times(1)).containsAll(eq(mockCollection));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void isEmptyCallsSelectResultsIsEmpty() {

		doReturn(false).when(this.mockSelectResults).isEmpty();

		assertThat(new TestSelectResults(this.mockSelectResults).isEmpty()).isFalse();

		verify(this.mockSelectResults, times(1)).isEmpty();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void iteratorCallsSelectResultsIterator() {

		Iterator<Object> mockIterator = mock(Iterator.class);

		doReturn(mockIterator).when(this.mockSelectResults).iterator();

		assertThat(new TestSelectResults(this.mockSelectResults).iterator()).isEqualTo(mockIterator);

		verify(this.mockSelectResults, times(1)).iterator();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void removeCallsSelectResultsRemove() {

		doReturn(true).when(this.mockSelectResults).remove(any());

		assertThat(new TestSelectResults(this.mockSelectResults).remove("mock")).isTrue();

		verify(this.mockSelectResults, times(1)).remove(eq("mock"));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void removeAllCallsSelectResultsRemoveAll() {

		Collection<Object> mockCollection = mock(Collection.class);

		doReturn(false).when(this.mockSelectResults).removeAll(anyCollection());

		assertThat(new TestSelectResults(this.mockSelectResults).removeAll(mockCollection)).isFalse();

		verify(this.mockSelectResults, times(1)).removeAll(eq(mockCollection));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void retainAllCallsSelectResultsRetainAll() {

		Collection<Object> mockCollection = mock(Collection.class);

		doReturn(true).when(this.mockSelectResults).retainAll(anyCollection());

		assertThat(new TestSelectResults(this.mockSelectResults).retainAll(mockCollection)).isTrue();

		verify(this.mockSelectResults, times(1)).retainAll(eq(mockCollection));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void sizeCallsSelectResultsSize() {

		doReturn(50).when(this.mockSelectResults).size();

		assertThat(new TestSelectResults(this.mockSelectResults).size()).isEqualTo(50);

		verify(this.mockSelectResults, times(1)).size();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void toArrayCallsSelectResultsToArray() {

		Object[] array = { "test", "mock" };

		doReturn(array).when(this.mockSelectResults).toArray();

		assertThat(new TestSelectResults(this.mockSelectResults).toArray()).isEqualTo(array);

		verify(this.mockSelectResults, times(1)).toArray();
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	@Test
	public void toArrayWithArrayArgumentCallsSelectResultsToArray() {

		Object[] array = { "test", "mock" };

		doReturn(array).when(this.mockSelectResults).toArray(eq(new Object[0]));

		assertThat(new TestSelectResults(this.mockSelectResults).toArray(new Object[0])).isEqualTo(array);

		verify(this.mockSelectResults, times(1)).toArray(eq(new Object[0]));
		verifyNoMoreInteractions(this.mockSelectResults);
	}

	static class TestSelectResults extends AbstractSelectResults<Object> {

		TestSelectResults(@NonNull SelectResults<Object> selectResults) {
			super(selectResults);
		}
	}
}
