/*
 * Copyright 2010-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.function.sample;

import org.apache.geode.cache.execute.Function;

import org.springframework.data.gemfire.function.annotation.OnServer;

/**
 * The {@link ExceptionThrowingFunctionExecution} interface defines a GemFire {@link Function}
 * that throws a {@link RuntimeException}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.execute.Function
 * @see org.springframework.data.gemfire.function.annotation.OnServer
 * @since 1.7.0
 */
@OnServer
@SuppressWarnings("unused")
public interface ExceptionThrowingFunctionExecution {

	Integer exceptionThrowingFunction();

}
