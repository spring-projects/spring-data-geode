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

import java.util.Set;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;

import org.springframework.util.Assert;

/**
 * An {@link AbstractFunctionTemplate} implementation for {@link Execution executing} a {@link Function}
 * on a target {@link Region}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionTemplate
 * @see org.springframework.data.gemfire.function.execution.GemfireOnRegionOperations
 */
public class GemfireOnRegionFunctionTemplate extends AbstractFunctionTemplate implements GemfireOnRegionOperations {

	private final Region<?, ?> region;

	/**
	 * Constructs a new instance of the {@link GemfireOnRegionFunctionTemplate} initialized with
	 * the given {@link Region}.
	 *
	 * @param region {@link Region} on which the {@link Function} will be executed.
	 * @throws IllegalArgumentException if {@link Region} is {@literal null}.
	 * @see org.apache.geode.cache.Region
	 */
	public GemfireOnRegionFunctionTemplate(Region<?, ?> region) {

		Assert.notNull(region, "Region must not be null");

		this.region = region;
	}

	@Override
	protected OnRegionFunctionExecution getFunctionExecution() {
		return new OnRegionFunctionExecution(getRegion());
	}

	protected Region<?, ?> getRegion() {
		return this.region;
	}

	@Override
	public <T> Iterable<T> execute(String functionId, Set<?> keys, Object... args) {

		return execute(getFunctionExecution()
			.setKeys(keys)
			.setArguments(args)
			.setFunctionId(functionId)
			.setTimeout(getTimeout()));
	}

	@Override
	public <T> T executeAndExtract(String functionId, Set<?> keys, Object... args) {

		return executeAndExtract(getFunctionExecution()
			.setKeys(keys)
			.setFunctionId(functionId)
			.setTimeout(getTimeout()).setArguments(args));
	}

	@Override
	public void executeWithNoResult(String functionId, Set<?> keys, Object... args) {

		execute(getFunctionExecution()
			.setKeys(keys)
			.setArguments(args)
			.setFunctionId(functionId)
			.setTimeout(getTimeout()), false);
	}
}
