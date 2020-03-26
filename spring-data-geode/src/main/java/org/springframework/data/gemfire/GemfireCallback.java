/*
 * Copyright 2010-2020 the original author or authors.
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
package org.springframework.data.gemfire;

import org.apache.geode.GemFireCheckedException;
import org.apache.geode.GemFireException;
import org.apache.geode.cache.Region;

/**
 * Callback interface for GemFire code.
 *
 * Implementations of this interface are to be used with {@link GemfireTemplate}'s execution methods, often as anonymous
 * classes within a method implementation. A typical implementation will call Region.get/put/query to perform some
 * operations on stored objects.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.apache.geode.cache.Region
 */
@FunctionalInterface
public interface GemfireCallback<T> {

	/**
	 * This methods gets called by {@link GemfireTemplate#execute(GemfireCallback)}.
	 *
	 * The method implementation does not need to care about handling transactions or exceptions.
	 *
	 * Allows a result {@link Object} created within this callback to be returned, i.e. an application domain object
	 * or a collection of application domain objects.
	 *
	 * A custom thrown {@link RuntimeException} is treated as an application exception; the exception is propagated to
	 * the caller of the template.
	 *
	 * @param region {@link Region} on which the operation of this callback will be performed.
	 * @return a result {@link Object}, or {@literal null} if no result.
	 * @throws GemFireCheckedException for checked {@link Exception Exceptions} occurring in GemFire.
	 * @throws GemFireException for {@link RuntimeException RuntimeExceptions} occurring in GemFire.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 * @see org.apache.geode.cache.Region
	 */
	T doInGemfire(Region<?, ?> region) throws GemFireCheckedException, GemFireException;

}
