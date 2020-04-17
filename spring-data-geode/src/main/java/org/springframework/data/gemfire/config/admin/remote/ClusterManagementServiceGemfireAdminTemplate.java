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

import org.apache.geode.management.api.ClusterManagementListResult;
import org.apache.geode.management.api.ClusterManagementRealizationResult;
import org.apache.geode.management.api.ClusterManagementService;
import org.apache.geode.management.api.ConnectionConfig;
import org.apache.geode.management.api.RestTemplateClusterManagementServiceTransport;
import org.apache.geode.management.client.ClusterManagementServiceBuilder;
import org.apache.geode.management.configuration.Index;
import org.apache.geode.management.configuration.Region;
import org.apache.geode.management.configuration.RegionType;
import org.apache.geode.management.runtime.IndexInfo;
import org.apache.geode.management.runtime.RuntimeRegionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.gemfire.IndexType;
import org.springframework.data.gemfire.config.admin.AbstractGemfireAdminOperations;
import org.springframework.data.gemfire.config.admin.GemfireAdminOperations;
import org.springframework.data.gemfire.config.schema.definitions.IndexDefinition;
import org.springframework.data.gemfire.config.schema.definitions.RegionDefinition;
import org.springframework.data.gemfire.config.support.RestTemplateConfigurer;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link ClusterManagementServiceGemfireAdminTemplate} is class implementing the {@link GemfireAdminOperations} interface,
 * extending the {@link AbstractGemfireAdminOperations} to support administrative (management) operations
 * on a VMware GemFire or Apache Geode cluster using the Management REST API.
 *
 *
 * @author Patrick Johnson
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.management.api.ClusterManagementService
 * @see org.apache.geode.management.api.ClusterManagementServiceTransport
 * @see org.apache.geode.management.api.ConnectionConfig
 * @see org.springframework.data.gemfire.config.admin.GemfireAdminOperations
 * @see org.springframework.data.gemfire.config.admin.remote.FunctionGemfireAdminTemplate
 * @see org.springframework.http.client.ClientHttpRequestInterceptor
 * @since 2.0.0
 */
public class ClusterManagementServiceGemfireAdminTemplate extends AbstractGemfireAdminOperations {

	private final ClusterManagementService clusterManagementService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClusterManagementServiceGemfireAdminTemplate(ClusterManagementService clusterManagementService) {
		this.clusterManagementService = clusterManagementService;
	}

	public ClusterManagementServiceGemfireAdminTemplate(String host, int port, boolean requireHttps, boolean followRedirects, List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors, List<RestTemplateConfigurer> restTemplateConfigurers) {
		ConnectionConfig connectionConfig = new ConnectionConfig(host, port).setFollowRedirects(followRedirects);

		if(requireHttps) {
			try {
				connectionConfig.setSslContext(SSLContext.getDefault());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		RestTemplate restTemplate = new RestTemplate();

		Optional.ofNullable(clientHttpRequestInterceptors)
				.ifPresent(restTemplate.getInterceptors()::addAll);

		CollectionUtils.nullSafeList(restTemplateConfigurers).stream()
				.filter(Objects::nonNull)
				.forEach(configurer -> configurer.configure(restTemplate));

		this.clusterManagementService = new ClusterManagementServiceBuilder()
				.setTransport(new RestTemplateClusterManagementServiceTransport(restTemplate, connectionConfig))
				.build();
	}

	@Override
	public Iterable<String> getAvailableServerRegions() {
		ClusterManagementListResult<Region, RuntimeRegionInfo> result = this.clusterManagementService.list(new Region());
		if(!result.isSuccessful()) {
			logger.warn("Failed to get regions!");
		}
		List<Region> results = result.getConfigResult();
		return results.stream().map(Region::getName).collect(Collectors.toList());
	}

	@Override
	public Iterable<String> getAvailableServerRegionIndexes() {
		ClusterManagementListResult<Index, IndexInfo> result = this.clusterManagementService.list(new Index());
		if(!result.isSuccessful()) {
			logger.warn("Failed to get indexes!");
		}
		List<Index> results = result.getConfigResult();
		return results.stream().map(Index::getName).collect(Collectors.toList());
	}

	@Override
	public void createRegion(RegionDefinition regionDefinition) {
		Region region = new Region();
		region.setName(regionDefinition.getName());
		RegionType type = RegionType.valueOf(regionDefinition.getRegionShortcut().name());
		region.setType(type);
		ClusterManagementRealizationResult result = clusterManagementService.create(region);
		if(!result.isSuccessful()) {
			logger.warn("Failed to create region!");
		}
	}

	@Override
	public void createIndex(IndexDefinition indexDefinition) {
		Index index = new Index();
		index.setRegionPath(indexDefinition.getFromClause());
		index.setName(indexDefinition.getName());
		index.setExpression(indexDefinition.getExpression());

		IndexType indexType = indexDefinition.getIndexType();
		if(indexType.isKey()) {
			index.setIndexType(org.apache.geode.management.configuration.IndexType.KEY);
		} else if(indexType.isFunctional()) {
			index.setIndexType(org.apache.geode.management.configuration.IndexType.RANGE);
		}
		else if (indexType.isHash()){
			index.setIndexType(org.apache.geode.management.configuration.IndexType.HASH_DEPRECATED);
		}

		ClusterManagementRealizationResult result = this.clusterManagementService.create(index);
		if(!result.isSuccessful()) {
			logger.warn("Failed to create index!");
		}
	}

//	@Override
//	public void createLuceneIndex(SchemaObjectDefinition luceneIndexDefinition) {
//		// not implemented in v2 api

//	}
//	@Override
//	public void createDiskStore(SchemaObjectDefinition diskStoreDefinition) {
//		// not implemented in v2 api
//	}
}