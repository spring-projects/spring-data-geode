/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.gemfire.config.xml.GemfireConstants;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.lang.NonNull;

/**
 * {@link RepositoryConfigurationExtension} implementation handling Apache Geode specific extensions
 * in the {@link Repository} XML and Annotation-based configuration metadata.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension
 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport
 */
public class GemfireRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	private static final String GEMFIRE_MODULE_PREFIX = "gemfire";
	private static final String MAPPING_CONTEXT_PROPERTY_NAME = "gemfireMappingContext";
	private static final String MAPPING_CONTEXT_REF_ATTRIBUTE_NAME = "mappingContextRef";

	static final String DEFAULT_MAPPING_CONTEXT_BEAN_NAME =
		String.format("%1$s.%2$s", GemfireMappingContext.class.getName(), "DEFAULT");

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Region.REGION_ANNOTATION_TYPES;
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(GemfireRepository.class);
	}

	@Override
	protected String getModulePrefix() {
		return GEMFIRE_MODULE_PREFIX;
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return GemfireRepositoryFactoryBean.class.getName();
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		super.postProcess(builder, source);

		builder.addPropertyReference("cache", GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource configurationSource) {
		addMappingContextPropertyReference(builder, configurationSource);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource configurationSource) {
		addMappingContextPropertyReference(builder, configurationSource);
	}

	/**
	 * Adds a property reference to the data store-specific {@link MappingContext}
	 * in the given {@link BeanDefinitionBuilder bean definition}.
	 *
	 * @param builder {@link BeanDefinitionBuilder} used to build the target bean definition.
	 * @param configurationSource {@link RepositoryConfigurationSource} containing {@link Repository}
	 * configuration metadata.
	 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource
	 */
	private void addMappingContextPropertyReference(@NonNull BeanDefinitionBuilder builder,
			@NonNull RepositoryConfigurationSource configurationSource) {

		builder.addPropertyReference(MAPPING_CONTEXT_PROPERTY_NAME,
			configurationSource.getAttribute(MAPPING_CONTEXT_REF_ATTRIBUTE_NAME)
				.orElse(DEFAULT_MAPPING_CONTEXT_BEAN_NAME));
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		super.registerBeansForRoot(registry, configurationSource);
		registerMappingContextForRoot(registry, configurationSource);
	}

	/**
	 * Registers a {@link GemfireMappingContext} if a {@link MappingContext} is not already registered in
	 * the {@link BeanDefinitionRegistry}.
	 *
	 * @param registry {@link BeanDefinitionRegistry} containing registered bean definitions.
	 * @param configurationSource {@link RepositoryConfigurationSource} containing the configuration metadata
	 * for Apache Geode {@link Repository Repositories}.
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see #noMappingContextIsConfigured(RepositoryConfigurationSource)
	 */
	private void registerMappingContextForRoot(@NonNull BeanDefinitionRegistry registry,
			@NonNull RepositoryConfigurationSource configurationSource) {

		if (noMappingContextIsConfigured(configurationSource)) {
			registry.registerBeanDefinition(DEFAULT_MAPPING_CONTEXT_BEAN_NAME,
				new RootBeanDefinition(GemfireMappingContext.class));
		}
	}

	/**
	 * Determines whether a {@link GemfireMappingContext} has already been configured.
	 *
	 * @param configurationSource {@link RepositoryConfigurationSource} used to check for the presence of
	 * an existing {@link MappingContext} configuration.
	 * @return a boolean value indicating whether a {@link GemfireMappingContext} has already been configured.
	 */
	private boolean noMappingContextIsConfigured(@NonNull RepositoryConfigurationSource configurationSource) {
		return !configurationSource.getAttribute(MAPPING_CONTEXT_REF_ATTRIBUTE_NAME).isPresent();
	}
}
