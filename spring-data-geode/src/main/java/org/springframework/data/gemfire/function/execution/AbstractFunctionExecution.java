/*
<<<<<<< HEAD
 * Copyright 2020-2022 the original author or authors.
=======
 * Copyright 2020-2022 the original author or authors.
>>>>>>> 9eae72c6a... DATAGEODE-295 - Polish.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.function.execution;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;

import org.springframework.data.gemfire.function.ExecutionTimeoutFunctionException;
import org.springframework.data.gemfire.function.UncategorizedFunctionException;
import org.springframework.data.gemfire.util.SpringExtensions;
import org.springframework.data.gemfire.util.SpringExtensions.ValueReturningThrowableOperation;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for creating a {@link Function} {@link Execution} using the {@link FunctionService}.
 *
 * @author David Turanski
 * @author John Blum
 * @author Patrick Johnson
 * @see java.util.concurrent.TimeUnit
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.apache.geode.cache.execute.ResultCollector
 */
@SuppressWarnings("unused")
abstract class AbstractFunctionExecution {

	private static final boolean DEFAULT_RETURN_RESULT = true;

	private static final String FUNCTION_EXECUTION_TIMEOUT_ERROR_MESSAGE =
		"Failed to collect Function [%1$s] results in the configured timeout [%2$d ms]";

	private static final String NO_RESULT_ERROR_MESSAGE =
		"Cannot return any result as the Function#hasResult() is false";

	private long timeout;

	@SuppressWarnings("rawtypes")
	private Function function;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private Object[] arguments;

	private volatile ResultCollector<?, ?> resultCollector;

	private String functionId;

	@SuppressWarnings("rawtypes")
	public AbstractFunctionExecution(Function function, Object... arguments) {

		Assert.notNull(function, "Function cannot be null");

		this.function = function;
		this.functionId = function.getId();
		this.arguments = arguments;
	}

	public AbstractFunctionExecution(String functionId, Object... arguments) {

		Assert.hasText(functionId, "Function ID must not be null or empty");

		this.function = null;
		this.functionId = functionId;
		this.arguments = arguments;
	}

	AbstractFunctionExecution() { }

	protected Object[] getArguments() {
		return this.arguments;
	}

	@SuppressWarnings("rawtypes")
	protected abstract Execution getExecution();

	@SuppressWarnings("rawtypes")
	protected Function getFunction() {
		return this.function;
	}

	protected String getFunctionId() {
		return this.functionId;
	}

	protected Set<?> getKeys() {
		return null;
	}

	protected Logger getLogger() {
		return this.logger;
	}

	protected ResultCollector<?, ?> getResultCollector() {
		return this.resultCollector;
	}

	protected long getTimeout() {
		return this.timeout;
	}

	String resolveFunctionIdentifier() {

		return Optional.ofNullable(getFunction())
			.map(ObjectUtils::nullSafeClassName)
			.orElseGet(() -> getFunctionId());
	}

	/**
	 * Executes the configured {@link Function}.
	 *
	 * @param <T> {@link Class type} of the result.
	 * @return an {@link Iterable} containing the results from the {@link Function} {@link Execution}.
	 * @see java.lang.Iterable
	 * @see #execute(Boolean)
	 */
	<T> Iterable<T> execute() {
		return execute(DEFAULT_RETURN_RESULT);
	}

