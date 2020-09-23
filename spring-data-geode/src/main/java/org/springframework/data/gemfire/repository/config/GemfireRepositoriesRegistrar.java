/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.gemfire.repository.config;

import java.lang.annotation.Annotation;

import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.lang.NonNull;

/**
 * {@link ImportBeanDefinitionRegistrar} to configure and setup Apache Geode {@link Repository Repositories}
 * via {@link EnableGemfireRepositories}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport
 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension
 */
public class GemfireRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

	/**
	 * Identifies the {@link Annotation} enabling Apache Geode {@link Repository Repositories}.
	 *
	 * Defaults to {@link EnableGemfireRepositories}.
	 *
	 * @return the {@link Annotation} {@link Class} enabling Apache Geode {@link Repository Repositories}.
	 * @see java.lang.annotation.Annotation
	 * @see java.lang.Class
	 */
	@Override
	protected @NonNull Class<? extends Annotation> getAnnotation() {
		return EnableGemfireRepositories.class;
	}

	/**
	 * Returns the {@link RepositoryConfigurationExtension} implementing class to configure Apache Geode
	 * {@link Repository Repositories}.
	 *
	 * @return the {@link RepositoryConfigurationExtension} implementing class to configure Apache Geode
	 * {@link Repository Repositories}.
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension
	 */
	@Override
	protected @NonNull RepositoryConfigurationExtension getExtension() {
		return new GemfireRepositoryConfigurationExtension();
	}
}
