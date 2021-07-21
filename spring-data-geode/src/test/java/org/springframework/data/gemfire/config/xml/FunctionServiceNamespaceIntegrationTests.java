/*
 * Copyright 2010-2021 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for SDG Function XML namespace configuration metadata
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionContext
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations="function-service-ns.xml",
	initializers= GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class FunctionServiceNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void testFunctionsRegistered() {

		assertEquals(2, FunctionService.getRegisteredFunctions().size());
		assertNotNull(FunctionService.getFunction("function1"));
		assertNotNull(FunctionService.getFunction("function2"));
	}

	public static class Function1 implements Function<Object> {

		@Override
		public void execute(FunctionContext functionContext) { }

		@Override
		public String getId() {
			return "function1";
		}
	}

	public static class Function2 implements Function<Object> {

		@Override
		public void execute(FunctionContext functionContext) { }

		@Override
		public String getId() {
			return "function2";
		}
	}
}
