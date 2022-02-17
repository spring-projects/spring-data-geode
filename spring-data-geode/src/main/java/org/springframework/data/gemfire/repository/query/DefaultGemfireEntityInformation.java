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
package org.springframework.data.gemfire.repository.query;

import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.lang.NonNull;

/**
 * Implementation of {@link GemfireEntityInformation} and Spring Data's {@link PersistentEntityInformation}
 * that returns the Region name associated with the {@link PersistentEntity}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
 * @see org.springframework.data.gemfire.repository.query.GemfireEntityInformation
 * @see org.springframework.data.repository.core.support.PersistentEntityInformation
 */
// TODO: Move to org.springframework.data.gemfire.repository.core.support
public class DefaultGemfireEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
		implements GemfireEntityInformation<T, ID> {

	private final GemfirePersistentEntity<T> entity;

	/**
	 * Constructs a new instance of {@link DefaultGemfireEntityInformation}
	 * for the given {@link GemfirePersistentEntity}.
	 *
	 * @param entity {@link GemfirePersistentEntity} to wrap; must not be {@literal null}.
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
	 */
	public DefaultGemfireEntityInformation(@NonNull GemfirePersistentEntity<T> entity) {
		super(entity);
		this.entity = entity;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String getRegionName() {
		return entity.getRegionName();
	}
}
