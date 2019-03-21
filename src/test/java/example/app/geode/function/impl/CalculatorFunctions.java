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

package example.app.geode.function.impl;

import org.springframework.data.gemfire.function.annotation.GemfireFunction;
import org.springframework.util.Assert;

/**
 * The {@link CalculatorFunctions} class implements several different calculations.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.function.annotation.GemfireFunction
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class CalculatorFunctions {

	@GemfireFunction(id = "add", hasResult = true)
	public double add(double operandOne, double operandTwo) {
		return operandOne + operandTwo;
	}

	@GemfireFunction(id = "divide", hasResult = true)
	public double divide(double numerator, double divisor) {
		return numerator / divisor;
	}

	@GemfireFunction(id = "factorial", hasResult = true)
	public long factorial(long number) {

		Assert.isTrue(number > -1, "Number be greater than -1");

		long result = number == 2 ? 2 : 1;

		while (number > 1) {
			result *= number--;
		}

		return result;
	}

	@GemfireFunction(id = "identity", hasResult = true)
	public double identity(double value) {
		return value;
	}

	@GemfireFunction(id = "multiply", hasResult = true)
	public double multiply(double  operandOne, double operandTwo) {
		return operandOne * operandTwo;
	}

	@GemfireFunction(id = "squareRoot", hasResult = true)
	public double squareRoot(double number) {
		return Math.sqrt(number);
	}

	@GemfireFunction(id = "squared", hasResult = true)
	public double squared(double number) {
		return number * number;
	}

	@GemfireFunction(id = "subtract", hasResult = true)
	public double subtract(double operandOne, double operandTwo) {
		return operandOne - operandTwo;
	}
}
