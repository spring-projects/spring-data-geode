/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire.snapshot.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.apache.geode.cache.snapshot.SnapshotFilter;

import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.snapshot.filter.ComposableSnapshotFilter.Operator;

/**
 * Unit Tests for {@link ComposableSnapshotFilter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.snapshot.SnapshotFilter
 * @see org.springframework.data.gemfire.snapshot.filter.ComposableSnapshotFilter
 * @see org.springframework.data.gemfire.snapshot.filter.ComposableSnapshotFilter.Operator
 * @since 1.7.0
 */
@SuppressWarnings("unchecked")
public class ComposableSnapshotFilterUnitTests {

	private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

	private SnapshotFilter<Object, Object> mockSnapshotFilter(boolean accept) {

		SnapshotFilter<Object, Object> mockSnapshotFilter =
			mock(SnapshotFilter.class, String.format("MockSnapshotFilter-%1$d", ID_SEQUENCE.incrementAndGet()));

		when(mockSnapshotFilter.accept(any())).thenReturn(accept);

		return mockSnapshotFilter;
	}

	@Test
	public void operatorIdentityIsSuccessful() {

		assertThat(Operator.AND.isAnd()).isTrue();
		assertThat(Operator.AND.isOr()).isFalse();
		assertThat(Operator.OR.isAnd()).isFalse();
		assertThat(Operator.OR.isOr()).isTrue();
	}

	@Test
	public void andOperatorOperationIsValid() {

		assertThat(Operator.AND.operate(true, true)).isTrue();
		assertThat(Operator.AND.operate(true, false)).isFalse();
		assertThat(Operator.AND.operate(false, true)).isFalse();
		assertThat(Operator.AND.operate(false, false)).isFalse();
	}

	@Test
	public void orOperatorOperationIsValid() {

		assertThat(Operator.OR.operate(true, true)).isTrue();
		assertThat(Operator.OR.operate(true, false)).isTrue();
		assertThat(Operator.OR.operate(false, true)).isTrue();
		assertThat(Operator.OR.operate(false, false)).isFalse();
	}

	@Test
	public void composeSingle() {

		SnapshotFilter<Object, Object> mockSnapshotFilter = mockSnapshotFilter(false);
		SnapshotFilter<Object, Object> composedFilter = ComposableSnapshotFilter.compose(Operator.AND, mockSnapshotFilter);

		assertThat(composedFilter).isSameAs(mockSnapshotFilter);
	}

	@Test
	public void composeMultiple() throws Exception {

		SnapshotFilter<Object, Object> mockSnapshotFilterOne = mockSnapshotFilter(false);
		SnapshotFilter<Object, Object> mockSnapshotFilterTwo = mockSnapshotFilter(true);

		SnapshotFilter<Object, Object> composedFilter =
			ComposableSnapshotFilter.compose(Operator.AND, mockSnapshotFilterOne,mockSnapshotFilterTwo);

		assertThat(composedFilter).isNotSameAs(mockSnapshotFilterOne);
		assertThat(composedFilter).isNotSameAs(mockSnapshotFilterTwo);
		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);
		assertThat(TestUtils.<SnapshotFilter<Object, Object>>readField("leftOperand", composedFilter))
			.isEqualTo(mockSnapshotFilterTwo);
		assertThat((Operator) TestUtils.readField("operator", composedFilter)).isEqualTo(Operator.AND);
		assertThat(TestUtils.<SnapshotFilter<Object, Object>>readField("rightOperand", composedFilter))
			.isEqualTo(mockSnapshotFilterOne);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void composeAndThenAccept() {

		SnapshotFilter<Object, Object> falseFilter = mockSnapshotFilter(false);
		SnapshotFilter<Object, Object> trueFilter = mockSnapshotFilter(true);

		SnapshotFilter<Object, Object> composedFilter = ComposableSnapshotFilter.and(trueFilter, trueFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isTrue();

		composedFilter = ComposableSnapshotFilter.and(falseFilter, trueFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isFalse();

		composedFilter = ComposableSnapshotFilter.and(falseFilter, falseFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void composeOrThenAccept() {

		SnapshotFilter<Object, Object> falseFilter = mockSnapshotFilter(false);
		SnapshotFilter<Object, Object> trueFilter = mockSnapshotFilter(true);

		SnapshotFilter<Object, Object> composedFilter = ComposableSnapshotFilter.or(trueFilter, trueFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isTrue();

		composedFilter = ComposableSnapshotFilter.or(falseFilter, trueFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isTrue();

		composedFilter = ComposableSnapshotFilter.or(falseFilter, falseFilter);

		assertThat((ComposableSnapshotFilter<Object, Object>) composedFilter)
			.isInstanceOf(ComposableSnapshotFilter.class);

		assertThat(composedFilter.accept(null)).isFalse();
	}
}
