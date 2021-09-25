/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.fork.ServerProcess;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * Integration Tests for {@link ClientCache} {@link Pool Pools}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("all")
public class ClientCachePoolIntegrationTests extends ForkingClientServerIntegrationTestsSupport {

	@BeforeClass
	public static void startGeodeServer() throws Exception {
		startGemFireServer(ServerProcess.class,
			getServerContextXmlFileLocation(ClientCachePoolIntegrationTests.class));
	}

	@Autowired
	@Qualifier("Factorials")
	private Region<Long, Long> factorials;

	@Test
	public void computeFactorials() {

		assertThat(factorials.get(0l)).isEqualTo(1l);
		assertThat(factorials.get(1l)).isEqualTo(1l);
		assertThat(factorials.get(2l)).isEqualTo(2l);
		assertThat(factorials.get(3l)).isEqualTo(6l);
		assertThat(factorials.get(4l)).isEqualTo(24l);
		assertThat(factorials.get(5l)).isEqualTo(120l);
		assertThat(factorials.get(6l)).isEqualTo(720l);
		assertThat(factorials.get(7l)).isEqualTo(5040l);
		assertThat(factorials.get(8l)).isEqualTo(40320l);
		assertThat(factorials.get(9l)).isEqualTo(362880l);
	}

	public static class FactorialsClassLoader implements CacheLoader<Long, Long> {

		@Override
		public Long load(LoaderHelper<Long, Long> helper) throws CacheLoaderException {

			Long number = helper.getKey();

			Assert.notNull(number, "number must not be null");
			Assert.isTrue(number >= 0, String.format("number [%1$d] must be greater than equal to 0", number));

			if (number <= 2l) {
				return (number < 2l ? 1l : 2l);
			}

			long result = number;

			while (--number > 1) {
				result *= number;
			}

			return result;
		}

		@Override
		public void close() { }

	}
}
