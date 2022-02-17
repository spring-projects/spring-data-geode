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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.fork.LocatorProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.data.gemfire.tests.util.FileUtils;
import org.springframework.data.gemfire.tests.util.ThrowableUtils;
import org.springframework.data.gemfire.tests.util.ZipUtils;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.util.StringUtils;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration Tests testing the integration of Spring Data for Apache Geode with Apache Geode's new shared, persistent,
 * cluster configuration service.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.data.gemfire.fork.LocatorProcess
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @since 1.5.0
 */
@SuppressWarnings("unused")
public class CacheClusterConfigurationIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private static final int ASSERTJ_MAX_STACK_TRACE_ELEMENTS = 500;

	private static File locatorWorkingDirectory;

	// The List of Strings represents each line of the Locator process output (System.out).
	private static final List<String> locatorProcessOutput = Collections.synchronizedList(new ArrayList<>());

	private static final Logger logger = LoggerFactory.getLogger(CacheClusterConfigurationIntegrationTests.class);

	private static ProcessWrapper locatorProcess;

	private static final String LOG_LEVEL = "error";
	private static final String LOG_FILE = "Locator.log";

	@Rule
	public TestRule watchman = new TestWatcher() {

		@Override
		protected void failed(Throwable throwable, Description description) {

			logger.error("Test [%s] failed...{}", description.getDisplayName());
			logger.error(ThrowableUtils.toString(throwable));
			logger.error("Locator process log file contents were...");
			logger.error(getLocatorProcessOutput(description));
		}

		@Override
		protected void finished(Description description) {

			if (Arrays.asList("config", "debug", "info").contains(LOG_LEVEL.toLowerCase())) {
				try {
					FileUtils.write(new File(locatorWorkingDirectory.getParent(),
						String.format("%s-clusterconfiglocator.log", description.getMethodName())),
							getLocatorProcessOutput(description));
				}
				catch (IllegalArgumentException | IOException cause) {
					throw newRuntimeException(cause, "Failed to write the contents of the Locator process log to a file");
				}
			}
		}

		private String getLocatorProcessOutput(Description description) {

			try {

				String locatorProcessOutputString = StringUtils.collectionToDelimitedString(locatorProcessOutput,
					FileUtils.LINE_SEPARATOR, String.format("[%s] - ", description.getMethodName()), "");

				locatorProcessOutputString = StringUtils.hasText(locatorProcessOutputString)
					? locatorProcessOutputString
					: locatorProcess.readLogFile();

				return locatorProcessOutputString;

			}
			catch (IOException cause) {
				throw newRuntimeException(cause, "Failed to read the contents of the Locator process log file");
			}
		}
	};

	@BeforeClass
	public static void configureAssertJ() {
		Assertions.setMaxStackTraceElementsDisplayed(ASSERTJ_MAX_STACK_TRACE_ELEMENTS);
	}

	@BeforeClass
	public static void startLocator() throws IOException {

		int locatorPort = findAndReserveAvailablePort();

		String locatorName = String.format("ClusterConfigLocator-%d", System.currentTimeMillis());

		locatorWorkingDirectory = createDirectory(new File(FileSystemUtils.WORKING_DIRECTORY, locatorName.toLowerCase()));

		ZipUtils.unzip(new ClassPathResource("cluster_config.zip"), locatorWorkingDirectory);

		List<String> arguments = new ArrayList<>();

		arguments.add(String.format("-Dgemfire.name=%s", locatorName));
		arguments.add(String.format("-Dlog4j.geode.log.level=%s", LOG_LEVEL));
		arguments.add(String.format("-Dlogback.log.level=%s", LOG_LEVEL));
		arguments.add("-Dspring.data.gemfire.enable-cluster-configuration=true");
		arguments.add("-Dspring.data.gemfire.load-cluster-configuration=true");
		arguments.add(String.format("-Dgemfire.log-file=%s", LOG_FILE));
		arguments.add(String.format("-Dgemfire.log-level=%s", LOG_LEVEL));
		arguments.add(String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		locatorProcess = run(locatorWorkingDirectory, LocatorProcess.class, arguments.toArray(new String[0]));
		locatorProcess.register(input -> locatorProcessOutput.add(input));
		locatorProcess.registerShutdownHook();

		waitForServerToStart("localhost", locatorPort);

		System.setProperty("spring.data.gemfire.locator.port", String.valueOf(locatorPort));
	}

	@AfterClass
	public static void stopLocator() {

		stop(locatorProcess);

		System.clearProperty("spring.data.gemfire.locator.port");

		FilenameFilter logFileFilter = (directory, name) -> name.endsWith(".log");

		File[] logFiles = ArrayUtils.nullSafeArray(locatorWorkingDirectory.listFiles(logFileFilter), File.class);

		Arrays.stream(logFiles).forEach(File::delete);

		if (Boolean.parseBoolean(System.getProperty("spring.gemfire.fork.clean", Boolean.TRUE.toString()))) {
			FileSystemUtils.deleteRecursive(locatorWorkingDirectory);
		}
	}

	private Region<?, ?> assertRegion(Region<?, ?> actualRegion, String expectedRegionName) {
		return assertRegion(actualRegion, expectedRegionName, Region.SEPARATOR+expectedRegionName);
	}

	private Region<?, ?> assertRegion(Region<?, ?> actualRegion, String expectedRegionName,
			String expectedRegionFullPath) {

		assertThat(actualRegion)
			.describedAs("The [%s] was not properly configured and initialized!", expectedRegionName)
			.isNotNull();

		assertThat(actualRegion.getName()).isEqualTo(expectedRegionName);
		assertThat(actualRegion.getFullPath()).isEqualTo(expectedRegionFullPath);

		return actualRegion;
	}

	private Region<?, ?> assertRegionAttributes(Region<?, ?> actualRegion, DataPolicy expectedDataPolicy,
			Scope expectedScope) {

		assertThat(actualRegion).isNotNull();
		assertThat(actualRegion.getAttributes()).isNotNull();
		assertThat(actualRegion.getAttributes().getDataPolicy()).isEqualTo(expectedDataPolicy);
		assertThat(actualRegion.getAttributes().getScope()).isEqualTo(expectedScope);

		return actualRegion;
	}

	private String getLocation(String configLocation) {

		String baseLocation = getClass().getPackage().getName().replace('.', File.separatorChar);

		return baseLocation.concat(File.separator).concat(configLocation);
	}

	private Region<?, ?> getRegion(ConfigurableApplicationContext applicationContext, String regionBeanName) {
		return applicationContext.getBean(regionBeanName, Region.class);
	}

	private ConfigurableApplicationContext newApplicationContext(String... configLocations) {

		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocations);

		applicationContext.registerShutdownHook();

		return applicationContext;
	}

	@Test
	@Ignore
	public void clusterConfigurationTest() {

		ConfigurableApplicationContext applicationContext =
			newApplicationContext(getLocation("cacheUsingClusterConfigurationIntegrationTest.xml"));

		assertRegionAttributes(assertRegion(getRegion(applicationContext, "ClusterConfigRegion"), "ClusterConfigRegion"),
			DataPolicy.PARTITION, Scope.DISTRIBUTED_NO_ACK);

		assertRegionAttributes(assertRegion(getRegion(applicationContext, "NativeLocalRegion"), "NativeLocalRegion"),
			DataPolicy.NORMAL, Scope.LOCAL);

		assertRegionAttributes(assertRegion(getRegion(applicationContext, "NativePartitionRegion"), "NativePartitionRegion"),
			DataPolicy.PARTITION, Scope.DISTRIBUTED_NO_ACK);

		assertRegionAttributes(assertRegion(getRegion(applicationContext, "NativeReplicateRegion"), "NativeReplicateRegion"),
			DataPolicy.REPLICATE, Scope.DISTRIBUTED_ACK);

		assertRegionAttributes(assertRegion(getRegion(applicationContext, "LocalRegion"), "LocalRegion"),
			DataPolicy.NORMAL, Scope.LOCAL);
	}

	@Test(expected = BeanCreationException.class)
	public void localConfigurationTest() {

		ConfigurableApplicationContext applicationContext = null;

		try {

			applicationContext = newApplicationContext(getLocation("cacheUsingLocalConfigurationIntegrationTest.xml"));

			fail("Loading the 'cacheUsingLocalOnlyConfigurationIntegrationTest.xml' Spring ApplicationContext"
				+ " configuration file should have resulted in an Exception due to the Region lookup on"
				+ " 'ClusterConfigRegion' when GemFire Cluster Configuration is disabled!");
		}
		catch (BeanCreationException expected) {

			assertThat(expected).hasCauseInstanceOf(BeanInitializationException.class);

			assertThat(expected.getCause().getMessage()
				.matches("Region \\[ClusterConfigRegion\\] in Cache \\[.*\\] not found"))
				.as(String.format("Message was [%s]", expected.getMessage())).isTrue();

			throw expected;
		}
		finally {
			closeApplicationContext(applicationContext);
		}
	}
}
