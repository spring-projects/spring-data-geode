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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.InterestResultPolicy;

/**
 * Unit Tests for {@link InterestResultPolicyType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.InterestResultPolicy
 * @see org.springframework.data.gemfire.client.InterestResultPolicyTypeUnitTests
 * @since 1.6.0
 */
public class InterestResultPolicyTypeUnitTests {

	@Test
	public void testStaticGetInterestResultPolicy() {

		assertThat(InterestResultPolicyType.getInterestResultPolicy(InterestResultPolicyType.KEYS)).isEqualTo(InterestResultPolicy.KEYS);
		assertThat(InterestResultPolicyType.getInterestResultPolicy(InterestResultPolicyType.KEYS_VALUES)).isEqualTo(InterestResultPolicy.KEYS_VALUES);
	}

	@Test
	public void testStaticGetInterestResultPolicyWithNull() {
		assertThat(InterestResultPolicyType.getInterestResultPolicy(null)).isNull();
	}

	@Test
	public void testDefault() {

		assertThat(InterestResultPolicyType.valueOf(InterestResultPolicy.DEFAULT)).isEqualTo(InterestResultPolicyType.DEFAULT);
		assertThat(InterestResultPolicyType.DEFAULT.getInterestResultPolicy()).isEqualTo(InterestResultPolicy.DEFAULT);
		assertThat(InterestResultPolicyType.DEFAULT).isSameAs(InterestResultPolicyType.KEYS_VALUES);
	}

	@Test
	public void testValueOf() {

		try {
			for (byte ordinal = 0; ordinal < Byte.MAX_VALUE; ordinal++) {

				InterestResultPolicy interestResultPolicy = InterestResultPolicy.fromOrdinal(ordinal);

				InterestResultPolicyType interestResultPolicyType =
					InterestResultPolicyType.valueOf(interestResultPolicy);

				assertThat(interestResultPolicyType).isNotNull();
				assertThat(interestResultPolicyType.getInterestResultPolicy()).isEqualTo(interestResultPolicy);
			}
		}
		catch (ArrayIndexOutOfBoundsException ignore) {
		}
	}

	@Test
	public void testValueOfWithNull() {
		assertThat(InterestResultPolicyType.valueOf((InterestResultPolicy) null)).isNull();
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(InterestResultPolicyType.valueOfIgnoreCase("KEYS")).isEqualTo(InterestResultPolicyType.KEYS);
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("Keys_Values")).isEqualTo(InterestResultPolicyType.KEYS_VALUES);
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("none")).isEqualTo(InterestResultPolicyType.NONE);
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("nONE")).isEqualTo(InterestResultPolicyType.NONE);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("keyz")).isNull();

		assertThat(InterestResultPolicyType.valueOfIgnoreCase("KEY_VALUE")).isNull();
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("all")).isNull();
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("  ")).isNull();
		assertThat(InterestResultPolicyType.valueOfIgnoreCase("")).isNull();
		assertThat(InterestResultPolicyType.valueOfIgnoreCase(null)).isNull();
	}
}
