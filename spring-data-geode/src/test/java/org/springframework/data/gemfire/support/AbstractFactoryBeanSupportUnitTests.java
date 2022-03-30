/*
 * Copyright 2017-2022 the original author or authors.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.BeanFactory;

import org.slf4j.Logger;

/**
 * Unit Tests for {@link AbstractFactoryBeanSupport}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.Spy
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 * @since 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractFactoryBeanSupportUnitTests {

	@Mock
	private Logger mockLogger;

	@Spy
	private TestFactoryBeanSupport<?> factoryBeanSupport;

	@Before
	public void setup() {
		doReturn(this.mockLogger).when(this.factoryBeanSupport).getLogger();
	}

	@Test
	public void setAndGetBeanClassLoader() {

		assertThat(this.factoryBeanSupport.getBeanClassLoader()).isNull();

		ClassLoader mockClassLoader = mock(ClassLoader.class);

		this.factoryBeanSupport.setBeanClassLoader(mockClassLoader);

		assertThat(this.factoryBeanSupport.getBeanClassLoader()).isSameAs(mockClassLoader);

		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

		this.factoryBeanSupport.setBeanClassLoader(systemClassLoader);

		assertThat(this.factoryBeanSupport.getBeanClassLoader()).isSameAs(systemClassLoader);

		this.factoryBeanSupport.setBeanClassLoader(null);

		assertThat(this.factoryBeanSupport.getBeanClassLoader()).isNull();
	}

	@Test
	public void setAndGetBeanFactory() {

		assertThat(this.factoryBeanSupport.getBeanFactory()).isNull();

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		this.factoryBeanSupport.setBeanFactory(mockBeanFactory);

		assertThat(this.factoryBeanSupport.getBeanFactory()).isSameAs(mockBeanFactory);

		this.factoryBeanSupport.setBeanFactory(null);

		assertThat(this.factoryBeanSupport.getBeanFactory()).isNull();
	}

	@Test
	public void setAndGetBeanName() {

		assertThat(this.factoryBeanSupport.getBeanName()).isNullOrEmpty();

		this.factoryBeanSupport.setBeanName("test");

		assertThat(this.factoryBeanSupport.getBeanName()).isEqualTo("test");

		this.factoryBeanSupport.setBeanName(null);

		assertThat(this.factoryBeanSupport.getBeanName()).isNullOrEmpty();
	}

	@Test
	public void isSingletonDefaultsToTrue() {
		assertThat(this.factoryBeanSupport.isSingleton()).isTrue();
	}

	@Test
	public void logsDebugWhenDebugIsEnabled() {

		when(this.mockLogger.isDebugEnabled()).thenReturn(true);

		this.factoryBeanSupport.logDebug("%s log test", "debug");

		verify(this.mockLogger, times(1)).isDebugEnabled();
		verify(this.mockLogger, times(1)).debug(eq("debug log test"));
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void logsInfoWhenInfoIsEnabled() {

		when(this.mockLogger.isInfoEnabled()).thenReturn(true);

		this.factoryBeanSupport.logInfo("%s log test", "info");

		verify(this.mockLogger, times(1)).isInfoEnabled();
		verify(this.mockLogger, times(1)).info(eq("info log test"));
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void logsWarningWhenWarnIsEnabled() {

		when(this.mockLogger.isWarnEnabled()).thenReturn(true);

		this.factoryBeanSupport.logWarning("%s log test", "warn");

		verify(this.mockLogger, times(1)).isWarnEnabled();
		verify(this.mockLogger, times(1)).warn(eq("warn log test"));
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void logsErrorWhenErrorIsEnabled() {

		when(this.mockLogger.isErrorEnabled()).thenReturn(true);

		this.factoryBeanSupport.logError("%s log test", "error");

		verify(this.mockLogger, times(1)).isErrorEnabled();
		verify(this.mockLogger, times(1)).error(eq("error log test"));
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void suppressesDebugLoggingWhenDebugIsDisabled() {

		when(this.mockLogger.isDebugEnabled()).thenReturn(false);

		this.factoryBeanSupport.logDebug(() -> "test");

		verify(this.mockLogger, times(1)).isDebugEnabled();
		verify(this.mockLogger, never()).debug(any());
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void suppressesInfoLoggingWhenInfoIsDisabled() {

		when(this.mockLogger.isInfoEnabled()).thenReturn(false);

		this.factoryBeanSupport.logInfo(() -> "test");

		verify(this.mockLogger, times(1)).isInfoEnabled();
		verify(this.mockLogger, never()).info(any());
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void suppressesWarnLoggingWhenWarnIsDisabled() {

		when(this.mockLogger.isWarnEnabled()).thenReturn(false);

		this.factoryBeanSupport.logWarning(() -> "test");

		verify(this.mockLogger, times(1)).isWarnEnabled();
		verify(this.mockLogger, never()).warn(any());
		verifyNoMoreInteractions(this.mockLogger);
	}

	@Test
	public void suppressesErrorLoggingWhenErrorIsDisabled() {

		when(this.mockLogger.isErrorEnabled()).thenReturn(false);

		this.factoryBeanSupport.logError(() -> "test");

		verify(this.mockLogger, times(1)).isErrorEnabled();
		verify(this.mockLogger, never()).error(any());
		verifyNoMoreInteractions(this.mockLogger);
	}

	private static class TestFactoryBeanSupport<T> extends AbstractFactoryBeanSupport<T> {

		@Override
		public T getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return Object.class;
		}
	}
}
