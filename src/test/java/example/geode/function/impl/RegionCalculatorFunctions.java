/*
 * Copyright 2018 the original author or authors.
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

package example.geode.function.impl;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.shiro.util.Assert;
import org.springframework.data.gemfire.function.annotation.GemfireFunction;

/**
 * The RegionCalculatorFunctions class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class RegionCalculatorFunctions {

	@GemfireFunction(id = "divideInHalf")
	public void divideInHalf(FunctionContext functionContext) {

		Assert.isInstanceOf(RegionFunctionContext.class, functionContext,
			"[divideInHalf] must be executed on a Region");

		RegionFunctionContext regionFunctionContext = (RegionFunctionContext) functionContext;

		Region<String, Number> calculations = regionFunctionContext.getDataSet();

		Number number = calculations.get("number");

		Assert.notNull(number, "Number to divide in half must not be null");

		calculations.put("number", number.intValue() / 2);
	}

	@GemfireFunction(id = "doubleInValue")
	public void doubleInValue(FunctionContext functionContext) {

		Assert.isInstanceOf(RegionFunctionContext.class, functionContext,
			"[doubleInValue] must be executed on a Region");

		RegionFunctionContext regionFunctionContext = (RegionFunctionContext) functionContext;

		Region<String, Number> calculations = regionFunctionContext.getDataSet();

		Number number = calculations.get("number");

		Assert.notNull(number, "Number to double in value must not be null");

		calculations.put("number", number.intValue() * 2);
	}

	@GemfireFunction(id = "factorialOfNumber")
	public void factorial(FunctionContext functionContext) {

		Assert.isInstanceOf(RegionFunctionContext.class, functionContext,
			"[factorial] must be executed on a Region");

		RegionFunctionContext regionFunctionContext = (RegionFunctionContext) functionContext;

		Region<String, Number> calculations = regionFunctionContext.getDataSet();

		Number number = calculations.get("number");

		Assert.notNull(number, "The number to compute a factorial for must not be null");

		calculations.put("number", factorial(number.longValue()));
	}

	private long factorial(long number) {

		org.springframework.util.Assert.isTrue(number > -1, "Number be greater than -1");

		long result = number == 2 ? 2 : 1;

		while (number > 1) {
			result *= number--;
		}

		return result;
	}
}
