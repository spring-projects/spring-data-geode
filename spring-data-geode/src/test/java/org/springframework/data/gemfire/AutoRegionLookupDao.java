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

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.support.DaoSupport;
import org.springframework.stereotype.Repository;

/**
 * {@link AutoRegionLookupDao} is a Data Access Object (DAO) encapsulating references to several cache
 * {@link Region Regions} defined in native Apache Geode {@literal cache.xml} and registered as beans in the Spring
 * context using Spring Data for Apache Geode's auto {@link Region} lookup functionality.
 *
 * This class is used by the {@link AutoRegionLookupWithComponentScanningIntegrationTests} class to ensure
 * this {@link Repository @Repository} component is auto-wired properly.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.springframework.dao.support.DaoSupport
 * @see org.springframework.stereotype.Repository
 * @since 1.5.0
 */
//@Lazy
@DependsOn("gemfireCache")
@Repository("autoRegionLookupDao")
@SuppressWarnings("unused")
public class AutoRegionLookupDao extends DaoSupport {

	@Autowired
	@Qualifier("NativePartitionedRegion")
	private Region<?, ?> nativePartitionedRegion;

	@Autowired
	@Qualifier("NativeReplicateParent")
	private Region<?, ?> nativeReplicateParent;

	@Autowired
	@Qualifier("/NativeReplicateParent/NativeReplicateChild")
	private Region<?, ?> nativeReplicateChild;

	@Autowired
	@Qualifier("/NativeReplicateParent/NativeReplicateChild/NativeReplicateGrandchild")
	private Region<?, ?> nativeReplicateGrandchild;

	protected static void assertRegionMetaData(Region<?, ?> region, String expectedName, DataPolicy expectedDataPolicy) {
		assertRegionMetaData(region, expectedName, Region.SEPARATOR + expectedName, expectedDataPolicy);
	}

	protected static void assertRegionMetaData(Region<?, ?> region, String expectedName, String expectedFullPath,
			DataPolicy expectedDataPolicy) {

		assertThat(region)
			.describedAs("Region [%s] was not properly configured and initialized", expectedName)
			.isNotNull();

		assertThat(region.getName()).isEqualTo(expectedName);
		assertThat(region.getFullPath()).isEqualTo(expectedFullPath);

		assertThat(region.getAttributes())
			.describedAs("Region [%s] must have RegionAttributes defined", expectedName)
			.isNotNull();

		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(expectedDataPolicy);
		assertThat(region.getAttributes().getDataPolicy().withPersistence()).isFalse();
	}

	@Override
	protected void checkDaoConfig() throws IllegalArgumentException {

		assertRegionMetaData(nativePartitionedRegion, "NativePartitionedRegion", DataPolicy.PARTITION);
		assertRegionMetaData(nativeReplicateParent, "NativeReplicateParent", DataPolicy.REPLICATE);
		assertRegionMetaData(nativeReplicateChild, "NativeReplicateChild",
			"/NativeReplicateParent/NativeReplicateChild", DataPolicy.REPLICATE);
		assertRegionMetaData(nativeReplicateGrandchild, "NativeReplicateGrandchild",
			"/NativeReplicateParent/NativeReplicateChild/NativeReplicateGrandchild", DataPolicy.REPLICATE);
	}
}
