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

import java.lang.annotation.Annotation;
import java.util.Collections;

import org.apache.geode.cache.wan.GatewaySender;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.gemfire.config.annotation.support.AbstractAnnotationConfigSupport;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.wan.AsyncEventQueueFactoryBean;
import org.springframework.util.Assert;

/**
 * The AsyncEventQueueConfiguration class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class AsyncEventQueueConfiguration extends AbstractAnnotationConfigSupport
		implements ImportBeanDefinitionRegistrar {

	protected static final boolean DEFAULT_BATCH_CONFLATION_ENABLED = false;
	protected static final boolean DEFAULT_DISK_SYNCHRONOUS = true;
	protected static final boolean DEFAULT_FORWARD_EXPIRATION_DESTROY = false;
	protected static final boolean DEFAULT_PARALLEL = false;
	protected static final boolean DEFAULT_PERSISTENT = false;

	protected static final int DEFAULT_BATCH_SIZE = 100;
	protected static final int DEFAULT_BATCH_TIME_INTERVAL = 5; // Milliseconds (ms)
	protected static final int DEFAULT_DISPATCHER_THREADS = 5;
	protected static final int DEFAULT_MAXIMUM_QUEUE_MEMORY = 100; // MB

	protected static final GatewaySender.OrderPolicy DEFAULT_ORDER_POLICY = GatewaySender.OrderPolicy.KEY;

	protected static final String DEFAULT_DISK_STORE_NAME = "";

	@Override
	protected Class<? extends Annotation> getAnnotationType() {
		return EnableAsyncEventQueue.class;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		if (isAnnotationPresent(importingClassMetadata)) {

			AnnotationAttributes enableAsyncEventQueueAttributes = getAnnotationAttributes(importingClassMetadata);

			registerAsyncEventQueueBeanDefinition(enableAsyncEventQueueAttributes, registry);
		}
	}

	protected void registerAsyncEventQueueBeanDefinition(AnnotationAttributes enableAsyncEventQueueAttributes,
			BeanDefinitionRegistry registry) {

		String asyncEventQueueName = enableAsyncEventQueueAttributes.getString("name");

		Assert.hasText(asyncEventQueueName, "AsyncEventQueue name is required");

		String asyncEventListenerBeanName =
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "listener.bean-name"),
				resolveProperty(asyncEventQueueProperty("listener.bean-name"),
					enableAsyncEventQueueAttributes.getString("asyncEventListenerBeanName")));

		Assert.hasText(asyncEventListenerBeanName, () ->
			String.format("An AsyncEventListener for AsyncEventQueue [%s] is required", asyncEventListenerBeanName));

		BeanDefinitionBuilder asyncEventQueueBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(AsyncEventQueueFactoryBean.class);

		asyncEventQueueBuilder.addConstructorArgReference(GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME);
		asyncEventQueueBuilder.addConstructorArgReference(asyncEventListenerBeanName);
		asyncEventQueueBuilder.addPropertyValue("name", asyncEventQueueName);

		asyncEventQueueBuilder.addPropertyValue("batchConflationEnabled",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "batch.conflation-enabled"),
				resolveProperty(asyncEventQueueProperty("batch.conflation-enabled"),
					enableAsyncEventQueueAttributes.getBoolean("batchConflationEnabled"))));

		asyncEventQueueBuilder.addPropertyValue("batchSize",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "batch.size"),
				resolveProperty(asyncEventQueueProperty("batch.size"),
					enableAsyncEventQueueAttributes.<Integer>getNumber("batchSize"))));

		asyncEventQueueBuilder.addPropertyValue("batchTimeInterval",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "batch.time-interval"),
				resolveProperty(asyncEventQueueProperty("batch.time-interval"),
					enableAsyncEventQueueAttributes.<Integer>getNumber("batchTimeInterval"))));

		asyncEventQueueBuilder.addPropertyReference("diskStoreRef",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "disk-store.bean-name"),
				resolveProperty(asyncEventQueueProperty("disk-store.bean-name"),
					enableAsyncEventQueueAttributes.getString("diskStoreBeanName"))));

		asyncEventQueueBuilder.addPropertyValue("diskSynchronous",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "disk-store.synchronous"),
				resolveProperty(asyncEventQueueProperty("disk-store.synchronous"),
					enableAsyncEventQueueAttributes.getBoolean("diskSynchronous"))));

		asyncEventQueueBuilder.addPropertyValue("dispatcherThreads",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "dispatcher-threads"),
				resolveProperty(asyncEventQueueProperty("dispatcher-threads"),
					enableAsyncEventQueueAttributes.<Integer>getNumber("dispatcherThreads"))));

		asyncEventQueueBuilder.addPropertyValue("forwardExpirationDestroy",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "forward-expiration-destroy"),
				resolveProperty(asyncEventQueueProperty("forward-expiration-destroy"),
					enableAsyncEventQueueAttributes.getBoolean("forwardExpirationDestroy"))));

		String[] eventFilters = ArrayUtils.nullSafeArray(
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "event.filter.bean-names"), String[].class,
				resolveProperty(asyncEventQueueProperty("event.filter.bean-names"), String[].class,
					enableAsyncEventQueueAttributes.getStringArray("gatewayEventFilterBeanNames"))), String.class);

		ManagedList<String> eventFilterBeanNames = new ManagedList<>(eventFilters.length);

		Collections.addAll(eventFilterBeanNames, eventFilters);

		asyncEventQueueBuilder.addPropertyValue("gatewayEventFilters", eventFilterBeanNames);

		asyncEventQueueBuilder.addPropertyReference("gatewayEventSubstitutionFilter",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "event.substitution-filter.bean-names"),
				resolveProperty(asyncEventQueueProperty("event.substitution-filter.bean-names"),
					enableAsyncEventQueueAttributes.getString("gatewayEventSubstitutionFilterBeanName"))));

		asyncEventQueueBuilder.addPropertyValue("maximumQueueMemory",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "max-memory"),
				resolveProperty(asyncEventQueueProperty("max-memory"),
					enableAsyncEventQueueAttributes.<Integer>getNumber("maximumQueueMemory"))));

		asyncEventQueueBuilder.addPropertyValue("orderPolicy",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "order-policy"), GatewaySender.OrderPolicy.class,
				resolveProperty(asyncEventQueueProperty("order-policy"), GatewaySender.OrderPolicy.class,
					enableAsyncEventQueueAttributes.getEnum("orderPolicy"))));

		asyncEventQueueBuilder.addPropertyValue("parallel",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "parallel"),
				resolveProperty(asyncEventQueueProperty("parallel"),
					enableAsyncEventQueueAttributes.getBoolean("parallel"))));

		asyncEventQueueBuilder.addPropertyValue("persistent",
			resolveProperty(namedAsyncEventQueueProperty(asyncEventQueueName, "persistent"),
				resolveProperty(asyncEventQueueProperty("persistent"),
					enableAsyncEventQueueAttributes.getBoolean("persistent"))));

		BeanDefinitionHolder asyncEventQueueHolder =
			newBeanDefinitionHolder(asyncEventQueueBuilder.getBeanDefinition(), asyncEventQueueName);

		BeanDefinitionReaderUtils.registerBeanDefinition(asyncEventQueueHolder, registry);
	}
}
