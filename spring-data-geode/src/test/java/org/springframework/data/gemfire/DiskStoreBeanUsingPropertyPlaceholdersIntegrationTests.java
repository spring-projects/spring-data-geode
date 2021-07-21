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

import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DiskStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the use of Spring PropertyPlaceholders to configure and initialize a {@link DiskStore} bean
 * properties using property placeholders in the SDG XML namespace &lt;disk-store&gt; bean definition attributes.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DiskStore
 * @see org.springframework.data.gemfire.DiskStoreFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @link https://jira.springsource.org/browse/SGF-249
 * @since 1.3.4
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = "diskstore-using-propertyplaceholders-config.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class DiskStoreBeanUsingPropertyPlaceholdersIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private DiskStore testDataStore;

	@Resource(name="diskStoreConfiguration")
	private Map<String, Object> diskStoreConfiguration;

	private Object getExpectedValue(final String propertyPlaceholderName) {
		return this.diskStoreConfiguration.get(propertyPlaceholderName);
	}

	@Test
	public void testDiskStoreBeanWithPropertyPlaceholderConfiguration() {

		assertThat(testDataStore).describedAs("The Disk Store was not configured and initialized!").isNotNull();
		assertThat(testDataStore.getAllowForceCompaction()).isEqualTo(getExpectedValue("allowForceCompaction"));
		assertThat(testDataStore.getAutoCompact()).isEqualTo(getExpectedValue("autoCompact"));
		assertThat(testDataStore.getCompactionThreshold()).isEqualTo(getExpectedValue("compactionThreshold"));
		assertThat(testDataStore.getMaxOplogSize()).isEqualTo(getExpectedValue("maxOplogSize"));
		assertThat(testDataStore.getName()).isEqualTo("TestDataStore");
		assertThat(testDataStore.getQueueSize()).isEqualTo(getExpectedValue("queueSize"));
		assertThat(testDataStore.getTimeInterval()).isEqualTo(getExpectedValue("timeInterval"));
		assertThat(testDataStore.getWriteBufferSize()).isEqualTo(getExpectedValue("writeBufferSize"));
	}
}
