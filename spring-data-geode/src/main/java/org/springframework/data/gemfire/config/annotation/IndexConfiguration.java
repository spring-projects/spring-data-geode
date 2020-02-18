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
package org.springframework.data.gemfire.config.annotation;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.lucene.LuceneIndex;
import org.apache.geode.cache.query.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.IndexFactoryBean;
import org.springframework.data.gemfire.IndexType;
import org.springframework.data.gemfire.config.xml.SpringGemFireConstants;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.gemfire.mapping.GemfirePersistentProperty;
import org.springframework.data.gemfire.mapping.annotation.Indexed;
import org.springframework.data.gemfire.mapping.annotation.LuceneIndexed;
import org.springframework.data.gemfire.search.lucene.LuceneIndexFactoryBean;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.StringUtils;

/**
 * The {@link IndexConfiguration} class is a Spring {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar}
 * and extension of {@link EntityDefinedRegionsConfiguration} used in the {@link EnableIndexing} annotation
 * to dynamically create GemFire/Geode {@link org.apache.geode.cache.Region} {@link Index Indexes} based on
 * {@link GemfirePersistentEntity} {@link GemfirePersistentProperty} annotations.
 *
 * @author John Blum
 * @see java.lang.annotation.Annotation
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.lucene.LuceneIndex
 * @see org.apache.geode.cache.query.Index
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @see org.springframework.core.type.AnnotationMetadata
 * @see org.springframework.data.annotation.Id
 * @see org.springframework.data.gemfire.IndexFactoryBean
 * @see org.springframework.data.gemfire.IndexType
 * @see org.springframework.data.gemfire.config.annotation.EnableIndexing
 * @see org.springframework.data.gemfire.config.annotation.EntityDefinedRegionsConfiguration
 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
 * @see org.springframework.data.gemfire.mapping.GemfirePersistentProperty
 * @see org.springframework.data.gemfire.mapping.annotation.Indexed
 * @see org.springframework.data.gemfire.mapping.annotation.LuceneIndexed
 * @see org.springframework.data.gemfire.search.lucene.LuceneIndexFactoryBean
 * @since 1.9.0
 */
public class IndexConfiguration extends EntityDefinedRegionsConfiguration {

	@Autowired(required = false)
	private List<IndexConfigurer> indexConfigurers = Collections.emptyList();

	/**
	 * Returns the {@link Annotation} {@link Class type} that configures and creates {@link Region} Indexes
	 * from application persistent entity properties.
	 *
	 * @return the {@link Annotation} {@link Class type} that configures and creates {@link Region Region} Indexes
	 * from application persistent entity properties.
	 * @see EnableIndexing
	 * @see java.lang.annotation.Annotation
	 * @see java.lang.Class
	 */
	protected Class<? extends Annotation> getEnableIndexingAnnotationType() {
		return EnableIndexing.class;
	}

	/**
	 * Returns the name of the {@link Annotation} {@link Class type} that configures and creates {@link Region} Indexes
	 * from application persistent entity properties.
	 *
	 * @return the name of the {@link Annotation} {@link Class type} that configures and creates {@link Region Region}
	 * Indexes from application persistent entity properties.
	 * @see #getEnableIndexingAnnotationType()
	 * @see java.lang.Class#getName()
	 */
	protected String getEnableIndexingAnnotationTypeName() {
		return getEnableIndexingAnnotationType().getName();
	}

