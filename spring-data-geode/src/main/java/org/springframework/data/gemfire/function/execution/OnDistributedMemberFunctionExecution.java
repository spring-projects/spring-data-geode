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

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.distributed.DistributedMember;

import org.springframework.util.Assert;

/**
 * Creates an {@literal OnMember} {@link Function} {@link Execution} initialized with a {@link DistributedMember}
 * using {@link FunctionService#onMember(DistributedMember)}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.apache.geode.distributed.DistributedMember
 */
class OnDistributedMemberFunctionExecution extends AbstractFunctionExecution {

	private final DistributedMember distributedMember;

	public OnDistributedMemberFunctionExecution(DistributedMember distributedMember) {

		Assert.notNull(distributedMember, "DistributedMember must not be null");

		this.distributedMember = distributedMember;
	}

	protected DistributedMember getDistributedMember() {
		return this.distributedMember;
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Execution getExecution() {
		return FunctionService.onMember(getDistributedMember());
	}
}
