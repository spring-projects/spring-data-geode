/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.data.gemfire.function.execution.onservers;

import java.io.Serializable;

/**
 * @author Patrick Johnson
 */
public class Metric implements Serializable {

	private String name;

	private Number value;

	private String category;

	private String type;

	public Metric(String name, Number value, String category, String type) {
		this.name = name;
		this.value = value;
		this.category = category;
		this.type = type;
	}
}