	/**
	 * Returns the simple name of the {@link Annotation} {@link Class type} that configures and creates {@link Region}
	 * Indexes from application persistent entity properties.
	 *
	 * @return the simple name of the {@link Annotation} {@link Class type} that configures and creates
	 * {@link Region Region} Indexes from application persistent entity properties.
	 * @see #getEnableIndexingAnnotationType()
	 * @see java.lang.Class#getSimpleName()
	 */
	@SuppressWarnings("unused")
	protected String getEnableIndexingAnnotationTypeSimpleName() {
		return getEnableIndexingAnnotationType().getSimpleName();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected GemfirePersistentEntity<?> postProcess(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry, GemfirePersistentEntity<?> persistentEntity) {

		GemfirePersistentEntity<?> localPersistentEntity =
			super.postProcess(importingClassMetadata, registry, persistentEntity);

		if (isAnnotationPresent(importingClassMetadata, getEnableIndexingAnnotationTypeName())) {

			AnnotationAttributes enableIndexingAttributes =
				getAnnotationAttributes(importingClassMetadata, getEnableIndexingAnnotationTypeName());

			localPersistentEntity.doWithProperties((PropertyHandler<GemfirePersistentProperty>) persistentProperty -> {

				Optional<Id> idAnnotation = Optional.ofNullable(persistentProperty.findAnnotation(Id.class));

				idAnnotation.ifPresent(id ->
					registerIndexBeanDefinition(enableIndexingAttributes, localPersistentEntity, persistentProperty,
						IndexType.KEY, id, registry));

				Optional<Indexed> indexedAnnotation =
					Optional.ofNullable(persistentProperty.findAnnotation(Indexed.class));

				indexedAnnotation.ifPresent(indexed ->
					registerIndexBeanDefinition(enableIndexingAttributes, localPersistentEntity, persistentProperty,
						indexed.type(), indexed, registry));

				Optional<LuceneIndexed> luceneIndexedAnnotation =
					Optional.ofNullable(persistentProperty.findAnnotation(LuceneIndexed.class));

				luceneIndexedAnnotation.ifPresent(luceneIndexed ->
					registerLuceneIndexBeanDefinition(enableIndexingAttributes, localPersistentEntity,
						persistentProperty, luceneIndexed, registry));
			});
		}

		return persistentEntity;
	}

	/**
	 * Registers an Index of the given {@link IndexType} for the {@link GemfirePersistentProperty}
	 * on the {@link GemfirePersistentEntity} using the {@link Annotation} meta-data to define the Index.
	 *
	 * @param enableIndexingAttributes {@link AnnotationAttributes} containing meta-data
	 * for the {@link EnableIndexing} annotation.
	 * @param persistentEntity {@link GemfirePersistentEntity} containing the {@link GemfirePersistentProperty}
	 * to be indexed.
	 * @param persistentProperty {@link GemfirePersistentProperty} for which the Index will be created.
	 * @param indexType {@link IndexType} enum specifying the Index type (e.g. KEY, HASH, etc).
	 * @param indexAnnotation Index {@link Annotation}.
	 * @param registry {@link BeanDefinitionRegistry} used to register the Index bean definition.
	 * @see java.lang.annotation.Annotation
	 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see org.springframework.data.gemfire.IndexType
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentProperty
	 */
	protected void registerIndexBeanDefinition(AnnotationAttributes enableIndexingAttributes,
			GemfirePersistentEntity<?> persistentEntity, GemfirePersistentProperty persistentProperty,
			IndexType indexType, Annotation indexAnnotation, BeanDefinitionRegistry registry) {

		Optional.ofNullable(indexAnnotation).ifPresent(localIndexAnnotation -> {

			AnnotationAttributes indexedAttributes = getAnnotationAttributes(localIndexAnnotation);

			BeanDefinitionBuilder indexFactoryBeanBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(IndexFactoryBean.class);

			String indexName = resolveName(persistentEntity, persistentProperty, indexedAttributes, indexType);

			indexFactoryBeanBuilder.addPropertyReference("cache", SpringGemFireConstants.DEFAULT_GEMFIRE_CACHE_NAME);

			indexFactoryBeanBuilder.addPropertyValue("define", resolveDefine(enableIndexingAttributes));

			indexFactoryBeanBuilder.addPropertyValue("expression",
				resolveExpression(persistentEntity, persistentProperty, indexedAttributes));

			String from = resolveFrom(persistentEntity, persistentProperty, indexedAttributes);

			indexFactoryBeanBuilder.addPropertyValue("from", toRegionPath(from));

			indexFactoryBeanBuilder.addPropertyValue("ignoreIfExists", Boolean.TRUE);

			indexFactoryBeanBuilder.addPropertyValue("indexConfigurers", resolveIndexConfigurers());

			indexFactoryBeanBuilder.addPropertyValue("name", indexName);

			indexFactoryBeanBuilder.addPropertyValue("override", Boolean.FALSE);

			indexFactoryBeanBuilder.addPropertyValue("type",
				resolveType(persistentEntity, persistentProperty, indexedAttributes, indexType).toString());

			indexFactoryBeanBuilder.addDependsOn(toRegionName(from));

			registry.registerBeanDefinition(indexName, indexFactoryBeanBuilder.getBeanDefinition());
		});
	}

	private String toRegionName(String from) {

		return Optional.ofNullable(from)
			.filter(StringUtils::hasText)
			.map(it -> {

				boolean isSubRegionPath = from.lastIndexOf(Region.SEPARATOR) > 0;

				return !isSubRegionPath && from.startsWith(Region.SEPARATOR) ? from.substring(1) : from;

			})
			.orElseThrow(() -> newIllegalArgumentException("From clause [%s] is required", from));
	}

	private String toRegionPath(String from) {

		return Optional.ofNullable(from)
			.filter(StringUtils::hasText)
			.map(it -> from.startsWith(Region.SEPARATOR) ? from : GemfireUtils.toRegionPath(from))
			.orElseThrow(() -> newIllegalArgumentException("From clause [%s] is required", from));
	}

	/**
	 * Registers a {@link LuceneIndex} for the {@link GemfirePersistentProperty} on the {@link GemfirePersistentEntity}
	 * using the {@link Annotation} meta-data to define the Index.
	 *
	 * @param enableIndexingAttributes {@link AnnotationAttributes} containing meta-data
	 * for the {@link EnableIndexing} annotation.
	 * @param persistentEntity {@link GemfirePersistentEntity} containing the {@link GemfirePersistentProperty}
	 * to be indexed.
	 * @param persistentProperty {@link GemfirePersistentProperty} for which the {@link LuceneIndex} will be created.
	 * @param luceneIndexAnnotation {@link LuceneIndexed} {@link Annotation}.
	 * @param registry {@link BeanDefinitionRegistry} used to register the {@link LuceneIndex} bean definition.
	 * @see java.lang.annotation.Annotation
	 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentEntity
	 * @see org.springframework.data.gemfire.mapping.GemfirePersistentProperty
	 * @see org.springframework.data.gemfire.mapping.annotation.LuceneIndexed
	 */
	@SuppressWarnings("unused")
	protected void registerLuceneIndexBeanDefinition(AnnotationAttributes enableIndexingAttributes,
			GemfirePersistentEntity<?> persistentEntity, GemfirePersistentProperty persistentProperty,
			Annotation luceneIndexAnnotation, BeanDefinitionRegistry registry) {

		Optional.ofNullable(luceneIndexAnnotation).ifPresent(localLuceneIndexAnnotation -> {

			AnnotationAttributes luceneIndexAttributes =
				AnnotationAttributes.fromMap(AnnotationUtils.getAnnotationAttributes(localLuceneIndexAnnotation));

			BeanDefinitionBuilder luceneIndexFactoryBeanBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(LuceneIndexFactoryBean.class);

			String indexName = luceneIndexAttributes.getString("name");

			boolean destroy = luceneIndexAttributes.containsKey("destroy")
				&& luceneIndexAttributes.getBoolean("destroy");

			luceneIndexFactoryBeanBuilder.addPropertyReference("cache",
				SpringGemFireConstants.DEFAULT_GEMFIRE_CACHE_NAME);

			luceneIndexFactoryBeanBuilder.addPropertyValue("destroy", destroy);

			luceneIndexFactoryBeanBuilder.addPropertyValue("fields", persistentProperty.getName());

			luceneIndexFactoryBeanBuilder.addPropertyValue("indexConfigurers", resolveIndexConfigurers());

			luceneIndexFactoryBeanBuilder.addPropertyValue("indexName", indexName);

			luceneIndexFactoryBeanBuilder.addPropertyValue("regionPath", persistentEntity.getRegionName());

			registry.registerBeanDefinition(indexName, luceneIndexFactoryBeanBuilder.getBeanDefinition());
		});
	}

	private List<IndexConfigurer> resolveIndexConfigurers() {

		return Optional.ofNullable(this.indexConfigurers)
			.filter(indexConfigurers -> !indexConfigurers.isEmpty())
			.orElseGet(() ->
				Collections.singletonList(LazyResolvingComposableIndexConfigurer.create(getBeanFactory())));
	}

	private boolean resolveDefine(AnnotationAttributes enableIndexingAttributes) {

		return enableIndexingAttributes.containsKey("define")
			&& enableIndexingAttributes.getBoolean("define");
	}

	@SuppressWarnings("unused")
	private String resolveExpression(GemfirePersistentEntity<?> persistentEntity,
			GemfirePersistentProperty persistentProperty, AnnotationAttributes indexedAttributes) {

		String expression = indexedAttributes.containsKey("expression")
			? indexedAttributes.getString("expression")
			: null;

		return (StringUtils.hasText(expression) ? expression : persistentProperty.getName());
	}

	@SuppressWarnings("unused")
	private String resolveFrom(GemfirePersistentEntity<?> persistentEntity,
			GemfirePersistentProperty persistentProperty, AnnotationAttributes indexedAttributes) {

		String from = indexedAttributes.containsKey("from")
			? indexedAttributes.getString("from")
			: null;

		return (StringUtils.hasText(from) ? from : persistentEntity.getRegionName());
	}

	private String resolveName(GemfirePersistentEntity<?> persistentEntity,
			GemfirePersistentProperty persistentProperty, AnnotationAttributes indexedAttributes, IndexType indexType) {

		String indexName = indexedAttributes.containsKey("name")
			? indexedAttributes.getString("name")
			: null;

		return (StringUtils.hasText(indexName) ? indexName
			: generateIndexName(persistentEntity, persistentProperty, indexType));
	}

	private String generateIndexName(GemfirePersistentEntity persistentEntity,
			GemfirePersistentProperty persistentProperty, IndexType indexType) {

		return String.format("%1$s%2$s%3$sIdx", persistentEntity.getRegionName(),
			StringUtils.capitalize(persistentProperty.getName()),
				StringUtils.capitalize(indexType.name().toLowerCase()));
	}

	@SuppressWarnings("unused")
	private IndexType resolveType(GemfirePersistentEntity<?> persistentEntity,
			GemfirePersistentProperty persistentProperty, AnnotationAttributes indexedAttributes, IndexType indexType) {

		IndexType resolvedIndexType = indexedAttributes.containsKey("type")
			? indexedAttributes.getEnum("type")
			: null;

		return Optional.ofNullable(resolvedIndexType).orElse(indexType);
	}
}
