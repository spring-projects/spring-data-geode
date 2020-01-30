/*
 * Copyright 2010-2020 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.distributed.ServerLauncher;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.fork.GemFireBasedServerProcess;
import org.springframework.data.gemfire.tests.process.ProcessExecutor;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.data.gemfire.tests.util.ThreadUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * The GemFireDataSourceUsingNonSpringConfiguredGemFireServerIntegrationTest class is a test suite of test cases
 * testing the contract and functionality of the GemfireDataSourcePostProcessor using the &lt;gfe-data:datasource&gt;
 * element in Spring config to setup a GemFire ClientCache connecting to a native, non-Spring configured GemFire Server
 * as the DataSource to assert that client Region Proxies are registered as Spring beans
 * in the Spring ApplicationContext correctly.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.client.GemfireDataSourcePostProcessor
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings({ "rawtypes", "unused"})
// TODO: slow test!
// TODO: Use ForkingClientServerIntegrationTestsSupport when it supports custom classpath and working directory.
public class GemFireDataSourceUsingNonSpringConfiguredGemFireServerIntegrationTest {

	private static final String GEMFIRE_LOG_LEVEL = "error";

	private static ProcessWrapper gemfireServer;

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

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		System.setProperty("gemfire.log-level", GEMFIRE_LOG_LEVEL);

		String serverName = "GemFireDataSourceGemFireBasedServer";

		File serverWorkingDirectory = new File(FileSystemUtils.WORKING_DIRECTORY, serverName.toLowerCase());

		Assert.isTrue(serverWorkingDirectory.isDirectory() || serverWorkingDirectory.mkdirs(),
			String.format("Server working directory [%s] does not exist and could not be created", serverWorkingDirectory));

		writeAsCacheXmlFileToDirectory("gemfire-datasource-integration-test-cache.xml", serverWorkingDirectory);

		Assert.isTrue(new File(serverWorkingDirectory, "cache.xml").isFile(),
			String.format("Expected a cache.xml file to exist in directory [%s]", serverWorkingDirectory));

		List<String> arguments = new ArrayList<>(5);

		arguments.add(ServerLauncher.Command.START.getName());
		arguments.add(String.format("-Dgemfire.name=%s", serverName));
		arguments.add(String.format("-Dgemfire.log-level=%s", GEMFIRE_LOG_LEVEL));

		gemfireServer = ProcessExecutor.launch(serverWorkingDirectory, customClasspath(),
			GemFireBasedServerProcess.class, arguments.toArray(new String[0]));

		waitForProcessStart(TimeUnit.SECONDS.toMillis(20), gemfireServer,
			GemFireBasedServerProcess.getServerProcessControlFilename());
	}

	private static void writeAsCacheXmlFileToDirectory(String classpathResource, File serverWorkingDirectory) throws IOException {

		FileCopyUtils.copy(new ClassPathResource(classpathResource).getInputStream(),
			new FileOutputStream(new File(serverWorkingDirectory, "cache.xml")));
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

	private static void waitForProcessStart(long milliseconds, ProcessWrapper process, String processControlFilename) {

		ThreadUtils.timedWait(milliseconds, TimeUnit.MILLISECONDS.toMillis(500), new ThreadUtils.WaitCondition() {

			private File processControlFile = new File(process.getWorkingDirectory(), processControlFilename);

			@Override
			public boolean waiting() {
				return !processControlFile.isFile();
			}
		});
	}

	@AfterClass
	public static void stopGemFireServer() {

		gemfireServer.shutdown();

		if (Boolean.valueOf(System.getProperty("spring.gemfire.fork.clean", String.valueOf(true)))) {
			org.springframework.util.FileSystemUtils.deleteRecursively(gemfireServer.getWorkingDirectory());
		}
	}

	@SuppressWarnings("unchecked")
	private void assertRegion(Region actualRegion, String expectedRegionName) {

		Assertions.assertThat(actualRegion).isNotNull();
		Assertions.assertThat(actualRegion.getName()).isEqualTo(expectedRegionName);
		Assertions.assertThat(actualRegion.getFullPath()).isEqualTo(GemfireUtils.toRegionPath(expectedRegionName));
		Assertions.assertThat(gemfireClientCache.getRegion(actualRegion.getFullPath())).isSameAs(actualRegion);
		Assertions.assertThat(applicationContext.containsBean(expectedRegionName)).isTrue();
		Assertions.assertThat(applicationContext.getBean(expectedRegionName, Region.class)).isSameAs(actualRegion);
	}

	@Test
	public void clientProxyRegionBeansExist() {

		assertRegion(localRegion, "LocalRegion");
		assertRegion(serverRegion, "ServerRegion");
		assertRegion(anotherServerRegion, "AnotherServerRegion");
	}
}
