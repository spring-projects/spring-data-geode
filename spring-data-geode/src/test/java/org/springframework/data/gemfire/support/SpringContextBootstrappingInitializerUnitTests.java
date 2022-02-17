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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.Cache;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.ObjectUtils;

import org.slf4j.Logger;

/**
 * Unit Tests for {@link SpringContextBootstrappingInitializer}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ConfigurableApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @see org.springframework.data.gemfire.support.SpringContextBootstrappingInitializer
 * @since 1.4.0
 */
@SuppressWarnings("unused")
public class SpringContextBootstrappingInitializerUnitTests {

	private static Properties createParameters(String parameter, String value) {

		Properties parameters = new Properties();

		parameters.setProperty(parameter, value);

		return parameters;
	}

	private static Properties createParameters(Properties parameters, String parameter, String value) {

		parameters.setProperty(parameter, value);

		return parameters;
	}

	private final Cache mockCache = mock(Cache.class);

	@After
	public void tearDown() {
		SpringContextBootstrappingInitializer.destroy();
	}

	@Test
	public void getInitializedApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "testGetApplicationContext");

		SpringContextBootstrappingInitializer.applicationContext = mockApplicationContext;

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isSameAs(mockApplicationContext);
	}

	@Test(expected = IllegalStateException.class)
	public void getUninitializedApplicationContext() {

		try {
			SpringContextBootstrappingInitializer.getApplicationContext();
		}
		catch (IllegalStateException expected) {

			assertThat(expected.getMessage())
				.contains("A Spring ApplicationContext was not configured and initialized properly");

			assertThat(expected.getCause()).isNull();

			throw expected;
		}
	}

	@Test
	public void setBeanClassLoaderWithCurrentThreadContextClassLoader() {

		assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();

		SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Test
	public void setBeanClassLoaderWithCurrentThreadContextClassLoaderWhenApplicationContextIsInactive() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockApplicationContext");

		when(mockApplicationContext.isActive()).thenReturn(false);

		SpringContextBootstrappingInitializer.applicationContext = mockApplicationContext;
		SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

		verify(mockApplicationContext, times(1)).isActive();
	}

	@Test(expected = IllegalStateException.class)
	public void setBeanClassLoaderWithCurrentThreadContextClassLoaderWhenApplicationContextIsActive() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockApplicationContext");

		when(mockApplicationContext.isActive()).thenReturn(true);

		try {
			SpringContextBootstrappingInitializer.applicationContext = mockApplicationContext;
			SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
		}
		catch (IllegalStateException expected) {

			assertThat(expected.getMessage()).contains("A Spring ApplicationContext has already been initialized");

			assertThat(expected.getCause()).isNull();

			throw expected;
		}
		finally {
			verify(mockApplicationContext, times(1)).isActive();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void createApplicationContextWhenAnnotatedClassesBasePackagesAndConfigLocationsAreUnspecified() {

		try {
			new SpringContextBootstrappingInitializer().createApplicationContext(null, null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected.getMessage())
				.contains("'AnnotatedClasses', 'basePackages' or 'configLocations' must be specified"
					+ " in order to construct and configure an instance of the ConfigurableApplicationContext");

			assertThat(expected.getCause()).isNull();

			throw expected;
		}
	}

	@Test
	public void createAnnotationBasedApplicationContextWithAnnotatedClasses() {

		AnnotationConfigApplicationContext mockAnnotationApplicationContext =
			mock(AnnotationConfigApplicationContext.class, "MockAnnotationApplicationContext");

		ConfigurableApplicationContext mockXmlApplicationContext =
			mock(ConfigurableApplicationContext.class, "MockXmlApplicationContext");

		Class<?>[] annotatedClasses = { TestAppConfigOne.class, TestAppConfigTwo.class };

		Arrays.stream(annotatedClasses).forEach(SpringContextBootstrappingInitializer::register);

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer() {

			@Override
			ConfigurableApplicationContext newApplicationContext(String[] configLocations) {

				return ObjectUtils.isEmpty(configLocations)
					? mockAnnotationApplicationContext
					: mockXmlApplicationContext;
			}
		});

		doReturn(mockAnnotationApplicationContext)
			.when(initializer).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));

		ConfigurableApplicationContext actualApplicationContext =
			initializer.createApplicationContext(null, null);

		assertThat(actualApplicationContext).isSameAs(mockAnnotationApplicationContext);

		verify(initializer, times(1))
			.doRegister(eq(mockAnnotationApplicationContext), eq(annotatedClasses));

		verifyNoInteractions(mockXmlApplicationContext);
	}

	@Test
	public void createAnnotationBasedApplicationContextWithBasePackages() {

		AnnotationConfigApplicationContext mockAnnotationApplicationContext =
			mock(AnnotationConfigApplicationContext.class,"MockAnnotationApplicationContext");

		ConfigurableApplicationContext mockXmlApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockXmlApplicationContext");

		String[] basePackages = { "org.example.app" };

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer() {

			@Override
			ConfigurableApplicationContext newApplicationContext(String[] configLocations) {

				return ObjectUtils.isEmpty(configLocations)
					? mockAnnotationApplicationContext
					: mockXmlApplicationContext;
			}
		});

		doReturn(mockAnnotationApplicationContext)
			.when(initializer).doScan(any(ConfigurableApplicationContext.class), any(String[].class));

		ConfigurableApplicationContext actualApplicationContext =
			initializer.createApplicationContext(basePackages, null);

		assertThat(actualApplicationContext).isSameAs(mockAnnotationApplicationContext);

		verify(initializer, times(1))
			.scanBasePackages(eq(mockAnnotationApplicationContext), eq(basePackages));
	}

	@Test
	public void createXmlBasedApplicationContext() {

		ConfigurableApplicationContext mockAnnotationApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockAnnotationApplicationContext");

		ConfigurableApplicationContext mockXmlApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockXmlApplicationContext");

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer() {

			@Override
			ConfigurableApplicationContext newApplicationContext(final String[] configLocations) {

				return ObjectUtils.isEmpty(configLocations)
					? mockAnnotationApplicationContext
					: mockXmlApplicationContext;
			}
		};

		ConfigurableApplicationContext actualApplicationContext =
			initializer.createApplicationContext(null, new String[] { "/path/to/application/context.xml" });

		assertThat(actualApplicationContext).isSameAs(mockXmlApplicationContext);
	}

	@Test
	public void initApplicationContext() {

		AbstractApplicationContext mockApplicationContext =
			mock(AbstractApplicationContext.class,"MockApplicationContext");

		SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer();

		assertThat(initializer.initApplicationContext(mockApplicationContext)).isSameAs(mockApplicationContext);

		verify(mockApplicationContext, times(1)).addApplicationListener(same(initializer));
		verify(mockApplicationContext, times(1)).registerShutdownHook();
		verify(mockApplicationContext, times(1)).setClassLoader(eq(Thread.currentThread().getContextClassLoader()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void initApplicationContextWithNull() {

		try {
			new SpringContextBootstrappingInitializer().initApplicationContext(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected.getMessage()).contains("ConfigurableApplicationContext must not be null");

			assertThat(expected.getCause()).isNull();

			throw expected;
		}
	}

	@Test
	public void refreshApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class,"MockApplicationContext");

		assertThat(new SpringContextBootstrappingInitializer().refreshApplicationContext(mockApplicationContext))
			.isSameAs(mockApplicationContext);

		verify(mockApplicationContext, times(1)).refresh();
	}

	@Test(expected = IllegalArgumentException.class)
	public void refreshApplicationContextWithNull() {

		try {
			new SpringContextBootstrappingInitializer().refreshApplicationContext(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected.getMessage()).contains("ConfigurableApplicationContext must not be null");

			assertThat(expected.getCause()).isNull();

			throw expected;
		}
	}

	@Test
	public void registerAnnotatedClasses() {

		AnnotationConfigApplicationContext mockApplicationContext =
			mock(AnnotationConfigApplicationContext.class,"MockApplicationContext");

		Class<?>[] annotatedClasses = { TestAppConfigOne.class, TestAppConfigTwo.class };

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));

		assertThat(initializer.registerAnnotatedClasses(mockApplicationContext, annotatedClasses))
			.isSameAs(mockApplicationContext);

		verify(initializer, times(1))
			.doRegister(eq(mockApplicationContext), eq(annotatedClasses));
	}

	@Test
	public void registerAnnotatedClassesWithEmptyAnnotatedClassesArray() {

		AnnotationConfigApplicationContext mockApplicationContext =
			mock(AnnotationConfigApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));

		assertThat(initializer.registerAnnotatedClasses(mockApplicationContext, new Class<?>[0]))
			.isSameAs(mockApplicationContext);

		verify(initializer, never()).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));
	}

	@Test
	public void registerAnnotatedClassesWithNonAnnotationBasedApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));

		assertThat(
			initializer.registerAnnotatedClasses(mockApplicationContext, new Class<?>[] { TestAppConfigOne.class }))
			.isSameAs(mockApplicationContext);

		verify(initializer, never()).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));
	}

	@Test
	public void scanBasePackages() {

		AnnotationConfigApplicationContext mockApplicationContext =
			mock(AnnotationConfigApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doScan(any(ConfigurableApplicationContext.class), any(String[].class));

		String[] basePackages = { "org.example.app", "org.example.plugins" };

		assertThat(initializer.scanBasePackages(mockApplicationContext, basePackages)).isSameAs(mockApplicationContext);

		verify(initializer, times(1)).doScan(eq(mockApplicationContext), eq(basePackages));
	}

	@Test
	public void scanBasePackagesWithEmptyBasePackagesArray() {

		AnnotationConfigApplicationContext mockApplicationContext =
			mock(AnnotationConfigApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doScan(any(ConfigurableApplicationContext.class), any(String[].class));

		assertThat(initializer.scanBasePackages(mockApplicationContext, null)).isSameAs(mockApplicationContext);

		verify(initializer, never()).doScan(any(ConfigurableApplicationContext.class), any(String[].class));
	}

	@Test
	public void scanBasePackagesWithNonAnnotationBasedApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer());

		doReturn(mockApplicationContext)
			.when(initializer).doScan(any(ConfigurableApplicationContext.class), any(String[].class));

		assertThat(initializer.scanBasePackages(mockApplicationContext, new String[] { "org.example.app" }))
			.isSameAs(mockApplicationContext);

		verify(initializer, never()).doScan(any(ConfigurableApplicationContext.class), any(String[].class));
	}

	@Test
	public void setClassLoader() {

		AbstractApplicationContext mockApplicationContext =
			mock(AbstractApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

		assertThat(new SpringContextBootstrappingInitializer().setClassLoader(mockApplicationContext))
			.isSameAs(mockApplicationContext);

		verify(mockApplicationContext, times(1))
			.setClassLoader(eq(Thread.currentThread().getContextClassLoader()));
	}

	@Test
	public void setClassLoaderWithNonSettableClassLoaderApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

		assertThat(new SpringContextBootstrappingInitializer().setClassLoader(mockApplicationContext))
			.isSameAs(mockApplicationContext);
	}

	@Test
	public void setClassLoaderWithNullClassLoader() {

		AbstractApplicationContext mockApplicationContext =
			mock(AbstractApplicationContext.class, "MockApplicationContext");

		SpringContextBootstrappingInitializer.setBeanClassLoader(null);

		assertThat(new SpringContextBootstrappingInitializer().setClassLoader(mockApplicationContext))
			.isSameAs(mockApplicationContext);

		verify(mockApplicationContext, never()).setClassLoader(any(ClassLoader.class));
	}

	@Test
	public void nullSafeGetApplicationContextIdWithNullReference() {
		assertThat(new SpringContextBootstrappingInitializer().nullSafeGetApplicationContextId(null)).isNull();
	}

	@Test
	public void nullSafeGetApplicationContextIdWithNonNullReference() {

		ApplicationContext mockApplicationContext = mock(ApplicationContext.class, "MockApplicationContext");

		when(mockApplicationContext.getId()).thenReturn("123");

		assertThat(new SpringContextBootstrappingInitializer().nullSafeGetApplicationContextId(mockApplicationContext))
			.isEqualTo("123");
	}

	@Test
	public void testInitWithAnnotatedClasses() {

		AnnotationConfigApplicationContext mockApplicationContext =
			mock(AnnotationConfigApplicationContext.class, "testInitWithAnnotatedClasses");

		doNothing().when(mockApplicationContext).addApplicationListener(any(ApplicationListener.class));
		doNothing().when(mockApplicationContext).registerShutdownHook();
		doNothing().when(mockApplicationContext).refresh();

		when(mockApplicationContext.getId()).thenReturn("testInitWithAnnotatedClasses");
		when(mockApplicationContext.isRunning()).thenReturn(true);

		assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();

		SpringContextBootstrappingInitializer.register(TestAppConfigOne.class);
		SpringContextBootstrappingInitializer.register(TestAppConfigTwo.class);

		SpringContextBootstrappingInitializer initializer = spy(new SpringContextBootstrappingInitializer() {

			@Override
			protected ConfigurableApplicationContext newApplicationContext(String[] configLocations) {
				return mockApplicationContext;
			}
		});

		doReturn(mockApplicationContext)
			.when(initializer).doRegister(any(ConfigurableApplicationContext.class), any(Class[].class));

		initializer.init(this.mockCache, createParameters("test", "test"));

		verify(mockApplicationContext, times(1)).addApplicationListener(same(initializer));
		verify(mockApplicationContext, times(1)).registerShutdownHook();
		verify(initializer, never()).doScan(any(ConfigurableApplicationContext.class), any(String[].class));
		verify(initializer, times(1)).doRegister(eq(mockApplicationContext), eq(new Class[] {
			TestAppConfigOne.class, TestAppConfigTwo.class }));

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isEqualTo(mockApplicationContext);
	}

	@Test
	public void testInitWithExistingApplicationContext() {

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "testInitWithExistingApplicationContext");

		when(mockApplicationContext.isActive()).thenReturn(true);
		when(mockApplicationContext.getId()).thenReturn("testInitWithExistingApplicationContext");

		SpringContextBootstrappingInitializer.applicationContext = mockApplicationContext;

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isSameAs(mockApplicationContext);

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer();

		initializer.init(this.mockCache, createParameters("test", "test"));

		verify(mockApplicationContext, never()).addApplicationListener(any(SpringContextBootstrappingInitializer.class));
		verify(mockApplicationContext, never()).registerShutdownHook();
		verify(mockApplicationContext, never()).refresh();

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isSameAs(mockApplicationContext);
	}

	@Test
	public void testInitWhenApplicationContextIsNull() {

		assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class, "testInitWhenApplicationContextIsNull");

		when(mockApplicationContext.getId()).thenReturn("testInitWhenApplicationContextIsNull");
		when(mockApplicationContext.isRunning()).thenReturn(true);

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer() {

			@Override
			protected ConfigurableApplicationContext createApplicationContext(String[] basePackages,
					String[] configLocations) {

				return mockApplicationContext;
			}
		};

		Properties parameters = createParameters(SpringContextBootstrappingInitializer.CONTEXT_CONFIG_LOCATIONS_PARAMETER,
			"/path/to/spring/application/context.xml");

		initializer.init(this.mockCache, parameters);

		verify(mockApplicationContext, times(1)).addApplicationListener(same(initializer));
		verify(mockApplicationContext, times(1)).registerShutdownHook();
		verify(mockApplicationContext, times(1)).refresh();

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isSameAs(mockApplicationContext);
	}

	@Test
	public void testInitWhenApplicationContextIsInactive() {

		ConfigurableApplicationContext mockInactiveApplicationContext =
			mock(ConfigurableApplicationContext.class, "testInitWhenApplicationContextIsInactive.Inactive");

		when(mockInactiveApplicationContext.isActive()).thenReturn(false);

		SpringContextBootstrappingInitializer.applicationContext = mockInactiveApplicationContext;

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext())
			.isSameAs(mockInactiveApplicationContext);

		final ConfigurableApplicationContext mockNewApplicationContext = mock(ConfigurableApplicationContext.class,
			"testInitWhenApplicationContextIsInactive.New");

		when(mockNewApplicationContext.getId()).thenReturn("testInitWhenApplicationContextIsInactive.New");
		when(mockNewApplicationContext.isRunning()).thenReturn(true);

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer() {

			@Override
			protected ConfigurableApplicationContext createApplicationContext(String[] basePackages,
					String[] configLocations) {

				return mockNewApplicationContext;
			}
		};

		initializer.init(this.mockCache, createParameters(SpringContextBootstrappingInitializer.BASE_PACKAGES_PARAMETER,
			"org.example.app"));

		verify(mockNewApplicationContext, times(1)).addApplicationListener(same(initializer));
		verify(mockNewApplicationContext, times(1)).registerShutdownHook();
		verify(mockNewApplicationContext, times(1)).refresh();

		assertThat(SpringContextBootstrappingInitializer.getApplicationContext()).isSameAs(mockNewApplicationContext);
	}

	@Test(expected = ApplicationContextException.class)
	public void testInitWhenBasePackagesAndContextConfigLocationsParametersAreUnspecified() {

		assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();

		try {

			Properties parameters = createParameters(createParameters(
				SpringContextBootstrappingInitializer.CONTEXT_CONFIG_LOCATIONS_PARAMETER, ""),
				SpringContextBootstrappingInitializer.BASE_PACKAGES_PARAMETER, "  ");

			new SpringContextBootstrappingInitializer().init(this.mockCache, parameters);
		}
		catch (ApplicationContextException expected) {

			assertThat(expected.getMessage()).contains("Failed to bootstrap the Spring ApplicationContext");
			assertThat(expected.getCause()).isInstanceOf(IllegalArgumentException.class);

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testInitWhenApplicationContextIsNotRunning() {
		assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();

		ConfigurableApplicationContext mockApplicationContext =
			mock(ConfigurableApplicationContext.class,"testInitWhenApplicationContextIsNotRunning");

		when(mockApplicationContext.getId()).thenReturn("testInitWhenApplicationContextIsNotRunning");
		when(mockApplicationContext.isRunning()).thenReturn(false);

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer() {

			@Override
			protected ConfigurableApplicationContext createApplicationContext(String[] basePackages,
					String[] configLocations) {

				return mockApplicationContext;
			}
		};

		try {

			Properties parameters = createParameters(SpringContextBootstrappingInitializer.BASE_PACKAGES_PARAMETER,
				"org.example.app, org.example.plugins");

			initializer.init(this.mockCache, parameters);

			SpringContextBootstrappingInitializer.getApplicationContext();
		}
		catch (ApplicationContextException expected) {

			assertThat(expected.getMessage().contains("Failed to bootstrap the Spring ApplicationContext")).isTrue();
			assertThat(expected.getCause() instanceof IllegalStateException).isTrue();
			assertThat(expected.getCause().getMessage()).isEqualTo(
				"The Spring ApplicationContext (testInitWhenApplicationContextIsNotRunning) failed to be properly initialized with the context config files ([]) or base packages ([org.example.app, org.example.plugins])!");

			throw (IllegalStateException) expected.getCause();
		}
		finally {

			verify(mockApplicationContext, times(1)).addApplicationListener(same(initializer));
			verify(mockApplicationContext, times(1)).registerShutdownHook();
			verify(mockApplicationContext, times(1)).refresh();

			assertThat(SpringContextBootstrappingInitializer.applicationContext).isNull();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testInitLogsErrors() throws Throwable {

		Logger mockLog = mock(Logger.class, "testInitLogsErrors.MockLog");

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer() {

			@Override
			protected Logger initLogger() {
				return mockLog;
			}

			@Override
			protected ConfigurableApplicationContext createApplicationContext(String[] basePackages,
					String[] configLocations) {

				throw new IllegalStateException("TEST");
			}
		};

		try {

			Properties parameters = createParameters(SpringContextBootstrappingInitializer.CONTEXT_CONFIG_LOCATIONS_PARAMETER,
				"classpath/to/spring/application/context.xml");

			initializer.init(this.mockCache, parameters);
		}
		catch (ApplicationContextException expected) {

			assertThat(expected.getMessage().contains("Failed to bootstrap the Spring ApplicationContext")).isTrue();
			assertThat(expected.getCause() instanceof IllegalStateException).isTrue();
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected.getCause();
		}
		finally {
			verify(mockLog, times(1))
				.error(eq("Failed to bootstrap the Spring ApplicationContext"), any(RuntimeException.class));
		}
	}

	protected static void assertNotified(TestApplicationListener listener, ApplicationContextEvent expectedEvent) {

		assertThat(listener).isNotNull();
		assertThat(listener.isNotified()).isTrue();
		assertThat(listener.getActualEvent()).isSameAs(expectedEvent);
	}

	protected static void assertUnnotified(TestApplicationListener listener) {

		assertThat(listener).isNotNull();
		assertThat(listener.isNotified()).isFalse();
		assertThat(listener.getActualEvent()).isNull();
	}

	@Test
	public void onContextClosedApplicationEvent() {

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testOnContextClosedApplicationEvent");

		try {

			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);

			assertUnnotified(testApplicationListener);

			SpringContextBootstrappingInitializer.contextRefreshedEvent =
				mock(ContextRefreshedEvent.class,"MockContextRefreshedEvent");

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent)
				.isInstanceOf(ContextRefreshedEvent.class);

			new SpringContextBootstrappingInitializer()
				.onApplicationEvent(mock(ContextClosedEvent.class,"MockContextClosedEvent"));

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();
			assertUnnotified(testApplicationListener);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void onContextRefreshedApplicationEvent() {

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testOnContextRefreshedApplicationEvent");

		try {
			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();
			assertUnnotified(testApplicationListener);

			ContextRefreshedEvent mockContextRefreshedEvent =
				mock(ContextRefreshedEvent.class, "MockContextRefreshedEvent");

			new SpringContextBootstrappingInitializer().onApplicationEvent(mockContextRefreshedEvent);

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isSameAs(mockContextRefreshedEvent);
			assertNotified(testApplicationListener, mockContextRefreshedEvent);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void onContextStartedApplicationEvent() {

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testOnContextStartedApplicationEvent");

		try {
			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();
			assertUnnotified(testApplicationListener);

			new SpringContextBootstrappingInitializer().onApplicationEvent(mock(ContextStartedEvent.class,
				"MockContextStartedEvent"));

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();
			assertUnnotified(testApplicationListener);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void onContextStoppedApplicationEvent() {

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testOnContextStartedApplicationEvent");

		try {
			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);

			assertUnnotified(testApplicationListener);

			ContextRefreshedEvent mockContextRefreshedEvent = mock(ContextRefreshedEvent.class,
				"MockContextRefreshedEvent");

			SpringContextBootstrappingInitializer.contextRefreshedEvent = mockContextRefreshedEvent;

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isSameAs(mockContextRefreshedEvent);

			new SpringContextBootstrappingInitializer().onApplicationEvent(mock(ContextStoppedEvent.class,
				"MockContextStoppedEvent"));

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isSameAs(mockContextRefreshedEvent);
			assertUnnotified(testApplicationListener);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void onApplicationEventWithMultipleRegisteredApplicationListeners() {

		TestApplicationListener testApplicationListenerOne = new TestApplicationListener("TestApplicationListener.1");

		TestApplicationListener testApplicationListenerTwo = new TestApplicationListener("TestApplicationListener.2");

		TestApplicationListener testApplicationListenerThree = new TestApplicationListener("TestApplicationListener.3");

		try {
			testApplicationListenerOne = SpringContextBootstrappingInitializer.register(testApplicationListenerOne);
			testApplicationListenerTwo = SpringContextBootstrappingInitializer.register(testApplicationListenerTwo);
			testApplicationListenerThree = SpringContextBootstrappingInitializer.register(testApplicationListenerThree);

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();
			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);

			ContextRefreshedEvent mockContextRefreshedEvent =
				mock(ContextRefreshedEvent.class,"MockContextRefreshedEvent");

			new SpringContextBootstrappingInitializer().onApplicationEvent(mockContextRefreshedEvent);

			assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isSameAs(mockContextRefreshedEvent);
			assertNotified(testApplicationListenerOne, mockContextRefreshedEvent);
			assertNotified(testApplicationListenerTwo, mockContextRefreshedEvent);
			assertNotified(testApplicationListenerThree, mockContextRefreshedEvent);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerOne);
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerTwo);
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerThree);
		}
	}

	@Test
	public void onApplicationEventWithNoRegisteredApplicationListener() {

		TestApplicationListener testApplicationListener = new TestApplicationListener("TestApplicationListener");

		try {

			testApplicationListener = SpringContextBootstrappingInitializer.
				unregister(SpringContextBootstrappingInitializer.register(testApplicationListener));

			assertUnnotified(testApplicationListener);

			ContextRefreshedEvent mockEvent = mock(ContextRefreshedEvent.class,"MockContextRefreshedEvent");

			new SpringContextBootstrappingInitializer().onApplicationEvent(mockEvent);

			assertUnnotified(testApplicationListener);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void testNotifyOnExistingContextRefreshedEventBeforeApplicationContextExists() {

		assertThat(SpringContextBootstrappingInitializer.contextRefreshedEvent).isNull();

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testNotifyOnExistingContextRefreshedEventBeforeApplicationContextExists");

		try {
			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);
			assertUnnotified(testApplicationListener);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
		}
	}

	@Test
	public void testNotifyOnExistingContextRefreshedEventAfterContextRefreshed() {

		ContextRefreshedEvent testContextRefreshedEvent = new ContextRefreshedEvent(mock(ApplicationContext.class));

		new SpringContextBootstrappingInitializer().onApplicationEvent(testContextRefreshedEvent);

		TestApplicationListener testApplicationListener =
			new TestApplicationListener("testNotifyOnExistingContextRefreshedEventAfterContextRefreshed");

		try {
			testApplicationListener = SpringContextBootstrappingInitializer.register(testApplicationListener);
			assertNotified(testApplicationListener, testContextRefreshedEvent);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListener);
   		}
	}

	@Test
	public void testOnApplicationEventAndNotifyOnExistingContextRefreshedEvent() {

		ConfigurableApplicationContext mockApplicationContext = mock(ConfigurableApplicationContext.class,
			"testOnApplicationEventAndNotifyOnExistingContextRefreshedEvent");

		SpringContextBootstrappingInitializer initializer = new SpringContextBootstrappingInitializer();

		TestApplicationListener testApplicationListenerOne =
			new TestApplicationListener("testOnApplicationEventAndNotifyOnExistingContextRefreshedEvent.1");

		TestApplicationListener testApplicationListenerTwo =
			new TestApplicationListener("testOnApplicationEventAndNotifyOnExistingContextRefreshedEvent.2");

		TestApplicationListener testApplicationListenerThree =
			new TestApplicationListener("testOnApplicationEventAndNotifyOnExistingContextRefreshedEvent.3");

		try {

			testApplicationListenerOne = SpringContextBootstrappingInitializer.register(testApplicationListenerOne);

			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);

			ContextRefreshedEvent testContextRefreshedEvent = new ContextRefreshedEvent(mockApplicationContext);

			initializer.onApplicationEvent(testContextRefreshedEvent);

			assertNotified(testApplicationListenerOne, testContextRefreshedEvent);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);

			testApplicationListenerTwo = SpringContextBootstrappingInitializer.register(testApplicationListenerTwo);

			assertNotified(testApplicationListenerTwo, testContextRefreshedEvent);
			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerThree);

			ContextStoppedEvent testContextStoppedEvent = new ContextStoppedEvent(mockApplicationContext);

			initializer.onApplicationEvent(testContextStoppedEvent);

			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);

			initializer.onApplicationEvent(testContextRefreshedEvent);

			assertNotified(testApplicationListenerOne, testContextRefreshedEvent);
			assertNotified(testApplicationListenerTwo, testContextRefreshedEvent);
			assertUnnotified(testApplicationListenerThree);

			ContextClosedEvent testContextClosedEvent = new ContextClosedEvent(mockApplicationContext);

			initializer.onApplicationEvent(testContextClosedEvent);

			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);

			SpringContextBootstrappingInitializer.register(testApplicationListenerThree);

			assertUnnotified(testApplicationListenerOne);
			assertUnnotified(testApplicationListenerTwo);
			assertUnnotified(testApplicationListenerThree);
		}
		finally {
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerOne);
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerTwo);
			SpringContextBootstrappingInitializer.unregister(testApplicationListenerThree);
		}
	}

	@Configuration
	protected static class TestAppConfigOne { }

	@Configuration
	protected static class TestAppConfigTwo { }

	protected static class TestApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

		private volatile boolean notified = false;

		private volatile ApplicationContextEvent actualEvent;

		private final String name;

		public TestApplicationListener(String name) {
			this.name = name;
		}

		public ApplicationContextEvent getActualEvent() {
			ApplicationContextEvent localActualEvent = this.actualEvent;
			this.actualEvent = null;
			return localActualEvent;
		}

		public boolean isNotified() {
			boolean localNotified = this.notified;
			this.notified = false;
			return localNotified;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.actualEvent = event;
			this.notified = true;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
