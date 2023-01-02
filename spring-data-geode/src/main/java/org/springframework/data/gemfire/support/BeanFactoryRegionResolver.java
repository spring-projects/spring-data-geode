/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.gemfire.support;

import org.apache.geode.cache.Region;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.gemfire.RegionResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link RegionResolver} implementation used to resolve a {@link Region} by {@link String name}
 * from a Spring {@link BeanFactory}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Region
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.data.gemfire.RegionResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingRegionResolver
 * @since 2.3.0
 */
public class BeanFactoryRegionResolver extends AbstractCachingRegionResolver implements BeanFactoryAware {

	private BeanFactory beanFactory;

	/**
	 * Constructs a new instance of {@link BeanFactoryRegionResolver} initialized with the given {@link BeanFactory}.
	 *
	 * @param beanFactory {@link BeanFactory} used to resolve cache {@link Region Regions}.
	 * @throws IllegalArgumentException if {@link BeanFactory} is {@literal null}.
	 * @see org.springframework.beans.factory.BeanFactory
	 * @see #setBeanFactory(BeanFactory)
	 */
	public BeanFactoryRegionResolver(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}

	/**
	 * Sets a reference to the {@link BeanFactory} used to resolve cache {@link Region Regions}.
	 *
	 * @param beanFactory {@link BeanFactory} used to resolve cache {@link Region Regions}.
	 * @throws IllegalArgumentException if {@link BeanFactory} is {@literal null}.
	 * @throws BeansException if configuration of the {@link BeanFactory} fails.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	@Override
	public final void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		this.beanFactory = beanFactory;
	}

	/**
	 * Returns the configured reference to the {@link BeanFactory} used to resolve cache {@link Region Regions}.
	 *
	 * @return a reference to the configured {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	protected @NonNull BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * @inheritDoc
	 */
	@Nullable @Override @SuppressWarnings("unchecked")
	protected <K, V> Region<K, V> doResolve(@Nullable String regionName) {

		BeanFactory beanFactory = getBeanFactory();

		return StringUtils.hasText(regionName) && beanFactory.containsBean(regionName)
			? beanFactory.getBean(regionName, Region.class)
			: null;
	}
}
