/*
 * Copyright 2020-2021 the original author or authors.
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

import org.apache.geode.cache.execute.FunctionException;

/**
 * A {@link FunctionException} indicating a timeout during execution.
 *
 * @author John Blum
 * @see org.apache.geode.cache.execute.FunctionException
 * @since 2.3.0
 */
@SuppressWarnings("unused")
public class ExecutionTimeoutFunctionException extends FunctionException {

	public ExecutionTimeoutFunctionException() { }

	public ExecutionTimeoutFunctionException(String message) {
		super(message);
	}

	public ExecutionTimeoutFunctionException(Throwable cause) {
		super(cause);
	}

	public ExecutionTimeoutFunctionException(String message, Throwable cause) {
		super(message, cause);
	}
}
