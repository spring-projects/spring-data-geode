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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.gemfire.tests.util.FileSystemUtils.FileExtensionFilter.newFileExtensionFilter;

import java.io.FileFilter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.data.gemfire.util.CacheUtils;

/**
 * Integration Tests for {@link GenericRegionFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.GenericRegionFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @since 1.7.0
 */
public class GenericRegionFactoryBeanIntegrationTests extends IntegrationTestsSupport {

	// As defined in com.gemstone.gemfire.internal.cache.AbstractRegion
	private static final Scope DEFAULT_SCOPE = Scope.DISTRIBUTED_NO_ACK;

	private static Region<Object, Object> defaultRegion;
	private static Region<Object, Object> emptyRegion;
	private static Region<Object, Object> localRegion;
	private static Region<Object, Object> normalRegion;
	private static Region<Object, Object> persistentPartitionRegion;
	private static Region<Object, Object> preloadedRegion;
	private static Region<Object, Object> replicateRegion;

	@BeforeClass
	public static void setup() throws Exception {

		Cache gemfireCache = new CacheFactory()
			.set("name", GenericRegionFactoryBeanIntegrationTests.class.getSimpleName())
			.set("log-level", "error")
			.create();

		PeerRegionFactoryBean<Object, Object> defaultRegionFactory = new GenericRegionFactoryBean<>();

		defaultRegionFactory.setCache(gemfireCache);
		defaultRegionFactory.setName("DefaultRegion");
		defaultRegionFactory.afterPropertiesSet();

		defaultRegion = defaultRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> emptyRegionFactory = new GenericRegionFactoryBean<>();

		emptyRegionFactory.setCache(gemfireCache);
		emptyRegionFactory.setDataPolicy(DataPolicy.EMPTY);
		emptyRegionFactory.setName("EmptyRegion");
		emptyRegionFactory.setScope(Scope.DISTRIBUTED_NO_ACK);
		emptyRegionFactory.afterPropertiesSet();

		emptyRegion = emptyRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> localRegionFactory = new GenericRegionFactoryBean<>();

		localRegionFactory.setCache(gemfireCache);
		localRegionFactory.setDataPolicy(DataPolicy.NORMAL);
		localRegionFactory.setName("LocalRegion");
		localRegionFactory.setScope(Scope.LOCAL);
		localRegionFactory.afterPropertiesSet();

		localRegion = localRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> normalRegionFactory = new GenericRegionFactoryBean<>();

		normalRegionFactory.setCache(gemfireCache);
		normalRegionFactory.setDataPolicy(DataPolicy.NORMAL);
		normalRegionFactory.setName("NormalRegion");
		normalRegionFactory.setScope(Scope.DISTRIBUTED_ACK);
		normalRegionFactory.afterPropertiesSet();

		normalRegion = normalRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> persistentPartitionRegionFactory = new GenericRegionFactoryBean<>();

		persistentPartitionRegionFactory.setCache(gemfireCache);
		persistentPartitionRegionFactory.setDataPolicy(DataPolicy.PERSISTENT_PARTITION);
		persistentPartitionRegionFactory.setName("PersistentPartitionRegion");
		persistentPartitionRegionFactory.setPersistent(true);
		persistentPartitionRegionFactory.afterPropertiesSet();

		persistentPartitionRegion = persistentPartitionRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> preloadedRegionFactory = new GenericRegionFactoryBean<>();

		preloadedRegionFactory.setCache(gemfireCache);
		preloadedRegionFactory.setDataPolicy(DataPolicy.PRELOADED);
		preloadedRegionFactory.setName("PreloadedRegion");
		preloadedRegionFactory.afterPropertiesSet();

		preloadedRegion = preloadedRegionFactory.getObject();

		PeerRegionFactoryBean<Object, Object> replicateRegionFactory = new GenericRegionFactoryBean<>();

		replicateRegionFactory.setCache(gemfireCache);
		replicateRegionFactory.setDataPolicy(DataPolicy.REPLICATE);
		replicateRegionFactory.setName("ReplicateRegion");
		replicateRegionFactory.setScope(Scope.GLOBAL);
		replicateRegionFactory.afterPropertiesSet();

		replicateRegion = replicateRegionFactory.getObject();
	}

	@AfterClass
	public static void tearDown() {
		CacheUtils.closeCache();

		FileFilter fileFilter = FileSystemUtils.CompositeFileFilter.or(
			newFileExtensionFilter(".if"), newFileExtensionFilter(".crf"), newFileExtensionFilter(".drf"),
				newFileExtensionFilter(".log"));

		FileSystemUtils.deleteRecursive(FileSystemUtils.WORKING_DIRECTORY, fileFilter);
	}

	protected void assertRegionAttributes(Region<?, ?> region, String expectedRegionName, DataPolicy expectedDataPolicy,
			Scope expectedScope) {

		assertRegionAttributes(region, expectedRegionName, String.format("%1$s%2$s", Region.SEPARATOR, expectedRegionName),
			expectedDataPolicy, expectedScope);
	}

	protected void assertRegionAttributes(Region<?, ?> region, String expectedRegionName, String expectedRegionPath,
			DataPolicy expectedDataPolicy, Scope expectedScope) {

		assertThat(region).as("The GemFire Cache Region must not be null!").isNotNull();
		assertThat(region.getName()).isEqualTo(expectedRegionName);
		assertThat(region.getFullPath()).isEqualTo(expectedRegionPath);
		assertThat(region.getAttributes()).as("The RegionAttributes must be specified!").isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(expectedDataPolicy);
		assertThat(region.getAttributes().getScope()).isEqualTo(expectedScope);
	}

	@Test
	public void defaultRegionAttributes() {
		assertRegionAttributes(defaultRegion, "DefaultRegion", DataPolicy.DEFAULT, DEFAULT_SCOPE);
	}

	@Test
	public void emptyRegionAttributes() {
		assertRegionAttributes(emptyRegion, "EmptyRegion", DataPolicy.EMPTY, Scope.DISTRIBUTED_NO_ACK);
	}

	@Test
	public void localRegionAttributes() {
		assertRegionAttributes(localRegion, "LocalRegion", DataPolicy.NORMAL, Scope.LOCAL);
	}

	@Test
	public void normalRegionAttributes() {
		assertRegionAttributes(normalRegion, "NormalRegion", DataPolicy.NORMAL, Scope.DISTRIBUTED_ACK);
	}

	@Test
	public void persistentPartitionRegionAttributes() {
		assertRegionAttributes(persistentPartitionRegion, "PersistentPartitionRegion", DataPolicy.PERSISTENT_PARTITION,
			DEFAULT_SCOPE);
	}

	@Test
	public void preloadedRegionAttributes() {
		assertRegionAttributes(preloadedRegion, "PreloadedRegion", DataPolicy.PRELOADED, DEFAULT_SCOPE);
	}

	@Test
	public void replicateRegionAttributes() {
		assertRegionAttributes(replicateRegion, "ReplicateRegion", DataPolicy.REPLICATE, Scope.GLOBAL);
	}
}
