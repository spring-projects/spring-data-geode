/*
 * Copyright 2010-2021 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.Scope;
import org.apache.geode.compression.Compressor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.ResolvableRegionFactoryBean;
import org.springframework.data.gemfire.SimpleCacheListener;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

/**
 * Integration Tests for the Local Region XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.LocalRegionFactoryBean
 * @see org.springframework.data.gemfire.config.xml.LocalRegionParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations="local-ns.xml", initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class LocalRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testSimpleLocalRegion() {

		assertThat(applicationContext.containsBean("simple")).isTrue();

		Region<?, ?> simple = applicationContext.getBean("simple", Region.class);

		assertThat(simple)
			.describedAs("The 'simple' Region was not properly configured or initialized!")
			.isNotNull();

		assertThat(simple.getName()).isEqualTo("simple");
		assertThat(simple.getFullPath()).isEqualTo(Region.SEPARATOR + "simple");
		assertThat(simple.getAttributes()).isNotNull();
		assertThat(simple.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
	}

	@Test
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public void testPublisherLocalRegion() throws Exception {

		assertThat(applicationContext.containsBean("pub")).isTrue();

		PeerRegionFactoryBean publisherRegionFactoryBean = applicationContext.getBean("&pub", PeerRegionFactoryBean.class);

		assertThat(publisherRegionFactoryBean).isNotNull();
		assertThat(TestUtils.<DataPolicy>readField("dataPolicy", publisherRegionFactoryBean)).isEqualTo(DataPolicy.NORMAL);
		assertThat(TestUtils.<String>readField("name", publisherRegionFactoryBean)).isEqualTo("publisher");
		assertThat(TestUtils.<Scope>readField("scope", publisherRegionFactoryBean)).isEqualTo(Scope.LOCAL);

		RegionAttributes publisherRegionAttributes = TestUtils.readField("attributes", publisherRegionFactoryBean);

		assertThat(publisherRegionAttributes).isNotNull();
		assertThat(publisherRegionAttributes.getPublisher()).isFalse();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testComplexLocal() throws Exception {

		assertThat(applicationContext.containsBean("complex")).isTrue();

		PeerRegionFactoryBean complexRegionFactoryBean = applicationContext.getBean("&complex", PeerRegionFactoryBean.class);

		assertThat(complexRegionFactoryBean).isNotNull();

		CacheListener[] cacheListeners = TestUtils.readField("cacheListeners", complexRegionFactoryBean);

		assertThat(ObjectUtils.isEmpty(cacheListeners)).isFalse();
		assertThat(cacheListeners.length).isEqualTo(2);
		assertThat(cacheListeners[0]).isSameAs(applicationContext.getBean("c-listener"));
		assertThat(cacheListeners[1] instanceof SimpleCacheListener).isTrue();
		assertThat(cacheListeners[1]).isNotSameAs(cacheListeners[0]);
		assertThat(TestUtils.<String>readField("cacheLoader", complexRegionFactoryBean))
			.isSameAs(applicationContext.getBean("c-loader"));
		assertThat(TestUtils.<String>readField("cacheWriter", complexRegionFactoryBean))
			.isSameAs(applicationContext.getBean("c-writer"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testLocalWithAttributes() {

		assertThat(applicationContext.containsBean("local-with-attributes")).isTrue();

		Region region = applicationContext.getBean("local-with-attributes", Region.class);

		assertThat(region)
			.describedAs("The 'local-with-attributes' Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(region.getName()).isEqualTo("local-with-attributes");
		assertThat(region.getFullPath()).isEqualTo(Region.SEPARATOR + "local-with-attributes");

		RegionAttributes localRegionAttributes = region.getAttributes();

		assertThat(localRegionAttributes.getDataPolicy()).isEqualTo(DataPolicy.PRELOADED);
		assertThat(localRegionAttributes.isDiskSynchronous()).isTrue();
		assertThat(localRegionAttributes.getIgnoreJTA()).isTrue();
		assertThat(localRegionAttributes.getIndexMaintenanceSynchronous()).isFalse();
		assertThat(localRegionAttributes.getInitialCapacity()).isEqualTo(10);
		assertThat(localRegionAttributes.getKeyConstraint()).isEqualTo(String.class);
		assertThat(String.valueOf(localRegionAttributes.getLoadFactor())).isEqualTo("0.9");
		assertThat(localRegionAttributes.getOffHeap()).isTrue();
		assertThat(localRegionAttributes.getValueConstraint()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testRegionLookup() throws Exception {

		Cache cache = applicationContext.getBean(Cache.class);

		Region existing = cache.createRegionFactory().create("existing");

		assertThat(applicationContext.containsBean("lookup")).isTrue();

		ResolvableRegionFactoryBean localRegionFactoryBean = applicationContext.getBean("&lookup", ResolvableRegionFactoryBean.class);

		assertThat(TestUtils.<String>readField("name", localRegionFactoryBean)).isEqualTo("existing");
		assertThat(applicationContext.getBean("lookup")).isSameAs(existing);
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testLocalPersistent() {

		Region persistentLocalRegion = applicationContext.getBean("persistent", Region.class);

		assertThat(persistentLocalRegion)
			.describedAs("The 'persistent' Local Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(persistentLocalRegion.getName()).isEqualTo("persistent");
		assertThat(persistentLocalRegion.getFullPath()).isEqualTo(Region.SEPARATOR + "persistent");

		RegionAttributes persistentRegionAttributes = persistentLocalRegion.getAttributes();

		assertThat(persistentRegionAttributes).isNotNull();
		assertThat(persistentRegionAttributes.getDataPolicy().withPersistence()).isTrue();
	}

	@Test
	public void testCompressedLocalRegion() {

		assertThat(applicationContext.containsBean("Compressed")).isTrue();

		Region<?, ?> compressed = applicationContext.getBean("Compressed", Region.class);

		assertThat(compressed).as("The 'Compressed' Local Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(compressed.getName()).isEqualTo("Compressed");
		assertThat(compressed.getFullPath()).isEqualTo(Region.SEPARATOR + "Compressed");
		assertThat(compressed.getAttributes()).isNotNull();
		assertThat(compressed.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(compressed.getAttributes().getScope()).isEqualTo(Scope.LOCAL);
		assertThat(compressed.getAttributes().getCompressor() instanceof TestCompressor).isTrue();
		assertThat(compressed.getAttributes().getCompressor().toString()).isEqualTo("ABC");
	}

	public static class TestCompressor implements Compressor {

		private String name;

		public void setName(final String name) {
			this.name = name;
		}

		@Override
		public byte[] compress(byte[] input) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public byte[] decompress(byte[] input) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
