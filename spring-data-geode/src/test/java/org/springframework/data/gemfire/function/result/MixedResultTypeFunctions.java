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
import java.util.Collections;
import java.util.List;

import org.apache.geode.cache.execute.Function;

import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.stereotype.Component;

/**
 * The {@link MixedResultTypeFunctions} class defines (implements) various Apache Geode {@link Function Functions}
 * using SDG's {@link Function} implementation annotation support.
 *
 * @author John Blum
 * @since 2.6.0
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @see org.springframework.stereotype.Component
 */
@Component
@SuppressWarnings("unused")
public class MixedResultTypeFunctions {

	@GemfireFunction(id = "returnSingleObject", hasResult = true)
	public BigDecimal returnSingleObject() {
		return new BigDecimal(5);
	}

	@GemfireFunction(id = "returnList", hasResult = true)
	public List<BigDecimal> returnList() {
		return Collections.singletonList(new BigDecimal(10));
	}

	@GemfireFunction(id = "returnPrimitive", hasResult = true)
	public int returnPrimitive() {
		return 7;
	}
}
