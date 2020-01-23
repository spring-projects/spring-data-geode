/*
 * Copyright 2010-2020 the original author or authors.
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

package org.springframework.data.gemfire.function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.isA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.geode.cache.execute.FunctionAdapter;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.function.sample.ExceptionThrowingFunctionExecution;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * The ExceptionThrowingFunctionExecutionIntegrationTest class is a test suite of test cases testing the invocation
 * of a GemFire Function using Spring Data GemFire Function Execution Annotation support when that Function throws
 * an Exception.
 *
 * @author John Blum
 * @see org.junit.Rule
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.data.gemfire.fork.ServerProcess
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.function.sample.ExceptionThrowingFunctionExecution
 * @see org.springframework.data.gemfire.tests.process.ProcessExecutor
 * @see org.springframework.data.gemfire.tests.process.ProcessWrapper
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ExceptionThrowingFunctionExecutionIntegrationTest extends ForkingClientServerIntegrationTestsSupport {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Autowired
	private ExceptionThrowingFunctionExecution exceptionThrowingFunctionExecution;

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		String serverName = ExceptionThrowingFunctionExecutionIntegrationTest.class.getSimpleName().concat("Server");

		List<String> arguments = new ArrayList<>();

		arguments.add("-Dgemfire.name=" + serverName);
		arguments.add("-Dgemfire.log-level=error");
		arguments.add(ExceptionThrowingFunctionExecutionIntegrationTest.class.getName().replace(".", "/")
			.concat("-server-context.xml"));

		startGemFireServer(ServerProcess.class, arguments.toArray(new String[arguments.size()]));
	}

	@Test
	public void exceptionThrowingFunctionExecutionRethrowsException() {

		exception.expect(FunctionException.class);
		exception.expectCause(isA(IllegalArgumentException.class));
		exception.expectMessage(containsString("Execution of Function with ID [exceptionThrowingFunction] failed"));

		exceptionThrowingFunctionExecution.exceptionThrowingFunction();
	}

	public static class ExceptionThrowingFunction extends FunctionAdapter {

		@Override
		public String getId() {
			return "exceptionThrowingFunction";
		}
		@Override
		public void execute(final FunctionContext context) {
			context.getResultSender().sendException(new IllegalArgumentException("TEST"));
		}
	}
}
