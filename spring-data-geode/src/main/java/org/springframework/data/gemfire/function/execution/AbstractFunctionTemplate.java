/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.ResultCollector;

import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract base class for all {@link Function} templates, containing operations common to invoking Apache Geode
 * or Pivotal GemFire {@link Function Functions}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.ResultCollector
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.data.gemfire.function.execution.GemfireFunctionOperations
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionExecution
 */
abstract class AbstractFunctionTemplate implements GemfireFunctionOperations, InitializingBean {

	private volatile long timeout;

	private volatile ResultCollector<?, ?> resultCollector;

	@Override
	public void afterPropertiesSet() throws Exception { }

	@Override
	@SuppressWarnings("rawtypes")
	public <T> Iterable<T> execute(Function function, Object... args) {

		AbstractFunctionExecution functionExecution = getFunctionExecution()
			.setArguments(args)
			.setFunction(function);

		return execute(functionExecution);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public <T> T executeAndExtract(Function function, Object... args) {

		AbstractFunctionExecution functionExecution = getFunctionExecution()
			.setArguments(args)
			.setFunction(function);

		return executeAndExtract(functionExecution);
	}

	@Override
	public <T> Iterable<T> execute(String functionId, Object... args) {

		AbstractFunctionExecution functionExecution = getFunctionExecution()
			.setArguments(args)
			.setFunctionId(functionId);

		return execute(functionExecution);
	}

	@Override
	public <T> T executeAndExtract(String functionId, Object... args) {

		AbstractFunctionExecution functionExecution = getFunctionExecution()
			.setArguments(args)
			.setFunctionId(functionId);

		return executeAndExtract(functionExecution);
	}

	@Override
	public void executeWithNoResult(String functionId, Object... args) {

		AbstractFunctionExecution functionExecution = getFunctionExecution()
			.setArguments(args)
			.setFunctionId(functionId);

		execute(functionExecution, false);
	}

	@Override
	public <T> T execute(GemfireFunctionCallback<T> callback) {
		return callback.doInGemfire(getFunctionExecution().getExecution());
	}

	protected <T> Iterable<T> execute(AbstractFunctionExecution functionExecution) {
		 return prepare(functionExecution).execute();
	}

	protected <T> Iterable<T> execute(AbstractFunctionExecution functionExecution, boolean returnResult) {
		 return prepare(functionExecution).execute(returnResult);
	}

	protected <T> T executeAndExtract(AbstractFunctionExecution functionExecution) {
		 return prepare(functionExecution).executeAndExtract();
	}

	AbstractFunctionExecution prepare(AbstractFunctionExecution functionExecution) {

		return functionExecution
			.setResultCollector(getResultCollector())
			.setTimeout(getTimeout());
	}

	protected abstract AbstractFunctionExecution getFunctionExecution();

	public void setResultCollector(ResultCollector<?,?> resultCollector) {
		this.resultCollector = resultCollector;
	}

	public ResultCollector<?,?> getResultCollector() {
		return this.resultCollector;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public long getTimeout() {
		return this.timeout;
	}
}
