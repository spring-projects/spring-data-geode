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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;

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

		String configLocation = "/org/springframework/data/gemfire/config/xml/MultipleCacheTest-context.xml";

		ConfigurableApplicationContext context1 = new ClassPathXmlApplicationContext(configLocation);
		ConfigurableApplicationContext context2 = new ClassPathXmlApplicationContext(configLocation);

		Cache cache1 = context1.getBean(Cache.class);
		Cache cache2 = context2.getBean(Cache.class);

		assertThat(cache1).isNotNull();
		assertThat(cache2).isSameAs(cache1);

		Region<?, ?> region1 = context1.getBean(Region.class);
		Region<?, ?> region2 = context2.getBean(Region.class);

		assertThat(region1).isNotNull();
		assertThat(region2).isSameAs(region1);
		assertThat(cache1.isClosed()).isFalse();
		assertThat(region1.isDestroyed()).isFalse();

		context1.close();

		assertThat(cache1.isClosed()).isFalse();
		assertThat(region1.isDestroyed()).as("region was destroyed").isFalse();
	}
}
