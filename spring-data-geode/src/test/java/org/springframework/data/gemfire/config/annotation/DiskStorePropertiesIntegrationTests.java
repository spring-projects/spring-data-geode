/*
 * Copyright 2017-2021 the original author or authors.
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

import java.util.function.Function;

import org.junit.Test;

import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.DiskStoreFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects;
import org.springframework.mock.env.MockPropertySource;

/**
 * Integration Tests for {@link EnableDiskStore}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DiskStore
 * @see org.apache.geode.cache.DiskStoreFactory
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.data.gemfire.config.annotation.EnableDiskStore
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.annotation.EnableGemFireMockObjects
 * @see org.springframework.mock.env.MockPropertySource
 * @since 2.0.0
 */
public class DiskStorePropertiesIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
			Class<?>... annotatedClasses) {

		Function<ConfigurableApplicationContext, ConfigurableApplicationContext> applicationContextInitializer = applicationContext -> {

			MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();

			propertySources.addFirst(testPropertySource);

			return applicationContext;
		};

		return newApplicationContext(applicationContextInitializer, annotatedClasses);
	}

	@SuppressWarnings("all")
	private void assertDiskStore(DiskStore diskStore, String name, boolean allowForceCompaction, boolean autoCompact,
			int compactionThreshold, float diskUsageCriticalPercentage, float diskUsageWarningPercentage,
			long maxOplogSize, int queueSize, long timeInterval, int writeBufferSize) {

		assertThat(diskStore).isNotNull();
		assertThat(diskStore.getAllowForceCompaction()).isEqualTo(allowForceCompaction);
		assertThat(diskStore.getAutoCompact()).isEqualTo(autoCompact);
		assertThat(diskStore.getCompactionThreshold()).isEqualTo(compactionThreshold);
		assertThat(diskStore.getDiskUsageCriticalPercentage()).isEqualTo(diskUsageCriticalPercentage);
		assertThat(diskStore.getDiskUsageWarningPercentage()).isEqualTo(diskUsageWarningPercentage);
		assertThat(diskStore.getMaxOplogSize()).isEqualTo(maxOplogSize);
		assertThat(diskStore.getName()).isEqualTo(name);
		assertThat(diskStore.getQueueSize()).isEqualTo(queueSize);
		assertThat(diskStore.getTimeInterval()).isEqualTo(timeInterval);
		assertThat(diskStore.getWriteBufferSize()).isEqualTo(writeBufferSize);
	}

	@Test
	public void diskStoreConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.disk.store.compaction-threshold", 85)
			.withProperty("spring.data.gemfire.disk.store.disk-usage-critical-percentage", 90.0f)
			.withProperty("spring.data.gemfire.disk.store.disk-usage-warning-percentage", 80.0f)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStore.disk-usage-warning-percentage", 85.0f)
			.withProperty("spring.data.gemfire.disk.store.time-interval", 500L)
			.withProperty("spring.data.gemfire.disk.store.NonExistingDiskStore.time-interval", 30000L);

		newApplicationContext(testPropertySource, TestDiskStoreConfiguration.class);

		assertThat(containsBean("TestDiskStore")).isTrue();

		DiskStore testDiskStore = getBean("TestDiskStore", DiskStore.class);

		assertThat(testDiskStore).isNotNull();
		assertThat(testDiskStore.getName()).isEqualTo("TestDiskStore");
		assertThat(testDiskStore.getAllowForceCompaction()).isTrue();
		assertThat(testDiskStore.getAutoCompact()).isTrue();
		assertThat(testDiskStore.getCompactionThreshold()).isEqualTo(75);
		assertThat(testDiskStore.getDiskUsageCriticalPercentage()).isEqualTo(90.0f);
		assertThat(testDiskStore.getDiskUsageWarningPercentage()).isEqualTo(85.0f);
		assertThat(testDiskStore.getMaxOplogSize()).isEqualTo(512L);
		assertThat(testDiskStore.getQueueSize()).isEqualTo(DiskStoreFactory.DEFAULT_QUEUE_SIZE);
		assertThat(testDiskStore.getTimeInterval()).isEqualTo(500L);
		assertThat(testDiskStore.getWriteBufferSize()).isEqualTo(DiskStoreFactory.DEFAULT_WRITE_BUFFER_SIZE);
	}

	@Test
	public void diskStoresConfiguration() {

		MockPropertySource testPropertySource = new MockPropertySource()
			.withProperty("spring.data.gemfire.disk.store.allow-force-compaction", true)
			.withProperty("spring.data.gemfire.disk.store.auto-compact", false)
			.withProperty("spring.data.gemfire.disk.store.compaction-threshold", 60)
			.withProperty("spring.data.gemfire.disk.store.disk-usage-critical-percentage", 90.0f)
			.withProperty("spring.data.gemfire.disk.store.disk-usage-warning-percentage", 75.0f)
			.withProperty("spring.data.gemfire.disk.store.max-oplog-size", 512L)
			.withProperty("spring.data.gemfire.disk.store.queue-size", 1024)
			.withProperty("spring.data.gemfire.disk.store.time-interval", 500L)
			.withProperty("spring.data.gemfire.disk.store.write-buffer-size", 16384)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.allow-force-compaction", true)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.auto-compact", false)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.compaction-threshold", 75)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.disk-usage-critical-percentage", 95.0f)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.disk-usage-warning-percentage", 80.0f)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.max-oplog-size", 2048L)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.queue-size", 512)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.time-interval", 250L)
			.withProperty("spring.data.gemfire.disk.store.TestDiskStoreTwo.write-buffer-size", 65535);

		newApplicationContext(testPropertySource, TestDiskStoresConfiguration.class);

		assertThat(containsBean("TestDiskStoreOne")).isTrue();
		assertThat(containsBean("TestDiskStoreTwo")).isTrue();

		DiskStore testDiskStoreOne = getBean("TestDiskStoreOne", DiskStore.class);

		assertDiskStore(testDiskStoreOne, "TestDiskStoreOne", true, false,
			60, 90.0f, 75.0f, 512L,
			1024, 500L, 16384);

		DiskStore testDiskStoreTwo = getBean("TestDiskStoreTwo", DiskStore.class);

		assertDiskStore(testDiskStoreTwo, "TestDiskStoreTwo", true, false,
			75, 95.0f, 80.0f, 2048L,
			512, 250L, 65535);
	}

	@PeerCacheApplication
	@EnableGemFireMockObjects
	@EnableDiskStore(name = "TestDiskStore", allowForceCompaction = true, compactionThreshold = 65,
		diskUsageCriticalPercentage = 95.0f, diskUsageWarningPercentage = 75.0f, maxOplogSize = 2048L)
	@SuppressWarnings("unused")
	static class TestDiskStoreConfiguration {

		@Bean
		DiskStoreConfigurer testDiskStoreConfigurer() {

			return (beanName, factoryBean) -> {
				factoryBean.setCompactionThreshold(75);
				//factoryBean.setDiskUsageWarningPercentage(95.0f);
				factoryBean.setMaxOplogSize(512L);
			};
		}
	}

	@PeerCacheApplication
	@EnableGemFireMockObjects
	@EnableDiskStores(diskStores = {
		@EnableDiskStore(name = "TestDiskStoreOne"), @EnableDiskStore(name = "TestDiskStoreTwo")
	})
	static class TestDiskStoresConfiguration { }

}
