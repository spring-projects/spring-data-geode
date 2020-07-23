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

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.query.Index;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.IndexFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableHttpService;
import org.springframework.data.gemfire.config.annotation.EnableManager;
import org.springframework.data.gemfire.config.annotation.LocatorApplication;
import org.springframework.data.gemfire.config.schema.definitions.IndexDefinition;
import org.springframework.data.gemfire.config.schema.definitions.RegionDefinition;
import org.springframework.data.gemfire.process.ProcessWrapper;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClusterManagementServiceGemfireAdminTemplate}.
 *
 * @author Patrick Johnson
 * @see Test
 * @see Region
 * @see Index
 * @see ClusterManagementServiceGemfireAdminTemplate
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ClusterManagementServiceGemfireAdminTemplateIntegrationTests.TestGeodeClientApplication.class)
public class ClusterManagementServiceGemfireAdminTemplateIntegrationTests extends ClientServerIntegrationTestsSupport {

	@Resource(name = "stringsIndex")
	private Index index;

	@Resource(name = "stringsRegion")
	private Region<Long, String> region;

	private static ClusterManagementServiceGemfireAdminTemplate template;

	private static ProcessWrapper locatorProcess, serverProcess;

	@BeforeClass
	public static void startCluster() throws IOException {

		int locatorPort = findAvailablePort();

		locatorProcess = run(TestLocatorApplication.class,
				String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		waitForServerToStart("localhost", locatorPort);

		int serverPort = findAvailablePort();

		serverProcess = run(TestGeodeServerApplication.class,
				String.format("-Dspring.data.gemfire.locators=localhost[%1$d] -Dspring.data.gemfire.cache.server.port=%2$d",
						locatorPort, serverPort));

		waitForServerToStart("localhost", serverPort);

		System.setProperty("spring.data.gemfire.pool.locators",
				String.format("localhost[%d]", locatorPort));

		template = new ClusterManagementServiceGemfireAdminTemplate("localhost", 7070,
				false, false, Collections.emptyList(), Collections.emptyList());
	}

	@AfterClass
	public static void stopCluster() {
		stop(serverProcess);
		stop(locatorProcess);
		System.clearProperty("spring.data.gemfire.pool.locators");
	}

	@Test
	public void testRegionCreation() {
		assertThat(template.getAvailableServerRegions()).isEmpty();

		RegionDefinition regionDefinition = RegionDefinition.from(region);

		template.createRegion(regionDefinition);

		List<String> regions = new LinkedList<>();
		template.getAvailableServerRegions().forEach(regions::add);
		assertThat(regions.size()).isEqualTo(1);
		assertThat(regions).contains(region.getName());
	}

	@Test
	public void testIndexCreation() {

		assertThat(template.getAvailableServerRegionIndexes()).isEmpty();

		IndexDefinition indexDefinition = IndexDefinition.from(index);

		template.createIndex(indexDefinition);

		List<String> indexes = new LinkedList<>();
		template.getAvailableServerRegionIndexes().forEach(indexes::add);
		assertThat(indexes.size()).isEqualTo(1);
		assertThat(indexes).contains(index.getName());
	}

	@Configuration
	@CacheServerApplication
	static class TestGeodeServerApplication {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
					new AnnotationConfigApplicationContext(TestGeodeServerApplication.class);

			applicationContext.registerShutdownHook();
		}

	}

	@Configuration
	@ClientCacheApplication
	static class TestGeodeClientApplication {

		@Bean("stringsRegion")
		protected ClientRegionFactoryBean<Long, String> createStringsRegion(GemFireCache gemFireCache) {
			ClientRegionFactoryBean<Long,String> clientRegionFactoryBean = new ClientRegionFactoryBean<>();
			clientRegionFactoryBean.setCache(gemFireCache);
			clientRegionFactoryBean.setName("strings");
			clientRegionFactoryBean.setShortcut(ClientRegionShortcut.PROXY);
			return clientRegionFactoryBean;
		}

		@Bean("stringsIndex")
		protected IndexFactoryBean createStringsIndex(@Qualifier("stringsRegion") Region<Long, String> strings) {
			IndexFactoryBean indexFactoryBean = new IndexFactoryBean();
			indexFactoryBean.setCache(strings.getRegionService());
			indexFactoryBean.setName("stringsIndex");
			indexFactoryBean.setExpression("key");
			indexFactoryBean.setFrom("/strings");
			return indexFactoryBean;
		}
	}

	@Configuration
	@LocatorApplication
	@EnableHttpService(bindAddress = "localhost")
	@EnableManager
	static class TestLocatorApplication {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
					new AnnotationConfigApplicationContext(TestLocatorApplication.class);

			applicationContext.registerShutdownHook();

			new Scanner(System.in).nextLine();
		}
	}
}