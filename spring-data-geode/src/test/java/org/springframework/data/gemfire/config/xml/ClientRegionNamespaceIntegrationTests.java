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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.InterestResultPolicy;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.apache.geode.compression.Compressor;

import org.springframework.data.gemfire.SimpleCacheListener;
import org.springframework.data.gemfire.SimpleObjectSizer;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.Interest;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

/**
 * Unit Tests for SDG's XML namespace configuration metadata for client {@link Region Regions}.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.client.ClientRegionFactoryBean
 * @see org.springframework.data.gemfire.config.xml.ClientRegionParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class ClientRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

	private void assertInterest(boolean expectedDurable, boolean expectedReceiveValues,

		InterestResultPolicy expectedPolicy, Interest<Object> actualInterest) {

		assertThat(actualInterest).isNotNull();
		assertThat(actualInterest.isDurable()).isEqualTo(expectedDurable);
		assertThat(actualInterest.isReceiveValues()).isEqualTo(expectedReceiveValues);
		assertThat(actualInterest.getPolicy()).isEqualTo(expectedPolicy);
	}

	@SuppressWarnings("rawtypes")
	private Interest getInterestWithKey(String key, Interest... interests) {

		for (Interest interest : interests) {
			if (interest.getKey().equals(key)) {
				return interest;
			}
		}

		return null;
	}

	@Test
	public void beanNamesAreCorrect() {

		assertThat(requireApplicationContext().containsBean("SimpleRegion")).isTrue();
		assertThat(requireApplicationContext().containsBean("Publisher")).isTrue();
		assertThat(requireApplicationContext().containsBean("ComplexRegion")).isTrue();
		assertThat(requireApplicationContext().containsBean("PersistentRegion")).isTrue();
		assertThat(requireApplicationContext().containsBean("OverflowRegion")).isTrue();
		assertThat(requireApplicationContext().containsBean("Compressed")).isTrue();
	}

	@Test
	public void simpleClientRegionConfigurationIsCorrect() {

		assertThat(requireApplicationContext().containsBean("simple")).isTrue();

		Region<?, ?> simple = requireApplicationContext().getBean("simple", Region.class);

		assertThat(simple).as("The 'SimpleRegion' Client Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(simple.getName()).isEqualTo("SimpleRegion");
		assertThat(simple.getFullPath()).isEqualTo(Region.SEPARATOR + "SimpleRegion");
		assertThat(simple.getAttributes()).isNotNull();
		assertThat(simple.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void publishingClientRegionConfigurationIsCorrect() throws Exception {

		assertThat(requireApplicationContext().containsBean("empty")).isTrue();

		ClientRegionFactoryBean emptyClientRegionFactoryBean =
			requireApplicationContext().getBean("&empty", ClientRegionFactoryBean.class);

		assertThat(emptyClientRegionFactoryBean).isNotNull();
		assertThat(TestUtils.<Object>readField("dataPolicy", emptyClientRegionFactoryBean)).isEqualTo(DataPolicy.EMPTY);
		assertThat(TestUtils.<Object>readField("beanName", emptyClientRegionFactoryBean)).isEqualTo("empty");
		assertThat(TestUtils.<Object>readField("name", emptyClientRegionFactoryBean)).isEqualTo("Publisher");
		assertThat(TestUtils.<Object>readField("poolName", emptyClientRegionFactoryBean)).isEqualTo("gemfire-pool");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void complexClientRegionConfigurationIsCorrect() throws Exception {

		assertThat(requireApplicationContext().containsBean("complex")).isTrue();

		ClientRegionFactoryBean complexClientRegionFactoryBean =
			requireApplicationContext().getBean("&complex", ClientRegionFactoryBean.class);

		assertThat(complexClientRegionFactoryBean).isNotNull();

		CacheListener[] cacheListeners = TestUtils.readField("cacheListeners", complexClientRegionFactoryBean);

		assertThat(ObjectUtils.isEmpty(cacheListeners)).isFalse();
		assertThat(cacheListeners.length).isEqualTo(2);
		assertThat(requireApplicationContext().getBean("c-listener")).isSameAs(cacheListeners[0]);
		assertThat(cacheListeners[1] instanceof SimpleCacheListener).isTrue();
		assertThat(cacheListeners[1]).isNotSameAs(cacheListeners[0]);

		RegionAttributes complexRegionAttributes =
			TestUtils.<RegionAttributes<?, ?>>readField("attributes", complexClientRegionFactoryBean);

		assertThat(complexRegionAttributes).isNotNull();
		assertThat(complexRegionAttributes.getLoadFactor()).isCloseTo(0.5f, offset(0.001f));
		assertThat(complexRegionAttributes.getEntryTimeToLive().getAction()).isEqualTo(ExpirationAction.INVALIDATE);
		assertThat(complexRegionAttributes.getEntryTimeToLive().getTimeout()).isEqualTo(500);
		assertThat(complexRegionAttributes.getEvictionAttributes().getMaximum()).isEqualTo(5);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void persistentClientRegionConfigurationIsCorrect() {

		assertThat(requireApplicationContext().containsBean("persistent")).isTrue();

		Region<?, ?> persistent = requireApplicationContext().getBean("persistent", Region.class);

		assertThat(persistent)
			.describedAs("The 'PersistentRegion' Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(persistent.getName()).isEqualTo("PersistentRegion");
		assertThat(persistent.getFullPath()).isEqualTo(Region.SEPARATOR + "PersistentRegion");

		RegionAttributes persistentRegionAttributes = persistent.getAttributes();

		assertThat(persistentRegionAttributes.getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
		assertThat(persistentRegionAttributes.getDiskStoreName()).isEqualTo("diskStore");
		assertThat(persistentRegionAttributes.getPoolName()).isEqualTo("gemfire-pool");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void overflowClientRegionConfigurationIsCorrect() throws Exception {

		assertThat(requireApplicationContext().containsBean("overflow")).isTrue();

		ClientRegionFactoryBean overflowClientRegionFactoryBean =
			requireApplicationContext().getBean("&overflow", ClientRegionFactoryBean.class);

		assertThat(overflowClientRegionFactoryBean).isNotNull();
		assertThat(TestUtils.<Object>readField("diskStoreName", overflowClientRegionFactoryBean)).isEqualTo("diskStore");
		assertThat(TestUtils.<Object>readField("poolName", overflowClientRegionFactoryBean)).isEqualTo("gemfire-pool");

		RegionAttributes overflowRegionAttributes =
			TestUtils.<RegionAttributes<?, ?>>readField("attributes", overflowClientRegionFactoryBean);

		assertThat(overflowRegionAttributes).isNotNull();
		assertThat(overflowRegionAttributes.getDataPolicy()).isEqualTo(DataPolicy.NORMAL);

		EvictionAttributes overflowEvictionAttributes = overflowRegionAttributes.getEvictionAttributes();

		assertThat(overflowEvictionAttributes).isNotNull();
		assertThat(overflowEvictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(overflowEvictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);
		assertThat(overflowEvictionAttributes.getMaximum()).isEqualTo(10);
		assertThat(overflowEvictionAttributes.getObjectSizer() instanceof SimpleObjectSizer).isTrue();
	}

	@Test
	public void clientRegionWithCacheLoaderAndCacheWriterConfigurationIsCorrect() throws Exception {

		assertThat(requireApplicationContext().containsBean("loadWithWrite")).isTrue();

		ClientRegionFactoryBean<?, ?> factory =
			requireApplicationContext().getBean("&loadWithWrite", ClientRegionFactoryBean.class);

		assertThat(factory).isNotNull();
		assertThat(TestUtils.<Object>readField("name", factory)).isEqualTo("LoadedFullOfWrites");
		assertThat(TestUtils.<Object>readField("shortcut", factory)).isEqualTo(ClientRegionShortcut.LOCAL);
		assertThat(TestUtils.<Object>readField("cacheLoader", factory)).isInstanceOf(TestCacheLoader.class);
		assertThat(TestUtils.<Object>readField("cacheWriter", factory)).isInstanceOf(TestCacheWriter.class);
	}

	@Test
	public void compressedReplicateRegionConfigurationIsCorrect() {

		assertThat(requireApplicationContext().containsBean("Compressed")).isTrue();

		Region<?, ?> compressed = requireApplicationContext().getBean("Compressed", Region.class);

		assertThat(compressed).as("The 'Compressed' Client Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(compressed.getName()).isEqualTo("Compressed");
		assertThat(compressed.getFullPath()).isEqualTo(Region.SEPARATOR + "Compressed");
		assertThat(compressed.getAttributes()).isNotNull();
		assertThat(compressed.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.EMPTY);
		assertThat(compressed.getAttributes().getPoolName()).isEqualTo("gemfire-pool");
		assertThat(compressed.getAttributes().getCompressor() instanceof TestCompressor)
			.describedAs(String.format("Expected 'TestCompressor'; but was '%s'!",
				ObjectUtils.nullSafeClassName(compressed.getAttributes().getCompressor())))
			.isTrue();
		assertThat(compressed.getAttributes().getCompressor().toString()).isEqualTo("STD");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void clientRegionWithAttributesConfigurationIsCorrect() {

		assertThat(requireApplicationContext().containsBean("client-with-attributes")).isTrue();

		Region<Long, String> clientRegion =
			requireApplicationContext().getBean("client-with-attributes", Region.class);

		assertThat(clientRegion)
			.describedAs("The 'client-with-attributes' Client Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(clientRegion.getName()).isEqualTo("client-with-attributes");
		assertThat(clientRegion.getFullPath()).isEqualTo(Region.SEPARATOR + "client-with-attributes");
		assertThat(clientRegion.getAttributes()).isNotNull();
		assertThat(clientRegion.getAttributes().getCloningEnabled()).isFalse();
		assertThat(clientRegion.getAttributes().getConcurrencyChecksEnabled()).isTrue();
		assertThat(clientRegion.getAttributes().getConcurrencyLevel()).isEqualTo(8);
		assertThat(clientRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.NORMAL);
		assertThat(clientRegion.getAttributes().getDataPolicy().withPersistence()).isFalse();
		assertThat(clientRegion.getAttributes().getInitialCapacity()).isEqualTo(64);
		assertThat(clientRegion.getAttributes().getKeyConstraint()).isEqualTo(Long.class);
		assertThat(String.valueOf(clientRegion.getAttributes().getLoadFactor())).isEqualTo("0.85");
		assertThat(clientRegion.getAttributes().getPoolName()).isEqualTo("gemfire-pool");
		assertThat(clientRegion.getAttributes().getValueConstraint()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void clientRegionWithRegisteredInterestsConfigurationIsCorrect() throws Exception {

		assertThat(requireApplicationContext().containsBean("client-with-interests")).isTrue();

		ClientRegionFactoryBean<?, ?> factoryBean =
			requireApplicationContext().getBean("&client-with-interests", ClientRegionFactoryBean.class);

		assertThat(factoryBean).isNotNull();

		Interest<?>[] interests = TestUtils.readField("interests", factoryBean);

		assertThat(interests).isNotNull();
		assertThat(interests.length).isEqualTo(2);

		assertInterest(true, false, InterestResultPolicy.KEYS, getInterestWithKey(".*", interests));
		assertInterest(true, false, InterestResultPolicy.KEYS_VALUES, getInterestWithKey("keyPrefix.*", interests));

		Region<Object, Object> mockClientRegion =
			requireApplicationContext().getBean("client-with-interests", Region.class);

		assertThat(mockClientRegion).isNotNull();

		verify(mockClientRegion, times(1)).registerInterest(eq(".*"),
			eq(InterestResultPolicy.KEYS), eq(true), eq(false));

		verify(mockClientRegion, times(1)).registerInterestRegex(eq("keyPrefix.*"),
			eq(InterestResultPolicy.KEYS_VALUES), eq(true), eq(false));
	}

	public static final class TestCacheLoader implements CacheLoader<Object, Object> {

		@Override
		public Object load(final LoaderHelper<Object, Object> helper) throws CacheLoaderException {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public void close() { }

	}

	public static final class TestCacheWriter extends CacheWriterAdapter<Object, Object> { }

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
