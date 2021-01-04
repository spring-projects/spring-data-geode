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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.eventing.config.AsCacheListenerBeanPostProcessorRegistrar;
import org.springframework.data.gemfire.eventing.config.AsCacheWriterBeanPostProcessorRegistrar;
import org.springframework.data.gemfire.eventing.config.PojoCacheListenerWrapper;

/**
 * Enables GemFire annotated EventHandler implementations. These implementation will include {@link org.apache.geode.cache.CacheListener},
 * {@link org.apache.geode.cache.CacheWriter}, {@link org.apache.geode.cache.TransactionListener} and {@link org.apache.geode.cache.CacheLoader}
 *
 * This annotation results in the container discovering any beans that are annotated with:
 * <ul><
 * <li>	{code}@AsCacheListener{code},wraps them in a {@link PojoCacheListenerWrapper}</li>
 * <li>	{code}@AsCacheWriter{code},wraps them in a {@link ComposableCacheWriterWrapper}</li>
 * <li></li>and register them with the corresponding {@link org.apache.geode.cache.Region}.
 * </ul>
 *
 * @author Udo Kohlmeyer
 * @since 2.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import({ AsCacheListenerBeanPostProcessorRegistrar.class,
		  AsCacheWriterBeanPostProcessorRegistrar.class})
@SuppressWarnings("unused")
public @interface EnableEventProcessing {

}
