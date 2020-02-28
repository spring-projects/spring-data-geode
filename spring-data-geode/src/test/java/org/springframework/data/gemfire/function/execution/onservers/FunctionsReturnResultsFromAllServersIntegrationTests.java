/*
 * Copyright 2021 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.StatisticDescriptor;
import org.apache.geode.Statistics;
import org.apache.geode.StatisticsType;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.statistics.StatisticsManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.process.ProcessWrapper;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Patrick Johnson
 */
@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionsReturnResultsFromAllServersIntegrationTests.GeodeClientConfiguration.class)
@Ignore
public class FunctionsReturnResultsFromAllServersIntegrationTests extends ClientServerIntegrationTestsSupport {

	private static final int PORT_1 = 40407;
	private static final int PORT_2 = 40403;

	private static ProcessWrapper gemfireServer1;
	private static ProcessWrapper gemfireServer2;

	@Autowired
	private AllServersAdminFunctions allServersAdminFunctions;

	@Autowired
	private SingleServerAdminFunctions singleServerAdminFunctions;

	@BeforeClass
	public static void startGemFireServer() throws Exception {

		gemfireServer1 = run(MetricsFunctionServerProcess.class,
				String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, PORT_1));

		waitForServerToStart(DEFAULT_HOSTNAME, PORT_1);

		gemfireServer2 = run(MetricsFunctionServerProcess.class,
				String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, PORT_2));

		waitForServerToStart(DEFAULT_HOSTNAME, PORT_2);
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
	}

	@Test
	public void executeFunctionOnSingleServer() {
		List<Metric> metrics = singleServerAdminFunctions.getAllMetrics();
		assertThat(metrics.size()).isEqualTo(672);
	}

	@ClientCacheApplication(servers = {
		@ClientCacheApplication.Server(port = PORT_1),
		@ClientCacheApplication.Server(port = PORT_2)}
	)
	@EnableGemfireFunctionExecutions(basePackageClasses = AllServersAdminFunctions.class)
	static class GeodeClientConfiguration { }

	static class MetricsFunctionServerProcess {

		private static final int DEFAULT_CACHE_SERVER_PORT = 40404;

		private static final String CACHE_SERVER_PORT_PROPERTY = "spring.data.gemfire.cache.server.port";
		private static final String GEMFIRE_NAME = "MetricsServer" + getCacheServerPort();

		public static void main(String[] args) throws Exception {
			registerFunctions(startCacheServer(newGemFireCache()));
		}

		private static Cache newGemFireCache() {

			return new CacheFactory()
					.set("name", GEMFIRE_NAME)
					.create();
		}

		private static Cache startCacheServer(Cache gemfireCache) throws IOException {
			CacheServer cacheServer = gemfireCache.addCacheServer();
			cacheServer.setPort(getCacheServerPort());
			cacheServer.start();
			return gemfireCache;
		}

		private static int getCacheServerPort() {
			return Integer.getInteger(CACHE_SERVER_PORT_PROPERTY, DEFAULT_CACHE_SERVER_PORT);
		}

		private static Cache registerFunctions(Cache gemfireCache) {

			FunctionService.registerFunction(new GetAllMetricsFunction());

			return gemfireCache;
		}
	}

	static class GetAllMetricsFunction implements Function<List<Metric>> {

		private final InternalDistributedSystem system =
				(InternalDistributedSystem) CacheFactory.getAnyInstance().getDistributedSystem();

		@Override
		public void execute(FunctionContext context) {
			List<Metric> allMetrics = new ArrayList<>();
			StatisticsManager statisticsManager = system.getStatisticsManager();
			for (Statistics statistics : statisticsManager.getStatsList()) {
				StatisticsType statisticsType = statistics.getType();
				for (StatisticDescriptor descriptor : statisticsType.getStatistics()) {
					String statName = descriptor.getName();
					Metric metric = new Metric(statName, statistics.get(statName), statisticsType.getName(), statistics.getTextId());
					allMetrics.add(metric);
				}
			}
			context.getResultSender().lastResult(allMetrics);
		}

		@Override
		public String getId() {
			return getClass().getSimpleName();
		}
	}
}
