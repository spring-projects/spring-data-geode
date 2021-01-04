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

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * Invokes a given {@link Object POJO} {@link Method} as a GemFire/Geode {@link org.apache.geode.cache.CacheListener}.
 * This proxy will process events triggered from CRUD operations against the {@link org.apache.geode.cache.Region} data.
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.CacheListener
 * @see org.apache.geode.cache.util.CacheListenerAdapter
 * @see org.apache.geode.cache.Region
 * @see ReflectionUtils
 * @since 2.4.0
 */
public class PojoCacheListenerWrapper extends CacheListenerAdapter {

	private static final String AFTER_CREATE = "afterCreate";
	private static final String AFTER_UPDATE = "afterUpdate";
	private static final String AFTER_DESTROY = "afterDestroy";
	private static final String AFTER_INVALIDATE = "afterInvalidate";

	private static transient Logger logger = LoggerFactory.getLogger(PojoCacheListenerWrapper.class);
	private final Method method;
	private final Object targetInvocationObject;
	private final int eventTypeMask;

	public PojoCacheListenerWrapper(Method method, Object targetInvocationClass, CacheListenerEventType[] eventTypes) {
		this.method = method;
		this.targetInvocationObject = targetInvocationClass;
		this.eventTypeMask = createEventTypeMask(eventTypes);
	}

	private static int createEventTypeMask(CacheListenerEventType[] eventTypes) {
		int mask = 0x0;
		for (CacheListenerEventType eventType : eventTypes) {
			mask |= eventType.mask;
		}
		return mask;
	}

	@Override
	public void afterCreate(EntryEvent event) {
		logDebug(AFTER_CREATE);

		executeEventHandler(event, CacheListenerEventType.AFTER_CREATE);
	}

	@Override
	public void afterUpdate(EntryEvent event) {
		logDebug(AFTER_UPDATE);

		executeEventHandler(event, CacheListenerEventType.AFTER_UPDATE);
	}

	@Override
	public void afterDestroy(EntryEvent event) {
		logDebug(AFTER_DESTROY);

		executeEventHandler(event, CacheListenerEventType.AFTER_DESTROY);
	}

	@Override
	public void afterInvalidate(EntryEvent event) {
		logDebug(AFTER_INVALIDATE);

		executeEventHandler(event, CacheListenerEventType.AFTER_INVALIDATE);
	}

	private void executeEventHandler(EntryEvent event, CacheListenerEventType eventType) {

		if ((eventTypeMask & eventType.mask) == eventType.mask) {

			ReflectionUtils.invokeMethod(this.method, this.targetInvocationObject, event);

		}
	}

	private void logDebug(String eventType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + eventType);
		}
	}
}
