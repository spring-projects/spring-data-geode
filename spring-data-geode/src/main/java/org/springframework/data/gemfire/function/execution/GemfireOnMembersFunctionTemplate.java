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

import java.util.Set;

import org.apache.geode.distributed.DistributedMember;

/**
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.springframework.data.gemfire.function.execution.AbstractFunctionTemplate
 */
public class GemfireOnMembersFunctionTemplate extends AbstractFunctionTemplate {

	private final Set<DistributedMember> distributedMembers;

    private final String[] groups;

	public GemfireOnMembersFunctionTemplate() {
		this.distributedMembers = null;
		this.groups = null;
	}

	public GemfireOnMembersFunctionTemplate(Set<DistributedMember> distributedMembers) {
		this.distributedMembers = distributedMembers;
		this.groups = null;
	}

	public GemfireOnMembersFunctionTemplate(String[] groups) {
		this.distributedMembers = null;
		this.groups = groups;
	}

	protected AbstractFunctionExecution getFunctionExecution() {

		if (this.distributedMembers == null && this.groups == null) {
			return new OnAllMembersFunctionExecution();
		}
		else if (this.distributedMembers == null) {
			return new OnMembersInGroupsFunctionExecution(this.groups);
		}

		return new OnDistributedMembersFunctionExecution(this.distributedMembers);
	}
}
