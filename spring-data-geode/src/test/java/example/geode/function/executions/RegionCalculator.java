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
package example.geode.function.executions;

import org.springframework.data.gemfire.function.annotation.FunctionId;
import org.springframework.data.gemfire.function.annotation.OnRegion;

/**
 * The RegionCalculator class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@OnRegion(region = "Calculations")
@SuppressWarnings("unused")
public interface RegionCalculator {

	void divideInHalf();

	void doubleInValue();

	@FunctionId("factorialOfNumber")
	void factorial();

}
