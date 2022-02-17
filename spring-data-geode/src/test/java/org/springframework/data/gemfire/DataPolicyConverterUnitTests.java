/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.DataPolicy;

/**
 * Unit Tests for {@link DataPolicyConverter}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.DataPolicyConverter
 */
public class DataPolicyConverterUnitTests {

	private final DataPolicyConverter converter = new DataPolicyConverter();

	private int getDataPolicyEnumerationSize() {

		int count = 0;

		for (byte ordinal = 0; ordinal < Byte.MAX_VALUE; ordinal++) {
			try {
				if (DataPolicy.fromOrdinal(ordinal) != null) {
					count++;
				}
			}
			catch (ArrayIndexOutOfBoundsException ignore) {
				break;
			}
			catch (Throwable ignore) {
			}
		}

		return count;
	}

	@Test
	public void policyToDataPolicyConversion() {

		assertThat(DataPolicyConverter.Policy.values().length - 1).isEqualTo(getDataPolicyEnumerationSize());
		assertThat(DataPolicyConverter.Policy.EMPTY.toDataPolicy()).isEqualTo(DataPolicy.EMPTY);
		assertThat(DataPolicyConverter.Policy.NORMAL.toDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(DataPolicyConverter.Policy.PRELOADED.toDataPolicy()).isEqualTo(DataPolicy.PRELOADED);
		assertThat(DataPolicyConverter.Policy.PARTITION.toDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(DataPolicyConverter.Policy.PERSISTENT_PARTITION.toDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(DataPolicyConverter.Policy.REPLICATE.toDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(DataPolicyConverter.Policy.PERSISTENT_REPLICATE.toDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
		assertThat(DataPolicyConverter.Policy.DEFAULT.toDataPolicy()).isEqualTo(DataPolicy.DEFAULT);
	}

	@Test
	public void convertDataPolicyStrings() {

		assertThat(converter.convert("empty")).isEqualTo(DataPolicy.EMPTY);
		assertThat(converter.convert("Partition")).isEqualTo(DataPolicy.PARTITION);
		assertThat(converter.convert("PERSISTENT_REPLICATE")).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
		assertThat(converter.convert("invalid")).isNull();
		assertThat(converter.convert(null)).isNull();
	}
}
