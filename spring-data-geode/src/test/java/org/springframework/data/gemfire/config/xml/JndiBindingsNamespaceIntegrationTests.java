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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.datasource.GemFireBasicDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for Apache Geode JNDI context bindings.
 *
 * This test requires a real cache
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class JndiBindingsNamespaceIntegrationTests extends IntegrationTestsSupport {

	@AfterClass
	public static void cleanupAfterTests() {
		FileSystemUtils.deleteRecursive(new File(FileSystemUtils.WORKING_DIRECTORY, "newDB"));
		FileSystemUtils.newFile(FileSystemUtils.WORKING_DIRECTORY, "derby.log").delete();
	}

	@Autowired
	private Cache cache;

	@Test
	public void testJndiBindings() throws Exception {

		Object dataSourceObject = cache.getJNDIContext().lookup("java:/SimpleDataSource");

		assertThat(dataSourceObject).isInstanceOf(GemFireBasicDataSource.class);

		GemFireBasicDataSource dataSource = (GemFireBasicDataSource) dataSourceObject;

		assertThat(dataSource.getJDBCDriver()).isEqualTo("org.apache.derby.jdbc.EmbeddedDriver");
		assertThat(dataSource.getLoginTimeout()).isEqualTo(60);
	}
}
