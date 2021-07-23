/*
 * Copyright 2016-2021 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.eviction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

/**
 * Unit Tests for {@link EvictionPolicyConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.EvictionAttributes
 * @see org.springframework.data.gemfire.eviction.EvictionPolicyConverter
 * @see org.springframework.data.gemfire.eviction.EvictionPolicyType
 * @since 1.6.0
 */
public class EvictionPolicyConverterUnitTests {

	private final EvictionPolicyConverter converter = new EvictionPolicyConverter();

	@After
	public void tearDown() {
		converter.setValue(null);
	}

	@Test
	public void convert() {

		assertThat(converter.convert("entry_count")).isEqualTo(EvictionPolicyType.ENTRY_COUNT);
		assertThat(converter.convert("Heap_Percentage")).isEqualTo(EvictionPolicyType.HEAP_PERCENTAGE);
		assertThat(converter.convert("MEMorY_SiZe")).isEqualTo(EvictionPolicyType.MEMORY_SIZE);
		assertThat(converter.convert("NONE")).isEqualTo(EvictionPolicyType.NONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertIllegalValue() {

		try {
			converter.convert("LIFO_MEMORY");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[LIFO_MEMORY] is not a valid EvictionPolicyType");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAsText() {

		assertThat(converter.getValue()).isNull();

		converter.setAsText("heap_percentage");

		assertThat(converter.getValue()).isEqualTo(EvictionPolicyType.HEAP_PERCENTAGE);

		converter.setAsText("NOne");

		assertThat(converter.getValue()).isEqualTo(EvictionPolicyType.NONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setAsTextWithIllegalValue() {

		try {
			converter.setAsText("LRU_COUNT");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[LRU_COUNT] is not a valid EvictionPolicyType");
			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			assertThat(converter.getValue()).isNull();
		}
	}
}
