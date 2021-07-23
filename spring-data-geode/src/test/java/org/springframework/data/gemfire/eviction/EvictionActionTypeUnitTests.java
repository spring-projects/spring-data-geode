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

import org.junit.Test;

import org.apache.geode.cache.EvictionAction;

/**
 * Unit Tests for {@link EvictionActionType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.EvictionAction
 * @see org.springframework.data.gemfire.eviction.EvictionActionType
 * @since 1.6.0
 */
public class EvictionActionTypeUnitTests {

	@Test
	public void testStaticGetEvictionAction() {

		assertThat(EvictionActionType.getEvictionAction(
			EvictionActionType.LOCAL_DESTROY)).isEqualTo(EvictionAction.LOCAL_DESTROY);

		assertThat(EvictionActionType.getEvictionAction(
			EvictionActionType.OVERFLOW_TO_DISK)).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
	}

	@Test
	public void testStaticGetEvictionActionWithNull() {
		assertThat(EvictionActionType.getEvictionAction(null)).isNull();
	}

	@Test
	public void testGetEvictionAction() {

		assertThat(EvictionActionType.LOCAL_DESTROY.getEvictionAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(EvictionActionType.NONE.getEvictionAction()).isEqualTo(EvictionAction.NONE);
		assertThat(EvictionActionType.OVERFLOW_TO_DISK.getEvictionAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(EvictionActionType.DEFAULT.getEvictionAction()).isEqualTo(EvictionAction.DEFAULT_EVICTION_ACTION);
	}

	@Test
	public void testDefault() {

		assertThat(EvictionActionType.DEFAULT.getEvictionAction()).isEqualTo(EvictionAction.DEFAULT_EVICTION_ACTION);
		assertThat(EvictionActionType.DEFAULT).isSameAs(EvictionActionType.LOCAL_DESTROY);
	}

	@Test
	public void testValueOf() {

		assertThat(EvictionActionType.valueOf(EvictionAction.LOCAL_DESTROY)).isEqualTo(EvictionActionType.LOCAL_DESTROY);
		assertThat(EvictionActionType.valueOf(EvictionAction.NONE)).isEqualTo(EvictionActionType.NONE);
		assertThat(EvictionActionType.valueOf(EvictionAction.OVERFLOW_TO_DISK)).isEqualTo(EvictionActionType.OVERFLOW_TO_DISK);
	}

	@Test
	public void testValueOfWithNull() {
		assertThat(EvictionActionType.valueOf((EvictionAction) null)).isNull();
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(EvictionActionType.valueOfIgnoreCase("Local_Destroy")).isEqualTo(EvictionActionType.LOCAL_DESTROY);
		assertThat(EvictionActionType.valueOfIgnoreCase("none")).isEqualTo(EvictionActionType.NONE);
		assertThat(EvictionActionType.valueOfIgnoreCase("NONE")).isEqualTo(EvictionActionType.NONE);
		assertThat(EvictionActionType.valueOfIgnoreCase("OverFlow_TO_DiSk")).isEqualTo(EvictionActionType.OVERFLOW_TO_DISK);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {

		assertThat(EvictionActionType.valueOfIgnoreCase("REMOTE_DESTROY")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase("All")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase(" none  ")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase("underflow_from_disk")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase("  ")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase("")).isNull();
		assertThat(EvictionActionType.valueOfIgnoreCase(null)).isNull();
	}
}
