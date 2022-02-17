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

import org.junit.Test;

/**
 * Unit Tests for {@link IndexType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.IndexType
 * @since 1.5.2
 */
@SuppressWarnings("deprecation")
public class IndexTypeUnitTests {

	@Test
	public void testGetGemfireIndexType() {

		assertThat(IndexType.FUNCTIONAL.getGemfireIndexType()).isEqualTo(org.apache.geode.cache.query.IndexType.FUNCTIONAL);
		assertThat(IndexType.HASH.getGemfireIndexType()).isEqualTo(org.apache.geode.cache.query.IndexType.HASH);
		assertThat(IndexType.KEY.getGemfireIndexType()).isEqualTo(org.apache.geode.cache.query.IndexType.PRIMARY_KEY);
		assertThat(IndexType.PRIMARY_KEY.getGemfireIndexType()).isEqualTo(org.apache.geode.cache.query.IndexType.PRIMARY_KEY);
	}

	@Test
	public void testValueOf() {

		assertThat(IndexType.valueOf(org.apache.geode.cache.query.IndexType.FUNCTIONAL)).isEqualTo(IndexType.FUNCTIONAL);
		assertThat(IndexType.valueOf(org.apache.geode.cache.query.IndexType.HASH)).isEqualTo(IndexType.HASH);
		assertThat(IndexType.valueOf(org.apache.geode.cache.query.IndexType.PRIMARY_KEY)).isEqualTo(IndexType.PRIMARY_KEY);
	}

	@Test
	public void testValueOfWithNull() {
		assertThat(IndexType.valueOf((org.apache.geode.cache.query.IndexType) null)).isNull();
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(IndexType.valueOfIgnoreCase("functional")).isEqualTo(IndexType.FUNCTIONAL);
		assertThat(IndexType.valueOfIgnoreCase("HasH")).isEqualTo(IndexType.HASH);
		assertThat(IndexType.valueOfIgnoreCase("Key")).isEqualTo(IndexType.KEY);
		assertThat(IndexType.valueOfIgnoreCase("PriMary_Key")).isEqualTo(IndexType.PRIMARY_KEY);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {

		assertThat(IndexType.valueOfIgnoreCase("Prime_Index")).isNull();
		assertThat(IndexType.valueOfIgnoreCase("SECONDARY_INDEX")).isNull();
		assertThat(IndexType.valueOfIgnoreCase("unique_index")).isNull();
		assertThat(IndexType.valueOfIgnoreCase(null)).isNull();
		assertThat(IndexType.valueOfIgnoreCase("  ")).isNull();
		assertThat(IndexType.valueOfIgnoreCase("")).isNull();
	}

	@Test
	public void testIsFunctional() {

		assertThat(IndexType.FUNCTIONAL.isFunctional()).isTrue();
		assertThat(IndexType.HASH.isFunctional()).isFalse();
		assertThat(IndexType.KEY.isFunctional()).isFalse();
		assertThat(IndexType.PRIMARY_KEY.isFunctional()).isFalse();
	}

	@Test
	public void testIsNullSafeFunctional() {

		assertThat(IndexType.isFunctional(null)).isFalse();
		assertThat(IndexType.isFunctional(IndexType.FUNCTIONAL)).isTrue();
		assertThat(IndexType.isFunctional(IndexType.HASH)).isFalse();
	}

	@Test
	public void testIsHash() {

		assertThat(IndexType.FUNCTIONAL.isHash()).isFalse();
		assertThat(IndexType.HASH.isHash()).isTrue();
		assertThat(IndexType.KEY.isHash()).isFalse();
		assertThat(IndexType.PRIMARY_KEY.isHash()).isFalse();
	}

	@Test
	public void testIsNullSafeHash() {

		assertThat(IndexType.isHash(null)).isFalse();
		assertThat(IndexType.isHash(IndexType.HASH)).isTrue();
		assertThat(IndexType.isHash(IndexType.KEY)).isFalse();
	}

	@Test
	public void testIsKey() {

		assertThat(IndexType.FUNCTIONAL.isKey()).isFalse();
		assertThat(IndexType.HASH.isKey()).isFalse();
		assertThat(IndexType.KEY.isKey()).isTrue();
		assertThat(IndexType.PRIMARY_KEY.isKey()).isTrue();
	}

	@Test
	public void testIsNullSafeKey() {

		assertThat(IndexType.isKey(null)).isFalse();
		assertThat(IndexType.isKey(IndexType.FUNCTIONAL)).isFalse();
		assertThat(IndexType.isKey(IndexType.KEY)).isTrue();
		assertThat(IndexType.isKey(IndexType.PRIMARY_KEY)).isTrue();
	}
}
