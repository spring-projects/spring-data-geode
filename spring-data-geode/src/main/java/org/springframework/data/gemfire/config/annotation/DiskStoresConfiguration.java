/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.Arrays;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.gemfire.util.ArrayUtils;

/**
 * The {@link DiskStoresConfiguration} class is a Spring {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar}
 * used to register multiple GemFire/Geode {@link org.apache.geode.cache.DiskStore} bean definitions.
 *
 * @author John Blum
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @see org.springframework.data.gemfire.config.annotation.DiskStoreConfiguration
 * @see org.springframework.data.gemfire.config.annotation.EnableDiskStore
 * @see org.springframework.data.gemfire.config.annotation.EnableDiskStores
 * @see org.apache.geode.cache.DiskStore
 * @since 1.9.0
 */
public class DiskStoresConfiguration extends DiskStoreConfiguration {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		if (importingClassMetadata.hasAnnotation(EnableDiskStores.class.getName())) {

			AnnotationAttributes enableDiskStoresAttributes =
				getAnnotationAttributes(importingClassMetadata, EnableDiskStores.class.getName());

			AnnotationAttributes[] diskStores =
				enableDiskStoresAttributes.getAnnotationArray("diskStores");

			Arrays.stream(ArrayUtils.nullSafeArray(diskStores, AnnotationAttributes.class))
				.forEach(diskStoreAttributes ->  registerDiskStoreBeanDefinition(
					mergeDiskStoreAttributes(enableDiskStoresAttributes, diskStoreAttributes), registry));
		}
	}

	protected AnnotationAttributes mergeDiskStoreAttributes(AnnotationAttributes enableDiskStoresAttributes,
			AnnotationAttributes diskStoreAttributes) {

		setAttributeIfNotDefault(diskStoreAttributes, "autoCompact",
			enableDiskStoresAttributes.getBoolean("autoCompact"), false);

		setAttributeIfNotDefault(diskStoreAttributes, "compactionThreshold",
			enableDiskStoresAttributes.<Integer>getNumber("compactionThreshold"), 50);

		setAttributeIfNotDefault(diskStoreAttributes, "maxOplogSize",
			enableDiskStoresAttributes.<Long>getNumber("maxOplogSize"), 1024L);

		return diskStoreAttributes;
	}

	private <T> void setAttributeIfNotDefault(AnnotationAttributes diskStoreAttributes,
			String attributeName, T newValue, T defaultValue) {

		if (!diskStoreAttributes.containsKey(attributeName)
				|| toString(diskStoreAttributes.get(attributeName)).equals(toString(defaultValue))) {

			diskStoreAttributes.put(attributeName, newValue);
		}
	}

	private String toString(Object value) {
		return String.valueOf(value);
	}
}
