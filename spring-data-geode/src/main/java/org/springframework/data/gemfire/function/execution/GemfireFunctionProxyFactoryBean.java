/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.stream.StreamSupport;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.gemfire.support.AbstractFactoryBeanSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * A Proxy {@link FactoryBean} for all non-Region Function Execution interfaces.
 *
 * @author David Turanski
 * @author John Blum
 * @author Patrick Johnson
 * @see java.lang.reflect.Method
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.beans.factory.BeanClassLoaderAware
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 */
public class GemfireFunctionProxyFactoryBean extends AbstractFactoryBeanSupport<Object> implements MethodInterceptor {

	private volatile boolean initialized;

	private final Class<?> functionExecutionInterface;

	private FunctionExecutionMethodMetadata<MethodMetadata> methodMetadata;

	private final GemfireFunctionOperations gemfireFunctionOperations;

	private volatile Object functionExecutionProxy;

	/**
	 * Constructs a new instance of the {@link GemfireFunctionProxyFactoryBean} initialized with the given
	 * {@link Class Function Excution Interface} and {@link GemfireFunctionOperations}.
	 *
	 * @param functionExecutionInterface {@link Class Function Execution Interface} to proxy.
	 * @param gemfireFunctionOperations Template class used to delegate the Function invocation.
	 * @see org.springframework.data.gemfire.function.execution.GemfireFunctionOperations
	 * @throws IllegalArgumentException if the {@link Class Function Execution Interface} is {@literal null}
	 * or the {@link Class Function Execution Type} is not an actual interface.
	 */
	public GemfireFunctionProxyFactoryBean(Class<?> functionExecutionInterface,
			GemfireFunctionOperations gemfireFunctionOperations) {

		Assert.notNull(functionExecutionInterface, "Function Execution Interface must not be null");

		Assert.isTrue(functionExecutionInterface.isInterface(),
			String.format("Function Execution type [%s] must be an interface",
				functionExecutionInterface.getName()));

		this.functionExecutionInterface = functionExecutionInterface;
		this.gemfireFunctionOperations = gemfireFunctionOperations;
		this.methodMetadata = new DefaultFunctionExecutionMethodMetadata(functionExecutionInterface);
	}

	@Override
	public ClassLoader getBeanClassLoader() {

		ClassLoader beanClassLoader = super.getBeanClassLoader();

		return beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader();
	}

	protected Class<?> getFunctionExecutionInterface() {
		return this.functionExecutionInterface;
	}

	protected FunctionExecutionMethodMetadata<MethodMetadata> getFunctionExecutionMethodMetadata() {
		return this.methodMetadata;
	}

	protected GemfireFunctionOperations getGemfireFunctionOperations() {
		return this.gemfireFunctionOperations;
	}

	@Override
	public Object invoke(MethodInvocation invocation) {

		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return String.format("Function Proxy for interface [%s]", getFunctionExecutionInterface().getName());
		}

		logDebug("Invoking method {}", invocation.getMethod().getName());

		Object result = invokeFunction(invocation.getMethod(), invocation.getArguments());

		return resolveResult(invocation, result);
	}

	protected Object invokeFunction(Method method, Object[] args) {

		return getGemfireFunctionOperations()
			.executeAndExtract(getFunctionExecutionMethodMetadata().getMethodMetadata(method).getFunctionId(), args);
	}

	protected Object resolveResult(MethodInvocation invocation, Object result) {

		// TODO: This conditional logic needs more work!  For instance, this conditional logic fails if the result
		//  is a List, but the Function (Execution method) return type is a Set.
		return isIterable(result) && isNotInstanceOfFunctionReturnType(invocation, result)
			? resolveSingleResultIfPossible((Iterable<?>) result)
			: result;
	}

	protected Object resolveSingleResultIfPossible(Iterable<?> results) {

		// TODO: Determine whether to throw an IncorrectResultSizeDataAccessException if the cardinality does not match.
		return StreamSupport.stream(results.spliterator(), false).count() == 1
			? results.iterator().next()
			: results;
	}

	protected boolean isInstanceOfFunctionReturnType(MethodInvocation invocation, Object value) {
		return invocation.getMethod().getReturnType().isInstance(value);
	}

	protected boolean isNotInstanceOfFunctionReturnType(MethodInvocation invocation, Object value) {
		return !isInstanceOfFunctionReturnType(invocation, value);
	}

	protected boolean isIterable(Object value) {
		return value instanceof Iterable;
	}

	@Override
	public Object getObject() throws Exception {

		if (this.functionExecutionProxy == null) {
			onInit();
			Assert.notNull(this.functionExecutionProxy, "Failed to initialize Function Proxy");
		}

		return this.functionExecutionProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getFunctionExecutionInterface();
	}

	protected void onInit() {

		if (!this.initialized) {

			ProxyFactory proxyFactory = new ProxyFactory(getFunctionExecutionInterface(), this);

			this.functionExecutionProxy = proxyFactory.getProxy(getBeanClassLoader());
			this.initialized = true;
		}
	}
}
