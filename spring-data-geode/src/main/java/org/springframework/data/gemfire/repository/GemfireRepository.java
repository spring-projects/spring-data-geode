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

import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Apache Geode extension of the Spring Data {@link PagingAndSortingRepository} interface.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.repository.PagingAndSortingRepository
 */
public interface GemfireRepository<T, ID> extends PagingAndSortingRepository<T, ID> {

	/**
	 * Save the entity wrapped by the given {@link Wrapper}.
	 *
	 * @param wrapper {@link Wrapper} object wrapping the entity and the identifier of the entity (i.e. key).
	 * @return the saved entity.
	 * @see org.springframework.data.gemfire.repository.Wrapper
	 */
	T save(Wrapper<T, ID> wrapper);

}
