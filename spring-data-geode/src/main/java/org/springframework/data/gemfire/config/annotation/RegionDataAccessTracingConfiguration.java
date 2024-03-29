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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.annotation.support.RegionDataAccessTracingAspect;

/**
 * The RegionDataAccessTracingConfiguration class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@Configuration
@SuppressWarnings("unused")
public class RegionDataAccessTracingConfiguration {

	@Bean
	public RegionDataAccessTracingAspect regionDataAccessTracingAspect() {
		return new RegionDataAccessTracingAspect();
	}
}
