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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.InterestResultPolicy;

/**
 * Unit Tests for {@link InterestResultPolicyConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.InterestResultPolicy
 * @see org.springframework.data.gemfire.client.InterestResultPolicyConverter
 * @see org.springframework.data.gemfire.client.InterestResultPolicyType
 * @since 1.6.0
 */
public class InterestResultPolicyConverterUnitTests {

	private final InterestResultPolicyConverter converter = new InterestResultPolicyConverter();

	@After
	public void tearDown() {
		converter.setValue(null);
	}

	@Test
	public void convert() {

		assertThat(converter.convert("NONE")).isEqualTo(InterestResultPolicy.NONE);
		assertThat(converter.convert("kEyS_ValUes")).isEqualTo(InterestResultPolicy.KEYS_VALUES);
		assertThat(converter.convert("nONe")).isEqualTo(InterestResultPolicy.NONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertIllegalValue() {

		try {
			converter.convert("illegal_value");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[illegal_value] is not a valid InterestResultPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAsText() {

		assertThat(converter.getValue()).isNull();

		converter.setAsText("NOne");

		assertThat(converter.getValue()).isEqualTo(InterestResultPolicy.NONE);

		converter.setAsText("KeYs");

		assertThat(converter.getValue()).isEqualTo(InterestResultPolicy.KEYS);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setAsTextWithIllegalValue() {

		try {
			converter.setAsText("illegal_value");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[illegal_value] is not a valid InterestResultPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(converter.getValue()).isNull();
		}
	}
}
