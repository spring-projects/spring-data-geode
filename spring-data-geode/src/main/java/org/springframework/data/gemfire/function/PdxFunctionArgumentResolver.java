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
package org.springframework.data.gemfire.function;

import java.lang.reflect.Method;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.pdx.PdxInstance;

import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

/**
 * {@link PdxFunctionArgumentResolver} is a {@link FunctionArgumentResolver} that automatically resolves PDX types
 * when Apache Geode is configured with {@literal read-serialized} set to {@literal true}, but the application
 * domain model classes are actually on the application classpath.
 *
 * @author John Blum
 * @see org.apache.geode.pdx.PdxInstance
 * @see org.springframework.data.gemfire.function.DefaultFunctionArgumentResolver
 * @since 1.5.2
 */
@SuppressWarnings("unused")
class PdxFunctionArgumentResolver extends DefaultFunctionArgumentResolver {

	@Override
	@SuppressWarnings("rawtypes")
	public Object[] resolveFunctionArguments(@NonNull FunctionContext functionContext) {

		Object[] functionArguments = super.resolveFunctionArguments(functionContext);

		if (isPdxSerializerConfigured()) {

			int index = 0;

			for (Object functionArgument : functionArguments) {
				if (functionArgument instanceof PdxInstance) {

					String className = ((PdxInstance) functionArgument).getClassName();

					if (isDeserializationNecessary(className)) {
						functionArguments[index] = ((PdxInstance) functionArgument).getObject();
					}
				}

				index++;
			}
		}

		return functionArguments;
	}

	@Override
	public Method getFunctionAnnotatedMethod() {
		throw new UnsupportedOperationException("Not Implemented!");
	}

	boolean isPdxSerializerConfigured() {

		try {
			return (CacheFactory.getAnyInstance().getPdxSerializer() != null);
		}
		catch (CacheClosedException ignore) {
			return false;
		}
	}

	boolean isDeserializationNecessary(final String className) {
		return (isOnClasspath(className) && functionAnnotatedMethodHasParameterOfType(className));
	}

	boolean isOnClasspath(final String className) {
		return ClassUtils.isPresent(className, Thread.currentThread().getContextClassLoader());
	}

	boolean functionAnnotatedMethodHasParameterOfType(final String className) {

		for (Class<?> parameterType : getFunctionAnnotatedMethod().getParameterTypes()) {
			if (parameterType.getName().equals(className)) {
				return true;
			}
		}

		return false;
	}
}
