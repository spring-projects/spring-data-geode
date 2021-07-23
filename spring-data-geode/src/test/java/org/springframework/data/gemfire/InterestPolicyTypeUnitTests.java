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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.InterestPolicy;

/**
 * Unit Tests for {@link InterestPolicyType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.InterestPolicy
 * @see org.springframework.data.gemfire.InterestPolicyType
 * @since 1.6.0
 */
public class InterestPolicyTypeUnitTests {

	@Test
	public void testStaticGetInterestPolicy() {

		assertThat(InterestPolicyType.getInterestPolicy(InterestPolicyType.ALL)).isEqualTo(InterestPolicy.ALL);
		assertThat(InterestPolicyType.getInterestPolicy(InterestPolicyType.CACHE_CONTENT)).isEqualTo(InterestPolicy.CACHE_CONTENT);
	}

	@Test
	public void testStaticGetInterestPolicyWithNull() {
		assertThat(InterestPolicyType.getInterestPolicy(null)).isNull();
	}

	@Test
	public void testGetInterestPolicy() {

		assertThat(InterestPolicyType.ALL.getInterestPolicy()).isEqualTo(InterestPolicy.ALL);
		assertThat(InterestPolicyType.CACHE_CONTENT.getInterestPolicy()).isEqualTo(InterestPolicy.CACHE_CONTENT);
	}

	@Test
	public void testDefault() {

		assertThat(InterestPolicyType.DEFAULT.getInterestPolicy()).isEqualTo(InterestPolicy.DEFAULT);
		assertThat(InterestPolicyType.DEFAULT).isSameAs(InterestPolicyType.CACHE_CONTENT);
	}

	@Test
	public void testValueOf() {

		try {
			for (byte ordinal = 0; ordinal < Byte.MAX_VALUE; ordinal++) {
				InterestPolicy interestPolicy = InterestPolicy.fromOrdinal(ordinal);
				InterestPolicyType interestPolicyType = InterestPolicyType.valueOf(interestPolicy);

				assertThat(interestPolicyType).isNotNull();
				assertThat(interestPolicyType.getInterestPolicy()).isEqualTo(interestPolicy);
			}
		}
		catch (ArrayIndexOutOfBoundsException ignore) { }
	}

	@Test
	public void testValueOfWithNull() {
		assertThat(InterestPolicyType.valueOf((InterestPolicy) null)).isNull();
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(InterestPolicyType.valueOfIgnoreCase("all")).isEqualTo(InterestPolicyType.ALL);
		assertThat(InterestPolicyType.valueOfIgnoreCase("Cache_Content")).isEqualTo(InterestPolicyType.CACHE_CONTENT);
		assertThat(InterestPolicyType.valueOfIgnoreCase("ALL")).isEqualTo(InterestPolicyType.ALL);
		assertThat(InterestPolicyType.valueOfIgnoreCase("CACHE_ConTent")).isEqualTo(InterestPolicyType.CACHE_CONTENT);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {

		assertThat(InterestPolicyType.valueOfIgnoreCase("@11")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase("CACHE_KEYS")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase("invalid")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase("test")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase("  ")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase("")).isNull();
		assertThat(InterestPolicyType.valueOfIgnoreCase(null)).isNull();
	}
}
