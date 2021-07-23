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
package org.springframework.data.gemfire.function.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.distributed.DistributedMember;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.gemfire.function.execution.GemfireOnServerFunctionTemplate;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link Function} {@link Execution} on {@link ClientCache}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ClientCacheTestConfiguration.class)
public class FunctionExecutionClientCacheIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	ApplicationContext applicationContext;

	@Test
	public void contextCreated() {

		ClientCache cache = this.applicationContext.getBean("gemfireCache", ClientCache.class);

		Pool pool = this.applicationContext.getBean("gemfirePool", Pool.class);

		assertThat(pool.getName()).isEqualTo("gemfirePool");
		assertThat(cache.getDefaultPool().getLocators().isEmpty()).isTrue();
		assertThat(cache.getDefaultPool().getServers().size()).isEqualTo(1);
		assertThat(pool.getLocators().isEmpty()).isTrue();
		assertThat(pool.getServers().size()).isEqualTo(1);
		assertThat(cache.getDefaultPool().getServers().get(0)).isEqualTo(pool.getServers().get(0));

		Region<?, ?> region = this.applicationContext.getBean("r1", Region.class);

		assertThat(region.getName()).isEqualTo("r1");
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getPoolName()).isNull();

		GemfireOnServerFunctionTemplate template = this.applicationContext.getBean(GemfireOnServerFunctionTemplate.class);

		assertThat(template.getResultCollector() instanceof MyResultCollector).isTrue();
	}
}

@Configuration
@ImportResource("/org/springframework/data/gemfire/function/config/FunctionExecutionCacheClientTests-context.xml")
@EnableGemfireFunctionExecutions(basePackages = "org.springframework.data.gemfire.function.config.three")
@SuppressWarnings("unused")
class ClientCacheTestConfiguration {

	@Bean
	MyResultCollector myResultCollector() {
		return new MyResultCollector();
	}
}

@SuppressWarnings("rawtypes")
class MyResultCollector implements ResultCollector {

	@Override
	public void addResult(DistributedMember arg0, Object arg1) { }

	@Override
	public void clearResults() { }

	@Override
	public void endResults() { }

	@Override
	public Object getResult() throws FunctionException {
		return null;
	}

	@Override
	public Object getResult(long arg0, TimeUnit arg1) throws FunctionException {
		return null;
	}
}
