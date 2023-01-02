/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.data.gemfire;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;

/**
 * Abstract base test class that creates the Spring {@link ConfigurableApplicationContext} after each method (test case).
 * Used to properly destroy the beans defined inside Spring.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 */
public abstract class RecreatingSpringApplicationContextTest extends SpringApplicationContextIntegrationTestsSupport {

	@Before
	public void createContext() {

		GenericXmlApplicationContext applicationContext = configureContext(new GenericXmlApplicationContext());

		applicationContext.load(location());
		applicationContext.registerShutdownHook();
		applicationContext.refresh();

		setApplicationContext(applicationContext);
	}

	protected abstract String location();

	protected <T extends ConfigurableApplicationContext> T configureContext(T context){
		return context;
	}

	@After
	public void cleanupAfterTests() {

		destroyAllGemFireMockObjects();

		for (String name : new File(".").list((file, filename) -> filename.startsWith("BACKUP"))) {
			new File(name).delete();
		}
	}
}
