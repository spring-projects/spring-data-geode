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
package org.springframework.data.gemfire.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.gemfire.AbstractBasicCacheFactoryBean;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.server.CacheServerFactoryBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring {@link BeanPostProcessor} to enable GemFire/Geode Mock Objects for testing.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.server.CacheServerFactoryBean
 */
public class GemfireTestBeanPostProcessor implements BeanPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(GemfireTestBeanPostProcessor.class);

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if (bean instanceof AbstractBasicCacheFactoryBean) {

			String beanTypeName = bean.getClass().getName();

			bean = bean instanceof ClientCacheFactoryBean
				? new MockClientCacheFactoryBean((ClientCacheFactoryBean) bean)
				: new MockCacheFactoryBean((CacheFactoryBean) bean);

			logger.info("Replacing the [{}] bean definition of type [{}] with mock [{}]...",
				beanName, beanTypeName, bean.getClass().getName());
		}
		else if (bean instanceof CacheServerFactoryBean) {
			((CacheServerFactoryBean) bean).setCache(new StubCache());
		}

		return bean;
	}
}
