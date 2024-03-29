/*
 * Copyright 2017-2022 the original author or authors.
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

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * The {@link EnableGemFireAsLastResource} annotation is used to enable GemFire as a Last Resource in a Spring,
 * CMT/JTA Transaction.
 *
 * @author John Blum
 * @see org.springframework.context.annotation.EnableAspectJAutoProxy
 * @see org.springframework.context.annotation.Import
 * @see org.springframework.data.gemfire.config.annotation.GemFireAsLastResourceConfiguration
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableAspectJAutoProxy
@Import(GemFireAsLastResourceConfiguration.class)
@SuppressWarnings("unused")
public @interface EnableGemFireAsLastResource {

}
