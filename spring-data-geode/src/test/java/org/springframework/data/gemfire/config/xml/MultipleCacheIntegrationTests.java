/*
 * Copyright 2002-2023 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.util.SpringExtensions;

/**
 * Integration Tests for multiple Apache Geode {@link GemFireCache caches}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 */

public class MultipleCacheIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void testMultipleCaches() {

		String configLocation = getContextXmlFileLocation(MultipleCacheIntegrationTests.class);

		ConfigurableApplicationContext applicationContextOne = null;
		ConfigurableApplicationContext applicationContextTwo = null;

		try {

			applicationContextOne = new ClassPathXmlApplicationContext(configLocation);
			applicationContextTwo = new ClassPathXmlApplicationContext(configLocation);

			Cache cacheOne = applicationContextOne.getBean(Cache.class);
			Cache cacheTwo = applicationContextTwo.getBean(Cache.class);

			assertThat(cacheOne).isNotNull();
			assertThat(cacheTwo).isSameAs(cacheOne);

			Region<?, ?> regionOne = applicationContextOne.getBean(Region.class);
			Region<?, ?> regionTwo = applicationContextTwo.getBean(Region.class);

			assertThat(regionOne).isNotNull();
			assertThat(regionTwo).isSameAs(regionOne);
			assertThat(cacheOne.isClosed()).isFalse();
			assertThat(regionOne.isDestroyed()).isFalse();

			applicationContextOne.close();

			assertThat(cacheOne.isClosed()).isFalse();
			assertThat(regionOne.isDestroyed()).describedAs("Region was destroyed").isFalse();
		}
		finally {

			final ConfigurableApplicationContext applicationContextOneRef = applicationContextOne;
			final ConfigurableApplicationContext applicationContextTwoRef = applicationContextTwo;

			SpringExtensions.safeDoOperation(() -> closeApplicationContext(applicationContextOneRef));
			SpringExtensions.safeDoOperation(() -> closeApplicationContext(applicationContextTwoRef));
		}
	}
}
