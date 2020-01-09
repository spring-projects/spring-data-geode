/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.data.gemfire.function.result;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.function.annotation.FunctionId;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.function.annotation.OnRegion;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctions;
import org.springframework.data.gemfire.process.ProcessWrapper;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.transaction.config.EnableGemfireCacheTransactions;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Patrick Johnson
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionResultTypeIntegrationTest.FunctionInvocationClientApplicationConfig.class)
public class FunctionResultTypeIntegrationTest extends ClientServerIntegrationTestsSupport {

	private static ProcessWrapper server;

	@Autowired
	private FunctionExecutions functionExecutions;

	@BeforeClass
	public static void startServer() throws IOException {

		final String GEMFIRE_LOCALHOST_PORT = "localhost[%d]";

		int availablePort = findAvailablePort();

		System.setProperty(GEMFIRE_CACHE_SERVER_PORT_PROPERTY, String.valueOf(availablePort));
		System.setProperty(GEMFIRE_POOL_SERVERS_PROPERTY, String.format(GEMFIRE_LOCALHOST_PORT, availablePort));

		server = run(FunctionServerApplicationConfig.class,
				String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, availablePort));

		waitForServerToStart(DEFAULT_HOSTNAME, availablePort);
	}

	@AfterClass
	public static void stopServer() {
		server.stop();
		server = null;
	}

	@Test
	public void singleResultFunctionsExecuteCorrectly() {
		BigDecimal num = functionExecutions.returnFive();
		assertThat(num.doubleValue()).isEqualTo(5);
	}

	@Test
	public void listResultFunctionsExecuteCorrectly() {
		List<BigDecimal> list = functionExecutions.returnList();
		assertThat(list.size()).isEqualTo(1);
	}

	@Test
	public void primitiveResultFunctionsExecuteCorrectly() {
		int num = functionExecutions.returnPrimitive();
		assertThat(num).isEqualTo(7);
	}

	@EnableGemfireFunctionExecutions(basePackageClasses = FunctionExecutions.class)
	@ClientCacheApplication(name = "SingleResultFunctionExecutionTest")
	@EnableGemfireCacheTransactions
	public static class FunctionInvocationClientApplicationConfig {

		@Bean("Numbers")
		protected ClientRegionFactoryBean<Long, BigDecimal> configureProxyClientNumberRegion(GemFireCache gemFireCache) {
			ClientRegionFactoryBean<Long, BigDecimal> clientRegionFactoryBean = new ClientRegionFactoryBean<>();
			clientRegionFactoryBean.setCache(gemFireCache);
			clientRegionFactoryBean.setName("Numbers");
			clientRegionFactoryBean.setShortcut(ClientRegionShortcut.PROXY);
			return clientRegionFactoryBean;
		}
	}

	@EnableGemfireFunctions
	@CacheServerApplication(name = "SingleResultFunctionExecutionTestServer")
	public static class FunctionServerApplicationConfig {

		public static void main(String[] args) {

			AnnotationConfigApplicationContext applicationContext =
					new AnnotationConfigApplicationContext(FunctionServerApplicationConfig.class);

			applicationContext.registerShutdownHook();
		}

		@Bean
		ReplicatedRegionFactoryBean<Long, BigDecimal> createNumberRegion(GemFireCache gemfireCache) {
			ReplicatedRegionFactoryBean replicatedRegionFactoryBean = new ReplicatedRegionFactoryBean();
			replicatedRegionFactoryBean.setCache(gemfireCache);
			replicatedRegionFactoryBean.setRegionName("Numbers");
			replicatedRegionFactoryBean.setDataPolicy(DataPolicy.REPLICATE);
			return replicatedRegionFactoryBean;
		}

		@GemfireFunction(id = "returnSingleObject", hasResult = true)
		public BigDecimal returnSingleObject() {
			return new BigDecimal(5);
		}

		@GemfireFunction(id = "returnList", hasResult = true)
		public List<BigDecimal> returnList() {
			return Collections.singletonList(new BigDecimal(10));
		}

		@GemfireFunction(id = "returnPrimitive", hasResult = true)
		public int returnPrimitive() {
			return 7;
		}
	}
}

@OnRegion(region = "Numbers")
interface FunctionExecutions {

	@FunctionId("returnSingleObject")
	BigDecimal returnFive();

	@FunctionId("returnList")
	List<BigDecimal> returnList();

	@FunctionId("returnPrimitive")
	int returnPrimitive();
}
