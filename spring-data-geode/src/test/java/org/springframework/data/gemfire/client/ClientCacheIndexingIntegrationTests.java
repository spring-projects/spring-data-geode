/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.query.Index;
import org.apache.geode.cache.query.IndexType;
import org.apache.geode.cache.query.QueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for testing {@link ClientCache} {@link Index Indexes}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.query.Index
 * @see org.apache.geode.cache.query.QueryService
 * @see org.springframework.data.gemfire.IndexFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.5.2
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ClientCacheIndexingIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@Autowired
	private ClientCache clientCache;

	@Autowired
	private Index exampleIndex;

	@BeforeClass
	public static void startGeodeServer() throws IOException {

		File serverWorkingDirectory = createDirectory(new File(new File(FileSystemUtils.WORKING_DIRECTORY,
			asDirectoryName(ClientCacheIndexingIntegrationTests.class)), UUID.randomUUID().toString()));

		startGemFireServer(serverWorkingDirectory, ServerProcess.class,
			getServerContextXmlFileLocation(ClientCacheIndexingIntegrationTests.class));
	}

	private Index getIndex(GemFireCache gemfireCache, String indexName) {

		QueryService queryService = gemfireCache instanceof ClientCache
			? ((ClientCache) gemfireCache).getLocalQueryService()
			: gemfireCache.getQueryService();

		for (Index index : queryService.getIndexes()) {
			if (index.getName().equals(indexName)) {
				return index;
			}
		}

		return null;
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testIndexByName() {

		assertThat(clientCache)
			.describedAs("The GemFire ClientCache was not properly configured and initialized")
			.isNotNull();

		Index actualIndex = getIndex(clientCache, "ExampleIndex");

		assertThat(actualIndex).isNotNull();
		assertThat(actualIndex.getName()).isEqualTo("ExampleIndex");
		assertThat(actualIndex.getType()).isEqualTo(IndexType.HASH);
		assertThat(actualIndex).isSameAs(exampleIndex);
	}
}
