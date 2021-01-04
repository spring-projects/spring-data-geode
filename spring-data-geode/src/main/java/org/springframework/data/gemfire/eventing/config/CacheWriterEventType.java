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

/**
 * An Enum that represents event types defined within {@link org.apache.geode.cache.CacheWriter}. The event types
 * defined are CRUD operation orientated. These do not reflect the managements events that {@link org.apache.geode.cache.Region} have.
 * These events are handled by the {@link RegionCacheWriterEventType}.
 *
 * Due to constraints imposed by the {@link org.apache.geode.cache.Region} that only one {@link org.apache.geode.cache.CacheWriter}
 * can be added to a region, the event mask described here must not conflict with the mask described in {@link RegionCacheWriterEventType}.
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.CacheWriter
 * @since 2.4.0
 */
public enum CacheWriterEventType {
	ALL(0b00000111),
	BEFORE_CREATE(0b00000001),
	BEFORE_UPDATE(0b00000010),
	BEFORE_DESTROY(0b00000100);

	CacheWriterEventType(int mask){
		this.mask = mask;
	}

	int mask;
}
