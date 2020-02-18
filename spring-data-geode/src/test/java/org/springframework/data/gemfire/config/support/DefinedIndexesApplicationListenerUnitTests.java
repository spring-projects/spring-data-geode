/*
 * Copyright 2010-2020 the original author or authors.
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

package org.springframework.data.gemfire.config.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;

import org.apache.geode.cache.query.MultiIndexCreationException;
import org.apache.geode.cache.query.QueryService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.config.xml.SpringGemFireConstants;

/**
 * Unit tests for {@link DefinedIndexesApplicationListener}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see DefinedIndexesApplicationListener
 * @since 1.7.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DefinedIndexesApplicationListenerUnitTests {

	private static final String QUERY_SERVICE_BEAN_NAME =
		SpringGemFireConstants.DEFAULT_GEMFIRE_INDEX_DEFINITION_QUERY_SERVICE;

	@Mock
	private ApplicationContext mockApplicationContext;

	@Mock
	private ContextRefreshedEvent mockEvent;

	private DefinedIndexesApplicationListener listener = new DefinedIndexesApplicationListener();

	@Mock
	private QueryService mockQueryService;

	@Before
	public void setup() {
		when(mockEvent.getApplicationContext()).thenReturn(mockApplicationContext);
	}

	protected <K, V> HashMap<K, V> newHashMap(K key, V value) {
		return new HashMap<>(Collections.singletonMap(key, value));
	}

	protected MultiIndexCreationException newMultiIndexCreationException(String key, Exception cause) {
		return new MultiIndexCreationException(newHashMap(key, cause));
	}

	@Test
	public void createDefinedIndexesCalledOnContextRefreshedEvent() throws Exception {

		when(mockApplicationContext.containsBean(eq(QUERY_SERVICE_BEAN_NAME))).thenReturn(true);
		when(mockApplicationContext.getBean(eq(QUERY_SERVICE_BEAN_NAME), eq(QueryService.class)))
			.thenReturn(mockQueryService);

		listener.onApplicationEvent(mockEvent);

		verify(mockEvent, times(1)).getApplicationContext();
		verify(mockApplicationContext, times(1)).containsBean(eq(QUERY_SERVICE_BEAN_NAME));
		verify(mockApplicationContext, times(1)).getBean(eq(QUERY_SERVICE_BEAN_NAME), eq(QueryService.class));
		verify(mockQueryService, times(1)).createDefinedIndexes();
	}

	@Test
	public void createDefinedIndexesNotCalledOnContextRefreshedEvent() throws Exception {

		when(mockApplicationContext.containsBean(eq(QUERY_SERVICE_BEAN_NAME))).thenReturn(false);

		listener.onApplicationEvent(mockEvent);

		verify(mockEvent, times(1)).getApplicationContext();
		verify(mockApplicationContext, times(1)).containsBean(eq(QUERY_SERVICE_BEAN_NAME));
		verify(mockApplicationContext, never()).getBean(anyString(), any(QueryService.class));
		verify(mockQueryService, never()).createDefinedIndexes();
	}

	@Test
	public void createDefinedIndexesThrowingAnExceptionIsLogged() throws Exception {

		when(mockApplicationContext.containsBean(eq(QUERY_SERVICE_BEAN_NAME))).thenReturn(true);
		when(mockApplicationContext.getBean(eq(QUERY_SERVICE_BEAN_NAME), eq(QueryService.class)))
			.thenReturn(mockQueryService);
		when(mockQueryService.createDefinedIndexes())
			.thenThrow(newMultiIndexCreationException("TestKey", new RuntimeException("TEST")));

		Logger mockLogger = mock(Logger.class);

		DefinedIndexesApplicationListener listener = new DefinedIndexesApplicationListener() {
			@Override Logger initLogger() {
				return mockLogger;
			}
		};

		listener.onApplicationEvent(mockEvent);

		verify(mockEvent, times(1)).getApplicationContext();
		verify(mockApplicationContext, times(1)).containsBean(eq(QUERY_SERVICE_BEAN_NAME));
		verify(mockApplicationContext, times(1)).getBean(eq(QUERY_SERVICE_BEAN_NAME), eq(QueryService.class));
		verify(mockQueryService, times(1)).createDefinedIndexes();
		verify(mockLogger, times(1)).warn(startsWith("Failed to create pre-defined Indexes:"),
			isA(MultiIndexCreationException.class));
	}
}
