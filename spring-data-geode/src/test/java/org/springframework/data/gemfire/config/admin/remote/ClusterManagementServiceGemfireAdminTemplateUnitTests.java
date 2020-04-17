/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.gemfire.config.admin.remote;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.Index;
import org.apache.geode.management.api.ClusterManagementListResult;
import org.apache.geode.management.api.ClusterManagementRealizationResult;
import org.apache.geode.management.api.ClusterManagementResult;
import org.apache.geode.management.api.ClusterManagementService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.gemfire.IndexType;
import org.springframework.data.gemfire.config.schema.definitions.IndexDefinition;
import org.springframework.data.gemfire.config.schema.definitions.RegionDefinition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClusterManagementServiceGemfireAdminTemplate}.
 *
 * @author Patrick Johnson
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.query.Index
 * @see org.springframework.data.gemfire.config.admin.remote.ClusterManagementServiceGemfireAdminTemplate
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterManagementServiceGemfireAdminTemplateUnitTests {

	@Mock
	private Index mockIndex;

	@Mock
	private Region mockRegion;

	private ClusterManagementServiceGemfireAdminTemplate template;

	@Mock
	private ClusterManagementService mockClusterManagementService;

	@Before
	public void setup() {

		ClusterManagementRealizationResult clusterManagementRealizationResult = new ClusterManagementRealizationResult();
		clusterManagementRealizationResult.setStatus(ClusterManagementResult.StatusCode.OK, "");

		when(this.mockRegion.getName()).thenReturn("MockRegion");
		when(this.mockIndex.getType()).thenReturn(IndexType.FUNCTIONAL.getGemfireIndexType());
		when(this.mockIndex.getName()).thenReturn("MockIndex");
		when(this.mockIndex.getIndexedExpression()).thenReturn("age");
		when(this.mockIndex.getFromClause()).thenReturn("/Customers");
		when(this.mockClusterManagementService.create(any())).thenReturn(clusterManagementRealizationResult);
		when(this.mockClusterManagementService.list(any(org.apache.geode.management.configuration.Index.class)))
				.thenReturn(new ClusterManagementListResult<>());
		when(this.mockClusterManagementService.list(any(org.apache.geode.management.configuration.Region.class)))
				.thenReturn(new ClusterManagementListResult<>());

		this.template = new ClusterManagementServiceGemfireAdminTemplate(mockClusterManagementService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createIndexCallsClusterManagementService() {

		IndexDefinition indexDefinition = IndexDefinition.from(this.mockIndex);

		this.template.createIndex(indexDefinition);

		verify(this.mockClusterManagementService, times(1))
				.create(any(org.apache.geode.management.configuration.Index.class));
	}

	@Test
	public void createRegionCallsClusterManagementService() {

		RegionDefinition regionDefinition = RegionDefinition.from(this.mockRegion);

		this.template.createRegion(regionDefinition);

		verify(this.mockClusterManagementService, times(1))
				.create(any(org.apache.geode.management.configuration.Region.class));
	}

	@Test
	public void getAvailableServerRegionIndexesCallsClusterManagementService() {
		this.template.getAvailableServerRegionIndexes();

		verify(this.mockClusterManagementService, times(1))
				.list(any(org.apache.geode.management.configuration.Index.class));
	}

	@Test
	public void getAvailableServerRegionsCallsClusterManagementService() {

		this.template.getAvailableServerRegions();

		verify(this.mockClusterManagementService, times(1))
				.list(any(org.apache.geode.management.configuration.Region.class));
	}
}