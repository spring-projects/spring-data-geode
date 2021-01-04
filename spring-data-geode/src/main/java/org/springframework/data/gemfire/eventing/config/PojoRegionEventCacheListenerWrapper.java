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
package org.springframework.data.gemfire.eventing.config;

import java.lang.reflect.Method;

import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * Invokes a given {@link Object POJO} {@link Method} as a GemFire/Geode {@link org.apache.geode.cache.CacheListener}.
 * This proxy will specifically handle Region type events.
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.CacheListener
 * @since 2.4.0
 */
public class PojoRegionEventCacheListenerWrapper extends CacheListenerAdapter {

	private static final String AFTER_REGION_DESTROY = "afterRegionDestroy";
	private static final String AFTER_REGION_CREATE = "afterRegionCreate";
	private static final String AFTER_REGION_INVALIDATE = "afterRegionInvalidate";
	private static final String AFTER_REGION_CLEAR = "afterRegionClear";
	private static final String AFTER_REGION_LIVE = "afterRegionLive";

	private static transient Logger logger = LoggerFactory.getLogger(PojoRegionEventCacheListenerWrapper.class);
	private final Method method;
	private final Object targetInvocationObject;
	private final int eventTypeMask;

	public PojoRegionEventCacheListenerWrapper(Method method, Object targetInvocationClass, RegionCacheListenerEventType[] regionEventTypes) {
		this.method = method;
		this.targetInvocationObject = targetInvocationClass;
		this.eventTypeMask = createRegionEventTypeMask(regionEventTypes);
	}

	private static int createRegionEventTypeMask(RegionCacheListenerEventType[] regionEventTypes) {
		int mask = 0x0;
		for (RegionCacheListenerEventType eventType : regionEventTypes) {
			mask |= eventType.mask;
		}
		return mask;
	}

	@Override public void afterRegionDestroy(RegionEvent event) {
		logDebug(AFTER_REGION_DESTROY);

		executeEventHandler(event, RegionCacheListenerEventType.AFTER_REGION_DESTROY);
	}

	@Override public void afterRegionCreate(RegionEvent event) {
		logDebug(AFTER_REGION_CREATE);

		executeEventHandler(event, RegionCacheListenerEventType.AFTER_REGION_CREATE);
	}

	@Override public void afterRegionInvalidate(RegionEvent event) {
		logDebug(AFTER_REGION_INVALIDATE);

		executeEventHandler(event, RegionCacheListenerEventType.AFTER_REGION_INVALIDATE);
	}

	@Override public void afterRegionClear(RegionEvent event) {
		logDebug(AFTER_REGION_CLEAR);

		executeEventHandler(event, RegionCacheListenerEventType.AFTER_REGION_CLEAR);
	}

	@Override public void afterRegionLive(RegionEvent event) {
		logDebug(AFTER_REGION_LIVE);

		executeEventHandler(event, RegionCacheListenerEventType.AFTER_REGION_LIVE);
	}

	private void executeEventHandler(RegionEvent regionEvent, RegionCacheListenerEventType eventType) {

		if ((eventTypeMask & eventType.mask) == eventType.mask) {

			ReflectionUtils.invokeMethod(this.method, this.targetInvocationObject, regionEvent);

		}
	}

	private void logDebug(String eventType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + eventType);
		}
	}
}
