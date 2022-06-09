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
package org.springframework.data.gemfire.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * Integration Tests testing the contract and functionality of Spring Framework's Cache Abstraction using Apache Geode
 * as a caching provider applied with Spring Data for Apache Geode.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.5.1
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@ActiveProfiles("replica")
@SuppressWarnings("unused")
public class CachingWithGemFireIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private NamedNumbersService namedNumbersService;

	@Autowired
	@Qualifier("NamedNumbersRegion")
	private Region<String, Integer> namedNumbersRegion;

	@Test(expected = NullPointerException.class)
	public void regionCacheHitIsCorrect() {

		assertThat(namedNumbersRegion.get("eleven")).isNull();
		assertThat(namedNumbersRegion.containsKey("eleven")).isFalse();

		namedNumbersRegion.put("eleven", 11);

		assertThat(namedNumbersRegion.containsKey("eleven")).isTrue();
		assertThat(namedNumbersService.get("eleven").intValue()).isEqualTo(11);
		assertThat(namedNumbersService.wasCacheMiss()).isFalse();

		try {
			namedNumbersRegion.put("eleven", null); // GemFire does not accept null values on put(key, value)
		}
		finally {
			assertThat(namedNumbersRegion.containsKey("eleven")).isTrue();
			assertThat(namedNumbersRegion.get("eleven").intValue()).isEqualTo(11);
			assertThat(namedNumbersService.get("eleven").intValue()).isEqualTo(11);
			assertThat(namedNumbersService.wasCacheMiss()).isFalse();
		}
	}

	@Test
	public void regionCachingIsCorrect() {

		assertThat(namedNumbersService.wasCacheMiss()).isFalse();
		assertThat(namedNumbersService.get("one").intValue()).isEqualTo(1);
		assertThat(namedNumbersService.wasCacheMiss()).isTrue();
		assertThat(namedNumbersService.get("one").intValue()).isEqualTo(1);
		assertThat(namedNumbersService.wasCacheMiss()).isFalse();
		assertThat(namedNumbersService.get("two").intValue()).isEqualTo(2);
		assertThat(namedNumbersService.wasCacheMiss()).isTrue();
		assertThat(namedNumbersService.get("two").intValue()).isEqualTo(2);
		assertThat(namedNumbersService.wasCacheMiss()).isFalse();
		assertThat(namedNumbersService.get("twelve")).isNull();
		assertThat(namedNumbersService.wasCacheMiss()).isTrue();
		assertThat(namedNumbersService.get("twelve")).isNull();
		assertThat(namedNumbersService.wasCacheMiss()).isTrue();
	}

	public static class NamedNumbersService {

		private NamedNumbersInMemoryRepository namedNumbersRepo;

		public final void setNamedNumbersRepo(final NamedNumbersInMemoryRepository namedNumbersRepo) {
			Assert.notNull(namedNumbersRepo, "The 'NamedNumbers' Repository must not be null");
			this.namedNumbersRepo = namedNumbersRepo;
		}

		protected NamedNumbersInMemoryRepository getNamedNumbersRepo() {
			Assert.state(namedNumbersRepo != null,
				"A reference to the 'NamedNumbers' Repository was not properly configured and initialized");
			return namedNumbersRepo;
		}

		@Cacheable("NamedNumbersRegion")
		public Integer get(final String namedNumber) {
			return getNamedNumbersRepo().get(namedNumber);
		}

		public boolean wasCacheMiss() {
			return getNamedNumbersRepo().wasCacheMiss();
		}
	}

	public static class NamedNumbersInMemoryRepository {

		private volatile boolean cacheMiss;

		private Map<String, Integer> namedNumbers;

		@PostConstruct
		public void init() {
			getNamedNumbers();
		}

		public final void setNamedNumbers(final Map<String, Integer> namedNumbers) {
			Assert.notNull(namedNumbers, "The reference to the 'NamedNumbers' Map must not be null");
			this.namedNumbers = namedNumbers;
		}

		protected Map<String, Integer> getNamedNumbers() {
			Assert.state(namedNumbers != null, "The 'NamedNumbers' Map was not properly configured and initialized");
			return namedNumbers;
		}

		public Integer get(final String namedNumber) {
			this.cacheMiss = true;
			return namedNumbers.get(namedNumber);
		}

		public boolean wasCacheMiss() {
			boolean localCacheMiss = this.cacheMiss;
			this.cacheMiss = false;
			return localCacheMiss;
		}
	}
}
