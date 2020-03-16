/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.gemfire.repository.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests using Spring Data [Geode] {@link Repository} on Apache Geode {@link Region} configured with
 * a {@link CacheLoader}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.CacheLoader
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class RepositoryDataAccessOnRegionUsingCacheLoaderIntegrationTests {

	@Autowired
	private PersonRepository personRepository;

	@Resource(name = "simple")
	private Region<Long, Person> people;

	@Before
	public void setup() {

		assertThat(this.people).isNotNull();
		assertThat(this.people.getName()).isEqualTo("simple");
		assertThat(this.people).isEmpty();
	}

	@After
	public void tearDown() {
		this.people.clear();
	}

	@Test
	public void repositoryFindByIdInvokesRegionCacheLoader() {

		Optional<Person> optionalJonDoe = this.personRepository.findById(2L);

		Person jonDoe = optionalJonDoe.orElse(null);

		assertThat(jonDoe).isNotNull();
		assertThat(jonDoe.getId()).isEqualTo(2L);
		assertThat(jonDoe.getFirstname()).isEqualTo("Jon");
		assertThat(jonDoe.getLastname()).isEqualTo("Doe");
	}

	@Test
	public void repositoryDerivedQueryMethodDoesNotInvokeRegionCacheLoader() {

		Collection<Person> people = this.personRepository.findByFirstname("Jon");

		assertThat(people).isNotNull();
		assertThat(people).isEmpty();
	}

	@ClientCacheApplication(name = "RepositoryDataAccessOnRegionUsingCacheLoaderIntegrationTests")
	static class TestConfiguration {

		@Bean("simple")
		ClientRegionFactoryBean<Long, Person> peopleRegion(GemFireCache cache) {

			ClientRegionFactoryBean<Long, Person> people = new ClientRegionFactoryBean<>();

			people.setCache(cache);
			people.setCacheLoader(new PersonCacheLoader());
			people.setShortcut(ClientRegionShortcut.LOCAL);

			return people;
		}

		@Bean
		GemfireRepositoryFactoryBean<PersonRepository, Person, Long> personRepository(GemFireCache cache) {

			GemfireRepositoryFactoryBean<PersonRepository, Person, Long> personRepository =
				new GemfireRepositoryFactoryBean<>(PersonRepository.class);

			personRepository.setCache(cache);
			personRepository.setGemfireMappingContext(new GemfireMappingContext());

			return personRepository;
		}
	}

	static class PersonCacheLoader implements CacheLoader<Long, Person> {

		@Override
		public Person load(LoaderHelper<Long, Person> helper) throws CacheLoaderException {
			return new Person(helper.getKey(), "Jon", "Doe");
		}
	}
}
