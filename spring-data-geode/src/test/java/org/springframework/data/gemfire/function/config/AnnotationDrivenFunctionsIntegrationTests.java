/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.function.annotation.Filter;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.function.annotation.RegionData;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for SDG Annotation-driver Apache Geode {@link Function} configuration.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class AnnotationDrivenFunctionsIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testAnnotatedFunctions() {

		assertThat(FunctionService.isRegistered("foo")).isTrue();

		Function<?> function = FunctionService.getFunction("foo");

		assertThat(function.isHA()).isFalse();
		assertThat(function.optimizeForWrite()).isFalse();
		assertThat(function.hasResult()).isFalse();
		assertThat(FunctionService.isRegistered("bar")).isTrue();

		function = FunctionService.getFunction("bar");

		assertThat(function.isHA()).isTrue();
		assertThat(function.optimizeForWrite()).isFalse();
		assertThat(function.hasResult()).isTrue();
		assertThat(FunctionService.isRegistered("foo2")).isTrue();

		function = FunctionService.getFunction("foo2");

		assertThat(function.isHA()).isTrue();
		assertThat(function.optimizeForWrite()).isTrue();
		assertThat(function.hasResult()).isTrue();
		assertThat(FunctionService.isRegistered("injectFilter")).isTrue();

		function = FunctionService.getFunction("injectFilter");

		assertThat(function.isHA()).isTrue();
		assertThat(function.optimizeForWrite()).isTrue();
		assertThat(function.hasResult()).isTrue();
	}

	@Component
	public static class FooFunction {

		@GemfireFunction
		public void foo() { }

		@GemfireFunction(HA = true, optimizeForWrite = false)
		public String bar() {
			return null;
		}
	}

	@Component
	public static class Foo2Function {

		@GemfireFunction(id = "foo2", HA = true, optimizeForWrite = true)
		public List<String> foo(Object someVal, @RegionData Map<?, ?> region, Object someOtherValue) {
			return null;
		}

		@GemfireFunction(id = "injectFilter", HA = true, optimizeForWrite = true)
		public List<String> injectFilter(@Filter Set<?> keySet) {
			return null;
		}
	}
}
