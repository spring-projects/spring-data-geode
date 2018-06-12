/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.app.geode.function.executions;

import org.springframework.data.gemfire.function.annotation.OnServer;

/**
 * The Calculator class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@OnServer(cache = "gemfireCache")
@SuppressWarnings("unused")
public interface Calculator {

	Object add(double operandOne, double operandTwo);

	Object divide(double numerator, double divisor);

	Object factorial(long number);

	Object identity(double value);

	Object multiply(double operandOne, double operandTwo);

	Object squareRoot(double number);

	Object squared(double number);

	Object subtract(double operandOne, double operandTwo);

}
