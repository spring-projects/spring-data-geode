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

package org.springframework.data.gemfire.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.gemfire.eventing.config.CacheListenerEventType;
import org.springframework.data.gemfire.eventing.config.RegionCacheListenerEventType;

/**
 * Used to declare a concrete method as a {@link org.apache.geode.cache.CacheListener} event handler
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.CacheListener
 * @see CacheListenerEventType
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.RegionEvent
 * @see org.apache.geode.cache.EntryEvent
 * @since 2.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface AsCacheListener {

	/**
	 * An array of {@link CacheListenerEventType} that control what region CRUD events need to be observed
	 * {@link CacheListenerEventType} and {@link RegionCacheListenerEventType} cannot be set on the same method. As they
	 * are mutually exclusive and require that the implementing method uses {@link org.apache.geode.cache.RegionEvent} or
	 * {@link org.apache.geode.cache.EntryEvent}
	 */
	CacheListenerEventType[] eventTypes() default {};

	/**
	 * An array for {@link org.apache.geode.cache.Region} names which this {@link org.apache.geode.cache.CacheListener}
	 * will be link to. Not declaring any regions will result in the CacheListener to be configured against all defined
	 * regions.
	 */
	String[] regions() default {};
}
