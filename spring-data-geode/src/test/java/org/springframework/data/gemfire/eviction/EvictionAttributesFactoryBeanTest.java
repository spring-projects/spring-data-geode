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
package org.springframework.data.gemfire.eviction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.util.ObjectSizer;

/**
 * Unit Tests for {@link EvictionAttributesFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.EvictionAttributes
 * @see org.springframework.data.gemfire.eviction.EvictionAttributesFactoryBean
 * @since 1.3.4
 */
public class EvictionAttributesFactoryBeanTest {

	private EvictionAttributesFactoryBean factoryBean;

	private ObjectSizer mockObjectSizer;

	@Before
	public void setup() {
		factoryBean = new EvictionAttributesFactoryBean();
		mockObjectSizer = mock(ObjectSizer.class, "MockObjectSizer");
	}

	@After
	public void tearDown() {
		factoryBean = null;
		mockObjectSizer = null;
	}

	@Test
	public void testIsSingleton() {
		assertThat(new EvictionAttributesFactoryBean().isSingleton()).isTrue();
	}

	@Test
	public void testCreateEntryCountEvictionAttributesWithNullAction() {

		factoryBean.setAction(null);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(1024);
		factoryBean.setType(EvictionPolicyType.ENTRY_COUNT);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.DEFAULT_EVICTION_ACTION);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum()).isEqualTo(1024);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
	}

	@Test
	public void testCreateEntryCountEvictionAttributesWithLocalDestroy() {

		factoryBean.setAction(EvictionAction.LOCAL_DESTROY);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(128);
		factoryBean.setType(EvictionPolicyType.ENTRY_COUNT);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum()).isEqualTo(128);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
	}

	@Test
	public void testCreateEntryCountEvictionAttributesWithNone() {

		factoryBean.setAction(EvictionAction.NONE);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.ENTRY_COUNT);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.NONE);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum())
			.isEqualTo(EvictionAttributesFactoryBean.DEFAULT_LRU_MAXIMUM_ENTRIES);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
	}

	@Test
	public void testCreateEntryCountEvictionAttributesWithOverflowToDisk() {

		factoryBean.setAction(EvictionAction.OVERFLOW_TO_DISK);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.ENTRY_COUNT);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum())
			.isEqualTo(EvictionAttributesFactoryBean.DEFAULT_LRU_MAXIMUM_ENTRIES);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
	}

	@Test
	public void testCreateHeapPercentageEvictionAttributesWithNullAction() {

		factoryBean.setAction(null);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setType(EvictionPolicyType.HEAP_PERCENTAGE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.DEFAULT_EVICTION_ACTION);
		assertThat(evictionAttributes.getObjectSizer()).isSameAs(mockObjectSizer);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);
	}

	@Test
	public void testCreateHeapPercentageEvictionAttributesWithLocalDestroy() {

		factoryBean.setAction(EvictionAction.LOCAL_DESTROY);
		factoryBean.setObjectSizer(null);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.HEAP_PERCENTAGE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);
	}

	@Test
	public void testCreateHeapPercentageEvictionAttributesWithNone() {

		factoryBean.setAction(EvictionAction.NONE);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.HEAP_PERCENTAGE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.NONE);
		assertThat(evictionAttributes.getObjectSizer()).isSameAs(mockObjectSizer);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);
	}

	@Test
	public void testCreateHeapPercentageEvictionAttributesWithOverflowToDisk() {

		factoryBean.setAction(EvictionAction.OVERFLOW_TO_DISK);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.HEAP_PERCENTAGE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(evictionAttributes.getObjectSizer()).isSameAs(mockObjectSizer);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateHeapPercentageEvictionAttributesSettingThreshold() {

		EvictionAttributesFactoryBean factoryBean = new EvictionAttributesFactoryBean();

		try {
			factoryBean.setType(EvictionPolicyType.HEAP_PERCENTAGE);
			factoryBean.setThreshold(85);
			factoryBean.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
			assertThat(expected.getMessage())
				.isEqualTo("HEAP_PERCENTAGE (LRU_HEAP algorithm) does not support threshold (a.k.a. maximum)");
			assertThat(factoryBean.getThreshold().intValue()).isEqualTo(85);
			assertThat(factoryBean.getType()).isEqualTo(EvictionPolicyType.HEAP_PERCENTAGE);
			throw expected;
		}
	}

	@Test
	public void testCreateMemorySizeEvictionAttributesWithNullAction() {

		factoryBean.setAction(null);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.MEMORY_SIZE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.DEFAULT_EVICTION_ACTION);
		assertThat(evictionAttributes.getObjectSizer()).isSameAs(mockObjectSizer);
		assertThat(evictionAttributes.getMaximum())
			.isEqualTo(EvictionAttributesFactoryBean.DEFAULT_MEMORY_MAXIMUM_SIZE);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
	}

	@Test
	public void testCreateMemorySizeEvictionAttributesWithLocalDestroy() {

		factoryBean.setAction(EvictionAction.LOCAL_DESTROY);
		factoryBean.setObjectSizer(mockObjectSizer);
		factoryBean.setThreshold(1024);
		factoryBean.setType(EvictionPolicyType.MEMORY_SIZE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(evictionAttributes.getObjectSizer()).isSameAs(mockObjectSizer);
		assertThat(evictionAttributes.getMaximum()).isEqualTo(1024);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
	}

	@Test
	public void testCreateMemorySizeEvictionAttributesWithNone() {

		factoryBean.setAction(EvictionAction.NONE);
		factoryBean.setObjectSizer(null);
		factoryBean.setThreshold(256);
		factoryBean.setType(EvictionPolicyType.MEMORY_SIZE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.NONE);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum()).isEqualTo(256);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
	}

	@Test
	public void testCreateMemorySizeEvictionAttributesWithOverflowToDisk() {

		factoryBean.setAction(EvictionAction.OVERFLOW_TO_DISK);
		factoryBean.setObjectSizer(null);
		factoryBean.setThreshold(null);
		factoryBean.setType(EvictionPolicyType.MEMORY_SIZE);
		factoryBean.afterPropertiesSet();

		EvictionAttributes evictionAttributes = factoryBean.getObject();

		assertThat(evictionAttributes).isNotNull();
		assertThat(evictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(evictionAttributes.getObjectSizer()).isNull();
		assertThat(evictionAttributes.getMaximum())
			.isEqualTo(EvictionAttributesFactoryBean.DEFAULT_MEMORY_MAXIMUM_SIZE);
		assertThat(evictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
	}
}
