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
 */
package org.springframework.data.gemfire.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.Regions;
import org.springframework.data.gemfire.repository.sample.Person;
import org.springframework.data.gemfire.repository.sample.PersonRepository;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link GemfireRepositoryFactory}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.repository.support.GemfireRepositoryFactory
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
public abstract class AbstractGemfireRepositoryFactoryIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@SuppressWarnings("unused")
	private List<Region<?, ?>> regions;

	private Person boyd;
	private Person carter;
	private Person dave;
	private Person jeff;
	private Person leroi;
	private Person oliverAugust;
	private Person stefan;

	private PersonRepository repository;

	@Before
	public void setUp() {

		dave = new Person(1L, "Dave", "Matthews");
		carter = new Person(2L, "Carter", "Beauford");
		boyd = new Person(3L, "Boyd", "Tinsley");
		stefan = new Person(4L, "Stefan", "Lessard");
		leroi = new Person(5L, "Leroi", "Moore");
		jeff = new Person(6L, "Jeff", "Coffin");
		oliverAugust = new Person(7L, "Oliver August", "Matthews");

		Regions regions = new Regions(this.regions, new GemfireMappingContext());

		GemfireTemplate template = new GemfireTemplate(regions.getRegion(Person.class));

		template.put(dave.id, dave);
		template.put(carter.id, carter);
		template.put(boyd.id, boyd);
		template.put(stefan.id, stefan);
		template.put(leroi.id, leroi);
		template.put(jeff.id, jeff);
		template.put(oliverAugust.id, oliverAugust);

		repository = getRepository(regions);
	}

	protected abstract PersonRepository getRepository(Regions regions);

	@Test
	public void executesAnnotatedInQueryMethodCorrectly() {
		assertResultsFound(repository.findByFirstnameAnnotated("Dave"), dave);
		assertResultsFound(repository.findByFirstnamesAnnotated(Arrays.asList("Carter", "Dave")), carter, dave);
	}

	@Test
	public void executesInQueryMethodCorrectly() {
		assertResultsFound(repository.findByFirstnameIn(Arrays.asList("Carter", "Dave")), carter, dave);
	}

	@Test
	public void executesDerivedQueryCorrectly() {
		assertResultsFound(repository.findByFirstname("Carter"), carter);
		assertResultsFound(repository.findByFirstnameIn(Arrays.asList("Stefan", "Boyd")), stefan, boyd);
		assertResultsFound(repository.findByFirstnameIn("Leroi"), leroi);
	}

	@Test
	public void executesDerivedQueryWithAndCorrectly() {
		assertResultsFound(repository.findByFirstnameAndLastname("Carter", "Beauford"), carter);
	}

	@Test
	public void executesDerivedQueryWithOrCorrectly() {
		assertResultsFound(repository.findByFirstnameOrLastname("Carter", "Matthews"), carter, dave, oliverAugust);
	}

	@Test
	public void deletesAllEntitiesFromRegions() {

		repository.deleteAll();

		assertResultsFound(repository.findAll());
	}

	@Test
	public void findsPersonByLastname() {
		assertThat(repository.findByLastname("Beauford")).isEqualTo(carter);
	}

	@Test
	public void returnsNullForEmptyResultForSingleEntityQuery() {
		assertThat(repository.findByLastname("Foo")).isNull();
	}

	@Test
	public void throwsExceptionForMoreThanOneResultForSingleEntityQuery() {

		try {
			repository.findByLastname("Matthews");
			fail("Exception expected!");
		} catch (IncorrectResultSizeDataAccessException e) {
			assertThat(e.getExpectedSize()).isEqualTo(1);
			assertThat(e.getActualSize()).isEqualTo(2);
		}
	}

	@Test
	public void executesStartsWithCorrectly() {
		assertResultsFound(repository.findByFirstnameStartingWith("Da"), dave);
	}

	@Test
	public void executesEndsWithCorrectly() {
		assertResultsFound(repository.findByLastnameEndingWith("ews"), dave, oliverAugust);
	}

	@Test
	public void executesContainsCorrectly() {
		assertResultsFound(repository.findByFirstnameContaining("o"), boyd, leroi);
	}

	@Test
	public void executesLikeCorrectly() {
		assertResultsFound(repository.findByFirstnameLike("Da%"), dave);
	}

	@SafeVarargs
	private static <T> void assertResultsFound(Iterable<T> result, T... expected) {

		assertThat(result).isNotNull();
		assertThat(result).hasSize(expected.length);

		for (T element : expected) {
			assertThat(result).contains(element);
		}
	}
}
