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

import javax.annotation.Resource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests to test SSL configuration between a Pivotal GemFire or Apache Geode client and server
 * using GemFire/Geode System properties.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("all")
public class ClientCacheSecurityTest extends ForkingClientServerIntegrationTestsSupport {

	@Resource(name = "Example")
	private Region<String, String> example;

	@BeforeClass
	public static void setup() throws IOException {

		String serverName = "GemFireSecurityCacheServer";

		List<String> arguments = new ArrayList<String>();

		arguments.add(String.format("-Dgemfire.name=%1$s", serverName));
		arguments.add(String.format("-Djavax.net.ssl.keyStore=%1$s", System.getProperty("javax.net.ssl.keyStore")));
		arguments.add("/org/springframework/data/gemfire/client/ClientCacheSecurityTest-server-context.xml");

		startGemFireServer(ServerProcess.class, arguments.toArray(new String[arguments.size()]));
	}

	@Test
	public void exampleRegionGet() {
		assertThat(String.valueOf(example.get("TestKey")), is(equalTo("TestValue")));
	}

	@SuppressWarnings("unused")
	public static class TestCacheLoader implements CacheLoader<String, String> {

		@Override
		public String load(LoaderHelper<String, String> helper) throws CacheLoaderException {
			return "TestValue";
		}

		@Override
		public void close() { }
	}

	public static class SslGemFireServer {

		public static void main(String[] args) {

			String configLocation = "org/springframework/data/gemfire/client/ClientCacheSecurityTest-server-context.xml";

			ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocation);

			applicationContext.registerShutdownHook();
		}
	}
}
