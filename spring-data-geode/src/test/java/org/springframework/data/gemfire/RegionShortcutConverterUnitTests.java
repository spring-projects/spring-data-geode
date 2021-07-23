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

import org.apache.geode.cache.RegionShortcut;

/**
 * Unit Tests for {@link RegionShortcutConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.RegionShortcut
 * @see org.springframework.data.gemfire.RegionShortcutConverter
 * @since 1.3.4
 */
public class RegionShortcutConverterUnitTests {

	private final RegionShortcutConverter converter = new RegionShortcutConverter();

	@Test
	public void testToUpperCase() {

		assertThat(RegionShortcutConverter.toUpperCase("test")).isEqualTo("TEST");
		assertThat(RegionShortcutConverter.toUpperCase(" Test  ")).isEqualTo("TEST");
		assertThat(RegionShortcutConverter.toUpperCase("")).isEqualTo("");
		assertThat(RegionShortcutConverter.toUpperCase("  ")).isEqualTo("");
		assertThat(RegionShortcutConverter.toUpperCase("null")).isEqualTo("NULL");
		assertThat(RegionShortcutConverter.toUpperCase(null)).isEqualTo("null");
	}

	@Test
	public void testConvert() {

		for (RegionShortcut shortcut : RegionShortcut.values()) {
			assertThat(converter.convert(shortcut.name())).isEqualTo(shortcut);
		}

		assertThat(converter.convert("Partition_Proxy")).isEqualTo(RegionShortcut.PARTITION_PROXY);
		assertThat(converter.convert("replicate_overflow")).isEqualTo(RegionShortcut.REPLICATE_OVERFLOW);
		assertThat(converter.convert("local_Heap_LRU")).isEqualTo(RegionShortcut.LOCAL_HEAP_LRU);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertWithIllegalEnumeratedValue() {
		converter.convert("localPersistentOverflow");
	}
}
