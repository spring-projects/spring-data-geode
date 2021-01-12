/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;

/**
 * Creates an {@literal OnServers} {@link Function} {@link Execution} initialized with
 * either a {@link RegionService} or a {@link Pool}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.RegionService
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.execution.AbstractClientFunctionTemplate
 */
@SuppressWarnings("unused")
public class GemfireOnServersFunctionTemplate extends AbstractClientFunctionTemplate {

	public GemfireOnServersFunctionTemplate(RegionService cache) {
		super(cache);
	}

	public GemfireOnServersFunctionTemplate(Pool pool) {
		super(pool);
	}

	public GemfireOnServersFunctionTemplate(String poolName) {
		super(poolName);
	}

	@Override
	protected AbstractFunctionExecution newFunctionExecutionUsingPool(Pool pool) {
		return new OnServersUsingPoolFunctionExecution(pool);
	}

	@Override
	protected AbstractFunctionExecution newFunctionExecutionUsingRegionService(RegionService regionService) {
		return new OnServersUsingRegionServiceFunctionExecution(regionService);
	}
}
