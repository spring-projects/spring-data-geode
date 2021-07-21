/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.Pool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.repository.sample.Person;
import org.springframework.data.gemfire.repository.sample.PersonRepository;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for an Apache Geode DataSource.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
// TODO: merge with o.s.d.g.client.GemfireDataSourceIntegrationTest
public class GemFireDataSourceIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGemFireServer() throws Exception {
		startGemFireServer(ServerProcess.class,
			getServerContextXmlFileLocation(GemFireDataSourceIntegrationTests.class));
	}

	@Autowired
	private ApplicationContext applicationContext;

	private void assertRegion(Region<?, ?> region, String name, DataPolicy dataPolicy) {
		assertRegion(region, name, GemfireUtils.toRegionPath("simple"), dataPolicy);
	}

	private void assertRegion(Region<?, ?> region, String name, String fullPath, DataPolicy dataPolicy) {

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(name);
		assertThat(region.getFullPath()).isEqualTo(fullPath);
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(dataPolicy);
	}

	@Test
	public void gemfireServerDataSourceCreated() {

		Pool pool = this.applicationContext.getBean("gemfirePool", Pool.class);

		assertThat(pool).isNotNull();

		List<String> regionList = Arrays.asList(this.applicationContext.getBeanNamesForType(Region.class));

		assertThat(regionList).hasSize(3);
		assertThat(regionList).containsExactlyInAnyOrder("r1", "r2", "simple");

		Region<?, ?> simple = this.applicationContext.getBean("simple", Region.class);

		assertRegion(simple, "simple", DataPolicy.EMPTY);
	}

	@Test
	public void repositoryCreatedAndFunctional() {

		Person daveMathews = new Person(1L, "Dave", "Mathews");

		PersonRepository repository = this.applicationContext.getBean(PersonRepository.class);

		assertThat(repository.save(daveMathews)).isSameAs(daveMathews);

		Optional<Person> result = repository.findById(1L);

		assertThat(result.isPresent()).isTrue();
		assertThat(result.map(Person::getFirstname).orElse(null)).isEqualTo("Dave");
	}
}
