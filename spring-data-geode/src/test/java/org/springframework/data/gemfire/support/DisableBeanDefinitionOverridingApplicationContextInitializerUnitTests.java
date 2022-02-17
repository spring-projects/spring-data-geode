/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Unit Tests for {@link DisableBeanDefinitionOverridingApplicationContextInitializer}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.data.gemfire.support.DisableBeanDefinitionOverridingApplicationContextInitializer
 * @since 2.6.0
 */
public class DisableBeanDefinitionOverridingApplicationContextInitializerUnitTests {

	private final DisableBeanDefinitionOverridingApplicationContextInitializer applicationContextInitializer =
		new DisableBeanDefinitionOverridingApplicationContextInitializer();

	@Test
	public void initializeDisablesBeanDefinitionOverriding() {

		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class);

		DefaultListableBeanFactory beanFactory = spy(DefaultListableBeanFactory.class);

		doReturn(beanFactory).when(mockApplicationContext).getBeanFactory();

		assertThat(beanFactory.isAllowBeanDefinitionOverriding()).isTrue();

		applicationContextInitializer.initialize(mockApplicationContext);

		assertThat(beanFactory.isAllowBeanDefinitionOverriding()).isFalse();

		verify(mockApplicationContext, times(1)).getBeanFactory();
		verify(beanFactory, times(2)).isAllowBeanDefinitionOverriding();
		verify(beanFactory, times(1)).setAllowBeanDefinitionOverriding(eq(false));
		verifyNoMoreInteractions(mockApplicationContext, beanFactory);
	}
}
