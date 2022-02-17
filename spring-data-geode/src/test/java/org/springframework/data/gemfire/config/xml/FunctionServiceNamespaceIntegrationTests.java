/*
 * Copyright 2010-2022 the original author or authors.
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
 */
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link Function} configuration, declaration and registration
 * using SDG XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionContext
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class FunctionServiceNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void functionsAreRegistered() {

		assertThat(FunctionService.getRegisteredFunctions().size()).isEqualTo(2);
		assertThat(FunctionService.getFunction("function1")).isNotNull();
		assertThat(FunctionService.getFunction("function2")).isNotNull();
	}

	public static class FunctionOne implements Function<Object> {

		@Override
		public void execute(FunctionContext functionContext) { }

		@Override
		public String getId() {
			return "function1";
		}
	}

	public static class FunctionTwo implements Function<Object> {

		@Override
		public void execute(FunctionContext functionContext) { }

		@Override
		public String getId() {
			return "function2";
		}
	}
}