	/**
	 * Executes the configured {@link Function}.
	 *
	 * @param <T> {@link Class type} of the result.
	 * @param returnResult boolean value indicating whether the {@link Function} should return a result
	 * from the {@link Execution}.
	 * @return an {@link Iterable} containing the results from the {@link Function} {@link Execution}.
	 * @see java.lang.Iterable
	 * @see #getExecution()
	 * @see #getFunction()
	 * @see #getFunctionId()
	 * @see #getTimeout()
	 * @see #prepare(Execution)
	 */
	@SuppressWarnings({ "rawtypes" })
	<T> Iterable<T> execute(Boolean returnResult) {

		Execution execution = prepare(getExecution());

		Function function = getFunction();

		ResultCollector<?, ?> resultCollector = function != null
			? execution.execute(function)
			: execution.execute(getFunctionId());

		if (hasNoResult(returnResult, function, resultCollector)) {
			return null;
		}

		long timeout = getTimeout();

		logDebug("Configured timeout is [{} ms]", timeout);
		logDebug("Using ResultCollector [{}]", ObjectUtils.nullSafeClassName(resultCollector));

		Iterable<T> results = null;

		try {

			Object result = timeout > 0
				? SpringExtensions.<T>safeGetValue(getResultWithTimeoutThrowableOperation(resultCollector, timeout),
					newFunctionAndInterruptedExceptionHandler(timeout))
				: resultCollector.getResult();

			results = processResult(result);

			return results;
		}
		catch (FunctionException cause) {

			// TODO: Use a more reliable way to determine that the Function does not return a result!
			//  This only applies to Functions registered by ID!
			if (!cause.getMessage().contains(NO_RESULT_ERROR_MESSAGE)) {
				throw cause;
			}

			return results;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Execution prepare(Execution execution) {

		execution = execution.setArguments(getArguments());
		execution = getResultCollector() != null ? execution.withCollector(getResultCollector()) : execution;
		execution = getKeys() != null ? execution.withFilter(getKeys()) : execution;

		return execution;
	}

	@SuppressWarnings("rawtypes")
	private boolean hasResult(boolean returnResult, Function function, ResultCollector resultCollector) {
		return returnResult && (function == null || function.hasResult());
	}

	@SuppressWarnings("rawtypes")
	private boolean hasNoResult(boolean returnResult, Function function, ResultCollector resultCollector) {
		return !hasResult(returnResult, function, resultCollector);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> ValueReturningThrowableOperation<T> getResultWithTimeoutThrowableOperation(
		ResultCollector resultCollector, long timeout) {

		return () -> (T) resultCollector.getResult(timeout, TimeUnit.MILLISECONDS);
	}

	private <T> java.util.function.Function<Throwable, T> newFunctionAndInterruptedExceptionHandler(long timeout) {

		return cause -> {

			if (cause instanceof FunctionException) {
				throw (FunctionException) cause;
			}
			else if (cause instanceof InterruptedException) {

				String message =
					String.format(FUNCTION_EXECUTION_TIMEOUT_ERROR_MESSAGE, resolveFunctionIdentifier(), timeout);

				throw new ExecutionTimeoutFunctionException(message, cause);
			}

			throw new UncategorizedFunctionException(cause);
		};
	}

	private <T> Iterable<T> processResult(Object result) {

		return replaceSingleNullElementIterableWithEmptyIterable(throwOnExceptionOrReturn(toIterable(
			throwOnExceptionOrReturn(result))));
	}

	private <T> Iterable<T> replaceSingleNullElementIterableWithEmptyIterable(Iterable<T> results) {

		if (results != null) {

			Iterator<T> it = results.iterator();

			if (!it.hasNext()) {
				return results;
			}

			if (it.next() == null && !it.hasNext()) {
				return Collections::emptyIterator;
			}
		}

		return results;
	}

	private <T> Iterable<T> throwOnExceptionOrReturn(Iterable<T> result) {

		Iterator<T> resultIterator = result.iterator();

		if (resultIterator.hasNext()) {
			throwOnExceptionOrReturn(resultIterator.next());
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> Iterable<T> toIterable(Object result) {

		return result instanceof Iterable
			? (Iterable<T>) result
			: (Iterable<T>) Collections.singleton(result);
	}

	/**
	 * Executes the configured {@link Function} and extracts the result as a single value.
	 *
	 * @param <T> {@link Class type} of the result.
	 * @return the result of the {@link Function} {@link Execution} as a single value.
	 * @see #execute()
	 */
	<T> T executeAndExtract() {

		Iterable<T> results = execute();

		if (isEmpty(results)) {
			return null;
		}

		T result = results.iterator().next();

		return throwOnExceptionOrReturn(result);
	}

	private boolean isEmpty(Iterable<?> iterable) {
		return iterable == null || !iterable.iterator().hasNext();
	}

	private <T> T throwOnExceptionOrReturn(T result) {

		if (result instanceof Throwable) {

			Function<?> function = getFunction();

			String message = String.format("Execution of Function [%s] failed", function != null
				? function.getClass().getName()
				: String.format("with ID [%s]", getFunctionId()));

			throw new FunctionException(message, (Throwable) result);
		}

		return result;
	}

	@Deprecated
	protected AbstractFunctionExecution setArgs(Object... args) {
		return setArguments(args);
	}

	protected AbstractFunctionExecution setArguments(Object... arguments) {
		this.arguments = arguments;
		return this;
	}

	@SuppressWarnings("rawtypes")
	protected AbstractFunctionExecution setFunction(Function function) {
		this.function = function;
		return this;
	}

	protected AbstractFunctionExecution setFunctionId(String functionId) {
		this.functionId = functionId;
		return this;
	}

	protected AbstractFunctionExecution setResultCollector(ResultCollector<?, ?> resultCollector) {
		this.resultCollector = resultCollector;
		return this;
	}

	protected AbstractFunctionExecution setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	protected void logDebug(String message, Object... arguments) {

		Logger logger = getLogger();

		if (logger.isDebugEnabled()) {
			logger.debug(message, arguments);
		}
	}
}
