/*
 * Copyright 2018 the original author or authors.
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
 */
package org.springframework.data.gemfire.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.cache.asyncqueue.AsyncEventQueue;
import org.apache.geode.cache.wan.GatewaySender;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * The {@link EnableAsyncEventQueue} class...
 *
 * @author John Blum
 * @see java.lang.annotation.Annotation
 * @see java.lang.annotation.Documented
 * @see java.lang.annotation.Inherited
 * @see java.lang.annotation.Retention
 * @see java.lang.annotation.Target
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.asyncqueue.AsyncEventQueue
 * @see org.apache.geode.cache.wan.GatewaySender
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.data.gemfire.config.annotation.AsyncEventQueueConfiguration
 * @since 2.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(AsyncEventQueueConfiguration.class)
@SuppressWarnings("unused")
public @interface EnableAsyncEventQueue {

	/**
	 * Name of the {@link AsyncEventQueue} and Spring bean.
	 *
	 * Required!
	 */
	@AliasFor(attribute = "name")
	String value() default "";

	/**
	 * Name of the {@link AsyncEventQueue} and Spring bean.
	 *
	 * Required!
	 *
	 * This value of this attribute is also used to resolve {@link AsyncEventQueue} specific properties defined in
	 * {@literal application.properties}.
	 */
	@AliasFor(attribute = "value")
	String name() default "";

	/**
	 * Configures {@link String name} of the {@link AsyncEventListener} bean declared and registered in
	 * the Spring application context.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.listener-bean-name}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.listener-bean-name} property
	 * in {@literal application.properties}.
	 *
	 * Required!
	 *
	 * Defaults to {@literal empty} since the AEQ listener can be defined for all AEQs on the
	 * {@link EnableAsyncEventQueues} composing annotation.
	 */
	String asyncEventListenerBeanName() default "";

	/**
	 * Configures whether to enable batch conflation for this {@link AsyncEventQueue} (AEQ).
	 *
	 * Batch conflation essentially means that the Region data events put into the queue, triggering listeners,
	 * that the events in the queue representing the same Region entry (by key) are conflated, or reduced to the latest
	 * operation on that entry.  If the sequence of events were UPDATE then REMOVE, then the listener would not see
	 * the UPDATE if the events were conflated, only the REMOVE.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.batch.conflation-enabled}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.batch.conflation-enabled} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal false}.
	 */
	boolean batchConflationEnabled() default AsyncEventQueueConfiguration.DEFAULT_BATCH_CONFLATION_ENABLED;

	/**
	 * Configures batch size for this {@link AsyncEventQueue} (AEQ) triggering when the AEQ listener will be notified.
	 *
	 * That is, once the batch size has been reached or the batch time interval has expired, whichever occurs first,
	 * then the AEQ listener will be fired with the batch of events.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.batch.size}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.batch.size} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal 100}.
	 */
	int batchSize() default AsyncEventQueueConfiguration.DEFAULT_BATCH_SIZE;

	/**
	 * Configures batch time interval in milliseconds (ms) for this {@link AsyncEventQueue} (AEQ) when the AEQ listener
	 * should be notified.
	 *
	 * That is, once the batch size has been reached or the batch time interval has expired, whichever occurs first,
	 * then the AEQ listener will be fired with the batch of events.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.batch.time-interval}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.batch.time-interval} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal 5} milliseconds (ms).
	 */
	int batchTimeInterval() default AsyncEventQueueConfiguration.DEFAULT_BATCH_TIME_INTERVAL;

	/**
	 * Configures the name of the {@link DiskStore} bean in the declared and registered in the Spring application context
	 * for persisting {@link AsyncEventQueue} (AEQ) events.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.disk-store.bean-name}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.disk-store.bean-name} property
	 * in {@literal application.properties}.

	 * Defaults to {@literal empty}.
	 */
	String diskStoreBeanName() default "";

	/**
	 * Configures whether {@link AsyncEventQueue} events should be persisted to disk synchronously (default)
	 * or asynchronously, in a background {@link Thread}.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.disk-store.synchronous}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.disk-store.synchronous} property
	 * in {@literal application.properties}.

	 * Defaults to {@literal true}.
	 */
	boolean diskSynchronous() default AsyncEventQueueConfiguration.DEFAULT_DISK_SYNCHRONOUS;

	/**
	 * Configures the number of {@link Thread Threads} concurrently dispatching events to listeners
	 * from this {@link AsyncEventQueue}.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.dispatcher-threads}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.dispatcher-threads} property
	 * in {@literal application.properties}.

	 * Defaults to {@literal 5}.
	 */
	int dispatcherThreads() default AsyncEventQueueConfiguration.DEFAULT_DISPATCHER_THREADS;

	/**
	 * Configures whether expiration, destroy events for this {@link AsyncEventQueue} are forwarded.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.forward-expiration-destroy}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.forward-expiration-destroy} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal false}.
	 */
	boolean forwardExpirationDestroy() default AsyncEventQueueConfiguration.DEFAULT_FORWARD_EXPIRATION_DESTROY;

	String[] gatewayEventFilterBeanNames() default {};

	String gatewayEventSubstitutionFilterBeanName() default "";

	/**
	 * Configures the maximum amount of memory in megabytes used by this {@link AsyncEventQueue} (AEQ).
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.max-memory}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.max-memory} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@link GatewaySender.OrderPolicy#KEY}.
	 */
	int maximumQueueMemory() default AsyncEventQueueConfiguration.DEFAULT_MAXIMUM_QUEUE_MEMORY;

	/**
	 * Configures whether the {@link GatewaySender.OrderPolicy} for this {@link AsyncEventQueue} (AEQ), which determines
	 * the criteria (e.g. KEY, THREAD) used to order events.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.order-policy}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.order-policy} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@link GatewaySender.OrderPolicy#KEY}.
	 */
	GatewaySender.OrderPolicy orderPolicy() default GatewaySender.OrderPolicy.KEY;

	/**
	 * Configures whether this {@link AsyncEventQueue} (AEQ) dispatches events in parallel, across the cluster of nodes
	 * that all host the {@link Region} to which this AEQ is attached.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.parallel}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.parallel} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal false}.
	 */
	boolean parallel() default AsyncEventQueueConfiguration.DEFAULT_PARALLEL;

	/**
	 * Configures whether this {@link AsyncEventQueue} (AEQ) is persistent, i.e. persisting events to disk
	 * in the face of node failure.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.persistent}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.persistent} property
	 * in {@literal application.properties}.
	 *
	 * Defaults to {@literal false}.
	 */
	boolean persistent() default AsyncEventQueueConfiguration.DEFAULT_PERSISTENT;

	/**
	 * Configures the (bean) {@link String names} of {@link Region Regions} to which this {@link AsyncEventQueue}
	 * will be associated.
	 *
	 * Use the {@literal spring.data.gemfire.async-event-queue.region-names}
	 * or {@literal spring.data.gemfire.async-event-queue.<name>.region-names} property
	 * in {@literal application.properties}.

	 * Defaults to {@literal empty}, or all {@link Region Regions}.
	 */
	String[] regionNames() default {};

}
