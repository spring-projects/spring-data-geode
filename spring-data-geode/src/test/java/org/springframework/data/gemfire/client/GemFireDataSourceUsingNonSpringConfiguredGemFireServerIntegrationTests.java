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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.distributed.ServerLauncher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.fork.GemFireBasedServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.process.ProcessExecutor;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Integration Tests with test cases testing the contract and functionality of the {@link GemfireDataSourcePostProcessor}
 * using the &lt;gfe-data:datasource&gt; element in Spring config to setup a {@link ClientCache} connecting to a native,
 * non-Spring configured Apache Geode Server as the {@link DataSource} to assert that client {@link Region} proxies
 * are registered as Spring beans in the Spring {@link ApplicationContext} correctly.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.client.GemfireDataSourcePostProcessor
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings({ "rawtypes", "unused"})
public class GemFireDataSourceUsingNonSpringConfiguredGemFireServerIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	private static ProcessWrapper gemfireServer;

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		String serverName =
			GemFireDataSourceUsingNonSpringConfiguredGemFireServerIntegrationTests.class.getSimpleName() + "Server";

		int serverPort =findAvailablePort();

		System.setProperty("CACHE_SERVER_PORT", String.valueOf(serverPort));

		File serverWorkingDirectory = new File(FileSystemUtils.WORKING_DIRECTORY, serverName.toLowerCase());

		assertThat(serverWorkingDirectory.isDirectory() || serverWorkingDirectory.mkdirs())
			.describedAs("Server working directory [%s] does not exist and could not be created", serverWorkingDirectory)
			.isTrue();

		writeAsCacheXmlFileToDirectory("gemfire-datasource-integration-tests-cache.xml",
			serverWorkingDirectory);

		Assert.isTrue(new File(serverWorkingDirectory, "cache.xml").isFile(),
			String.format("Expected a cache.xml file to exist in directory [%s]", serverWorkingDirectory));

		List<String> arguments = new ArrayList<>(5);

		arguments.add(ServerLauncher.Command.START.getName());
		arguments.add(String.format("-Dgemfire.name=%s", serverName));
		arguments.add(String.format("-DCACHE_SERVER_PORT=%d", serverPort));

		gemfireServer = run(serverWorkingDirectory, customClasspath(),
			GemFireBasedServerProcess.class, arguments.toArray(new String[0]));

		waitForServerToStart("localhost", serverPort);
	}

	private static String customClasspath() {

		String[] classpathElements = ProcessExecutor.JAVA_CLASSPATH.split(File.pathSeparator);

		List<String> customClasspath = new ArrayList<>(classpathElements.length);

		for (String classpathElement : classpathElements) {
			if (!classpathElement.contains("spring-data-gemfire")) {
				customClasspath.add(classpathElement);
			}
		}

		return StringUtils.collectionToDelimitedString(customClasspath, File.pathSeparator);
	}

	private static void writeAsCacheXmlFileToDirectory(String classpathResource, File serverWorkingDirectory) throws IOException {

		FileCopyUtils.copy(new ClassPathResource(classpathResource).getInputStream(),
			new FileOutputStream(new File(serverWorkingDirectory, "cache.xml")));
	}

	@AfterClass
	public static void stopGemFireServer() {

		stop(gemfireServer);

		System.clearProperty("CACHE_SERVER_PORT");

		if (Boolean.parseBoolean(System.getProperty("spring.gemfire.fork.clean", String.valueOf(true)))) {
			org.springframework.util.FileSystemUtils.deleteRecursively(gemfireServer.getWorkingDirectory());
		}
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ClientCache gemfireClientCache;

	@Resource(name = "LocalRegion")
	private Region localRegion;

	@Resource(name = "ServerRegion")
	private Region serverRegion;

	@Resource(name = "AnotherServerRegion")
	private Region anotherServerRegion;

	@SuppressWarnings("unchecked")
	private void assertRegion(Region actualRegion, String expectedRegionName) {

		assertThat(actualRegion).isNotNull();
		assertThat(actualRegion.getName()).isEqualTo(expectedRegionName);
		assertThat(actualRegion.getFullPath()).isEqualTo(GemfireUtils.toRegionPath(expectedRegionName));
		assertThat(gemfireClientCache.getRegion(actualRegion.getFullPath())).isSameAs(actualRegion);
		assertThat(applicationContext.containsBean(expectedRegionName)).isTrue();
		assertThat(applicationContext.getBean(expectedRegionName, Region.class)).isSameAs(actualRegion);
	}

	@Test
	public void clientProxyRegionBeansExist() {

		assertRegion(localRegion, "LocalRegion");
		assertRegion(serverRegion, "ServerRegion");
		assertRegion(anotherServerRegion, "AnotherServerRegion");
	}
}
