/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.data.gemfire.config.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.geode.cache.CacheEvent;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.Region;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.gemfire.config.annotation.AsCacheWriter;
import org.springframework.data.gemfire.config.annotation.AsRegionEventHandler;
import org.springframework.data.gemfire.eventing.EventProcessorUtils;
import org.springframework.data.gemfire.eventing.config.ComposableCacheWriterWrapper;

/**
 * A {@link BeanPostProcessor} to create and register {@link org.apache.geode.cache.CacheWriter},
 * annotated with {@link AsCacheWriter}
 * and {@link AsRegionEventHandler} onto the configured {@link Region}s
 *
 * @author Udo Kohlmeyer
 * @see Region
 * @see CacheWriter
 * @see AsCacheWriter
 * @see BeanPostProcessor
 * @see AsRegionEventHandler
 * @see ComposableCacheWriterWrapper
 * @since 2.4.0
 */
@Configuration
public class CacheWriterPostProcessor<E extends Enum<E>> extends CallbackPostProcessor {

	private final Map<String, ComposableCacheWriterWrapper> composableCacheWriterWrappers = new HashMap<>();
	private List<String> configuredRegions;

	@Override
	protected Class getRegionEventHandlerClass() {
		return AsRegionEventHandler.class;
	}

	@Override
	protected Class getEventHandlerClass() {
		return AsCacheWriter.class;
	}

	@Override
	protected <T extends Annotation> void registerEventHandlers(Object bean, Class<T> writerAnnotationClazz,
		Method method, AnnotationAttributes cacheWriterAttributes) {

		String[] regionNames = cacheWriterAttributes.getStringArray("regions");

		List<String> regions = regionNames.length > 0 ? Arrays.asList(regionNames) : getConfiguredRegions();

		Optional<E[]> eventTypes = Optional.ofNullable(getEventTypes(writerAnnotationClazz, cacheWriterAttributes));

		eventTypes.ifPresent(events -> {
			if (events.length > 0) {
				for (String region : regions) {
					registerCacheWriterEventHandler(bean, method, events, region);
				}
			}
		});
	}

	private <T extends Annotation> E[] getEventTypes(Class<T> writerAnnotationClazz,
		AnnotationAttributes cacheWriterAttributes) {
		if (writerAnnotationClazz.isAssignableFrom(getEventHandlerClass())) {
			return (E[]) cacheWriterAttributes.get("eventTypes");
		}
		else if (writerAnnotationClazz.isAssignableFrom(getRegionEventHandlerClass())) {
			return (E[]) cacheWriterAttributes.get("regionWriterEventTypes");
		}
		return null;
	}

	private List<String> getConfiguredRegions() {
		if (configuredRegions == null) {
			configuredRegions = getRegionsForEventRegistration(getBeanFactory());
		}
		return configuredRegions;
	}


	private void registerCacheWriterEventHandler(Object bean, Method method, E[] eventTypes, String region) {
		ComposableCacheWriterWrapper composableCacheWriterWrapper =
			composableCacheWriterWrappers.getOrDefault(region, new ComposableCacheWriterWrapper());

		Optional<Class<? extends CacheEvent>> eventTypeForMethod = Optional
			.ofNullable(getEventTypeForMethod(eventTypes));
		eventTypeForMethod.ifPresent(eventTypeClass -> {
			EventProcessorUtils.validateEventHandlerMethodParameters(method, eventTypeClass);
			composableCacheWriterWrapper.addCacheWriter(method, bean, eventTypes);

			composableCacheWriterWrappers.put(region, composableCacheWriterWrapper);

			registerEventHandlerToRegion(region, composableCacheWriterWrapper);
		});
	}

	/**
	 * Validates the method parameters to be of the correct type dependent on the eventing Annotation. It then registers
	 * the defined {@link CacheWriter} onto the defined set of {@link Region}.
	 *
	 * on the annotation
	 *
	 * @param cacheWriter - The {@link CacheWriter} to be registered onto the {@link Region}
	 */
	private <T extends CacheEvent> void registerEventHandlerToRegion(String regionName, CacheWriter cacheWriter) {
		EventProcessorUtils.registerCacheWriterToRegion(regionName, beanFactory, cacheWriter);
	}
}
