/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.gemfire.test.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.Cache;

/**
 * Abstract base test class for native cache implementations.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.cache.Cache
 */
public abstract class AbstractNativeCacheTests<T> {

	protected static final String CACHE_NAME = "Example";

	private T nativeCache;

	private Cache cache;

	@Before
	public void setUp() throws Exception {

		this.nativeCache = newNativeCache();
		this.cache = newCache(nativeCache);
		this.cache.clear();
	}

	@SuppressWarnings("unchecked")
	protected <C extends Cache> C newCache() throws Exception {
		return (C) newCache(newNativeCache());
	}

	protected abstract Cache newCache(T nativeCache);

	protected abstract T newNativeCache() throws Exception;

	@Test
	public void cacheNameIsEqualToExpected() {
		assertThat(cache.getName()).isEqualTo(CACHE_NAME);
	}

	@Test
	public void nativeCacheIsSameAsExpected() {
		assertThat(this.cache.getNativeCache()).isSameAs(this.nativeCache);
	}

	@Test
	public void cachePutIsSuccessful() {

		assertThat(this.cache.get("enescu")).isNull();

		this.cache.put("enescu", "george");

		assertThat(this.cache.get("enescu").get()).isEqualTo("george");
	}

	@Test
	public void cachePutThenClearIsSuccessful() {

		this.cache.put("enescu", "george");
		this.cache.put("vlaicu", "aurel");

		assertThat(this.cache.get("enescu", String.class)).isEqualTo("george");
		assertThat(this.cache.get("vlaicu", String.class)).isEqualTo("aurel");

		this.cache.clear();

		assertThat(this.cache.get("vlaicu")).isNull();
		assertThat(this.cache.get("enescu")).isNull();
	}

	@Test
	public void cachePutThenGetForClassTypeIsSuccessful() {

		this.cache.put("one", Boolean.TRUE);
		this.cache.put("two", 'X');
		this.cache.put("three", 101);
		this.cache.put("four", Math.PI);
		this.cache.put("five", "TEST");

		assertThat(this.cache.get("one", Boolean.class)).isTrue();
		assertThat(this.cache.get("two", Character.class)).isEqualTo('X');
		assertThat(this.cache.get("three", Integer.class)).isEqualTo(101);
		assertThat(this.cache.get("four", Double.class)).isEqualTo(Math.PI);
		assertThat(this.cache.get("five", String.class)).isEqualTo("TEST");
	}
}
