/*
 * Copyright 2016-2022 the original author or authors.
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
package org.springframework.data.gemfire.expiration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.ExpirationAction;

/**
 * Unit Tests for {@link ExpirationActionType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see ExpirationActionType
 * @see org.apache.geode.cache.ExpirationAction
 * @since 1.6.0
 */
public class ExpirationActionTypeUnitTests {

	@Test
	public void testStaticGetExpirationAction() {

		assertThat(ExpirationActionType.getExpirationAction(ExpirationActionType.DESTROY))
			.isEqualTo(ExpirationAction.DESTROY);

		assertThat(ExpirationActionType.getExpirationAction(
			ExpirationActionType.LOCAL_DESTROY)).isEqualTo(ExpirationAction.LOCAL_DESTROY);
	}

	@Test
	public void testStaticGetExpirationActionWithNull() {
		assertThat(ExpirationActionType.getExpirationAction(null)).isNull();
	}

	@Test
	public void testGetExpirationAction() {

		assertThat(ExpirationActionType.DESTROY.getExpirationAction()).isEqualTo(ExpirationAction.DESTROY);
		assertThat(ExpirationActionType.INVALIDATE.getExpirationAction()).isEqualTo(ExpirationAction.INVALIDATE);
		assertThat(ExpirationActionType.LOCAL_DESTROY.getExpirationAction()).isEqualTo(ExpirationAction.LOCAL_DESTROY);
		assertThat(ExpirationActionType.LOCAL_INVALIDATE.getExpirationAction()).isEqualTo(ExpirationAction.LOCAL_INVALIDATE);
	}

	@Test
	public void testDefault() {
		assertThat(ExpirationActionType.DEFAULT.getExpirationAction()).isEqualTo(ExpirationAction.INVALIDATE);

		assertThat(ExpirationActionType.DEFAULT).isSameAs(ExpirationActionType.INVALIDATE);
	}

	@Test
	public void testValueOf() {

		assertThat(ExpirationActionType.valueOf(ExpirationAction.DESTROY)).isEqualTo(ExpirationActionType.DESTROY);
		assertThat(ExpirationActionType.valueOf(ExpirationAction.INVALIDATE)).isEqualTo(ExpirationActionType.INVALIDATE);
		assertThat(ExpirationActionType.valueOf(ExpirationAction.LOCAL_DESTROY)).isEqualTo(ExpirationActionType.LOCAL_DESTROY);
		assertThat(ExpirationActionType.valueOf(ExpirationAction.LOCAL_INVALIDATE)).isEqualTo(ExpirationActionType.LOCAL_INVALIDATE);
	}

	@Test
	public void testValueOfExpirationActionOrdinalValues() {

		try {
			for (int ordinal = 0; ordinal < Integer.MAX_VALUE; ordinal++) {

				ExpirationAction expirationAction = ExpirationAction.fromOrdinal(ordinal);
				ExpirationActionType expirationActionType = ExpirationActionType.valueOf(expirationAction);

				assertThat(expirationActionType).isNotNull();
				assertThat(expirationActionType.getExpirationAction()).isEqualTo(expirationAction);
			}
		}
		catch (ArrayIndexOutOfBoundsException ignore) {
		}
	}

	@Test
	public void testValueOfWithNull() {
		assertThat(ExpirationActionType.valueOf((ExpirationAction) null)).isNull();
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(ExpirationActionType.valueOfIgnoreCase("destroy")).isEqualTo(ExpirationActionType.DESTROY);
		assertThat(ExpirationActionType.valueOfIgnoreCase("Invalidate")).isEqualTo(ExpirationActionType.INVALIDATE);
		assertThat(ExpirationActionType.valueOfIgnoreCase("LOCAL_DESTROY")).isEqualTo(ExpirationActionType.LOCAL_DESTROY);
		assertThat(ExpirationActionType.valueOfIgnoreCase("LocaL_InValidAte")).isEqualTo(ExpirationActionType.LOCAL_INVALIDATE);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {

		assertThat(ExpirationActionType.valueOfIgnoreCase("Invalid")).isNull();
		assertThat(ExpirationActionType.valueOfIgnoreCase("local destroy")).isNull();
		assertThat(ExpirationActionType.valueOfIgnoreCase("  ")).isNull();
		assertThat(ExpirationActionType.valueOfIgnoreCase("")).isNull();
		assertThat(ExpirationActionType.valueOfIgnoreCase(null)).isNull();
	}
}
