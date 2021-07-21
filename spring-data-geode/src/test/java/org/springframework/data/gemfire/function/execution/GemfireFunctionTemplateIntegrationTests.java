/*
 * Copyright 2002-2021 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;

import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.fork.FunctionCacheServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;

/**
 * Integration Tests for Apache Geode Function templates.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 */
public class GemfireFunctionTemplateIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private ClientCache gemfireCache = null;

	private Pool gemfirePool = null;

	private final PoolResolver poolResolver = new PoolManagerPoolResolver();

	private Region<String, String> gemfireRegion = null;

	@BeforeClass
	public static void startGeodeServer() throws Exception {
		startGemFireServer(FunctionCacheServerProcess.class);
	}

	@Before
	public void setupGemFireClient() {

		this.gemfireCache = new ClientCacheFactory()
			.set("name", GemfireFunctionTemplateIntegrationTests.class.getSimpleName())
			.set("log-level", "error")
			.setPoolSubscriptionEnabled(true)
			.addPoolServer("localhost", Integer.getInteger(GEMFIRE_POOL_SERVERS_PROPERTY))
			.create();

		assertThat(this.gemfireCache).isNotNull();
		assertThat(this.gemfireCache.getName()).isEqualTo(GemfireFunctionTemplateIntegrationTests.class.getSimpleName());

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
		GemfireUtils.close(this.gemfireCache);
	}

	@Test
	public void testFunctionTemplates() {

		verifyFunctionTemplateExecution(new GemfireOnRegionFunctionTemplate(gemfireRegion));
		verifyFunctionTemplateExecution(new GemfireOnServerFunctionTemplate(gemfireCache));
		verifyFunctionTemplateExecution(new GemfireOnServerFunctionTemplate(gemfirePool));
		verifyFunctionTemplateExecution(new GemfireOnServersFunctionTemplate(gemfireCache));
		verifyFunctionTemplateExecution(new GemfireOnServersFunctionTemplate(gemfirePool));
	}

	private void verifyFunctionTemplateExecution(GemfireFunctionOperations functionTemplate) {

		Iterable<String> results = functionTemplate.execute("echoFunction", "1", "2", "3");

		int count = 1;

		for (String result : results) {
			assertThat(result).isEqualTo(String.valueOf(count++));
		}
	}
}
