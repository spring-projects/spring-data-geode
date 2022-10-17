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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.data.gemfire.util.DistributedSystemUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * Integration Tests to test Apache Geode durable clients.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.ClientCacheFactory
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.6.3
 */
@Ignore
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration
@SuppressWarnings("all")
public class DurableClientCacheIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	private static final boolean DEBUG = true;

	private static AtomicBoolean dirtiesContext = new AtomicBoolean(false);

	private static List<Integer> regionCacheListenerEventValues =
		Collections.synchronizedList(new ArrayList<Integer>());

	private static final String CLIENT_CACHE_INTEREST_RESULT_POLICY =
		DurableClientCacheIntegrationTests.class.getName().concat(".interests-result-policy");

	private static final String DURABLE_CLIENT_TIMEOUT =
		DurableClientCacheIntegrationTests.class.getName().concat(".durable-client-timeout");

	private static final String SERVER_HOST = "localhost";

	@BeforeClass
	public static void startGeodeServer() throws IOException {
		startGemFireServer(ServerProcess.class,
			getServerContextXmlFileLocation(DurableClientCacheIntegrationTests.class));
	}

	private static boolean isAfterDirtiesContext() {
		return dirtiesContext.get();
	}

	private static boolean isBeforeDirtiesContext() {
		return !isAfterDirtiesContext();
	}

	private boolean dirtiesContext() {
		return !dirtiesContext.getAndSet(true);
	}

	private <T> T valueBeforeAndAfterDirtiesContext(T valueBefore, T valueAfter) {
		return isBeforeDirtiesContext() ? valueBefore : valueAfter;
	}

	@Autowired
	private ClientCache clientCache;

	@Autowired
	@Qualifier("Example")
	private Region<String, Integer> example;

	@Before
	public void setup() {

		Properties distributedSystemProperties = this.clientCache.getDistributedSystem().getProperties();

		assertThat(distributedSystemProperties).isNotNull();

		assertThat(distributedSystemProperties.getProperty(DistributedSystemUtils.DURABLE_CLIENT_ID_PROPERTY_NAME))
			.isEqualTo(DurableClientCacheIntegrationTests.class.getSimpleName());

		assertThat(distributedSystemProperties.getProperty(DistributedSystemUtils.DURABLE_CLIENT_TIMEOUT_PROPERTY_NAME))
			.isEqualTo(valueBeforeAndAfterDirtiesContext("300", "600"));

		assertRegion(this.example, "Example", DataPolicy.NORMAL);
	}

	@After
	public void tearDown() {

		if (dirtiesContext()) {
			closeClientCache(this.clientCache, true);
			runClientCacheProducer();
			setSystemProperties();
		}

		regionCacheListenerEventValues.clear();
	}

	private void closeClientCache(ClientCache clientCache, boolean keepAlive) {

		Function<GemFireCache, GemFireCache> cacheClosingFunction = cacheToClose -> {
			((ClientCache) cacheToClose).close(keepAlive);
			return cacheToClose;
		};

		if (Objects.nonNull(clientCache)) {
			closeGemFireCacheWaitOnCacheClosedEvent(() -> clientCache, cacheClosingFunction,
				TimeUnit.SECONDS.toMillis(5L));
		}
	}

	private void runClientCacheProducer() {

		ClientCache clientCache = null;

		try {
			clientCache = new ClientCacheFactory()
				.addPoolServer(SERVER_HOST, Integer.getInteger(GEMFIRE_CACHE_SERVER_PORT_PROPERTY))
				.set("name", "ClientCacheProducer")
				.set("log-level", "error")
				.create();

			Region<String, Integer> exampleRegion =
				clientCache.<String, Integer>createClientRegionFactory(ClientRegionShortcut.PROXY)
					.create("Example");

			exampleRegion.put("four", 4);
			exampleRegion.put("five", 5);
		}
		finally {
			closeClientCache(clientCache, false);
		}
	}

	private void setSystemProperties() {

		System.setProperty(CLIENT_CACHE_INTEREST_RESULT_POLICY, InterestResultPolicyType.NONE.name());
		System.setProperty(DURABLE_CLIENT_TIMEOUT, "600");
	}

	private void assertRegion(Region<?, ?> region, String expectedName, DataPolicy expectedDataPolicy) {
		assertRegion(region, expectedName, Region.SEPARATOR + expectedName, expectedDataPolicy);
	}

	private void assertRegion(Region<?, ?> region, String expectedName, String expectedPath,
			DataPolicy expectedDataPolicy) {

		assertThat(region).isNotNull();
		assertThat(region.getName()).isEqualTo(expectedName);
		assertThat(region.getFullPath()).isEqualTo(expectedPath);
		assertThat(region.getAttributes()).isNotNull();
		assertThat(region.getAttributes().getDataPolicy()).isEqualTo(expectedDataPolicy);
	}

	private void assertRegionValues(Region<?, ?> region, Object... values) {

		assertThat(region.size()).isEqualTo(values.length);

		for (Object value : values) {
			assertThat(region.containsValue(value)).isTrue();
		}
	}

	private void log(String message, Object... args) {

		if (DEBUG) {
			System.err.printf(message, args);
			System.err.flush();
		}
	}

	private void waitForRegionEntryEvents() {

		AtomicInteger counter = new AtomicInteger(0);

		waitOn(() -> {

			if (counter.incrementAndGet() % 3 == 0) {
				//log("NOTIFIED!%n");
				this.clientCache.readyForEvents();
			}

			//log("WAITING...%n");

			return regionCacheListenerEventValues.size() < 2;

		}, TimeUnit.SECONDS.toMillis(15L), 500L);
	}

	@Test
	@DirtiesContext
	public void durableClientGetsInitializedWithDataOnServer() {

		assumeTrue(isBeforeDirtiesContext());
		assertRegionValues(this.example, 1, 2, 3);
		assertThat(regionCacheListenerEventValues.isEmpty()).isTrue();
	}

	@Test
	public void durableClientGetsUpdatesFromServerWhileClientWasOffline() {

		assumeTrue(isAfterDirtiesContext());
		assertThat(this.example.isEmpty()).isTrue();

		waitForRegionEntryEvents();

		assertThat(regionCacheListenerEventValues).containsExactly(4, 5);
	}

	public static class ClientCacheBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

			if (bean instanceof Pool && "gemfireServerPool".equals(beanName)) {

				Pool gemfireServerPool = (Pool) bean;

				if (isBeforeDirtiesContext()) {
					// NOTE: A value of -2 indicates the client connected to the server for the first time.
					assertThat(gemfireServerPool.getPendingEventCount()).isEqualTo(-2);
				}
				else {
					// NOTE: The pending event count could be 3 because it should minimally include the 2 puts
					// from the ClientCache producer and possibly a "marker" as well.
					assertThat(gemfireServerPool.getPendingEventCount()).isGreaterThanOrEqualTo(2);
				}
			}

			return bean;
		}
	}

	public static class RegionDataLoadingBeanPostProcessor<K, V> implements BeanPostProcessor {

		private Map<K, V> regionData;

		private final String regionName;

		public RegionDataLoadingBeanPostProcessor(String regionName) {

			Assert.hasText(regionName, "Region name must be specified");

			this.regionName = regionName;
		}

		public void setRegionData(Map<K, V> regionData) {
			this.regionData = regionData;
		}

		protected Map<K, V> getRegionData() {

			Assert.state(this.regionData != null, "Region data was not provided");

			return this.regionData;
		}

		protected String getRegionName() {
			return this.regionName;
		}

		protected void loadData(Region<K, V> region) {
			region.putAll(getRegionData());
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

			if (bean instanceof Region) {

				Region<K, V> region = (Region) bean;

				if (getRegionName().equals(region.getName())) {
					loadData(region);
				}
			}

			return bean;
		}
	}

	public static class RegionEntryEventRecordingCacheListener extends CacheListenerAdapter<String, Integer> {

		@Override
		public void afterCreate(EntryEvent<String, Integer> event) {
			regionCacheListenerEventValues.add(event.getNewValue());
		}
	}
}
