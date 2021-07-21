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

import java.io.File;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DiskStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests with test cases testing the contract and functionality of using SDG's XML namespace configuration
 * metadata to configure {@link DiskStore DiskStores}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DiskStore
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "diskstore-ns.xml", initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class DiskStoreNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("fullyConfiguredDiskStore")
	private DiskStore diskStore;

	@Autowired
	@Qualifier("props")
	private Properties props;

	@Test
	public void testDiskStoreConfiguration() {

		assertThat(diskStore).as("The 'fullyConfiguredDiskStore' was not properly configured and initialized").isNotNull();
		assertThat(diskStore.getName()).isEqualTo("fullyConfiguredDiskStore");
		assertThat(diskStore.getAllowForceCompaction()).isEqualTo(Boolean.valueOf(props.getProperty("allowForceCompaction")));
		assertThat(diskStore.getAutoCompact()).isEqualTo(Boolean.valueOf(props.getProperty("autoCompact")));
		assertThat(Long.valueOf(diskStore.getCompactionThreshold())).isEqualTo(Long.valueOf(props.getProperty("compactionThreshold")));
		assertThat(Double.valueOf(diskStore.getDiskUsageCriticalPercentage())).isEqualTo(Double.valueOf(props.getProperty("diskUsageCriticalPercentage")));
		assertThat(Double.valueOf(diskStore.getDiskUsageWarningPercentage())).isEqualTo(Double.valueOf(props.getProperty("diskUsageWarningPercentage")));
		assertThat(Long.valueOf(diskStore.getMaxOplogSize())).isEqualTo(Long.valueOf(props.getProperty("maxOplogSize")));
		assertThat(Long.valueOf(diskStore.getQueueSize())).isEqualTo(Long.valueOf(props.getProperty("queueSize")));
		assertThat(Long.valueOf(diskStore.getTimeInterval())).isEqualTo(Long.valueOf(props.getProperty("timeInterval")));
		assertThat(Long.valueOf(diskStore.getWriteBufferSize())).isEqualTo(Long.valueOf(props.getProperty("writeBufferSize")));
		assertThat(diskStore.getDiskDirs()).isNotNull();
		assertThat(diskStore.getDiskDirs().length).isEqualTo(1);
		assertThat(diskStore.getDiskDirs()[0]).isEqualTo(new File(props.getProperty("location")));
		assertThat(Long.valueOf(diskStore.getDiskDirSizes()[0])).isEqualTo(Long.valueOf(props.getProperty("maxSize")));
	}
}
