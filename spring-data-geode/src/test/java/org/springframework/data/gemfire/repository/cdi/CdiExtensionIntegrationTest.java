/*
 * Copyright 2012-2022 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.repository.cdi;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheFactory;

import org.springframework.data.gemfire.repository.sample.Person;

/**
 * Integration Tests for CDI.
 *
 * @author John Blum
 * @author Mark Paluch
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.repository.cdi.GemfireRepositoryBean
 * @see org.springframework.data.gemfire.repository.cdi.GemfireRepositoryExtension
 * @since 1.8.0
 */
public class CdiExtensionIntegrationTest {

	static SeContainer container;

	@BeforeClass
	public static void setUp() {

		container = SeContainerInitializer.newInstance()
			.disableDiscovery()
			.addPackages(RepositoryClient.class)
			.initialize();
	}

	@AfterClass
	public static void tearDown() {
		container.close();
		closeGemfireCache();
	}

	private static void closeGemfireCache() {
		try {
			CacheFactory.getAnyInstance().close();
		}
		catch (CacheClosedException ignore) {}
	}

	private void assertPerson(Person person, Long id, String name) {

		assertThat(person).isNotNull();
		assertThat(person.getId()).isGreaterThan(id);
		assertThat(person.getName()).isEqualTo(name);
	}

	private void assertPerson(Person actual, Person expected) {

		assertThat(actual.getId()).isEqualTo(expected.getId());
		assertThat(actual.getFirstname()).isEqualTo(expected.getFirstname());
		assertThat(actual.getLastname()).isEqualTo(expected.getLastname());
	}

	@Test // DATAGEODE-42
	public void bootstrapsRepositoryCorrectly() {

		RepositoryClient repositoryClient = container.select(RepositoryClient.class).get();

		assertThat(repositoryClient).isNotNull();
		assertThat(repositoryClient.getPersonRepository()).isNotNull();

		Person expectedJonDoe = repositoryClient.newPerson("Jon", "Doe");

		assertPerson(expectedJonDoe, 0L, "Jon Doe");

		Person savedJonDoe = repositoryClient.save(expectedJonDoe);

		assertPerson(savedJonDoe, expectedJonDoe);

		Person queriedJonDoe = repositoryClient.find(expectedJonDoe.getId());

		assertPerson(queriedJonDoe, expectedJonDoe);

		assertThat(repositoryClient.delete(queriedJonDoe)).isTrue();
		assertThat(repositoryClient.find(queriedJonDoe.getId())).isNull();
	}

	@Test // DATAGEODE-42
	public void returnOneFromCustomImplementation() {

		RepositoryClient repositoryClient = container.select(RepositoryClient.class).get();

		assertThat(repositoryClient.getPersonRepository().returnOne()).isEqualTo(1);
	}
}
