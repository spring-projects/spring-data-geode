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
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.DiskStoreFactory;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAlgorithm;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Region.Entry;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.util.ObjectSizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.SimpleObjectSizer;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

/**
 * Integration Tests for {@link Region}, {@link EvictionAttributes} and {@link DiskStore} SDG XML namespace
 * configuration metadata parsing.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DiskStore
 * @see org.apache.geode.cache.ExpirationAttributes
 * @see org.apache.geode.cache.EvictionAttributes
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.RegionAttributes
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "diskstore-ns.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
// TODO: Move test cases into a DiskStoreIntegrationTests class
public class DiskStoreEvictionAndExpirationRegionParsingIntegrationTests extends IntegrationTestsSupport {

	private static File diskStoreDirectory;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier("diskStore1")
	private DiskStore diskStore;

	@BeforeClass
	public static void setUp() {
		diskStoreDirectory = new File("./tmp");
		assertThat(diskStoreDirectory.isDirectory() || diskStoreDirectory.mkdirs()).isTrue();
	}

	@AfterClass
	public static void tearDown() {

		FileSystemUtils.deleteRecursively(diskStoreDirectory);

		for (String name : nullSafeArray(new File(".")
				.list((dir, name) -> name.startsWith("BACKUP")), String.class)) {

			new File(name).delete();
		}
	}

	@Test
	public void testDiskStore() {

		assertThat(applicationContext.getBean("ds2")).isNotNull();
		applicationContext.getBean("diskStore1");
 		assertThat(diskStore).isNotNull();
		assertThat(diskStore.getName()).isEqualTo("diskStore1");
		assertThat(diskStore.getQueueSize()).isEqualTo(50);
		assertThat(diskStore.getAutoCompact()).isTrue();
		assertThat(diskStore.getCompactionThreshold()).isEqualTo(DiskStoreFactory.DEFAULT_COMPACTION_THRESHOLD);
		assertThat(diskStore.getTimeInterval()).isEqualTo(9999);
		assertThat(diskStore.getMaxOplogSize()).isEqualTo(1);
		assertThat(diskStore.getDiskDirs()[0]).isEqualTo(diskStoreDirectory);
		Cache cache = applicationContext.getBean("gemfireCache", Cache.class);
		assertThat(cache.findDiskStore("diskStore1")).isSameAs(diskStore);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testReplicatedDataRegionAttributes() throws Exception {

		assertThat(applicationContext.containsBean("replicated-data")).isTrue();

		PeerRegionFactoryBean replicatedDataRegionFactoryBean = applicationContext.getBean("&replicated-data", PeerRegionFactoryBean.class);

		assertThat(replicatedDataRegionFactoryBean instanceof ReplicatedRegionFactoryBean).isTrue();
		assertThat(replicatedDataRegionFactoryBean.getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
		assertThat(replicatedDataRegionFactoryBean.getDataPolicy().withPersistence()).isFalse();
		assertThat(TestUtils.<String>readField("diskStoreName", replicatedDataRegionFactoryBean)).isEqualTo("diskStore1");
		assertThat(TestUtils.<Object>readField("scope", replicatedDataRegionFactoryBean)).isNull();

		Region replicatedDataRegion = applicationContext.getBean("replicated-data", Region.class);

		RegionAttributes replicatedDataRegionAttributes = TestUtils.readField("attributes", replicatedDataRegionFactoryBean);

		assertThat(replicatedDataRegionAttributes).isNotNull();
		assertThat(replicatedDataRegionAttributes.getScope()).isEqualTo(Scope.DISTRIBUTED_NO_ACK);

		EvictionAttributes replicatedDataEvictionAttributes = replicatedDataRegionAttributes.getEvictionAttributes();

		assertThat(replicatedDataEvictionAttributes).isNotNull();
		assertThat(replicatedDataEvictionAttributes.getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(replicatedDataEvictionAttributes.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_ENTRY);
		assertThat(replicatedDataEvictionAttributes.getMaximum()).isEqualTo(50);
		assertThat(replicatedDataEvictionAttributes.getObjectSizer()).isNull();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testPartitionDataOptions() throws Exception {

		assertThat(applicationContext.containsBean("partition-data")).isTrue();

		PeerRegionFactoryBean regionFactoryBean = applicationContext.getBean("&partition-data", PeerRegionFactoryBean.class);

		assertThat(regionFactoryBean instanceof PartitionedRegionFactoryBean).isTrue();
		assertThat(TestUtils.<Boolean>readField("persistent", regionFactoryBean)).isTrue();
		RegionAttributes attrs = TestUtils.readField("attributes", regionFactoryBean);

		EvictionAttributes evicAttr = attrs.getEvictionAttributes();

		assertThat(evicAttr.getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(evicAttr.getAlgorithm()).isEqualTo(EvictionAlgorithm.LRU_MEMORY);

		ObjectSizer sizer = evicAttr.getObjectSizer();

		assertThat(sizer.getClass()).isEqualTo(SimpleObjectSizer.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testEntryTtl() throws Exception {

		assertThat(applicationContext.containsBean("replicated-data")).isTrue();

		PeerRegionFactoryBean fb = applicationContext.getBean("&replicated-data", PeerRegionFactoryBean.class);
		RegionAttributes attrs = TestUtils.readField("attributes", fb);

		ExpirationAttributes entryTTL = attrs.getEntryTimeToLive();
		assertThat(entryTTL.getTimeout()).isEqualTo(100);
		assertThat(entryTTL.getAction()).isEqualTo(ExpirationAction.DESTROY);

		ExpirationAttributes entryTTI = attrs.getEntryIdleTimeout();
		assertThat(entryTTI.getTimeout()).isEqualTo(200);
		assertThat(entryTTI.getAction()).isEqualTo(ExpirationAction.INVALIDATE);

		ExpirationAttributes regionTTL = attrs.getRegionTimeToLive();
		assertThat(regionTTL.getTimeout()).isEqualTo(300);
		assertThat(regionTTL.getAction()).isEqualTo(ExpirationAction.DESTROY);

		ExpirationAttributes regionTTI = attrs.getRegionIdleTimeout();
		assertThat(regionTTI.getTimeout()).isEqualTo(400);
		assertThat(regionTTI.getAction()).isEqualTo(ExpirationAction.INVALIDATE);
	}


	@Test
	@SuppressWarnings("rawtypes")
	public void testCustomExpiry() throws Exception {

		assertThat(applicationContext.containsBean("replicated-data-custom-expiry")).isTrue();

		PeerRegionFactoryBean fb = applicationContext.getBean("&replicated-data-custom-expiry", PeerRegionFactoryBean.class);
		RegionAttributes attrs = TestUtils.readField("attributes", fb);

		assertThat(attrs.getCustomEntryIdleTimeout()).isNotNull();
		assertThat(attrs.getCustomEntryTimeToLive()).isNotNull();

		assertThat(attrs.getCustomEntryIdleTimeout() instanceof TestCustomExpiry).isTrue();
		assertThat(attrs.getCustomEntryTimeToLive() instanceof TestCustomExpiry).isTrue();
	}

	public static class TestCustomExpiry<K,V> implements CustomExpiry<K,V> {

		@Override
		public ExpirationAttributes getExpiry(Entry<K, V> entry) {
			return null;
		}

		@Override
		public void close() { }

	}
}
