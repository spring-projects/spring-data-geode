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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link Region} Eviction configuration settings ({@link EvictionAttributes})
 * using SDG XML namespace configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.EvictionAttributes
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.3.4
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class RegionEvictionAttributesNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "One")
	private Region<?, ?> one;

	@Resource(name = "Two")
	private Region<?, ?> two;

	@Resource(name = "Three")
	private Region<?, ?> three;

	@Resource(name = "Four")
	private Region<?, ?> four;

	@Resource(name = "Five")
	private Region<?, ?> five;

	@Resource(name = "Six")
	private Region<?, ?> six;

	@Test
	public void testEntryCountRegionEvictionAttributes() {

		assertThat(one).isNotNull();
		assertThat(one.getAttributes()).isNotNull();
		assertThat(one.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(one.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(one.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(one.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
		assertThat(one.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(4096);

		assertThat(two).isNotNull();
		assertThat(two.getAttributes()).isNotNull();
		assertThat(two.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(two.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(two.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(two.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
		assertThat(two.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(EvictionAttributes.DEFAULT_ENTRIES_MAXIMUM);
	}

	@Test
	public void testHeapPercentageRegionEvictionAttributes() {

		assertThat(three).isNotNull();
		assertThat(three.getAttributes()).isNotNull();
		assertThat(three.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(three.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(three.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(three.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);

		assertThat(four).isNotNull();
		assertThat(four.getAttributes()).isNotNull();
		assertThat(four.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(four.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(four.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(three.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_HEAP);
		assertThat(four.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(0);
	}

	@Test
	public void testMemorySizeRegionEvictionAttributes() {

		assertThat(five).isNotNull();
		assertThat(five.getAttributes()).isNotNull();
		assertThat(five.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(five.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(five.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(five.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
		assertThat(five.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(128);

		assertThat(six).isNotNull();
		assertThat(six.getAttributes()).isNotNull();
		assertThat(six.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(six.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(six.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(six.getAttributes().getEvictionAttributes().getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);

		int expectedMaximum = Boolean.getBoolean("org.springframework.data.gemfire.test.GemfireTestRunner.nomock")
			? 512 : 256;

		assertThat(six.getAttributes().getEvictionAttributes().getMaximum()).isEqualTo(expectedMaximum);
	}
}
