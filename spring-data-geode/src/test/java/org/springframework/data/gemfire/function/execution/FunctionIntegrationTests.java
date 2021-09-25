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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.data.gemfire.function.annotation.RegionData;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for SDG Function support.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class FunctionIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGemFireServer() throws Exception {
		startGemFireServer(ServerProcess.class,
			getServerContextXmlFileLocation(FunctionIntegrationTests.class));
	}

	@Autowired
	@Qualifier("TestRegion")
	private Region<String, Integer> region;

	@Before
	public void initializeRegion() {

		this.region.put("one", 1);
		this.region.put("two", 2);
		this.region.put("three", 3);
	}

	@Test
	public void withVoidReturnType() {

		GemfireOnRegionOperations template = new GemfireOnRegionFunctionTemplate(this.region);

		// Should work either way but the first invocation traps an exception if there is a result.
		template.executeWithNoResult("noResult");
		template.execute("noResult");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCollectionReturnTypes() {
		GemfireOnRegionOperations template = new GemfireOnRegionFunctionTemplate(region);

		Object result = template.executeAndExtract("getMapWithNoArgs");

		assertThat(result instanceof Map).as(result.getClass().getName()).isTrue();

		Map<String, Integer> map = (Map<String, Integer>) result;

		assertThat(map.get("one").intValue()).isEqualTo(1);
		assertThat(map.get("two").intValue()).isEqualTo(2);
		assertThat(map.get("three").intValue()).isEqualTo(3);

		result = template.executeAndExtract("collections", Arrays.asList(1, 2, 3, 4, 5));

		assertThat(result instanceof List).as(result.getClass().getName()).isTrue();

		List<?> list = (List<?>) result;

		assertThat(list.isEmpty()).isFalse();
		assertThat(list.size()).isEqualTo(5);

		int expectedNumber = 1;

		for (Object actualNumber : list) {
			assertThat(actualNumber).isEqualTo(expectedNumber++);
		}
	}

	@Test
	@SuppressWarnings("all")
	public void testArrayReturnTypes() {

		Object result = new GemfireOnRegionFunctionTemplate(this.region)
			.executeAndExtract("arrays", new int[] { 1, 2, 3, 4, 5 });

		assertThat(result instanceof int[]).as(result.getClass().getName()).isTrue();
		assertThat(((int[]) result).length).isEqualTo(5);
	}

	@Test
	//@Ignore
	public void testOnRegionFunctionExecution() {

		GemfireOnRegionOperations template = new GemfireOnRegionFunctionTemplate(this.region);

		assertThat(template.<Integer>execute("oneArg", "two").iterator().next().intValue()).isEqualTo(2);
		assertThat(template.<Integer>execute("oneArg", Collections.singleton("one"), "two").iterator().hasNext())
			.isFalse();
		assertThat(template.<Integer>execute("twoArg", "two", "three").iterator().next().intValue()).isEqualTo(5);
		assertThat(template.<Integer>executeAndExtract("twoArg", "two", "three").intValue()).isEqualTo(5);
	}

	/**
	 * This {@link Component} class gets wrapped in an Apache Geode {@link Function} and registered on the forked server.
	 */
	@Component
	@SuppressWarnings("unused")
	public static class Foo {

		@GemfireFunction(id = "oneArg")
		public Integer oneArg(String key, @RegionData Map<String, Integer> region) {
			return region.get(key);
		}

		@GemfireFunction(id = "twoArg")
		public Integer twoArg(String keyOne, String keyTwo, @RegionData Map<String, Integer> region) {

			if (region.get(keyOne) != null && region.get(keyTwo) != null) {
				return region.get(keyOne) + region.get(keyTwo);
			}

			return null;
		}

		@GemfireFunction(id = "collections")
		public List<Integer> collections(List<Integer> args) {
			return args;
		}

		@GemfireFunction(id = "getMapWithNoArgs")
		public Map<String, Integer> getMapWithNoArgs(@RegionData Map<String, Integer> region) {

			if (region.size() == 0) {
				return null;
			}

			return new HashMap<>(region);
		}

		@GemfireFunction(id = "arrays")
		// TODO causes OOME!
		//@GemfireFunction(id = "arrays", batchSize = 2)
		public int[] collections(int[] args) {
			return args;
		}

		@GemfireFunction
		public void noResult() { }

	}
}
