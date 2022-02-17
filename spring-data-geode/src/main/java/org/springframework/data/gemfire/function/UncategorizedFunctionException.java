/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.gemfire.function;

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;

/**
 * An {@link FunctionException} indicating a {@link Function} {@link Execution} {@link RuntimeException}
 * that has not be categorized, or identified by the framework.
 *
 * This {@link RuntimeException} was inspired by the {@link org.springframework.dao.UncategorizedDataAccessException}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionException
 * @since 2.3.0
 */
@SuppressWarnings("unused")
public class UncategorizedFunctionException extends FunctionException {

	public UncategorizedFunctionException() {
		super(null, null);
	}

	public UncategorizedFunctionException(String message) {
		super(message, null);
	}

	public UncategorizedFunctionException(Throwable cause) {
		super(null, cause);
	}

	public UncategorizedFunctionException(String message, Throwable cause) {
		super(message, cause);
	}
}
