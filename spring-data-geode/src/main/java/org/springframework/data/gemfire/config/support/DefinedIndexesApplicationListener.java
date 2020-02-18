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

import java.util.Optional;

import org.apache.geode.cache.query.MultiIndexCreationException;
import org.apache.geode.cache.query.QueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.config.xml.SpringGemFireConstants;

/**
 * {@link DefinedIndexesApplicationListener} is a Spring {@link ApplicationListener} used to create all
 * "defined" GemFire {@link org.apache.geode.cache.query.Index Indexes} by using the {@link QueryService},
 * {@literal defineXxxIndex(..)} methods.
 *
 * @author John Blum
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.ContextRefreshedEvent
 * @see org.apache.geode.cache.query.QueryService
 * @since 1.7.0
 */
public class DefinedIndexesApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

	protected final Logger logger = initLogger();

	/**
	 * Attempts to create all defined {@link org.apache.geode.cache.query.Index Indexes} using
	 * the {@link QueryService}, {@literal defineXxxIndex(..)} API once the Spring {@link ApplicationContext}
	 * has been refreshed.
	 *
	 * @param event {@link ContextRefreshedEvent} fired when the Spring {@link ApplicationContext} gets refreshed.
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.apache.geode.cache.query.QueryService#createDefinedIndexes()
	 * @see #getQueryService(ContextRefreshedEvent)
	 */
	@Override
	@SuppressWarnings("all")
	public void onApplicationEvent(ContextRefreshedEvent event) {

		Optional.ofNullable(getQueryService(event))
			.ifPresent(queryService -> {
				try {
					queryService.createDefinedIndexes();
				}
				catch (MultiIndexCreationException cause) {
					logger.warn(String.format("Failed to create pre-defined Indexes: %s", cause.getMessage()), cause);
				}
			});
	}

	Logger initLogger() {
		return LoggerFactory.getLogger(getClass());
	}

	private QueryService getQueryService(ContextRefreshedEvent event) {

		ApplicationContext applicationContext = event.getApplicationContext();

		String queryServiceBeanName = getQueryServiceBeanName();

		return (applicationContext.containsBean(queryServiceBeanName)
			? applicationContext.getBean(queryServiceBeanName, QueryService.class) : null);
	}

	private String getQueryServiceBeanName() {
		return SpringGemFireConstants.DEFAULT_GEMFIRE_INDEX_DEFINITION_QUERY_SERVICE;
	}
}
