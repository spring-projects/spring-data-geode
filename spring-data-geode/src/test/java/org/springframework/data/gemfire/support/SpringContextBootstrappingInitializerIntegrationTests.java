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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.repository.sample.User;
import org.springframework.data.gemfire.support.sample.TestUserDao;
import org.springframework.data.gemfire.support.sample.TestUserService;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.support.DataSourceAdapter;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.util.Assert;

/**
 * Integration Tests for the {@link SpringContextBootstrappingInitializer}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheFactory
 * @see org.apache.geode.cache.Region
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.data.gemfire.support.SpringContextBootstrappingInitializer
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @since 1.4.0
 */
@SuppressWarnings("unused")
public class SpringContextBootstrappingInitializerIntegrationTests extends IntegrationTestsSupport {

	protected static final String GEMFIRE_LOG_LEVEL = "error";
	protected static final String GEMFIRE_JMX_MANAGER = "true";
	protected static final String GEMFIRE_JMX_MANAGER_PORT = "1199";
	protected static final String GEMFIRE_JMX_MANAGER_START = "true";
	protected static final String GEMFIRE_NAME = SpringContextBootstrappingInitializerIntegrationTests.class.getSimpleName();
	protected static final String GEMFIRE_START_LOCATORS = "localhost[11235]";

	@AfterClass
	public static void cleanupAfterTests() {
		SpringContextBootstrappingInitializer.destroy();
	}

	@Before @After
	public void testSetupAndTearDown() {

		SpringUtils.safeDoOperation(() ->
			closeApplicationContext(SpringContextBootstrappingInitializer.getApplicationContext()));

		UserDataStoreCacheLoader.INSTANCE.set(null);

		closeAnyGemFireCache();
	}

	@SuppressWarnings("all")
	private void doSpringContextBootstrappingInitializationTest(String cacheXmlFile) {

		Cache gemfireCache = new CacheFactory()
			.set("name", GEMFIRE_NAME)
			.set("log-level", GEMFIRE_LOG_LEVEL)
			.set("cache-xml-file", cacheXmlFile)
			//.set("start-locator", GEMFIRE_START_LOCATORS)
			//.set("jmx-manager", GEMFIRE_JMX_MANAGER)
			//.set("jmx-manager-port", GEMFIRE_JMX_MANAGER_PORT)
			//.set("jmx-manager-start", GEMFIRE_JMX_MANAGER_START)
			.create();

		assertThat(gemfireCache)
			.describedAs("GemFireCache was not properly created and initialized")
			.isNotNull();

		assertThat(gemfireCache.isClosed())
			.describedAs("GemFireCache is closed")
			.isFalse();

		Set<Region<?, ?>> rootRegions = gemfireCache.rootRegions();

		assertThat(rootRegions).isNotNull();
		assertThat(rootRegions.isEmpty()).isFalse();
		assertThat(rootRegions.size()).isEqualTo(2);
		assertThat(gemfireCache.getRegion("/TestRegion")).isNotNull();
		assertThat(gemfireCache.getRegion("/Users")).isNotNull();

		ConfigurableApplicationContext applicationContext =
			SpringContextBootstrappingInitializer.getApplicationContext();

		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.containsBean(GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME)).isTrue();
		assertThat(applicationContext.containsBean("TestRegion")).isTrue();
		assertThat(applicationContext.containsBean("Users")).isFalse(); // Region 'Users' is defined in Pivotal GemFire cache.xml
		assertThat(applicationContext.containsBean("userDataSource")).isTrue();
		assertThat(applicationContext.containsBean("userDao")).isTrue();
		assertThat(applicationContext.containsBean("userService")).isTrue();

		DataSource userDataSource = applicationContext.getBean("userDataSource", DataSource.class);
		TestUserDao userDao = applicationContext.getBean("userDao", TestUserDao.class);
		TestUserService userService = applicationContext.getBean("userService", TestUserService.class);

		assertThat(userDao.getDataSource()).isSameAs(userDataSource);
		assertThat(userService.getUserDao()).isSameAs(userDao);

		// NOTE Pivotal GemFire declared component initialized by Spring!
		UserDataStoreCacheLoader usersCacheLoader = UserDataStoreCacheLoader.getInstance();

		assertThat(usersCacheLoader.getDataSource()).isSameAs(userDataSource);

		Region<String, User> users = gemfireCache.getRegion("/Users");

