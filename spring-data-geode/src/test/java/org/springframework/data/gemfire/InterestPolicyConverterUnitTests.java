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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.InterestPolicy;

/**
 * Unit Tests for {@link InterestPolicyConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.InterestPolicy
 * @see org.springframework.data.gemfire.InterestPolicyConverter
 * @since 1.6.0
 */
public class InterestPolicyConverterUnitTests {

	private final InterestPolicyConverter converter = new InterestPolicyConverter();

	@After
	public void tearDown() {
		converter.setValue(null);
	}

	@Test
	public void convert() {

		assertThat(converter.convert("all")).isEqualTo(InterestPolicy.ALL);
		assertThat(converter.convert("Cache_Content")).isEqualTo(InterestPolicy.CACHE_CONTENT);
		assertThat(converter.convert("CACHE_ConTent")).isEqualTo(InterestPolicy.CACHE_CONTENT);
		assertThat(converter.convert("ALL")).isEqualTo(InterestPolicy.ALL);
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertIllegalValue() {

		try {
			converter.convert("invalid_value");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[invalid_value] is not a valid InterestPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAsText() {

		assertThat(converter.getValue()).isNull();

		converter.setAsText("aLl");

		assertThat(converter.getValue()).isEqualTo(InterestPolicy.ALL);

		converter.setAsText("Cache_CoNTeNT");

		assertThat(converter.getValue()).isEqualTo(InterestPolicy.CACHE_CONTENT);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setAsTextWithInvalidValue() {

		try {
			converter.setAsText("none");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[none] is not a valid InterestPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(converter.getValue()).isNull();
		}
	}
}
