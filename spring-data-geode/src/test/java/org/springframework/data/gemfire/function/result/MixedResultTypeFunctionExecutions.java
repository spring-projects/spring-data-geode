/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.gemfire.function.result;

import java.math.BigDecimal;
import java.util.List;

import org.apache.geode.cache.execute.Function;

import org.springframework.data.gemfire.function.annotation.FunctionId;
import org.springframework.data.gemfire.function.annotation.OnRegion;

/**
 * The MixedResultTypeFunctionExecutions class declares various Apache Geode {@link Function Functions}
 *  * using SDG's {@link Function} implementation annotation support.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.function.annotation.FunctionId
 * @see org.springframework.data.gemfire.function.annotation.OnRegion
 * @since 2.6.0
 */
@OnRegion(region = "Numbers")
interface MixedResultTypeFunctionExecutions {

	@FunctionId("returnSingleObject")
	BigDecimal returnFive();

	@FunctionId("returnList")
	List<BigDecimal> returnList();

	@FunctionId("returnPrimitive")
	int returnPrimitive();

}
