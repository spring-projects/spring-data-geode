/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.SelectResults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.gemfire.repository.sample.User;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.util.CacheUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link GemfireTemplate}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.4.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class GemfireTemplateIntegrationTests extends IntegrationTestsSupport {

	protected static final String DEFAULT_GEMFIRE_LOG_LEVEL = "error";

	protected static final List<User> TEST_USERS = new ArrayList<>(9);

	static {
		TEST_USERS.add(newUser("jonDoe"));
		TEST_USERS.add(newUser("janeDoe", false));
		TEST_USERS.add(newUser("pieDoe", false));
		TEST_USERS.add(newUser("cookieDoe"));
		TEST_USERS.add(newUser("jackHandy"));
		TEST_USERS.add(newUser("mandyHandy", false));
		TEST_USERS.add(newUser("randyHandy", false));
		TEST_USERS.add(newUser("sandyHandy"));
		TEST_USERS.add(newUser("imaPigg"));
	}

	@Autowired
	private GemFireCache gemfireCache;

	@Autowired
	private GemfireTemplate usersTemplate;

	@Autowired
	@Qualifier("Users")
	private Region<String, User> users;

	private static User newUser(String username) {
		return newUser(username, true);
	}

	private static User newUser(String username, Boolean active) {
		return newUser(username, String.format("%1$s@companyx.com", username), Instant.now(), active);
	}

	private static User newUser(String username, String email, Instant since, Boolean active) {

		User user = new User(username);

		user.setActive(Boolean.TRUE.equals(active));
		user.setEmail(email);
		user.setSince(since);

		return user;
	}

	private String getKey(User user) {
		return user != null ? user.getUsername() : null;
	}

	private User getUser(String username) {

		for (User user : TEST_USERS) {
			if (user.getUsername().equals(username)) {
				return user;
			}
		}

		return null;
	}

	private List<User> getUsers(String... usernames) {

		List<String> usernameList = Arrays.asList(usernames);
		List<User> users = new ArrayList<>(usernames.length);

		for (User user : TEST_USERS) {
			if (usernameList.contains(user.getUsername())) {
				users.add(user);
			}
		}

		return users;
	}

	private Map<String, User> getUsersAsMap(String... usernames) {
		return getUsersAsMap(getUsers(usernames));
	}

	private Map<String, User> getUsersAsMap(User... users) {
		return getUsersAsMap(Arrays.asList(users));
	}

	private Map<String, User> getUsersAsMap(Iterable<User> users) {

		Map<String, User> userMap = new HashMap<>();

		for (User user : users) {
			userMap.put(getKey(user), user);
		}

		return userMap;
	}

	private void assertNullEquals(Object value1, Object value2) {
		assertThat(Objects.equals(value1, value2)).isTrue();
	}

	@Before
	public void setup() {

		assertThat(this.users).isNotNull();

		if (this.users.isEmpty()) {
			for (User user : TEST_USERS) {
				this.users.put(getKey(user), user);
			}

			assertThat(this.users.size()).isEqualTo(TEST_USERS.size());
		}
	}

	@Test
	public void containsKey() {

		assertThat(this.usersTemplate.containsKey(getKey(getUser("jonDoe")))).isTrue();
		assertThat(this.usersTemplate.containsKey("dukeNukem")).isFalse();
	}

	@Test
	public void containsKeyOnServer() {

		assumeThat(CacheUtils.isClient(this.gemfireCache)).isTrue();

		assertThat(this.usersTemplate.containsKeyOnServer(getKey(getUser("jackHandy")))).isTrue();
		assertThat(this.usersTemplate.containsKeyOnServer("maxPayne")).isFalse();
	}

	@Test
	public void containsValue() {

		assertThat(this.usersTemplate.containsValue(getUser("pieDoe"))).isTrue();
		assertThat(this.usersTemplate.containsValue(newUser("pieDough"))).isFalse();
	}

	@Test
	public void containsValueForKey() {

		assertThat(this.usersTemplate.containsValueForKey(getKey(getUser("cookieDoe")))).isTrue();
		assertThat(this.usersTemplate.containsValueForKey("chocolateChipCookieDoe")).isFalse();
	}

	@Test
	public void create() {

		User bartSimpson = newUser("bartSimpson");

		this.usersTemplate.create(getKey(bartSimpson), bartSimpson);

		assertThat(this.users.containsKey(getKey(bartSimpson))).isTrue();
		assertThat(this.users.containsValueForKey(getKey(bartSimpson))).isTrue();
		assertThat(this.users.containsValue(bartSimpson)).isTrue();
		assertThat(this.users.get(getKey(bartSimpson))).isEqualTo(bartSimpson);
	}

	@Test
	public void get() {

		String key = getKey(getUser("imaPigg"));

		assertThat(this.usersTemplate.<Object, Object>get(key)).isEqualTo(this.users.get(key));
		assertNullEquals(this.users.get("mrT"), this.usersTemplate.get("mrT"));
	}

	@Test
	public void put() {

		User peterGriffon = newUser("peterGriffon");

		assertThat(this.usersTemplate.put(getKey(peterGriffon), peterGriffon)).isNull();
		assertThat(this.users.get(getKey(peterGriffon))).isEqualTo(peterGriffon);
	}

	@Test
	public void putIfAbsent() {

		User stewieGriffon = newUser("stewieGriffon");

		assertThat(this.users.containsValue(stewieGriffon)).isFalse();
		assertThat(this.usersTemplate.putIfAbsent(getKey(stewieGriffon), stewieGriffon)).isNull();
		assertThat(this.users.containsValue(stewieGriffon)).isTrue();
		assertThat(this.usersTemplate.putIfAbsent(getKey(stewieGriffon), newUser("megGriffon"))).isEqualTo(stewieGriffon);
		assertThat(this.users.get(getKey(stewieGriffon))).isEqualTo(stewieGriffon);
	}

	@Test
	public void remove() {

		User mandyHandy = this.users.get(getKey(getUser("mandyHandy")));

		assertThat(mandyHandy).isNotNull();
		assertThat(this.usersTemplate.<Object, Object>remove(getKey(mandyHandy))).isEqualTo(mandyHandy);
		assertThat(this.users.containsKey(getKey(mandyHandy))).isFalse();
		assertThat(this.users.containsValue(mandyHandy)).isFalse();
		assertThat(this.users.containsKey("loisGriffon")).isFalse();
		assertThat(this.usersTemplate.<Object, Object>remove("loisGriffon")).isNull();
		assertThat(this.users.containsKey("loisGriffon")).isFalse();
	}

	@Test
	public void replace() {

		User randyHandy = this.users.get(getKey(getUser("randyHandy")));
		User lukeFluke = newUser("lukeFluke");
		User chrisGriffon = newUser("chrisGriffon");

		assertThat(randyHandy).isNotNull();
		assertThat(this.usersTemplate.replace(getKey(randyHandy), lukeFluke)).isEqualTo(randyHandy);
		assertThat(this.users.get(getKey(randyHandy))).isEqualTo(lukeFluke);
		assertThat(this.users.containsValue(randyHandy)).isFalse();
		assertThat(this.users.containsValue(chrisGriffon)).isFalse();
		assertThat(this.usersTemplate.replace(getKey(chrisGriffon), chrisGriffon)).isNull();
		assertThat(this.users.containsValue(chrisGriffon)).isFalse();
	}

	@Test
	public void replaceOldValueWithNewValue() {

		User jackHandy = getUser("jackHandy");
		User imaPigg = getUser("imaPigg");

		assertThat(this.users.containsValue(jackHandy)).isTrue();
		assertThat(this.usersTemplate.replace(getKey(jackHandy), null, imaPigg)).isFalse();
		assertThat(this.users.containsValue(jackHandy)).isTrue();
		assertThat(this.users.get(getKey(jackHandy))).isEqualTo(jackHandy);
		assertThat(this.usersTemplate.replace(getKey(jackHandy), jackHandy, imaPigg)).isTrue();
		assertThat(this.users.containsValue(jackHandy)).isFalse();
		assertThat(this.users.get(getKey(jackHandy))).isEqualTo(imaPigg);
	}

	@Test
	public void getAllReturnsNoResults() {

		List<String> keys = Arrays.asList("keyOne", "keyTwo", "keyThree");

		Map<String, User> users = this.usersTemplate.getAll(keys);

		assertThat(users).isNotNull();
		assertThat(users).isEqualTo(this.users.getAll(keys));
	}

	@Test
	public void getAllReturnsResults() {

		Map<String, User> users = this.usersTemplate.getAll(Arrays.asList(
			getKey(getUser("jonDoe")), getKey(getUser("pieDoe"))));

		assertThat(users).isNotNull();
		assertThat(users).isEqualTo(getUsersAsMap(getUser("jonDoe"), getUser("pieDoe")));
	}

	@Test
	public void putAll() {

		User batMan = newUser("batMan");
		User spiderMan = newUser("spiderMan");
		User superMan = newUser("superMan");

		Map<String, User> userMap = getUsersAsMap(batMan, spiderMan, superMan);

		assertThat(this.users.keySet().containsAll(userMap.keySet())).isFalse();
		assertThat(this.users.values().containsAll(userMap.values())).isFalse();

		this.usersTemplate.putAll(userMap);

		assertThat(this.users.keySet().containsAll(userMap.keySet())).isTrue();
		assertThat(this.users.values().containsAll(userMap.values())).isTrue();
	}

	@Test
	public void query() {

		SelectResults<User> queryResults = this.usersTemplate.query("username LIKE '%Doe'");

		assertThat(queryResults).isNotNull();

		List<User> usersFound = queryResults.asList();

		assertThat(usersFound).isNotNull();
		assertThat(usersFound.size()).isEqualTo(4);
		assertThat(usersFound.containsAll(getUsers("jonDoe", "janeDoe", "pieDoe", "cookieDoe"))).isTrue();
	}

	@Test
	public void find() {

		SelectResults<User> findResults =
			this.usersTemplate.find("SELECT u FROM /Users u WHERE u.username LIKE $1 AND u.active = $2", "%Doe", true);

		assertThat(findResults).isNotNull();

		List<User> usersFound = findResults.asList();

		assertThat(usersFound).isNotNull();
		assertThat(usersFound.size()).isEqualTo(2);
		assertThat(usersFound.containsAll(getUsers("jonDoe", "cookieDoe"))).isTrue();
	}

	// The following query is syntactically correct but does NOT work!!!
	// "SELECT keys FROM /Users u, u.keySet keys WHERE u.active = false ORDER BY u.username ASC"
	@Test
	public void findKeys() {

		User mandyHandy = getUser("mandyHandy");

		this.users.put(mandyHandy.getUsername(), mandyHandy);

		String query = "SELECT u.key FROM /Users.entrySet u WHERE u.value.active = false ORDER BY u.value.username ASC";

		SelectResults<String> results = this.usersTemplate.find(query);

		assertThat(results).isNotNull();
		assertThat(results).hasSize(4);
		assertThat(results.asList()).containsExactly("janeDoe", "mandyHandy", "pieDoe", "randyHandy");
	}

	@Test
	public void findLimitedKeys() {

		String query = "SELECT u.key"
			+ " FROM /Users.entrySet u"
			+ " WHERE u.value.active = false"
			+ " AND u.value.username LIKE '%Doe'"
			+ " ORDER BY u.value.username ASC"
			+ " LIMIT 1";

		SelectResults<String> results = this.usersTemplate.find(query);

		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
		assertThat(results.asList()).containsExactly("janeDoe");
	}

	@Test
	public void findUniqueReturnsResult() {

		User jonDoe =
			this.usersTemplate.findUnique("SELECT u FROM /Users u WHERE u.username = $1", "jonDoe");

		assertThat(jonDoe).isNotNull();
		assertThat(jonDoe).isEqualTo(getUser("jonDoe"));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findUniqueReturnsNoResult() {
		this.usersTemplate.findUnique("SELECT u FROM /Users u WHERE u.username = $1", "benDover");
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findUniqueReturnsTooManyResults() {
		this.usersTemplate.findUnique("SELECT u FROM /Users u WHERE u.username LIKE $1", "%Doe");
	}

	@Configuration
	static class GemfireTemplateConfiguration {

		Properties gemfireProperties() {

			Properties gemfireProperties = new Properties();

			gemfireProperties.setProperty("name", applicationName());
			gemfireProperties.setProperty("log-level", logLevel());

			return gemfireProperties;
		}

		String applicationName() {
			return GemfireTemplateIntegrationTests.class.getName();
		}

		String logLevel() {
			return System.getProperty("gemfire.log-level", DEFAULT_GEMFIRE_LOG_LEVEL);
		}

		@Bean
		CacheFactoryBean gemfireCache() {

			CacheFactoryBean gemfireCache = new CacheFactoryBean();

			gemfireCache.setClose(false);
			gemfireCache.setProperties(gemfireProperties());

			return gemfireCache;
		}

		@Bean(name = "Users")
		LocalRegionFactoryBean<String, User> usersRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<String, User> usersRegion = new LocalRegionFactoryBean<>();

			usersRegion.setCache(gemfireCache);
			usersRegion.setPersistent(false);

			return usersRegion;
		}

		@Bean
		GemfireTemplate usersTemplate(Region<Object, Object> simple) {
			return new GemfireTemplate(simple);
		}
	}
}
