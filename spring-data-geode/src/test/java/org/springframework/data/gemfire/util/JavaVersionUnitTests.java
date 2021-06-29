/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.gemfire.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit Tests for {@link JavaVersion}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.util.JavaVersion
 * @since 2.6.0
 */
public class JavaVersionUnitTests {

	@Test
	public void majorMinorPatchVersionIsCorrect() {

		assertThat(JavaVersion.EIGHT.getMajor()).isEqualTo(1);
		assertThat(JavaVersion.EIGHT.getMinor()).isEqualTo(8);
		assertThat(JavaVersion.EIGHT.getPatch()).isEqualTo(0);
		assertThat(JavaVersion.ELEVEN.getMajor()).isEqualTo(11);
		assertThat(JavaVersion.ELEVEN.getMinor()).isEqualTo(0);
		assertThat(JavaVersion.ELEVEN.getPatch()).isEqualTo(0);
		assertThat(JavaVersion.SIXTEEN.getMajor()).isEqualTo(16);
		assertThat(JavaVersion.SIXTEEN.getMinor()).isEqualTo(0);
		assertThat(JavaVersion.SIXTEEN.getPatch()).isEqualTo(0);
	}

	@Test
	public void iOlderThanIsTrue() {

		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.NINE)).isTrue();
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.ELEVEN)).isTrue();
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.SEVENTEEN)).isTrue();
	}

	@Test
	public void isOlderThanIsFalse() {

		assertThat(JavaVersion.SEVENTEEN.isOlderThan(JavaVersion.ELEVEN)).isFalse();
		assertThat(JavaVersion.ELEVEN.isOlderThan(JavaVersion.EIGHT)).isFalse();
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.EIGHT)).isFalse();
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.SEVEN)).isFalse();
	}

	@Test
	public void isNewerThanOrEqualToIsTrue() {

		assertThat(JavaVersion.SIXTEEN.isNewerThanOrEqualTo(JavaVersion.ELEVEN)).isTrue();
		assertThat(JavaVersion.ELEVEN.isNewerThanOrEqualTo(JavaVersion.EIGHT)).isTrue();
		assertThat(JavaVersion.EIGHT.isNewerThanOrEqualTo(JavaVersion.EIGHT)).isTrue();
		assertThat(JavaVersion.EIGHT.isNewerThanOrEqualTo(JavaVersion.SEVEN)).isTrue();
	}

	@Test
	public void isNewerThanOrEqualToIsFalse() {

		assertThat(JavaVersion.EIGHT.isNewerThanOrEqualTo(JavaVersion.ELEVEN)).isFalse();
		assertThat(JavaVersion.EIGHT.isNewerThanOrEqualTo(JavaVersion.SEVENTEEN)).isFalse();
		assertThat(JavaVersion.ELEVEN.isNewerThanOrEqualTo(JavaVersion.SEVENTEEN)).isFalse();
	}
}
