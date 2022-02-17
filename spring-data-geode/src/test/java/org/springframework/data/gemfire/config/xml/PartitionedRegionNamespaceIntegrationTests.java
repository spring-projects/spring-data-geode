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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.FixedPartitionAttributes;
import org.apache.geode.cache.PartitionAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.partition.PartitionListener;
import org.apache.geode.cache.partition.PartitionListenerAdapter;
import org.apache.geode.compression.Compressor;

import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.SimpleCacheListener;
import org.springframework.data.gemfire.SimplePartitionResolver;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

/**
 * Integration Tests for {@link DataPolicy#PARTITION} {@link Region} SDG XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DataPolicy#PARTITION
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.PartitionedRegionFactoryBean
 * @see org.springframework.data.gemfire.config.xml.PartitionedRegionParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class PartitionedRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void testSimplePartitionRegion() {

		assertThat(requireApplicationContext().containsBean("simple")).isTrue();

		Region<?, ?> simple = requireApplicationContext().getBean("simple", Region.class);

		assertThat(simple).isNotNull();
		assertThat(simple.getName()).isEqualTo("simple");
		assertThat(simple.getFullPath()).isEqualTo(Region.SEPARATOR + "simple");
		assertThat(simple.getAttributes()).isNotNull();
		assertThat(simple.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testOptionsPartitionRegion() throws Exception {

		assertThat(requireApplicationContext().containsBean("options")).isTrue();
		assertThat(requireApplicationContext().containsBean("redundant")).isTrue();

		Region<?, ?> options = requireApplicationContext().getBean("options", Region.class);

		assertThat(options).isNotNull();
		assertThat(options.getName()).isEqualTo("redundant");
		assertThat(options.getAttributes()).isNotNull();
		assertThat(options.getAttributes().getOffHeap()).isTrue();

		PeerRegionFactoryBean optionsRegionFactoryBean =
			requireApplicationContext().getBean("&options", PeerRegionFactoryBean.class);

		assertThat(optionsRegionFactoryBean).isInstanceOf(PartitionedRegionFactoryBean.class);
		assertThat(TestUtils.<Object>readField("scope", optionsRegionFactoryBean)).isNull();
		assertThat(TestUtils.<Object>readField("name", optionsRegionFactoryBean)).isEqualTo("redundant");
		assertThat(TestUtils.<Object>readField("scope", optionsRegionFactoryBean)).isNull();

		RegionAttributes optionsRegionAttributes = optionsRegionFactoryBean.getAttributes();

		assertThat(optionsRegionAttributes).isNotNull();
		assertThat(optionsRegionAttributes.getOffHeap()).isTrue();
		assertThat(optionsRegionAttributes.getStatisticsEnabled()).isTrue();

		PartitionAttributes optionsRegionPartitionAttributes = optionsRegionAttributes.getPartitionAttributes();

		assertThat(optionsRegionPartitionAttributes).isNotNull();
		assertThat(optionsRegionPartitionAttributes.getRedundantCopies()).isEqualTo(1);
		assertThat(optionsRegionPartitionAttributes.getTotalNumBuckets()).isEqualTo(4);
		assertThat(optionsRegionPartitionAttributes.getPartitionResolver() instanceof SimplePartitionResolver).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testComplexPartitionRegion() throws Exception {

		assertThat(requireApplicationContext().containsBean("complex")).isTrue();

		PeerRegionFactoryBean complexRegionFactoryBean =
			requireApplicationContext().getBean("&complex", PeerRegionFactoryBean.class);

		CacheListener[] cacheListeners = TestUtils.readField("cacheListeners", complexRegionFactoryBean);

		assertThat(ObjectUtils.isEmpty(cacheListeners)).isFalse();
		assertThat(cacheListeners.length).isEqualTo(2);
		assertThat(requireApplicationContext().getBean("c-listener")).isSameAs(cacheListeners[0]);
		assertThat(cacheListeners[1]).isInstanceOf(SimpleCacheListener.class);

		assertThat(TestUtils.<Object>readField("cacheLoader", complexRegionFactoryBean))
			.isSameAs(requireApplicationContext().getBean("c-loader"));
		assertThat(TestUtils.<Object>readField("cacheWriter", complexRegionFactoryBean))
			.isSameAs(requireApplicationContext().getBean("c-writer"));

		RegionAttributes complexRegionAttributes = TestUtils.readField("attributes", complexRegionFactoryBean);

		assertThat(complexRegionAttributes).isNotNull();

		PartitionAttributes complexRegionPartitionAttributes = complexRegionAttributes.getPartitionAttributes();

		assertThat(complexRegionPartitionAttributes).isNotNull();
		assertThat(complexRegionPartitionAttributes.getLocalMaxMemory()).isEqualTo(20);
		assertThat(complexRegionPartitionAttributes.getPartitionListeners()).isNotNull();
		assertThat(complexRegionPartitionAttributes.getPartitionListeners().length).isEqualTo(1);
		assertThat(complexRegionPartitionAttributes.getPartitionListeners()[0] instanceof TestPartitionListener)
			.isTrue();
	}

	@Test
	public void testCompressedPartitionRegion() {

		assertThat(requireApplicationContext().containsBean("compressed")).isTrue();

		Region<?, ?> compressed = requireApplicationContext().getBean("compressed", Region.class);

		assertThat(compressed)
			.describedAs("The 'compressed' PARTITION Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(compressed.getName()).isEqualTo("compressed");
		assertThat(compressed.getFullPath()).isEqualTo(Region.SEPARATOR + "compressed");
		assertThat(compressed.getAttributes()).isNotNull();
		assertThat(compressed.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(compressed.getAttributes().getCompressor()).isInstanceOf(TestCompressor.class);
		assertThat(compressed.getAttributes().getCompressor().toString()).isEqualTo("testCompressor");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testFixedPartitionRegion() throws Exception {

		PeerRegionFactoryBean fixedRegionFactoryBean =
			requireApplicationContext().getBean("&fixed", PeerRegionFactoryBean.class);

		assertThat(fixedRegionFactoryBean).isNotNull();

		RegionAttributes fixedRegionAttributes = TestUtils.readField("attributes", fixedRegionFactoryBean);

		assertThat(fixedRegionAttributes).isNotNull();

		PartitionAttributes fixedRegionPartitionAttributes = fixedRegionAttributes.getPartitionAttributes();

		assertThat(fixedRegionPartitionAttributes).isNotNull();

		assertThat(fixedRegionPartitionAttributes.getFixedPartitionAttributes()).isNotNull();
		assertThat(fixedRegionPartitionAttributes.getFixedPartitionAttributes().size()).isEqualTo(3);

		FixedPartitionAttributes fixedPartitionAttributes =
			(FixedPartitionAttributes) fixedRegionPartitionAttributes.getFixedPartitionAttributes().get(0);

		assertThat(fixedPartitionAttributes.getNumBuckets()).isEqualTo(3);
		assertThat(fixedPartitionAttributes.isPrimary()).isTrue();
	}

	@Test
	public void testMultiplePartitionListeners() {

		assertThat(requireApplicationContext().containsBean("listeners")).isTrue();

		Region<?, ?> listeners = requireApplicationContext().getBean("listeners", Region.class);

		assertThat(listeners)
			.describedAs("The 'listeners' PARTITION Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(listeners.getName()).isEqualTo("listeners");
		assertThat(listeners.getFullPath()).isEqualTo(Region.SEPARATOR + "listeners");
		assertThat(listeners.getAttributes()).isNotNull();
		assertThat(listeners.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);

		PartitionAttributes<?, ?> listenersPartitionAttributes = listeners.getAttributes().getPartitionAttributes();

		assertThat(listenersPartitionAttributes).isNotNull();
		assertThat(listenersPartitionAttributes.getPartitionListeners()).isNotNull();
		assertThat(listenersPartitionAttributes.getPartitionListeners().length).isEqualTo(4);

		List<String> expectedNames = Arrays.asList("X", "Y", "Z", "ABC");

		for (PartitionListener listener : listenersPartitionAttributes.getPartitionListeners()) {
			assertThat(listener).isInstanceOf(TestPartitionListener.class);
			assertThat(expectedNames.contains(listener.toString())).isTrue();
		}
	}

	@Test
	public void testSinglePartitionListeners() {

		assertThat(requireApplicationContext().containsBean("listenerRef")).isTrue();

		Region<?, ?> listeners = requireApplicationContext().getBean("listenerRef", Region.class);

		assertThat(listeners)
			.describedAs("The 'listenerRef' PARTITION Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(listeners.getName()).isEqualTo("listenerRef");
		assertThat(listeners.getFullPath()).isEqualTo(Region.SEPARATOR + "listenerRef");
		assertThat(listeners.getAttributes()).isNotNull();
		assertThat(listeners.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);

		PartitionAttributes<?, ?> listenersPartitionAttributes = listeners.getAttributes().getPartitionAttributes();

		assertThat(listenersPartitionAttributes).isNotNull();
		assertThat(listenersPartitionAttributes.getPartitionListeners()).isNotNull();
		assertThat(listenersPartitionAttributes.getPartitionListeners().length).isEqualTo(1);
		assertThat(listenersPartitionAttributes.getPartitionListeners()[0]).isInstanceOf(TestPartitionListener.class);
		assertThat(listenersPartitionAttributes.getPartitionListeners()[0].toString()).isEqualTo("ABC");
	}

	public static class TestCompressor implements Compressor {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public byte[] compress(final byte[] input) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public byte[] decompress(final byte[] input) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	public static class TestPartitionListener extends PartitionListenerAdapter {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
