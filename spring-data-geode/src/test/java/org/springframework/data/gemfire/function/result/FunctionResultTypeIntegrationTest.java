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
package org.springframework.data.gemfire.function.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.function.annotation.FunctionId;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.function.annotation.OnRegion;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctions;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for Function Execution Return Types.
 *
 * @author Patrick Johnson
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.data.gemfire.client.ClientRegionFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.function.annotation.OnRegion
 * @see org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions
 * @see org.springframework.data.gemfire.function.config.EnableGemfireFunctions
 * @see org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionResultTypeIntegrationTest.TestGeodeConfiguration.class)
@SuppressWarnings("unused")
public class FunctionResultTypeIntegrationTest extends ClientServerIntegrationTestsSupport {

	@Autowired
	private FunctionExecutions functionExecutions;

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

	@PeerCacheApplication(name = "FunctionResultTypeIntegrationTest")
	@EnableGemfireFunctionExecutions(basePackageClasses = FunctionExecutions.class)
	@EnableGemfireFunctions
	public static class TestGeodeConfiguration {

		@Bean("Numbers")
		protected ReplicatedRegionFactoryBean<Long, BigDecimal> numbersRegion(GemFireCache gemFireCache) {

			ReplicatedRegionFactoryBean<Long, BigDecimal> numbersRegion = new ReplicatedRegionFactoryBean<>();

			numbersRegion.setCache(gemFireCache);
			numbersRegion.setPersistent(false);

			return numbersRegion;
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
