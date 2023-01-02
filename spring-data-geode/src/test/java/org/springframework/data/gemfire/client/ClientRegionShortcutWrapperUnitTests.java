/*
 * Copyright 2016-2023 the original author or authors.
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
 * Unit Tests for {@link ClientRegionShortcutWrapper} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientRegionShortcut
 * @see org.springframework.data.gemfire.client.ClientRegionShortcutWrapper
 * @since 1.4.0
 */
public class ClientRegionShortcutWrapperUnitTests {

	@Test
	public void testOneToOneMapping() {

		for (ClientRegionShortcut shortcut : ClientRegionShortcut.values()) {
			assertThat(ClientRegionShortcutWrapper.valueOf(shortcut.name())).isNotNull();
			assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.equals(ClientRegionShortcutWrapper.valueOf(shortcut))).isFalse();
		}
	}

	@Test
	public void testClientRegionShortcutUnspecified() {

		assertThat(ClientRegionShortcutWrapper.valueOf(
			(ClientRegionShortcut) null)).isEqualTo(ClientRegionShortcutWrapper.UNSPECIFIED);
	}

	@Test
	public void testIsCaching() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isCaching()).isTrue();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isCaching()).isTrue();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isCaching()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.PROXY.isCaching()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isCaching()).isFalse();
	}

	@Test
	public void testIsHeapLru() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.PROXY.isHeapLru()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isHeapLru()).isFalse();
	}

	@Test
	public void testIsLocal() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isLocal()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isLocal()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isLocal()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isLocal()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isLocal()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isLocal()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isLocal()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isLocal()).isTrue();
		assertThat(ClientRegionShortcutWrapper.PROXY.isLocal()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isLocal()).isFalse();
	}

	@Test
	public void testIsOverflow() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isOverflow()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isOverflow()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isOverflow()).isTrue();
		assertThat(ClientRegionShortcutWrapper.PROXY.isOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isOverflow()).isFalse();
	}

	@Test
	public void testIsPersistent() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isPersistent()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isPersistent()).isTrue();
		assertThat(ClientRegionShortcutWrapper.PROXY.isPersistent()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isPersistent()).isFalse();
	}

	@Test
	public void testIsPersistentOverflow() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isPersistentOverflow()).isTrue();
		assertThat(ClientRegionShortcutWrapper.PROXY.isPersistentOverflow()).isFalse();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isPersistentOverflow()).isFalse();
	}

	@Test
	public void testIsProxy() {

		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY.isProxy()).isTrue();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_HEAP_LRU.isProxy()).isTrue();
		assertThat(ClientRegionShortcutWrapper.CACHING_PROXY_OVERFLOW.isProxy()).isTrue();
		assertThat(ClientRegionShortcutWrapper.LOCAL.isProxy()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_HEAP_LRU.isProxy()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_OVERFLOW.isProxy()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT.isProxy()).isFalse();
		assertThat(ClientRegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isProxy()).isFalse();
		assertThat(ClientRegionShortcutWrapper.PROXY.isProxy()).isTrue();
		assertThat(ClientRegionShortcutWrapper.UNSPECIFIED.isProxy()).isFalse();
	}
}
