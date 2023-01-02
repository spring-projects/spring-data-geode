/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.LocatorFactoryBean;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;

/**
 * Unit Tests for {@link EnableBeanFactoryLocator} and {@link BeanFactoryLocatorConfiguration}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.config.annotation.BeanFactoryLocatorConfiguration
 * @see org.springframework.data.gemfire.config.annotation.EnableBeanFactoryLocator
 * @since 2.2.1
 */
public class EnableBeanFactoryLocatorConfigurationUnitTests {

	private final BeanFactoryLocatorConfiguration configuration = new BeanFactoryLocatorConfiguration();

	private void testUseBeanFactoryLocatorBeanPostProcessorProcessesBean(Object cacheBean) {

		BeanPostProcessor beanPostProcessor = this.configuration.useBeanFactoryLocatorBeanPostProcessor();

		assertThat(beanPostProcessor).isNotNull();
		assertThat(beanPostProcessor.postProcessBeforeInitialization(cacheBean, "TestCache"))
			.isEqualTo(cacheBean);
	}

	@Test
	public void useBeanFactoryLocatorBeanPostProcessorProcessesCacheFactoryBean() {

		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();

		assertThat(cacheFactoryBean.isUseBeanFactoryLocator()).isFalse();

		testUseBeanFactoryLocatorBeanPostProcessorProcessesBean(cacheFactoryBean);

		assertThat(cacheFactoryBean.isUseBeanFactoryLocator()).isTrue();
	}

	@Test
	public void useBeanFactoryLocatorBeanPostProcessorProcessesClientCacheFactoryBean() {

		ClientCacheFactoryBean clientCacheFactoryBean = new ClientCacheFactoryBean();

		assertThat(clientCacheFactoryBean.isUseBeanFactoryLocator()).isFalse();

		testUseBeanFactoryLocatorBeanPostProcessorProcessesBean(clientCacheFactoryBean);

		assertThat(clientCacheFactoryBean.isUseBeanFactoryLocator()).isTrue();
	}

	@Test
	public void useBeanFactoryLocatorBeanPostProcessorProcessesLocatorFactoryBean() {

		LocatorFactoryBean locatorFactoryBean = new LocatorFactoryBean();

		assertThat(locatorFactoryBean.isUseBeanFactoryLocator()).isFalse();

		testUseBeanFactoryLocatorBeanPostProcessorProcessesBean(locatorFactoryBean);

		assertThat(locatorFactoryBean.isUseBeanFactoryLocator()).isTrue();
	}

	@Test
	public void useBeanFactoryLocatorBeanPostProcessorWillNotProcessObject() {

		Object mockObject = mock(Object.class);

		testUseBeanFactoryLocatorBeanPostProcessorProcessesBean(mockObject);

		verifyNoInteractions(mockObject);
	}

	@Test
	public void useBeanFactoryLocatorClientCacheConfigurerIsCorrect() throws Exception {

		ClientCacheFactoryBean factoryBean = new ClientCacheFactoryBean();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isFalse();

		factoryBean.setClientCacheConfigurers(this.configuration.useBeanFactoryLocatorClientCacheConfigurer());
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isTrue();
	}

	@Test
	public void useBeanFactoryLocatorLocatorConfigurerIsCorrect() throws Exception {

		LocatorFactoryBean factoryBean = spy(new LocatorFactoryBean());

		doNothing().when(factoryBean).init();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isFalse();

		factoryBean.setLocatorConfigurers(this.configuration.useBeanFactoryLocatorLocatorConfigurer());
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isTrue();

		verify(factoryBean, times(1)).setUseBeanFactoryLocator(eq(true));
	}

	@Test
	public void useBeanFactoryLocatorPeerCacheConfigurerIsCorrect() throws Exception {

		CacheFactoryBean factoryBean = new CacheFactoryBean();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isFalse();

		factoryBean.setPeerCacheConfigurers(this.configuration.useBeanFactoryLocatorPeerCacheConfigurer());
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.isUseBeanFactoryLocator()).isTrue();
	}
}
