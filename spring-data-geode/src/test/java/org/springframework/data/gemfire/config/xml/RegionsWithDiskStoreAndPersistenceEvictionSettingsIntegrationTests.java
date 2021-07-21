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

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the functionality of cache {@link Region Regions} when persistent/non-persistent with
 * and without Eviction settings when specifying a {@link DiskStore}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.4.0.RC1
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class RegionsWithDiskStoreAndPersistenceEvictionSettingsIntegrationTests extends IntegrationTestsSupport {

	@Resource(name = "NotPersistentNoOverflowRegion")
	private Region<?, ?> notPersistentNoOverflowRegion;

	@Resource(name = "NotPersistentOverflowRegion")
	private Region<?, ?> notPersistentOverflowRegion;

	@Resource(name = "PersistentNoOverflowRegion")
	private Region<?, ?> persistentNoOverflowRegion;

	@Resource(name = "PersistentOverflowRegion")
	private Region<?, ?> persistentOverflowRegion;

	@Test
	public void testNotPersistentNoOverflowRegion() {

		assertThat(notPersistentNoOverflowRegion)
			.describedAs("The Not Persistent, No Overflow Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(notPersistentNoOverflowRegion.getAttributes()).isNotNull();
		assertThat(notPersistentNoOverflowRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(notPersistentNoOverflowRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(notPersistentNoOverflowRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.NONE);
		assertThat(notPersistentNoOverflowRegion.getAttributes().getDiskStoreName()).isNull();
	}

	@Test
	public void testNotPersistentOverflowRegion() {

		assertThat(notPersistentOverflowRegion)
			.describedAs("The Not Persistent, Overflow Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(notPersistentOverflowRegion.getAttributes()).isNotNull();
		assertThat(notPersistentOverflowRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);
		assertThat(notPersistentOverflowRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(notPersistentOverflowRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(notPersistentOverflowRegion.getAttributes().getDiskStoreName()).isEqualTo("DiskStoreOne");
	}

	@Test
	public void testPersistentNoOverflowRegion() {

		assertThat(persistentNoOverflowRegion)
			.describedAs("The Persistent, No Overflow Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(persistentNoOverflowRegion.getAttributes()).isNotNull();
		assertThat(persistentNoOverflowRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(persistentNoOverflowRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(persistentNoOverflowRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.LOCAL_DESTROY);
		assertThat(persistentNoOverflowRegion.getAttributes().getDiskStoreName()).isEqualTo("DiskStoreOne");
	}

	@Test
	public void testPersistentOverflowRegion() {

		assertThat(persistentOverflowRegion)
			.describedAs("The Persistent, Overflow Region was not properly configured and initialized!")
			.isNotNull();

		assertThat(persistentOverflowRegion.getAttributes()).isNotNull();
		assertThat(persistentOverflowRegion.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(persistentOverflowRegion.getAttributes().getEvictionAttributes()).isNotNull();
		assertThat(persistentOverflowRegion.getAttributes().getEvictionAttributes().getAction()).isEqualTo(EvictionAction.OVERFLOW_TO_DISK);
		assertThat(persistentOverflowRegion.getAttributes().getDiskStoreName()).isEqualTo("DiskStoreOne");
	}
}
