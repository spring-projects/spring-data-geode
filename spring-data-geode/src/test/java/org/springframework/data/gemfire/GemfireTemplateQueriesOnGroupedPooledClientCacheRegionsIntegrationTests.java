/*
 * Copyright 2016-2022 the original author or authors.
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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.support.ConnectionEndpointList;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.tests.objects.geode.cache.RegionDataInitializingPostProcessor;
import org.springframework.data.gemfire.tests.process.ProcessWrapper;
import org.springframework.data.gemfire.util.PropertiesBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Integrations Tests for {@link GemfireTemplate} testing the proper function and behavior of executing OQL queries
 * from a cache client application using the {@link GemfireTemplate} to a cluster of Apache Geode servers that have
 * been grouped according to business function and data access in order to distribute the load.
 *
 * Each Apache Geode {@link Pool} is configured to target a specific server group.  Each group of servers in the cluster
 * defines specific {@link Region Regions} to manage data independently and separately from other data that might garner
 * high frequency access.
 *
 * Spring Data for Apache Geode's {@link GemfireTemplate} should intelligently employ the right
 * {@link org.apache.geode.cache.query.QueryService} configured with the {@link Region Region's} {@link Pool}
 * metadata when executing the query in order to ensure the right servers containing the {@link Region Region's}
 * with the data of interest are targeted.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see <a href="https://jira.spring.io/browse/SGF-555">Repository queries on client Regions associated with a Pool configured with a specified server group can lead to a RegionNotFoundException.</a>
 * @since 1.9.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes =
	GemfireTemplateQueriesOnGroupedPooledClientCacheRegionsIntegrationTests.GemFireClientCacheConfiguration.class)
@SuppressWarnings("unused")
public class GemfireTemplateQueriesOnGroupedPooledClientCacheRegionsIntegrationTests
		extends ForkingClientServerIntegrationTestsSupport {

	private static ProcessWrapper serverOne;
	private static ProcessWrapper serverTwo;

	@Autowired
	@Qualifier("catsTemplate")
	private GemfireTemplate catsTemplate;

	@Autowired
	@Qualifier("dogsTemplate")
	private GemfireTemplate dogsTemplate;

	@BeforeClass
	public static void runGemFireCluster() throws Exception {

		int locatorPort = findAndReserveAvailablePort();
		int cacheServerPortOne = findAndReserveAvailablePort();
		int cacheServerPortTwo = findAndReserveAvailablePort();

		serverOne = run(GemFireCacheServerOneConfiguration.class,
			String.format("-Dspring.data.gemfire.cache.server.port=%d", cacheServerPortOne),
			String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		waitForServerToStart("localhost", cacheServerPortOne);

		serverTwo = run(GemFireCacheServerTwoConfiguration.class,
			String.format("-Dspring.data.gemfire.cache.server.port=%d", cacheServerPortTwo),
			String.format("-Dspring.data.gemfire.locator.port=%d", locatorPort));

		waitForServerToStart("localhost", cacheServerPortTwo);

		System.setProperty("spring.data.gemfire.locator.port", String.valueOf(locatorPort));
	}

	@AfterClass
	public static void shutdownGemFireCluster() {
		stop(serverOne);
		stop(serverTwo);
	}

	@Test
	public void findsAllCats() {

		List<String> catNames = catsTemplate.<String>find("SELECT c.name FROM /Cats c").asList();

		assertThat(catNames).isNotNull();
		assertThat(catNames.size()).isEqualTo(5);
		assertThat(catNames).containsAll(Arrays.asList("Grey", "Patchit", "Tyger", "Molly", "Sammy"));
	}

	@Test
	public void findsAllDogs() {

		List<String> dogNames = dogsTemplate.<String>find("SELECT d.name FROM /Dogs d").asList();

		assertThat(dogNames).isNotNull();
		assertThat(dogNames.size()).isEqualTo(2);
		assertThat(dogNames).containsAll(Arrays.asList("Spuds", "Maha"));
	}

	@Getter
	@EqualsAndHashCode
	@Region("Cats")
	@RequiredArgsConstructor(staticName = "newCat")
	static class Cat {
		@Id @NonNull private final String name;
	}

	@Getter
	@EqualsAndHashCode
	@Region("Dogs")
	@RequiredArgsConstructor(staticName = "newDog")
	static class Dog {
		@Id @NonNull private final String name;
	}

	@Configuration
	static class GemFireClientCacheConfiguration {

		Properties gemfireProperties() {

			return PropertiesBuilder.create()
				.setProperty("name", applicationName())
				.setProperty("log-level", logLevel())
				.build();
		}

		String applicationName() {
			return GemfireTemplateQueriesOnGroupedPooledClientCacheRegionsIntegrationTests.class.getName();
		}

		String logLevel() {
			return System.getProperty("spring.data.gemfire.log.level", GEMFIRE_LOG_LEVEL);
		}

		@Bean
		ClientCacheFactoryBean gemfireCache() {

			ClientCacheFactoryBean gemfireCache = new ClientCacheFactoryBean();

			gemfireCache.setClose(true);
			gemfireCache.setPoolName("ServerOnePool");
			gemfireCache.setProperties(gemfireProperties());

			return gemfireCache;
		}

		@Bean(name = "ServerOnePool")
		PoolFactoryBean serverOnePool(@Value("${spring.data.gemfire.locator.port:11235}") int locatorPort) {

			PoolFactoryBean serverOnePool = new PoolFactoryBean();

			serverOnePool.setMaxConnections(2);
			serverOnePool.setPingInterval(TimeUnit.SECONDS.toMillis(5));
			serverOnePool.setReadTimeout(Long.valueOf(TimeUnit.SECONDS.toMillis(30)).intValue());
			serverOnePool.setRetryAttempts(1);
			serverOnePool.setServerGroup("serverOne");
			serverOnePool.setLocators(ConnectionEndpointList.from(ConnectionEndpoint.from("localhost", locatorPort)));

			return serverOnePool;
		}

		@Bean(name = "ServerTwoPool")
		PoolFactoryBean serverTwoPool(@Value("${spring.data.gemfire.locator.port:11235}") int locatorPort) {

			PoolFactoryBean serverOnePool = new PoolFactoryBean();

			serverOnePool.setMaxConnections(2);
			serverOnePool.setPingInterval(TimeUnit.SECONDS.toMillis(5));
			serverOnePool.setReadTimeout(Long.valueOf(TimeUnit.SECONDS.toMillis(30)).intValue());
			serverOnePool.setRetryAttempts(1);
			serverOnePool.setServerGroup("serverTwo");
			serverOnePool.setLocators(ConnectionEndpointList.from(ConnectionEndpoint.from("localhost", locatorPort)));

			return serverOnePool;
		}

		@Bean(name = "Cats")
		ClientRegionFactoryBean<String, Cat> catsRegion(GemFireCache gemfireCache,
				@Qualifier("ServerOnePool") Pool serverOnePool) {

			ClientRegionFactoryBean<String, Cat> catsRegion = new ClientRegionFactoryBean<>();

			catsRegion.setCache(gemfireCache);
			catsRegion.setPoolName(serverOnePool.getName());
			catsRegion.setShortcut(ClientRegionShortcut.PROXY);

			return catsRegion;
		}

		@Bean(name = "Dogs")
		ClientRegionFactoryBean<String, Cat> dogsRegion(GemFireCache gemfireCache,
				@Qualifier("ServerTwoPool") Pool serverTwoPool) {

			ClientRegionFactoryBean<String, Cat> dogsRegion = new ClientRegionFactoryBean<>();

			dogsRegion.setCache(gemfireCache);
			dogsRegion.setPoolName(serverTwoPool.getName());
			dogsRegion.setShortcut(ClientRegionShortcut.PROXY);

			return dogsRegion;
		}

		@Bean
		@DependsOn("Cats")
		GemfireTemplate catsTemplate(GemFireCache gemfireCache) {
			return new GemfireTemplate(gemfireCache.getRegion("Cats"));
		}

		@Bean
		@DependsOn("Dogs")
		GemfireTemplate dogsTemplate(GemFireCache gemfireCache) {
			return new GemfireTemplate(gemfireCache.getRegion("Dogs"));
		}
	}

	static abstract class AbstractGemFireCacheServerConfiguration {

		@Bean
		Properties gemfireProperties(@Value("${spring.data.gemfire.locator.port:11235}") int locatorPort) {

			return PropertiesBuilder.create()
				.setProperty("name", applicationName())
				.setProperty("log-level", logLevel())
				.setProperty("locators", String.format("localhost[%d]", locatorPort))
				.setProperty("groups", groups())
				.setProperty("start-locator", startLocator(locatorPort))
				.build();
		}

		String applicationName() {
			return getClass().getName();
		}

		abstract String groups();

		String logLevel() {
			return System.getProperty("spring.data.gemfire.log.level", GEMFIRE_LOG_LEVEL);
		}

		String startLocator(int locatorPort) {
			return "";
		}

		@Bean
		CacheFactoryBean gemfireCache(@Qualifier("gemfireProperties") Properties gemfireProperties) {

			CacheFactoryBean gemfireCache = new CacheFactoryBean();

			gemfireCache.setClose(true);
			gemfireCache.setProperties(gemfireProperties);

			return gemfireCache;
		}

		@Bean
		CacheServerFactoryBean gemfireCacheServer(GemFireCache gemfireCache,
				@Value("${spring.data.gemfire.cache.server.port:40404}") int cacheServerPort) {

			CacheServerFactoryBean gemfireCacheServer = new CacheServerFactoryBean();

			gemfireCacheServer.setAutoStartup(true);
			gemfireCacheServer.setCache((Cache) gemfireCache);
			gemfireCacheServer.setMaxTimeBetweenPings(Long.valueOf(TimeUnit.SECONDS.toMillis(60)).intValue());
			gemfireCacheServer.setPort(cacheServerPort);

			return gemfireCacheServer;
		}
	}

	@Configuration
	@SuppressWarnings("all")
	static class GemFireCacheServerOneConfiguration extends AbstractGemFireCacheServerConfiguration {

		public static void main(String[] args) {
			runSpringApplication(GemFireCacheServerOneConfiguration.class, args);
		}

		@Override
		String groups() {
			return "serverOne";
		}

		@Override
		String startLocator(int locatorPort) {
			return String.format("localhost[%d]", locatorPort);
		}

		@Bean(name = "Cats")
		LocalRegionFactoryBean<String, Cat> catsRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<String, Cat> catsRegion = new LocalRegionFactoryBean();

			catsRegion.setCache(gemfireCache);
			catsRegion.setPersistent(false);

			return catsRegion;
		}

		@Bean
		RegionDataInitializingPostProcessor<Cat> catsRegionDataInitializer() {

			return RegionDataInitializingPostProcessor.<Cat>withRegion("Cats")
				.useAsEntityIdentifier(Cat::getName)
				.store(Cat.newCat("Grey"))
				.store(Cat.newCat("Patchit"))
				.store(Cat.newCat("Tyger"))
				.store(Cat.newCat("Molly"))
				.store(Cat.newCat("Sammy"));
		}
	}

	@Configuration
	@SuppressWarnings("all")
	static class GemFireCacheServerTwoConfiguration extends AbstractGemFireCacheServerConfiguration {

		public static void main(String[] args) {
			runSpringApplication(GemFireCacheServerTwoConfiguration.class, args);
		}

		@Override
		String groups() {
			return "serverTwo";
		}

		@Bean(name = "Dogs")
		LocalRegionFactoryBean<String, Dog> dogsRegion(GemFireCache gemfireCache) {

			LocalRegionFactoryBean<String, Dog> dogsRegion = new LocalRegionFactoryBean<String, Dog>();

			dogsRegion.setCache(gemfireCache);
			dogsRegion.setPersistent(false);

			return dogsRegion;
		}

		@Bean
		RegionDataInitializingPostProcessor<Dog> dogsRegionDataInitializer() {

			return RegionDataInitializingPostProcessor.<Dog>withRegion("Dogs")
				.useAsEntityIdentifier(Dog::getName)
				.store(Dog.newDog("Spuds"))
				.store(Dog.newDog("Maha"));
		}
	}
}