		assertThat(users).isNotNull();
		assertThat(users.getName()).isEqualTo("Users");
		assertThat(users.getFullPath()).isEqualTo("/Users");
		assertThat(users.isEmpty()).isTrue();
		assertThat(users.get("jblum")).isEqualTo(UserDataStoreCacheLoader.USER_DATA.get("jblum"));
		assertThat(users.get("jdoe")).isEqualTo(UserDataStoreCacheLoader.USER_DATA.get("jdoe"));
		assertThat(users.get("jhandy")).isEqualTo(UserDataStoreCacheLoader.USER_DATA.get("jhandy"));
		assertThat(users.isEmpty()).isFalse();
		assertThat(users.size()).isEqualTo(3);
	}

	@Test
	public void springContextBootstrappingInitializerUsingAnnotatedClassesIsCorrect() {

		SpringContextBootstrappingInitializer.register(TestAppConfig.class);

		new SpringContextBootstrappingInitializer().init(null, new Properties());

		ConfigurableApplicationContext applicationContext = SpringContextBootstrappingInitializer.getApplicationContext();

		UserDataStoreCacheLoader userDataStoreCacheLoader = applicationContext.getBean(UserDataStoreCacheLoader.class);
		DataSource userDataSource = applicationContext.getBean(DataSource.class);

		assertThat(userDataStoreCacheLoader).isSameAs(UserDataStoreCacheLoader.getInstance());
		assertThat(userDataSource).isSameAs(userDataStoreCacheLoader.getDataSource());
	}

	@Test
	public void springContextBootstrappingInitializerUsingXmlWithBasePackages() {
		doSpringContextBootstrappingInitializationTest(
			"cache-with-spring-context-bootstrap-initializer-using-base-packages.xml");
	}

	@Test
	public void springContextBootstrappingInitializerUsingXmlWithContextConfigLocations() {
		doSpringContextBootstrappingInitializationTest(
			"cache-with-spring-context-bootstrap-initializer.xml");
	}

	@Configuration
	public static class TestAppConfig {

		@Bean
		public DataSource userDataSource() {
			return new TestDataSource();
		}

		@Bean
		public UserDataStoreCacheLoader userDataStoreCacheLoader() {
			return new UserDataStoreCacheLoader();
		}
	}

	public static final class TestDataSource extends DataSourceAdapter { }

	public static final class UserDataStoreCacheLoader extends LazyWiringDeclarableSupport
			implements CacheLoader<String, User> {

		private static final AtomicReference<UserDataStoreCacheLoader> INSTANCE = new AtomicReference<>();

		private static final Map<String, User> USER_DATA = new ConcurrentHashMap<>(3);

		static {
			USER_DATA.put("jblum", new User("jblum"));
			USER_DATA.put("jdoe", new User("jdoe"));
			USER_DATA.put("jhandy", new User("jhandy"));
		}

		@Autowired
		private DataSource userDataSource;

		static User createUser(String username) {
			return createUser(username, true, Instant.now(), String.format("%1$s@xcompay.com", username));
		}

		static User createUser(String username, Boolean active) {
			return createUser(username, active, Instant.now(), String.format("%1$s@xcompay.com", username));
		}

		static User createUser(String username, Boolean active, Instant since) {
			return createUser(username, active, since, String.format("%1$s@xcompay.com", username));
		}

		static User createUser(String username, Boolean active, Instant since, String email) {

			User user = new User(username);

			user.setActive(active);
			user.setEmail(email);
			user.setSince(since);

			return user;
		}

		public static UserDataStoreCacheLoader getInstance() {
			return INSTANCE.get();
		}

		public UserDataStoreCacheLoader() {
			Assert.state(INSTANCE.compareAndSet(null, this),
				String.format("An instance of %1$s was already created", getClass().getName()));
		}

		@Override
		protected void assertInitialized() {

			super.assertInitialized();

			Assert.state(this.userDataSource != null,
				String.format("The 'User' Data Source was not properly configured and initialized for use in (%s)",
					getClass().getName()));
		}

		DataSource getDataSource() {
			return this.userDataSource;
		}

		@Override
		public void close() {
			this.userDataSource = null;
		}

		@Override
		public void destroy() throws Exception {
			super.destroy();
			INSTANCE.set(null);
		}

		@Override
		public User load(LoaderHelper<String, User> helper) throws CacheLoaderException {

			assertInitialized();

			return USER_DATA.get(helper.getKey());
		}
	}
}
