/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.gemfire.repository.sample.Person;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link SimpleGemfireRepository}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @author Jens Schauder
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.LocalRegionFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheApplication
 * @see org.springframework.data.gemfire.repository.support.SimpleGemfireRepository
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SimpleGemfireRepositoryIntegrationTests {

	static final String GEMFIRE_LOG_LEVEL = "warning";

	@Autowired private GemfireTemplate template;

	@Resource(name = "People") private Region<?, ?> people;

	private RegionClearListener regionClearListener;

	private SimpleGemfireRepository<Person, Long> repository;

	@Before
	@SuppressWarnings("all")
	public void setUp() {

		this.people.clear();
		this.regionClearListener = new RegionClearListener();
		this.people.getAttributesMutator().addCacheListener(this.regionClearListener);

		GemfireMappingContext mappingContext = new GemfireMappingContext();

		GemfirePersistentEntity<Person> personEntity = (GemfirePersistentEntity<Person>) mappingContext
				.getPersistentEntity(Person.class);

		EntityInformation<Person, Long> information = new PersistentEntityInformation<>(personEntity);

		this.repository = new SimpleGemfireRepository<>(this.template, information);
	}

	@Test
	public void deleteAllFiresClearEvent() {

		assertThat(this.regionClearListener.eventFired).isFalse();

		this.repository.deleteAll();

		assertThat(this.regionClearListener.eventFired).isTrue();
	}

	@Test
	public void findAllPaged() {

		assertThat(this.repository.count()).isEqualTo(0);

		List<Person> people = Arrays.asList(new Person(1L, "Jon", "Doe"), new Person(2L, "Jane", "Doe"),
				new Person(3L, "Cookie", "Doe"), new Person(4L, "Pie", "Doe"), new Person(5L, "Sour", "Doe"));

		people.forEach(person -> this.template.put(person.getId(), person));

		assertThat(this.repository.count()).isEqualTo(5);

		Sort orderByFirstNameAscending = Sort.by("firstname").ascending();

		Page<Person> pageOne = this.repository.findAll(PageRequest.of(0, 3, orderByFirstNameAscending));

		assertThat(pageOne).isNotNull();
		assertThat(pageOne).isNotEmpty();
		assertThat(pageOne.getNumber()).isEqualTo(0);
		assertThat(pageOne.getNumberOfElements()).isEqualTo(3);
		assertThat(pageOne.getSize()).isEqualTo(3);
		assertThat(pageOne.getSort()).isEqualTo(orderByFirstNameAscending);
		assertThat(pageOne.getTotalElements()).isEqualTo(people.size());
		assertThat(pageOne.getTotalPages()).isEqualTo(2);
		assertThat(pageOne.getContent()).containsExactly(people.get(2), people.get(1), people.get(0));

		Page<Person> pageTwo = this.repository.findAll(PageRequest.of(1, 3, Sort.by("firstname").ascending()));

		assertThat(pageTwo).isNotNull();
		assertThat(pageTwo).isNotEmpty();
		assertThat(pageTwo.getNumber()).isEqualTo(1);
		assertThat(pageTwo.getNumberOfElements()).isEqualTo(2);
		assertThat(pageTwo.getSize()).isEqualTo(3);
		assertThat(pageTwo.getSort()).isEqualTo(orderByFirstNameAscending);
		assertThat(pageTwo.getTotalElements()).isEqualTo(people.size());
		assertThat(pageTwo.getTotalPages()).isEqualTo(2);
		assertThat(pageTwo.getContent()).containsExactly(people.get(3), people.get(4));
	}

	@Test
	public void findAllWithIds() {

		Person dave = new Person(1L, "Dave", "Matthews");
		Person carter = new Person(2L, "Carter", "Beauford");
		Person leroi = new Person(3L, "Leroi", "Moore");

		this.template.put(dave.getId(), dave);
		this.template.put(carter.getId(), carter);
		this.template.put(leroi.getId(), leroi);

		Iterable<Person> result = this.repository.findAllById(Arrays.asList(carter.getId(), leroi.getId()));

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).containsAll(Arrays.asList(carter, leroi));
	}

	@Test
	public void findAllWithIdsReturnsNoMatches() {

		Iterable<Person> results = this.repository.findAllById(Arrays.asList(1L, 2L));

		assertThat(results).isNotNull();
		assertThat(results).isEmpty();
	}

	@Test
	public void findAllWithIdsReturnsPartialMatches() {

		Person kurt = new Person(1L, "Kurt", "Cobain");
		Person eddie = new Person(2L, "Eddie", "Veddar");
		Person michael = new Person(3L, "Michael", "Jackson");

		this.template.put(kurt.getId(), kurt);
		this.template.put(eddie.getId(), eddie);

		Iterable<Person> results = this.repository.findAllById(Arrays.asList(0L, 1L, 2L, 4L));

		assertThat(results).isNotNull();
		assertThat(results).hasSize(2);
		assertThat(results).contains(kurt, eddie);
		assertThat(results).doesNotContain(michael);
	}

	@Test
	public void queryRegion() {

		Person oliverGierke = new Person(1L, "Oliver", "Gierke");

		assertThat(this.template.put(oliverGierke.getId(), oliverGierke)).isNull();

		SelectResults<Person> people = this.template.find("SELECT * FROM /People p WHERE p.firstname = $1",
				oliverGierke.getFirstname());

		assertThat(people.size()).isEqualTo(1);
		assertThat(people.iterator().next()).isEqualTo(oliverGierke);
	}

	@Test
	public void saveAndDeleteEntity() {

		Person oliverGierke = new Person(1L, "Oliver", "Gierke");

		assertThat(this.repository.save(oliverGierke)).isEqualTo(oliverGierke);
		assertThat(this.repository.count()).isEqualTo(1L);
		assertThat(this.repository.findById(oliverGierke.getId()).orElse(null)).isEqualTo(oliverGierke);
		assertThat(this.repository.findAll()).isEqualTo(Collections.singletonList(oliverGierke));

		this.repository.delete(oliverGierke);

		assertThat(this.repository.count()).isEqualTo(0L);
		assertThat(this.repository.findById(oliverGierke.getId()).orElse(null)).isNull();
		assertThat(this.repository.findAll()).isEmpty();
	}

	@Test // DATAGEODE-387
	public void deleteAllById() {

		assertThat(this.repository.count()).isEqualTo(0);

		List<Person> people = Arrays.asList(new Person(1L, "Jon", "Doe"), new Person(2L, "Jane", "Doe"),
				new Person(3L, "Cookie", "Doe"), new Person(4L, "Pie", "Doe"), new Person(5L, "Sour", "Doe"));

		people.forEach(person -> this.template.put(person.getId(), person));

		assertThat(this.repository.count()).isEqualTo(5);

		this.repository.deleteAllById(Arrays.asList(1L, 2L));

		assertThat(this.repository.count()).isEqualTo(3L);
		assertThat(this.repository.findAll()) //
				.extracting(Person::getFirstname) //
				.containsExactlyInAnyOrder("Cookie", "Pie", "Sour");
	}

	@Test
	public void saveEntities() {

		assertThat(this.template.getRegion()).isEmpty();

		Person johnBlum = new Person(1L, "John", "Blum");
		Person jonBloom = new Person(2L, "Jon", "Bloom");
		Person juanBlume = new Person(3L, "Juan", "Blume");

		this.repository.saveAll(Arrays.asList(johnBlum, jonBloom, juanBlume));

		assertThat(this.template.getRegion().size()).isEqualTo(3);
		assertThat((Person) this.template.get(johnBlum.getId())).isEqualTo(johnBlum);
		assertThat((Person) this.template.get(jonBloom.getId())).isEqualTo(jonBloom);
		assertThat((Person) this.template.get(juanBlume.getId())).isEqualTo(juanBlume);
	}

	@SuppressWarnings("rawtypes")
	public static class RegionClearListener extends CacheListenerAdapter {

		volatile boolean eventFired;

		@Override
		public void afterRegionClear(RegionEvent event) {
			this.eventFired = true;
		}
	}

	@PeerCacheApplication(name = "SimpleGemfireRepositoryIntegrationTests", logLevel = GEMFIRE_LOG_LEVEL)
	static class SimpleGemfireRepositoryConfiguration {

		@Bean(name = "People")
		LocalRegionFactoryBean<Object, Object> peopleRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<Object, Object> peopleRegion = new LocalRegionFactoryBean<>();

			peopleRegion.setCache(gemfireCache);
			peopleRegion.setClose(false);
			peopleRegion.setPersistent(false);

			return peopleRegion;
		}

		@Bean
		GemfireTemplate peopleRegionTemplate(Region<Object, Object> people) {
			return new GemfireTemplate(people);
		}
	}
}
