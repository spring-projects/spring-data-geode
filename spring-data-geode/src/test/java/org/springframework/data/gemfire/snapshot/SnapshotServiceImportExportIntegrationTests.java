/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.data.gemfire.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.cache.Region;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.repository.sample.Person;
import org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;
import org.springframework.data.gemfire.tests.util.ThreadUtils;
import org.springframework.util.FileCopyUtils;

/**
 * Integration Tests with test cases testing the import and export of cache {@link Region} data configured with
 * SDG's Data Namespace &gt;gfe-data:snapshot-service&lt; (XML) element.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.SpringApplicationContextIntegrationTestsSupport
 * @since 1.7.0
 */
@SuppressWarnings("unused")
public class SnapshotServiceImportExportIntegrationTests extends SpringApplicationContextIntegrationTestsSupport {

	private static final AtomicLong ID_SEQUENCE = new AtomicLong(0L);

	private static ConfigurableApplicationContext applicationContext;

	private static File importPeopleSnapshot;
	private static File snapshotsDirectory;

	private static Region<Long, Person> people;

	private static void assertPerson(Person expectedPerson, Person actualPerson) {

		assertThat(actualPerson).isNotNull();
		assertThat(actualPerson.getId()).isEqualTo(expectedPerson.getId());
		assertThat(actualPerson.getFirstname()).isEqualTo(expectedPerson.getFirstname());
		assertThat(actualPerson.getLastname()).isEqualTo(expectedPerson.getLastname());
	}

	private static void assertRegion(Region<?, ?> actualRegion, String expectedName, int expectedSize) {

		assertThat(actualRegion).isNotNull();
		assertThat(actualRegion.getName()).isEqualTo("People");
		assertThat(actualRegion.getFullPath()).isEqualTo(String.format("%1$s%2$s", Region.SEPARATOR, expectedName));
		assertThat(actualRegion.size()).isEqualTo(expectedSize);
	}

	private static Person newPerson(String firstName, String lastName) {
		return newPerson(ID_SEQUENCE.incrementAndGet(), firstName, lastName);
	}

	private static Person newPerson(Long id, String firstName, String lastName) {
		return new Person(id, firstName, lastName);
	}

	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setupBeforeClass() throws Exception {

		snapshotsDirectory = new File(new File(FileSystemUtils.WORKING_DIRECTORY, "gemfire"), "snapshots");

		File exportDirectory = new File(snapshotsDirectory, "export");
		File importDirectory = new File(snapshotsDirectory, "import");

		createDirectory(exportDirectory);
		createDirectory(importDirectory);

		importPeopleSnapshot = new File(importDirectory, "people-snapshot.gfd");

		FileCopyUtils.copy(new ClassPathResource("/people.snapshot").getFile(), importPeopleSnapshot);

		assertThat(importPeopleSnapshot).isFile();
		assertThat(importPeopleSnapshot).isNotEmpty();

		String configLocation = getContextXmlFileLocation(SnapshotServiceImportExportIntegrationTests.class);

		applicationContext = new ClassPathXmlApplicationContext(configLocation);
		applicationContext.registerShutdownHook();
		people = applicationContext.getBean("People", Region.class);
	}

	@AfterClass
	public static void tearDownAfterClass() {

		closeApplicationContext(applicationContext);

		File exportPeopleSnapshot = new File(new File(snapshotsDirectory, "export"), "people-snapshot.gfd");

		assertThat(exportPeopleSnapshot).isFile();
		assertThat(exportPeopleSnapshot).hasSize(importPeopleSnapshot.length());

		FileSystemUtils.deleteRecursive(snapshotsDirectory.getParentFile());
	}

	@Before
	public void setup() {
		//setupPeople();
		ThreadUtils.timedWait(TimeUnit.SECONDS.toMillis(5), 500, () -> !(people.size() > 0));
	}

	private void setupPeople() {

		save(newPerson("Jon", "Doe"));
		save(newPerson("Jane", "Doe"));
		save(newPerson("Cookie", "Doe"));
		save(newPerson("Fro", "Doe"));
		save(newPerson("Joe", "Doe"));
		save(newPerson("Lan", "Doe"));
		save(newPerson("Pie", "Doe"));
		save(newPerson("Play", "Doe"));
		save(newPerson("Sour", "Doe"));
	}

	private Person save(Person person) {
		people.putIfAbsent(person.getId(), person);
		return person;
	}

	@Test
	public void peopleRegionIsLoaded() {

		assertRegion(people, "People", 9);
		assertPerson(people.get(1L), newPerson(1L, "Jon", "Doe"));
		assertPerson(people.get(2L), newPerson(2L, "Jane", "Doe"));
		assertPerson(people.get(3L), newPerson(3L, "Cookie", "Doe"));
		assertPerson(people.get(4L), newPerson(4L, "Fro", "Doe"));
		assertPerson(people.get(5L), newPerson(5L, "Joe", "Doe"));
		assertPerson(people.get(6L), newPerson(6L, "Lan", "Doe"));
		assertPerson(people.get(7L), newPerson(7L, "Pie", "Doe"));
		assertPerson(people.get(8L), newPerson(8L, "Play", "Doe"));
		assertPerson(people.get(9L), newPerson(9L, "Sour", "Doe"));
	}
}
