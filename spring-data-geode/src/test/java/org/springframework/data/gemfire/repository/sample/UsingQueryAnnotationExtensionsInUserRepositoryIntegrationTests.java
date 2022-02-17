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
package org.springframework.data.gemfire.repository.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.repository.Query;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for custom OQL {@link Query} annotations.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.repository.GemfireRepository
 * @see org.springframework.data.gemfire.repository.Query
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class UsingQueryAnnotationExtensionsInUserRepositoryIntegrationTests extends IntegrationTestsSupport {

	private static User create(String username) {
		return new User(username);
	}

	private static List<User> users(String... usernames) {

		List<User> users = new ArrayList<>(usernames.length);

		for (String username : usernames) {
			users.add(create(username));
		}

		return users;
	}

	@Autowired
	@Qualifier("Users")
	private Region<String, User> users;

	@Autowired
	private UsingQueryAnnotationExtensionsInUserRepository userRepository;

	private User put(User user) {
		return put(users, user);
	}

	private User put(Region<String, User> users, User user) {
		users.put(user.getUsername(), user);
		return user;
	}

	@Before
	public void setup() {

		assertThat(users).isNotNull();

		if (users.isEmpty()) {

			assertThat(users.size()).isEqualTo(0);

			put(create("jonDoe"));
			put(create("joeDoe"));
			put(create("janeDoe"));
			put(create("cookieDoe"));
			put(create("froDoe"));
			put(create("pieDoe"));
			put(create("sourDoe"));
			put(create("toeDoe"));

			assertThat(users.size()).isEqualTo(8);
		}
	}

	@Test
	public void queryUsingFindBy() {

		List<User> users = userRepository.findBy("j%Doe");

		assertThat(users).isNotNull();
		assertThat(users.isEmpty()).isFalse();
		assertThat(users.size()).isEqualTo(1);
		assertThat(users.get(0)).isEqualTo(create("janeDoe"));
	}

	@Test
	public void queryUsingFindByUsernameOrderedAndLimited() {

		List<User> users = userRepository.findDistinctByUsernameLikeOrderByUsernameAsc("%o%Doe");

		assertThat(users).isNotNull();
		assertThat(users.isEmpty()).isFalse();
		assertThat(users.size()).isEqualTo(5);
		assertThat(users).isEqualTo(users("cookieDoe", "froDoe", "joeDoe", "jonDoe", "sourDoe"));
	}

	@Test
	public void queryUsingFindByUsernameOrderedAndUnderLimit() {

		List<User> users = userRepository.findDistinctByUsernameLikeOrderByUsernameAsc("%oo%Doe");

		assertThat(users).isNotNull();
		assertThat(users.isEmpty()).isFalse();
		assertThat(users.size()).isEqualTo(1);
		assertThat(users.get(0)).isEqualTo(create("cookieDoe"));
	}
}
