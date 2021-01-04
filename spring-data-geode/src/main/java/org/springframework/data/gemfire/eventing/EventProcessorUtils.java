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

package org.springframework.data.gemfire.eventing;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.CacheEvent;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.Region;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author Udo Kohlmeyer
 * @see BeanFactory
 * @see Region
 * @see List
 * @see CacheListener
 * @see CacheWriter
 * @see CacheEvent
 * @see org.apache.geode.cache.RegionEvent
 * @see org.apache.geode.cache.EntryEvent
 * @see org.springframework.data.gemfire.eventing.config.ComposableCacheWriterWrapper
 * @see org.springframework.data.gemfire.eventing.config.PojoCacheListenerWrapper
 * @see org.springframework.data.gemfire.eventing.config.PojoRegionEventCacheListenerWrapper
 * @see 2.4.0
 */
public class EventProcessorUtils {

	/**
	 * Registers a {@link CacheListener} with a {@link Region}
	 * @param regions a {@link List} of {@link String}s of region names on which the {@link CacheListener} needs to be registered
	 * @param beanFactory the Spring {@link BeanFactory}
	 * @param cacheListener the {@link CacheListener} that needs to be registered onto the {@link Region}(s)
	 */
	public static void registerCacheListenerToRegions(List<String> regions, BeanFactory beanFactory,
		CacheListener cacheListener) {
		for (String regionName : regions) {
			Optional<Region> regionBeanOptional = Optional.of(beanFactory.getBean(regionName, Region.class));

			regionBeanOptional.ifPresent(region -> region.getAttributesMutator().addCacheListener(cacheListener));
		}
	}

	/**
	 * Registers a {@link CacheWriter} with a {@link Region}
	 * @param regionName the region name on which the {@link CacheWriter} needs to be registered
	 * @param beanFactory the Spring {@link BeanFactory}
	 * @param cacheWriter the {@link CacheWriter} that needs to be registered
	 */
	public static void registerCacheWriterToRegion(String regionName, BeanFactory beanFactory,
		CacheWriter cacheWriter) {
		Optional<Region<?, ?>> regionBeanOptional = Optional.of(beanFactory.getBean(regionName, Region.class));

		regionBeanOptional.ifPresent(region -> region.getAttributesMutator().setCacheWriter(cacheWriter));
	}

	/**
	 * The {@link Method} method needs to be validated to have only 1 parameter AND of either {@link org.apache.geode.cache.EntryEvent}
	 * or {@link org.apache.geode.cache.RegionEvent}
	 * @param method the method to be validated
	 * @param requireParameterType the required type of the method parameter
	 */
	public static void validateEventHandlerMethodParameters(Method method,
		Class<? extends CacheEvent> requireParameterType) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new IllegalArgumentException(String
				.format("Callback Handler method: %s does not currently support more than one parameter",
					method.getName()));
		}
		if (!parameterTypes[0].isAssignableFrom(requireParameterType)) {
			throw new IllegalArgumentException(String
				.format("Callback Handler: %s requires an %s parameter type", method.getName(),
					requireParameterType.getName()));
		}
	}

}
