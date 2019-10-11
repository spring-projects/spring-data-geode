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
import java.util.List;

import org.apache.geode.cache.CacheEvent;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.gemfire.config.annotation.AsCacheListener;
import org.springframework.data.gemfire.config.annotation.AsRegionEventHandler;
import org.springframework.data.gemfire.eventing.EventProcessorUtils;
import org.springframework.data.gemfire.eventing.config.CacheListenerEventType;
import org.springframework.data.gemfire.eventing.config.PojoCacheListenerWrapper;
import org.springframework.data.gemfire.eventing.config.PojoRegionEventCacheListenerWrapper;
import org.springframework.data.gemfire.eventing.config.RegionCacheListenerEventType;

/**
 * A {@link BeanPostProcessor} to create and register {@link CacheListener}, annotated with {@link AsCacheListener}
 * and {@link AsRegionEventHandler} onto the configured {@link Region}s
 *
 * @author Udo Kohlmeyer
 * @see BeanPostProcessor
 * @see CacheListener
 * @see Region
 * @see AsCacheListener
 * @see AsRegionEventHandler
 * @see CallbackPostProcessor
 * @since 2.4.0
 */
@Configuration
public class CacheListenerPostProcessor extends CallbackPostProcessor {

	@Override
	protected Class getRegionEventHandlerClass() {
		return AsRegionEventHandler.class;
	}

	@Override
	protected Class getEventHandlerClass() {
		return AsCacheListener.class;
	}

	@Override
	protected <T extends Annotation> void registerEventHandlers(Object bean, Class<T> listenerAnnotationClazz,
		Method method, AnnotationAttributes cacheListenerAttributes) {
		if (listenerAnnotationClazz.isAssignableFrom(getEventHandlerClass())) {
			registerCacheListenerEventHandler(bean, method, cacheListenerAttributes);
		}
		else if (listenerAnnotationClazz.isAssignableFrom(getRegionEventHandlerClass())) {
			registerRegionEventHandler(bean, method, cacheListenerAttributes);
		}
	}

	/**
	 * Lookup {@link CacheListenerEventType} from the {@link AsCacheListener} annotation and create a {@link PojoCacheListenerWrapper}
	 * of type {@link CacheListener} that would register itself onto a {@link Region} for the configured events
	 */
	private void registerCacheListenerEventHandler(Object bean, Method method,
		AnnotationAttributes cacheListenerAttributes) {
		CacheListenerEventType[] eventTypes = (CacheListenerEventType[]) cacheListenerAttributes
			.get("eventTypes");
		registerEventHandlerToRegion(method, cacheListenerAttributes,
			new PojoCacheListenerWrapper(method, bean, eventTypes), EntryEvent.class);
	}

	/**
	 * Lookup {@link RegionCacheListenerEventType} from the {@link AsRegionEventHandler} annotation and
	 * create a {@link PojoRegionEventCacheListenerWrapper}
	 * of type {@link CacheListener} that would register itself onto a {@link Region} for the configured
	 * {@link Region} specific events
	 */
	private void registerRegionEventHandler(Object bean, Method method,
		AnnotationAttributes cacheListenerAttributes) {
		RegionCacheListenerEventType[] eventTypes = (RegionCacheListenerEventType[]) cacheListenerAttributes
			.get("regionListenerEventTypes");
		registerEventHandlerToRegion(method, cacheListenerAttributes,
			new PojoRegionEventCacheListenerWrapper(method, bean, eventTypes), RegionEvent.class);
	}

	/**
	 * Validates the method parameters to be of the correct type dependent on the eventing Annotation. It then registers
	 * the defined {@link CacheListener} onto the defined set of {@link Region}.
	 *
	 * @param method - The event handler callback method for event handling type
	 * @param cacheListenerAttributes - A set of {@link Annotation} attributes used to get the region names configured
	 * on the annotation
	 * @param cacheListener - The {@link CacheListener} to be registered onto the {@link Region}
	 * @param eventClass - The expected method parameter type. Can be either {@link EntryEvent} or {@link RegionEvent}
	 */
	private <T extends CacheEvent> void registerEventHandlerToRegion(Method method,
		AnnotationAttributes cacheListenerAttributes, CacheListener cacheListener, Class<T> eventClass) {
		List<String> regions = getRegionsForEventRegistration(cacheListenerAttributes.getStringArray("regions"),
			getBeanFactory());

		EventProcessorUtils.validateEventHandlerMethodParameters(method, eventClass);
		EventProcessorUtils.registerCacheListenerToRegions(regions, beanFactory, cacheListener);
	}

}
