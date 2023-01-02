/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for Apache Geode OQL query capabilities provided by Spring Data for Apache Geode Repositories.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class UserRepositoryQueriesIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("Users")
	@SuppressWarnings("rawtypes")
	private Region users;

	@Autowired
	private UserRepository userRepository;

	private static void assertQueryResults(Iterable<User> actualUsers, String... expectedUsernames) {

		assertThat(actualUsers).as("The query did not return any results").isNotNull();

		List<String> actualUsernames = new ArrayList<>(expectedUsernames.length);

		for (User actualUser : actualUsers) {
			actualUsernames.add(actualUser.getUsername());
		}

		assertThat(actualUsernames.size()).isEqualTo(expectedUsernames.length);
		assertThat(actualUsernames.containsAll(Arrays.asList(expectedUsernames))).isTrue();
	}

	private static User createUser(String username) {
		return createUser(username, true);
	}

	private static User createUser(String username, Boolean active) {
		return createUser(username, active, Instant.now(), String.format("%1$s@xcompany.com", username));
	}

	private static User createUser(String username, Boolean active, Instant since, String email) {

		User user = new User(username);

		user.setActive(active);
		user.setEmail(email);
		user.setSince(since);

		return user;
	}

	private static int toIntValue(final Integer value) {
		return value != null ? value : 0;
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setup() {

		assertThat(users).describedAs("The 'Users' GemFire Cache Region cannot be null").isNotNull();

		if (users.isEmpty()) {
			userRepository.save(createUser("blumj", true));
			userRepository.save(createUser("blums", true));
			userRepository.save(createUser("blume", false));
			userRepository.save(createUser("bloomr", false));
			userRepository.save(createUser("handyj", true));
			userRepository.save(createUser("handys", false));
			userRepository.save(createUser("doej", true));
			userRepository.save(createUser("doep", false));
			userRepository.save(createUser("doec", false));

			assertThat(users.isEmpty()).isFalse();
			assertThat(users.size()).isEqualTo(9);
		}
	}

	@Test
	public void testMultiResultQueries() {

		List<User> activeUsers = userRepository.findDistinctByActiveTrue();

		assertQueryResults(activeUsers, "blumj", "blums", "handyj", "doej");

		List<User> inactiveUsers = userRepository.findDistinctByActiveFalse();

		assertQueryResults(inactiveUsers, "blume", "bloomr", "handys", "doep", "doec");

		List<User> blumUsers = userRepository.findDistinctByUsernameLike("blum%");

		assertQueryResults(blumUsers, "blumj", "blums", "blume");
	}

	@Test
	public void testNonCollectionNonEntityResultQueries() {

		Integer count = userRepository.countUsersByUsernameLike("doe%");

		assertThat(toIntValue(count)).isEqualTo(3);

		count = userRepository.countUsersByUsernameLike("handy%");

		assertThat(toIntValue(count)).isEqualTo(2);

		count = userRepository.countUsersByUsernameLike("smith%");

		assertThat(count).isNotNull();
		assertThat(toIntValue(count)).isEqualTo(0);
	}
}
