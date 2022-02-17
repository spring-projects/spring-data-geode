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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.client.ClientRegionShortcut;

/**
 * Unit Tests for {@link ClientRegionShortcutConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientRegionShortcut
 * @see org.springframework.data.gemfire.client.ClientRegionShortcutConverter
 * @since 1.3.4
 */
public class ClientRegionShortcutConverterUnitTests {

	private final ClientRegionShortcutConverter converter = new ClientRegionShortcutConverter();

	@Test
	public void testToUpperCase() {

		assertThat(ClientRegionShortcutConverter.toUpperCase("test")).isEqualTo("TEST");
		assertThat(ClientRegionShortcutConverter.toUpperCase(" Test  ")).isEqualTo("TEST");
		assertThat(ClientRegionShortcutConverter.toUpperCase("")).isEqualTo("");
		assertThat(ClientRegionShortcutConverter.toUpperCase("  ")).isEqualTo("");
		assertThat(ClientRegionShortcutConverter.toUpperCase("null")).isEqualTo("NULL");
		assertThat(ClientRegionShortcutConverter.toUpperCase(null)).isEqualTo("null");
	}

	@Test
	public void testConvert() {

		for (ClientRegionShortcut shortcut : ClientRegionShortcut.values()) {
			assertThat(converter.convert(shortcut.name())).isEqualTo(shortcut);
		}

		assertThat(converter.convert("Proxy")).isEqualTo(ClientRegionShortcut.PROXY);
		assertThat(converter.convert("caching_proxy")).isEqualTo(ClientRegionShortcut.CACHING_PROXY);
		assertThat(converter.convert("local_Heap_LRU")).isEqualTo(ClientRegionShortcut.LOCAL_HEAP_LRU);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertWithIllegalEnumeratedValue() {
		converter.convert("LOCAL Persistent OverFlow");
	}
}
