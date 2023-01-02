/*
 * Copyright 2002-2023 the original author or authors.
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
package org.springframework.data.gemfire.function.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.fork.FunctionCacheServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.util.SpringExtensions;

/**
 * Integration Tests for SDG Function support.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 */
public class FunctionExecutionIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private ClientCache gemfireCache = null;

	private Pool gemfirePool = null;

	private final PoolResolver poolResolver = new PoolManagerPoolResolver();

	private Region<String, String> gemfireRegion = null;

	@BeforeClass
	public static void startGemFireServer() throws Exception {
		startGemFireServer(FunctionCacheServerProcess.class);
	}

	@Before
	public void setupGemFireClient() {

		this.gemfireCache = new ClientCacheFactory()
			.set("name", FunctionExecutionIntegrationTests.class.getSimpleName())
			.set("log-level", "error")
			.setPoolSubscriptionEnabled(true)
			.addPoolServer("localhost", Integer.getInteger(GEMFIRE_CACHE_SERVER_PORT_PROPERTY))
			.create();

		assertThat(this.gemfireCache).isNotNull();
		assertThat(this.gemfireCache.getName()).isEqualTo(FunctionExecutionIntegrationTests.class.getSimpleName());

		this.gemfireRegion = this.gemfireCache.<String, String>createClientRegionFactory(ClientRegionShortcut.PROXY)
			.create("test-function");

		assertThat(this.gemfireRegion).isNotNull();
		assertThat(this.gemfireRegion.getName()).isEqualTo("test-function");

		this.gemfirePool = this.poolResolver.resolve("DEFAULT");

		assertThat(this.gemfirePool).isNotNull();
		assertThat(this.gemfirePool.getName()).isEqualTo("DEFAULT");
	}

	@After
	public void tearDownGemFireClient() {
		Optional.ofNullable(this.gemfireCache).ifPresent(cache -> SpringExtensions.safeDoOperation(() -> cache.close()));
	}

	@Test
	public void basicFunctionExecutionsAreCorrect() {

		verifyFunctionExecution(new OnServerUsingPoolFunctionExecution(gemfirePool));
		verifyFunctionExecution(new OnRegionFunctionExecution(gemfireRegion));
		verifyFunctionExecution(new OnServerUsingRegionServiceFunctionExecution(gemfireCache));
		verifyFunctionExecution(new OnServersUsingRegionServiceFunctionExecution(gemfireCache));
	}

	private void verifyFunctionExecution(AbstractFunctionExecution functionExecution) {

		Iterable<String> results = functionExecution
			.setArguments("1", "2", "3")
			.setFunctionId("echoFunction")
			.execute();

		int count = 1;

		for (String result : results) {
			assertThat(result).isEqualTo(String.valueOf(count++));
		}
	}
}
