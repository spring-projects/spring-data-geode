/*
 * Copyright 2002-2022 the original author or authors.
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
package org.springframework.data.gemfire.function.config;

import java.util.Optional;

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.function.annotation.OnServer;
import org.springframework.data.gemfire.function.annotation.OnServers;
import org.springframework.data.gemfire.function.execution.GemfireFunctionProxyFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link OnServer} and {@link OnServers} {@link Function} {@link Execution}
 * {@link BeanDefinitionBuilder BeanDefinitionBuilders}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @see org.springframework.data.gemfire.function.annotation.OnServer
 * @see org.springframework.data.gemfire.function.annotation.OnServers
 * @see org.springframework.data.gemfire.function.config.AbstractFunctionExecutionBeanDefinitionBuilder
 */
abstract class ServerBasedFunctionExecutionBeanDefinitionBuilder
		extends AbstractFunctionExecutionBeanDefinitionBuilder {

	ServerBasedFunctionExecutionBeanDefinitionBuilder(FunctionExecutionConfiguration configuration) {
		super(configuration);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected BeanDefinitionBuilder getGemfireFunctionOperationsBeanDefinitionBuilder(BeanDefinitionRegistry registry) {

		String resolvedCacheBeanName = Optional.ofNullable(this.configuration.getAttribute("cache"))
			.map(String::valueOf)
			.filter(StringUtils::hasText)
			.orElse(GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME);

		Optional<String> poolBeanName = Optional.ofNullable(this.configuration.getAttribute("pool"))
			.map(String::valueOf)
			.filter(StringUtils::hasText);

		BeanDefinitionBuilder functionTemplateBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(getGemfireFunctionOperationsClass());

		functionTemplateBuilder.addConstructorArgReference(resolvedCacheBeanName);

		poolBeanName.ifPresent(it -> {
			functionTemplateBuilder.addDependsOn(it);
			functionTemplateBuilder.addPropertyReference("pool", it);
		});

		return functionTemplateBuilder;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected Class<?> getFunctionProxyFactoryBeanClass() {
		return GemfireFunctionProxyFactoryBean.class;
	}

	protected abstract Class<?> getGemfireFunctionOperationsClass();

}
