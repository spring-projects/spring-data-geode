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
import static org.assertj.core.data.Offset.offset;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.Scope;
import org.apache.geode.compression.Compressor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.ResolvableRegionFactoryBean;
import org.springframework.data.gemfire.SimpleCacheListener;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

/**
 * Integration Tests for {@link DataPolicy#REPLICATE} {@link Region} XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.ReplicatedRegionFactoryBean
 * @see org.springframework.data.gemfire.config.xml.ReplicatedRegionParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings({ "rawtypes", "unused" })
public class ReplicatedRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void simpleReplicateRegionConfigurationIsCorrect() throws Exception {

		assertThat(applicationContext.containsBean("simple")).isTrue();

		PeerRegionFactoryBean<?, ?> simpleRegionFactoryBean =
			applicationContext.getBean("&simple", PeerRegionFactoryBean.class);

		assertThat(TestUtils.<String>readField("beanName", simpleRegionFactoryBean)).isEqualTo("simple");
		assertThat(TestUtils.<Boolean>readField("close", simpleRegionFactoryBean)).isEqualTo(false);
		assertThat(TestUtils.<Scope>readField("scope", simpleRegionFactoryBean)).isNull();

		RegionAttributes<?, ?> simpleRegionAttributes = simpleRegionFactoryBean.getAttributes();

		assertThat(simpleRegionAttributes).isNotNull();
		assertThat(simpleRegionAttributes.getConcurrencyChecksEnabled()).isFalse();
		assertThat(simpleRegionAttributes.getConcurrencyLevel()).isEqualTo(13);
		assertThat(simpleRegionAttributes.getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void publisherReplicateRegionConfigurationIsCorrect() throws Exception {

		assertThat(applicationContext.containsBean("pub")).isTrue();

		PeerRegionFactoryBean publisherRegionFactoryBean =
			applicationContext.getBean("&pub", PeerRegionFactoryBean.class);

		assertThat(publisherRegionFactoryBean instanceof ReplicatedRegionFactoryBean).isTrue();
		assertThat(TestUtils.<String>readField("name", publisherRegionFactoryBean)).isEqualTo("publisher");
		assertThat(TestUtils.<Scope>readField("scope", publisherRegionFactoryBean)).isEqualTo(Scope.DISTRIBUTED_ACK);

		RegionAttributes publisherRegionAttributes = publisherRegionFactoryBean.getAttributes();

		assertThat(publisherRegionAttributes.getConcurrencyChecksEnabled()).isTrue();
		assertThat(publisherRegionAttributes.getPublisher()).isFalse();
	}

	@Test
	public void complexReplicateRegionConfigurationIsCorrect() throws Exception {

		assertThat(applicationContext.containsBean("complex")).isTrue();

		PeerRegionFactoryBean complexRegionFactoryBean =
			applicationContext.getBean("&complex", PeerRegionFactoryBean.class);

		assertThat(complexRegionFactoryBean).isNotNull();
		assertThat(TestUtils.<String>readField("beanName", complexRegionFactoryBean)).isEqualTo("complex");

		CacheListener[] cacheListeners = TestUtils.readField("cacheListeners", complexRegionFactoryBean);

		assertThat(ObjectUtils.isEmpty(cacheListeners)).isFalse();
		assertThat(cacheListeners.length).isEqualTo(2);
		assertThat(cacheListeners[0]).isSameAs(applicationContext.getBean("c-listener"));
		assertThat(cacheListeners[1] instanceof SimpleCacheListener).isTrue();
		assertThat(cacheListeners[1]).isNotSameAs(cacheListeners[0]);
		assertThat(TestUtils.<CacheLoader>readField("cacheLoader", complexRegionFactoryBean))
			.isSameAs(applicationContext.getBean("c-loader"));
		assertThat(TestUtils.<CacheWriter>readField("cacheWriter", complexRegionFactoryBean))
			.isSameAs(applicationContext.getBean("c-writer"));
	}

	@Test
	public void replicatedRegionWithAttributesConfigurationIsCorrect() {

		assertThat(applicationContext.containsBean("replicated-with-attributes")).isTrue();

		Region<?, ?> region = applicationContext.getBean("replicated-with-attributes", Region.class);

		assertThat(region)
			.describedAs("The 'replicated-with-attributes' Region was not properly configured and initialized!")
			.isNotNull();

		RegionAttributes regionAttributes = region.getAttributes();

		assertThat(regionAttributes).isNotNull();
		assertThat(regionAttributes.getCloningEnabled()).isFalse();
		assertThat(regionAttributes.getConcurrencyLevel()).isEqualTo(10);
		assertThat(regionAttributes.isDiskSynchronous()).isTrue();
		assertThat(regionAttributes.getEnableAsyncConflation()).isTrue();
		assertThat(regionAttributes.getEnableSubscriptionConflation()).isTrue();
		assertThat(regionAttributes.getIgnoreJTA()).isTrue();
		assertThat(regionAttributes.getInitialCapacity()).isEqualTo(10);
		assertThat(regionAttributes.getIndexMaintenanceSynchronous()).isFalse();
		assertThat(regionAttributes.getKeyConstraint()).isEqualTo(String.class);
		assertThat(regionAttributes.getLoadFactor()).isCloseTo(0.50f, offset(0.001f));
		assertThat(regionAttributes.isLockGrantor()).isTrue();
		assertThat(regionAttributes.getMulticastEnabled()).isTrue();
		assertThat(regionAttributes.getOffHeap()).isTrue();
		assertThat(regionAttributes.getScope()).isEqualTo(Scope.GLOBAL);
		assertThat(regionAttributes.getValueConstraint()).isEqualTo(String.class);
	}

	@Test
	public void replicatedWithSynchronousIndexUpdatesConfigurationIsCorrect() {

		assertThat(applicationContext.containsBean("replicated-with-synchronous-index-updates")).isTrue();

		Region<?, ?> region = applicationContext.getBean("replicated-with-synchronous-index-updates", Region.class);

		assertThat(region).as(String.format("The '%1$s' Region was not properly configured and initialized!",
			"replicated-with-synchronous-index-updates")).isNotNull();

		RegionAttributes<?, ?> regionAttributes = region.getAttributes();

		assertThat(regionAttributes).isNotNull();
		assertThat(regionAttributes.getIndexMaintenanceSynchronous()).isTrue();
	}

	@Test
	public void regionLookupConfigurationIsCorrect() throws Exception {

		Cache cache = applicationContext.getBean(Cache.class);

		Region existing = cache.createRegionFactory().create("existing");

		assertThat(applicationContext.containsBean("lookup")).isTrue();

		ResolvableRegionFactoryBean regionFactoryBean =
			applicationContext.getBean("&lookup", ResolvableRegionFactoryBean.class);

		assertThat(regionFactoryBean).isNotNull();
		assertThat(TestUtils.<String>readField("name", regionFactoryBean)).isEqualTo("existing");
		assertThat(applicationContext.getBean("lookup")).isSameAs(existing);
	}

	@Test
	public void compressedReplicateRegionConfigurationIsCorrect() {

		assertThat(applicationContext.containsBean("Compressed")).isTrue();

		Region<?, ?> compressed = applicationContext.getBean("Compressed", Region.class);

		assertThat(compressed).as("The 'Compressed' REPLICATE Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(compressed.getName()).isEqualTo("Compressed");
		assertThat(compressed.getFullPath()).isEqualTo(Region.SEPARATOR + "Compressed");
		assertThat(compressed.getAttributes()).isNotNull();
		assertThat(compressed.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(compressed.getAttributes().getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);
		assertThat(compressed.getAttributes().getCompressor() instanceof TestCompressor).isTrue();
		assertThat(compressed.getAttributes().getCompressor().toString()).isEqualTo("XYZ");
	}

	public static class TestCompressor implements Compressor {

		private String name;

		public void setName(final String name) {
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
}
