/*
 * Copyright 2012-2022 the original author or authors.
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
 */
package org.springframework.data.gemfire.repository;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Simple {@link Object value object} holding an entity along with the external key in which the entity will be mapped.
 *
 * @author Oliver Gierke
 * @author John Blum
 */
public class Wrapper<T, KEY> {

	private final T entity;

	private final KEY key;

	public Wrapper(@NonNull T entity, @NonNull KEY key) {

		Assert.notNull(entity, "Entity must not be null");
		Assert.notNull(key, "Key must not be null");

		this.entity = entity;
		this.key = key;
	}

	public T getEntity() {
		return this.entity;
	}

	public KEY getKey() {
		return this.key;
	}

	@Override
	public String toString() {
		return String.format("{ key: %1$s, value: %2$s }", getKey(), getEntity());
	}
}
