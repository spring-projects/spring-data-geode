/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.gemfire.repository.query.support;

/**
 * A Java {@link RuntimeException} indicating that the Apache Geode OQL query could not be executed (i.e. handled)
 * by the {@link OqlQueryExecutor}.
 *
 * @author John Blum
 * @see java.lang.RuntimeException
 * @since 2.4.0
 */
@SuppressWarnings("unused")
public class UnsupportedQueryExecutionException extends RuntimeException {

	/**
	 * Constructs a new, uninitialized instance of {@link UnsupportedQueryExecutionException}.
	 */
	public UnsupportedQueryExecutionException() { }

	/**
	 * Constructs a new instance of {@link UnsupportedQueryExecutionException} initialized with
	 * the given {@link String message} describing the exception.
	 *
	 * @param message {@link String} containing a description of the exception.
	 * @see java.lang.String
	 */
	public UnsupportedQueryExecutionException(String message) {
		super(message);
	}

	/**
	 * Constructs a new instance of {@link UnsupportedQueryExecutionException} initialized with
	 * the given {@link Throwable} as the underlying {@literal cause} of this exception.
	 *
	 * @param cause {@link Throwable} used as the cause of this exception.
	 * @see java.lang.Throwable
	 */
	public UnsupportedQueryExecutionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new instance of {@link UnsupportedQueryExecutionException} initialized with
	 * the given {@link String message} describing the exception and given {@link Throwable}
	 * as the underlying {@literal cause} of this exception.
	 *
	 * @param message {@link String} containing a description of the exception.
	 * @param cause {@link Throwable} used as the cause of this exception.
	 * @see java.lang.String
	 * @see java.lang.Throwable
	 */
	public UnsupportedQueryExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}
