/*
 * Copyright 2016-2022 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.support.GemfireBeanFactoryLocator.newBeanFactoryLocator;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.util.PropertiesBuilder;

/**
 * Unit Tests for {@link LazyWiringDeclarableSupport}.
 *
 * @author John Blum
 * @see org.junit.Rule
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.data.gemfire.support.GemfireBeanFactoryLocator
 * @see org.springframework.data.gemfire.support.LazyWiringDeclarableSupport
 * @since 1.3.4
 */
public class LazyWiringDeclarableSupportUnitTests {

	private static void assertParameters(Properties parameters, String expectedKey, String expectedValue) {

		assertThat(parameters).isNotNull();
		assertThat(parameters.containsKey(expectedKey)).isTrue();
		assertThat(parameters.getProperty(expectedKey)).isEqualTo(expectedValue);
	}

	private static Properties createParameters(String parameter, String value) {
		return PropertiesBuilder.create().setProperty(parameter, value).build();
	}

	@After
	public void tearDown() {
		SpringContextBootstrappingInitializer.destroy();
	}

	@Test
	public void assertInitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected boolean isInitialized() {
				return true;
			}
		};

		try {
			declarable.assertInitialized();
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void assertInitializedWhenUninitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected boolean isInitialized() {
				return false;
			}
		};

		try {
			declarable.assertInitialized();
		}
		catch (IllegalStateException expected) {

			assertThat(expected)
				.hasMessage("This Declarable object [%s] has not been properly configured and initialized",
					declarable.getClass().getName());

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test
	public void assertUninitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport();

		try {
			declarable.assertUninitialized();
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void assertUninitializedWhenInitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected boolean isInitialized() {
				return true;
			}
		};

		try {
			declarable.assertUninitialized();
		}
		catch (IllegalStateException expected) {

			assertThat(expected)
				.hasMessage("This Declarable object [%s] has already been configured and initialized",
					declarable.getClass().getName());

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test
	public void init() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport();

		try {
			assertThat(declarable.isInitialized()).isFalse();

			declarable.init(createParameters("param", "value"));

			assertParameters(declarable.nullSafeGetParameters(), "param", "value");

			declarable.init(createParameters("newParam", "newValue"));

			assertParameters(declarable.nullSafeGetParameters(), "newParam", "newValue");
			assertThat(declarable.isInitialized()).isFalse();
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test
	public void isInitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected boolean isInitialized() {
				return true;
			}
		};

		assertThat(declarable.isInitialized()).isTrue();
		assertThat(declarable.isNotInitialized()).isFalse();
	}

	@Test
	public void isUninitialized() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected boolean isInitialized() {
				return false;
			}
		};

		assertThat(declarable.isInitialized()).isFalse();
		assertThat(declarable.isNotInitialized()).isTrue();
	}

	@Test
	public void nullSafeGetParametersWithNullReference() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport();

		try {
			declarable.init(null);

			Properties parameters = declarable.nullSafeGetParameters();

			assertThat(parameters).isNotNull();
			assertThat(parameters.isEmpty()).isTrue();
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test
	public void onApplicationEvent() {

		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class);
		ConfigurableListableBeanFactory mockBeanFactory = mock(ConfigurableListableBeanFactory.class);

		when(mockApplicationContext.getBeanFactory()).thenReturn(mockBeanFactory);

		final AtomicBoolean doPostInitCalled = new AtomicBoolean(false);

		TestLazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected void doPostInit(final Properties parameters) {
				super.doPostInit(parameters);
				assertInitialized();
				LazyWiringDeclarableSupportUnitTests.assertParameters(parameters, "param", "value");
				doPostInitCalled.set(true);
			}
		};

		Properties parameters = createParameters("param", "value");

		try {
			declarable.init(parameters);
			declarable.onApplicationEvent(new ContextRefreshedEvent(mockApplicationContext));
			declarable.assertBeanFactory(mockBeanFactory);
			declarable.assertParameters(parameters);

			assertThat(declarable.isInitialized()).isTrue();
			assertThat(doPostInitCalled.get()).isTrue();

			verify(mockApplicationContext, times(1)).getBeanFactory();
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void onApplicationEventWithNullApplicationContext() {

		LazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport();

		try {
			ContextRefreshedEvent mockContextRefreshedEvent = mock(ContextRefreshedEvent.class);

			when(mockContextRefreshedEvent.getApplicationContext()).thenReturn(null);

			declarable.onApplicationEvent(mockContextRefreshedEvent);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected)
				.hasMessage("The Spring ApplicationContext [null] must be an instance of ConfigurableApplicationContext");

			assertThat(expected).hasNoCause();

			throw expected;
		}
		catch (Throwable t) {
			assertThat(declarable.isInitialized()).isFalse();
			throw t;
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(declarable);
		}
	}

	@Test
	public void fullLifecycleOnApplicationEventToDestroy() throws Exception {

		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class);
		ConfigurableListableBeanFactory mockBeanFactory = mock(ConfigurableListableBeanFactory.class);

		when(mockApplicationContext.getBeanFactory()).thenReturn(mockBeanFactory);

		final AtomicBoolean doPostInitCalled = new AtomicBoolean(false);

		TestLazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected void doPostInit(final Properties parameters) {
				super.doPostInit(parameters);
				assertInitialized();
				LazyWiringDeclarableSupportUnitTests.assertParameters(parameters, "param", "value");
				doPostInitCalled.set(true);
			}
		};

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer();

		Properties parameters = createParameters("param", "value");

		try {
			declarable.init(parameters);

			assertThat(declarable.isInitialized()).isFalse();
			assertThat(declarable.nullSafeGetParameters()).isSameAs(parameters);
			assertThat(doPostInitCalled.get()).isFalse();

			initializer.onApplicationEvent(new ContextRefreshedEvent(mockApplicationContext));

			assertThat(declarable.isInitialized()).isTrue();
			assertThat(doPostInitCalled.get()).isTrue();
			declarable.assertBeanFactory(mockBeanFactory);
			declarable.assertParameters(parameters);

			doPostInitCalled.set(false);
			declarable.destroy();

			assertThat(declarable.isInitialized()).isFalse();
			assertThat(declarable.nullSafeGetParameters()).isNotSameAs(parameters);
			assertThat(doPostInitCalled.get()).isFalse();

			initializer.onApplicationEvent(new ContextRefreshedEvent(mockApplicationContext));

			assertThat(declarable.isInitialized()).isFalse();
			assertThat(declarable.nullSafeGetParameters()).isNotSameAs(parameters);
			assertThat(doPostInitCalled.get()).isFalse();

			verify(mockApplicationContext, times(1)).getBeanFactory();
		}
		finally {
			initializer.onApplicationEvent(new ContextClosedEvent(mockApplicationContext));
		}
	}

	@Test
	public void initThenOnApplicationEventThenInitWhenInitialized() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class);

		ConfigurableListableBeanFactory mockConfigurableListableBeanFactory =
			mock(ConfigurableListableBeanFactory.class);

		when(mockApplicationContext.getBeanFactory()).thenReturn(mockConfigurableListableBeanFactory);
		when(mockBeanFactory.getAliases(anyString())).thenReturn(new String[0]);

		GemfireBeanFactoryLocator locator = newBeanFactoryLocator(mockBeanFactory, "MockBeanFactory");

		final AtomicBoolean doPostInitCalled = new AtomicBoolean(false);
		final AtomicReference<String> expectedKey = new AtomicReference<>("testParam");
		final AtomicReference<String> expectedValue = new AtomicReference<>("testValue");

		TestLazyWiringDeclarableSupport declarable = new TestLazyWiringDeclarableSupport() {

			@Override
			protected void doPostInit(final Properties parameters) {
				super.doPostInit(parameters);
				assertInitialized();
				LazyWiringDeclarableSupportUnitTests.assertParameters(parameters, expectedKey.get(), expectedValue.get());
				doPostInitCalled.set(true);
			}
		};

		Properties parameters = createParameters("testParam", "testValue");

		try {
			locator.afterPropertiesSet();

			assertThat(declarable.isInitialized()).isFalse();
			assertThat(declarable.nullSafeGetParameters()).isNotSameAs(parameters);
			assertThat(doPostInitCalled.get()).isFalse();

			declarable.init(parameters);
			declarable.assertBeanFactory(mockBeanFactory);
			declarable.assertParameters(parameters);

			assertThat(declarable.isInitialized()).isTrue();
			assertThat(declarable.nullSafeGetParameters()).isSameAs(parameters);
			assertThat(doPostInitCalled.get()).isTrue();

			doPostInitCalled.set(false);
			declarable.onApplicationEvent(new ContextRefreshedEvent(mockApplicationContext));
			declarable.assertBeanFactory(mockConfigurableListableBeanFactory);
			declarable.assertParameters(parameters);

			assertThat(declarable.isInitialized()).isTrue();
			assertThat(declarable.nullSafeGetParameters()).isSameAs(parameters);
			assertThat(doPostInitCalled.get()).isTrue();

			doPostInitCalled.set(false);
			expectedKey.set("mockKey");
			expectedValue.set("mockValue");
			parameters = createParameters("mockKey", "mockValue");

			declarable.init(parameters);
			declarable.assertBeanFactory(mockBeanFactory);
			declarable.assertParameters(parameters);

			assertThat(declarable.isInitialized()).isTrue();
			assertThat(declarable.nullSafeGetParameters()).isSameAs(parameters);
			assertThat(doPostInitCalled.get()).isTrue();

			verify(mockApplicationContext, times(1)).getBeanFactory();
		}
		finally {
			locator.destroy();
		}
	}

	private static class TestLazyWiringDeclarableSupport extends LazyWiringDeclarableSupport {

		private BeanFactory actualBeanFactory;
		private Properties actualParameters;

		private void assertBeanFactory(BeanFactory expectedBeanFactory) {
			assertThat(this.actualBeanFactory).isSameAs(expectedBeanFactory);
		}

		private void assertParameters(Properties expectedParameters) {
			assertThat(this.actualParameters).isEqualTo(expectedParameters);
		}

		@Override
		void doInit(BeanFactory beanFactory, Properties parameters) {

			this.actualBeanFactory = beanFactory;
			this.actualParameters = parameters;
			this.initialized = true;

			doPostInit(parameters);
		}
	}
}
