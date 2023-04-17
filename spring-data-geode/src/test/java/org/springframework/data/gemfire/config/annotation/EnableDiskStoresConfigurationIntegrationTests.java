/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import jakarta.annotation.Resource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link EnableDiskStore} and {@link EnableDiskStores} annotations
 * along with the {@link DiskStoreConfiguration} class.
 *
 * @author John Blum
 * @see java.io.File
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.DiskStore
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 3.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class EnableDiskStoresConfigurationIntegrationTests extends IntegrationTestsSupport {

	private static final String DISK_STORE_LOCATION_PROPERTY =
		"spring.data.gemfire.disk.store.ParkDiskStore.directory.location";

	private static final File DISK_STORE_LOCATION = new File(System.getProperty("java.io.tmpdir"),
		"/spring/geode/tests/EnableDiskStoresConfigurationIntegrationTests");

	@Autowired
	private GemFireCache cache;

	@Resource(name = "DiskBasedRegion")
	private Region<Object, Object> diskBasedRegion;

	@BeforeClass
	public static void setSpringDataGemFireDiskStoreDirectoryLocationAsSystemProperty() {
		System.setProperty(DISK_STORE_LOCATION_PROPERTY, DISK_STORE_LOCATION.getAbsolutePath());
		assertThat(DISK_STORE_LOCATION).doesNotExist();
	}

	@AfterClass
	public static void clearSystemProperties() {
		System.clearProperty(DISK_STORE_LOCATION_PROPERTY);
		FileSystemUtils.deleteRecursive(DISK_STORE_LOCATION);
	}

	@Before
	public void assertCache() {

		assertThat(this.cache).isNotNull();
		assertThat(this.cache.getName()).isEqualTo(EnableDiskStoresConfigurationIntegrationTests.class.getSimpleName());
	}

	@Before
	public void assertRegion() {

		assertThat(this.diskBasedRegion).isNotNull();
		assertThat(this.diskBasedRegion.getName()).isEqualTo(RegionUtils.toRegionName("DiskBasedRegion"));
		assertThat(this.diskBasedRegion.getFullPath())
			.isEqualTo(RegionUtils.toRegionPath("DiskBasedRegion"));
		assertThat(this.diskBasedRegion.getAttributes()).isNotNull();
		assertThat(this.diskBasedRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_REPLICATE);
	}

	@Test
	public void customDiskStoreConfigurationIsCorrect() {

		String diskStoreName = this.diskBasedRegion.getAttributes().getDiskStoreName();

		assertThat(diskStoreName).isNotBlank();

		DiskStore diskStore = this.cache.findDiskStore(diskStoreName);

		assertThat(diskStore).isNotNull();
		assertThat(diskStore.getName()).isEqualTo("ParkDiskStore");

		File[] diskDirectories = diskStore.getDiskDirs();

		assertThat(diskDirectories).isNotNull();
		assertThat(diskDirectories).hasSize(1);
		assertThat(diskDirectories[0]).isEqualTo(DISK_STORE_LOCATION);
		assertThat(diskDirectories[0]).isDirectory();
	}

	@PeerCacheApplication(name = "EnableDiskStoresConfigurationIntegrationTests")
	@EnableDiskStore(name = "ParkDiskStore")
	static class TestConfiguration {

		@Bean("DiskBasedRegion")
		@DependsOn("ParkDiskStore")
		ReplicatedRegionFactoryBean<Object, Object> diskBasedRegion(GemFireCache cache) {

			ReplicatedRegionFactoryBean<Object, Object> regionBean = new ReplicatedRegionFactoryBean<>();

			regionBean.setCache(cache);
			regionBean.setDiskStoreName("ParkDiskStore");
			regionBean.setPersistent(true);

			return regionBean;
		}
	}
}
