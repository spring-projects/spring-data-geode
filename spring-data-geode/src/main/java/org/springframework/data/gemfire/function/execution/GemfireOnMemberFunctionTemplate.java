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

import org.apache.geode.distributed.DistributedMember;

/**
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionTemplate
 */
public class GemfireOnMemberFunctionTemplate extends AbstractFunctionTemplate {

	private final DistributedMember distributedMember;

    private final String[] groups;

	public GemfireOnMemberFunctionTemplate() {
		this.distributedMember = null;
		this.groups = null;
	}

	public GemfireOnMemberFunctionTemplate(DistributedMember distributedMember) {
		this.distributedMember = distributedMember;
		this.groups = null;
	}

	public GemfireOnMemberFunctionTemplate(String[] groups) {
		this.distributedMember = null;
		this.groups = groups;
	}

	protected AbstractFunctionExecution getFunctionExecution() {

		if (this.distributedMember == null && this.groups == null) {
			return new OnDefaultMemberFunctionExecution();
		}
		else if (this.distributedMember == null) {
			return new OnMemberInGroupsFunctionExecution(this.groups);
		}

		return new OnDistributedMemberFunctionExecution(this.distributedMember);
	}
}
