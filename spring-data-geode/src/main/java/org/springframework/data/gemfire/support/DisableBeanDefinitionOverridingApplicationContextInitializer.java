/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.gemfire.support;

import java.util.Optional;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring {@link ApplicationContextInitializer} implementation that disables the Spring container's
 * ({@link ConfigurableApplicationContext}) default behavior of bean definition overriding.
 *
 * @author John Blum
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.context.ConfigurableApplicationContext
 * @since 2.6.0
 */
public final class DisableBeanDefinitionOverridingApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	/**
	 * @inheritDoc
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		Optional.ofNullable(applicationContext)
			.map(ConfigurableApplicationContext::getBeanFactory)
			.filter(DefaultListableBeanFactory.class::isInstance)
			.map(DefaultListableBeanFactory.class::cast)
			.ifPresent(beanFactory -> beanFactory.setAllowBeanDefinitionOverriding(false));
	}
}
