/*
 * Copyright 2002-2020 the original author or authors.
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
import org.apache.geode.cache.execute.FunctionService;

import org.apache.shiro.util.Assert;

import org.springframework.util.CollectionUtils;

/**
 * Creates an {@literal OnRegion} {@link Function} {@link Execution} initialized with a {@link Region}
 * using {@link FunctionService#onRegion(Region)}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionExecution
 */
class OnRegionFunctionExecution extends AbstractFunctionExecution {

	private final Region<?, ?> region;

	private volatile Set<?> keys;

	public OnRegionFunctionExecution(Region<?, ?> region) {

		Assert.notNull(region, "Region must not be null");

		this.region = region;
	}

	public OnRegionFunctionExecution setKeys(Set<?> keys) {
		this.keys = keys;
		return this;
	}

	protected Set<?> getKeys() {
		return this.keys;
	}

	protected Region<?, ?> getRegion() {
		return this.region;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Execution getExecution() {

		Execution execution = FunctionService.onRegion(getRegion());

		Set<?> keys = getKeys();

		execution = CollectionUtils.isEmpty(keys) ? execution : execution.withFilter(keys);

		return execution;
	}
}
