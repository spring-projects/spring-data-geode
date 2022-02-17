/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.Scope;

/**
 * Unit Tests for {@link ScopeConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.ScopeConverter
 * @see org.apache.geode.cache.Scope
 * @since 1.6.0
 */
public class ScopeConverterUnitTests {

	private final ScopeConverter converter = new ScopeConverter();

	@After
	public void tearDown() {
		converter.setValue(null);
	}

	@Test
	public void convert() {

		assertThat(converter.convert("distributed-ACK")).isEqualTo(Scope.DISTRIBUTED_ACK);
		assertThat(converter.convert(" Distributed_NO-aCK")).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
		assertThat(converter.convert("loCAL  ")).isEqualTo(Scope.LOCAL);
		assertThat(converter.convert(" GLOBal  ")).isEqualTo(Scope.GLOBAL);
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertIllegalValue() {

		try {
			converter.convert("illegal-value");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[illegal-value] is not a valid Scope");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAsText() {

		assertThat(converter.getValue()).isNull();

		converter.setAsText("DisTributeD-nO_Ack");

		assertThat(converter.getValue()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);

		converter.setAsText("distributed-ack");

		assertThat(converter.getValue()).isEqualTo(Scope.DISTRIBUTED_ACK);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setAsTextWithIllegalValue() {

		try {
			converter.setAsText("d!5tr!but3d-n0_@ck");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[d!5tr!but3d-n0_@ck] is not a valid Scope");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(converter.getValue()).isNull();
		}
	}
}
