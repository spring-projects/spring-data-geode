/*
 * Copyright 2010-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
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
public class ClientCacheSecurityIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGeodeServer() throws IOException {

		File serverWorkingDirectory = createDirectory(new File(new File(FileSystemUtils.WORKING_DIRECTORY,
			asDirectoryName(ClientCacheSecurityIntegrationTests.class)), UUID.randomUUID().toString()));

		List<String> arguments = new ArrayList<String>();

		org.springframework.core.io.Resource trustedKeystore = new ClassPathResource("trusted.keystore");

		arguments.add(String.format("-Dgemfire.name=%s",
			asApplicationName(ClientCacheSecurityIntegrationTests.class).concat("Server")));

		arguments.add(String.format("-Djavax.net.ssl.keyStore=%s", trustedKeystore.getFile().getAbsolutePath()));

		arguments.add(getServerContextXmlFileLocation(ClientCacheSecurityIntegrationTests.class));

		startGemFireServer(serverWorkingDirectory, ServerProcess.class, arguments.toArray(new String[arguments.size()]));

		System.setProperty("javax.net.ssl.keyStore", trustedKeystore.getFile().getAbsolutePath());
	}

	@Autowired
	@Qualifier("Example")
	private Region<String, String> example;

	@Test
	public void exampleRegionGet() {
		assertThat(String.valueOf(example.get("TestKey"))).isEqualTo("TestValue");
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
}
