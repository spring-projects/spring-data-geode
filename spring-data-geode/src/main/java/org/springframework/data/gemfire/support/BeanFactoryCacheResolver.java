/*
 * Copyright 2020 the original author or authors.
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

import org.apache.geode.cache.GemFireCache;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.gemfire.CacheResolver;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Cacheable {@link CacheResolver} implementation capable of resolving a {@link GemFireCache} instance
 * from the Spring {@link BeanFactory}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.AbstractCachingCacheResolver
 * @since 2.3.0
 */
public class BeanFactoryCacheResolver extends AbstractCachingCacheResolver<GemFireCache> implements BeanFactoryAware {

	private BeanFactory beanFactory;

	/**
	 * Constructs a new instance of {@link BeanFactoryCacheResolver} initialized with the given, required
	 * Spring {@link BeanFactory}.
	 *
	 * @param beanFactory {@link BeanFactory} used to resolve the {@link GemFireCache}.
	 * @throws IllegalArgumentException if {@link BeanFactory} is {@literal null}.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	public BeanFactoryCacheResolver(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}

	/**
	 * Sets a reference to the {@link BeanFactory} used to resolve the {@link GemFireCache}.
	 *
	 * @param beanFactory {@link BeanFactory} used to resolve the {@link GemFireCache}.
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
	 * Returns the configured reference to the Spring {@link BeanFactory} used to resolve the single instance
	 * of the {@link GemFireCache}.
	 *
	 * @return a reference to the configured Spring {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	protected @NonNull BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Uses the configured Spring {@link BeanFactory} to resolve a reference to
	 * the single {@link GemFireCache} instance.
	 *
	 * @return a reference to the {@link GemFireCache} bean.
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #getBeanFactory()
	 */
	@Override
	protected GemFireCache doResolve() {
		return getBeanFactory().getBean(GemFireCache.class);
	}
}
