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

package org.springframework.data.gemfire.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * The ClientCacheVariableServersTest class is a test suite of test cases testing the use of variable "servers"
 * attribute on &lt;gfe:pool/&lt; in Spring (Data GemFire) configuration meta-data when connecting a client/server.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.6.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@SuppressWarnings("all")
public class ClientCacheVariableServersTest extends ForkingClientServerIntegrationTestsSupport {

	@Resource(name = "Example")
	private Region<String, Integer> example;

	@BeforeClass
	public static void setup() throws IOException {
		String serverName = ClientCacheVariableServersTest.class.getSimpleName().concat("Server");

		List<String> arguments = new ArrayList<String>();

		arguments.add(String.format("-Dgemfire.name=%1$s", serverName));
		arguments.add("/".concat(ClientCacheVariableServersTest.class.getName().replace(".", "/")
			.concat("-server-context.xml")));

		startGemFireServer(ServerProcess.class, arguments.toArray(new String[arguments.size()]));
	}

	@Test
	public void clientServerConnectionSuccessful() {
		assertThat(example.get("one"), is(equalTo(1)));
		assertThat(example.get("two"), is(equalTo(2)));
		assertThat(example.get("three"), is(equalTo(3)));
	}

	public static class CacheMissCounterCacheLoader implements CacheLoader<String, Integer> {

		private static final AtomicInteger cacheMissCounter = new AtomicInteger(0);

		@Override
		public Integer load(final LoaderHelper<String, Integer> helper) throws CacheLoaderException {
			return cacheMissCounter.incrementAndGet();
		}

		@Override
		public void close() {
			cacheMissCounter.set(0);
		}
	}

}
