/*
 * Copyright 2021-2023 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.function.execution.onservers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.execute.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctions;
import org.springframework.data.gemfire.function.sample.AllServersAdminFunctions;
import org.springframework.data.gemfire.function.sample.Metric;
import org.springframework.data.gemfire.function.sample.SingleServerAdminFunctions;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the return values from {@link Function} executions.
 *
 * @author Patrick Johnson
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions
 * @see org.springframework.data.gemfire.function.config.EnableGemfireFunctions
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionsReturnResultsFromAllServersIntegrationTests.TestConfiguration.class)
public class FunctionsReturnResultsFromAllServersIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private static final int NUMBER_OF_METRICS = 10;

	private static ProcessWrapper gemfireServer1;
	private static ProcessWrapper gemfireServer2;

	@Autowired
	private AllServersAdminFunctions allServersAdminFunctions;

	@Autowired
	private SingleServerAdminFunctions singleServerAdminFunctions;

	@BeforeClass
	public static void startGemFireServer() throws Exception {

		final int port1 = findAndReserveAvailablePort();

		gemfireServer1 = run(MetricsFunctionServerConfiguration.class,
				String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, port1));

		waitForServerToStart(DEFAULT_HOSTNAME, port1);

		final int port2 = findAndReserveAvailablePort();

		gemfireServer2 = run(MetricsFunctionServerConfiguration.class,
				String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, port2));

		waitForServerToStart(DEFAULT_HOSTNAME, port2);

		System.setProperty(GEMFIRE_POOL_SERVERS_PROPERTY,
			String.format("%s[%d],%s[%d]", DEFAULT_HOSTNAME, port1, DEFAULT_HOSTNAME, port2));
	}

	@AfterClass
	public static void stopGemFireServer() {
		stop(gemfireServer1);
		stop(gemfireServer2);
	}

	@Test
	public void executeFunctionOnAllServers() {

		List<List<Metric>> metrics = allServersAdminFunctions.getAllMetrics();

		assertThat(metrics.size()).isEqualTo(2);
		assertThat(metrics.get(0).size()).isEqualTo(NUMBER_OF_METRICS);
		assertThat(metrics.get(1).size()).isEqualTo(NUMBER_OF_METRICS);
	}

	@Test
	public void executeFunctionOnSingleServer() {

		List<Metric> metrics = singleServerAdminFunctions.getAllMetrics();

		assertThat(metrics.size()).isEqualTo(NUMBER_OF_METRICS);
	}

	@ClientCacheApplication
	@EnableGemfireFunctionExecutions(basePackageClasses = AllServersAdminFunctions.class)
	static class TestConfiguration { }

	@CacheServerApplication
	@EnableGemfireFunctions
	public static class MetricsFunctionServerConfiguration {

		public static void main(String[] args) {
			runSpringApplication(MetricsFunctionServerConfiguration.class, args);
		}

		@GemfireFunction(id = "GetAllMetricsFunction", hasResult = true)
		public List<Metric> getMetrics() {

			List<Metric> allMetrics = new ArrayList<>();

			for (int i = 0; i < NUMBER_OF_METRICS; i++) {
				Metric metric = new Metric("statName" + i, i, "statCat" + i, "statType" + i);
				allMetrics.add(metric);
			}

			return allMetrics;
		}
	}
}
