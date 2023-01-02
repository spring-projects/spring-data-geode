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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.RegionShortcut;

/**
 * Unit Tests for {@link RegionShortcutWrapper} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.RegionShortcut
 * @see org.springframework.data.gemfire.RegionShortcutWrapper
 * @since 1.4.0
 */
public class RegionShortcutWrapperUnitTests {

	@Test
	public void oneForOneMapping() {

		for (RegionShortcut shortcut : RegionShortcut.values()) {

			RegionShortcutWrapper wrapper = RegionShortcutWrapper.valueOf(shortcut.name());

			assertThat(wrapper).isNotNull();
			assertThat(RegionShortcutWrapper.UNSPECIFIED.equals(wrapper)).isFalse();
		}
	}

	@Test
	public void unspecifiedRegionShortcut() {
		assertThat(RegionShortcutWrapper.valueOf((RegionShortcut) null)).isEqualTo(RegionShortcutWrapper.UNSPECIFIED);
	}

	@Test
	public void isHeapLru() {

		assertThat(RegionShortcutWrapper.LOCAL.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isHeapLru()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isHeapLru()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isHeapLru()).isFalse();
	}

	@Test
	public void isLocal() {

		assertThat(RegionShortcutWrapper.LOCAL.isLocal()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isLocal()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isLocal()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isLocal()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isLocal()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isLocal()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isLocal()).isFalse();
	}

	@Test
	public void isOverflow() {

		assertThat(RegionShortcutWrapper.LOCAL.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isOverflow()).isFalse();
	}

	@Test
	public void isPartition() {

		assertThat(RegionShortcutWrapper.LOCAL.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isPartition()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isPartition()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isPartition()).isFalse();
	}

	@Test
	public void isPersistent() {

		assertThat(RegionShortcutWrapper.LOCAL.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isPersistent()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isPersistent()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isPersistent()).isFalse();
	}

	@Test
	public void isPersistentOverflow() {

		assertThat(RegionShortcutWrapper.LOCAL.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isPersistentOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isPersistentOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isPersistentOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isPersistentOverflow()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isPersistentOverflow()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isPersistentOverflow()).isFalse();
	}
	@Test
	public void isProxy() {

		assertThat(RegionShortcutWrapper.LOCAL.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isProxy()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isProxy()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isProxy()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isProxy()).isTrue();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isProxy()).isFalse();
	}
	@Test
	public void isRedundant() {

		assertThat(RegionShortcutWrapper.LOCAL.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isRedundant()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isRedundant()).isFalse();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isRedundant()).isFalse();
	}

	@Test
	public void isReplicate() {

		assertThat(RegionShortcutWrapper.LOCAL.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_HEAP_LRU.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.LOCAL_PERSISTENT_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_HEAP_LRU.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PERSISTENT_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_PROXY_REDUNDANT.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_HEAP_LRU.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.PARTITION_REDUNDANT_PERSISTENT_OVERFLOW.isReplicate()).isFalse();
		assertThat(RegionShortcutWrapper.REPLICATE.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_HEAP_LRU.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_OVERFLOW.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PERSISTENT_OVERFLOW.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.REPLICATE_PROXY.isReplicate()).isTrue();
		assertThat(RegionShortcutWrapper.UNSPECIFIED.isReplicate()).isFalse();
	}
}
