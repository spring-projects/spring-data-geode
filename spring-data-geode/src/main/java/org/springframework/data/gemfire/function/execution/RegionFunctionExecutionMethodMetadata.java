/*
 * Copyright 2020-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.data.gemfire.function.GemfireFunctionUtils;
import org.springframework.data.gemfire.function.annotation.Filter;

/**
 *
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.data.gemfire.function.execution.FunctionExecutionMethodMetadata
 */
class RegionFunctionExecutionMethodMetadata extends FunctionExecutionMethodMetadata<RegionMethodMetadata>  {

	public RegionFunctionExecutionMethodMetadata(Class<?> serviceInterface) {
		super(serviceInterface);
	}

	@Override
	protected RegionMethodMetadata newMetadataInstance(Method method) {
		return new RegionMethodMetadata(method);
	}

}

class RegionMethodMetadata extends MethodMetadata {

	private final int filterArgPosition;

	public RegionMethodMetadata(Method method) {

		super(method);

		this.filterArgPosition = GemfireFunctionUtils.getAnnotationParameterPosition(method, Filter.class,
			new Class<?>[] { Set.class });
	}

	public int getFilterArgPosition() {
		return this.filterArgPosition;
	}
}
