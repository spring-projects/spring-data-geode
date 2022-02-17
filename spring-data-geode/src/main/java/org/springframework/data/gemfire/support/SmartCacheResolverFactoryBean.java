/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.CacheResolver;
import org.springframework.data.gemfire.client.support.ClientCacheFactoryCacheResolver;
import org.springframework.data.gemfire.util.CacheUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring {@link FactoryBean} used to construct a custom, determined {@link CacheResolver} that strategically
 * and lazily resolves a cache instance.
 *
 * @author John Blum
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.data.gemfire.CacheResolver
 * @see org.springframework.data.gemfire.support.AbstractFactoryBeanSupport
 * @since 2.3.0
 */
@SuppressWarnings({ "rawtypes", "unused" })
public class SmartCacheResolverFactoryBean extends AbstractFactoryBeanSupport<CacheResolver<GemFireCache>>
		implements InitializingBean {

	/**
	 * Factory method used to construct a new instance of the {@link SmartCacheResolverFactoryBean}.
	 *
	 * @return a new instance of {@link SmartCacheResolverFactoryBean}.
	 * @see org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean
	 */
	public static @NonNull SmartCacheResolverFactoryBean create() {
		return new SmartCacheResolverFactoryBean();
	}

	private CacheResolver<GemFireCache> cacheResolver;

	private CompositionStrategy compositionStrategy = CompositionStrategy.DEFAULT;

	private GemFireCache cache;

	private List<CacheResolver> configuredCacheResolvers;

	private String cacheBeanName;

	/**
	 * Initializes the {@link CacheResolver} [composition] created by this {@link FactoryBean}.
	 *
	 * @see org.springframework.data.gemfire.support.SmartCacheResolverFactoryBean.CompositionStrategy#use(SmartCacheResolverFactoryBean)
	 * @see #getCompositionStrategy()
	 */
	@Override
	public void afterPropertiesSet() {

		this.cacheResolver = getCompositionStrategy().use(this);

		Assert.state(this.cacheResolver != null,
			() -> String.format("No CacheResolver was composed with CompositionStrategy [%s]",
				getCompositionStrategy()));

	}

	/**
	 * Returns a reference to the constructed {@link CacheResolver} instance created by this {@link FactoryBean}.
	 *
	 * @return a reference to the constructed {@link CacheResolver} instance created by this {@link FactoryBean}.
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see org.apache.geode.cache.GemFireCache
	 */
	CacheResolver<GemFireCache> getCacheResolver() {
		return this.cacheResolver;
	}

	/**
	 * Returns the fully constructed and initialized instance of the {@link CacheResolver}
	 * created by this {@link FactoryBean}.
	 *
	 * If the {@link #afterPropertiesSet()} method has not yet been called, then this method returns a {@literal Proxy}
	 * to the {@link CacheResolver} instance that will eventually be created by this {@link FactoryBean}.
	 *
	 * @return the {@link CacheResolver} instance constructed and initialized by this {@link FactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 * @see CacheResolverProxy
	 */
	@NonNull
	public CacheResolver<GemFireCache> getObject() {
		return new CacheResolverProxy();
	}

	/**
	 * Returns the {@link Class type} of the {@link CacheResolver} returned by this {@link FactoryBean}.
	 *
	 * @return the {@link Class type} of the {@link CacheResolver} returned by this {@link FactoryBean}.
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see java.lang.Class
	 */
	@Nullable @Override
	public Class<?> getObjectType() {

		CacheResolver<?> cacheResolver = getCacheResolver();

		return cacheResolver != null ? cacheResolver.getClass() : CacheResolver.class;
	}

	/**
	 * Constructs a {@link CacheResolver} capable of resolving a cache instance from the Spring {@link BeanFactory}.
	 *
	 * Returns {@literal null} if the {@link BeanFactory} reference is {@literal null} (i.e. not configured).
	 *
	 * @return a {@link CacheResolver} capable of resolving a cache instance from the Spring {@link BeanFactory}.
	 * @see org.springframework.data.gemfire.support.BeanFactoryCacheResolver
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see #getBeanFactory()
	 */
	protected @Nullable CacheResolver newBeanFactoryCacheResolver() {

		// Depending on the application context, there may or may not be a Spring ApplicationContext...
		// Think Spring on the GemFire/Geode server-side with no Spring container
		return Optional.ofNullable(getBeanFactory())
			.map(BeanFactoryCacheResolver::new)
			.map(cacheResolver -> {
				getCacheBeanName().ifPresent(cacheResolver::setCacheBeanName);
				return cacheResolver;
			})
			.orElse(null);
	}

	/**
	 * Constructs a {@link CacheResolver} capable of resolving a {@link ClientCache} instance using
	 * the Apache Geode {@link ClientCacheFactory} API.
	 *
	 * @return a {@link CacheResolver} resolving a {@link ClientCache} instance.
	 * @see org.springframework.data.gemfire.client.support.ClientCacheFactoryCacheResolver
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected @NonNull CacheResolver newClientCacheFactoryCacheResolver() {
		return new ClientCacheFactoryCacheResolver();
	}

	/**
	 * Constructs a {@link CacheResolver} capable of resolving a {@literal peer} {@link Cache} instance using
	 * the Apache Geode {@link CacheFactory} API.
	 *
	 * @return a {@link CacheResolver} resolving a {@literal peer} {@link Cache} instance.
	 * @see org.springframework.data.gemfire.support.CacheFactoryCacheResolver
	 * @see org.springframework.data.gemfire.CacheResolver
	 */
	protected @NonNull CacheResolver newPeerCacheFactoryCacheResolver() {
		return new CacheFactoryCacheResolver();
	}

	/**
	 * Constructs a {@link CacheResolver} capable of resolving a single, configured cache instance.
	 *
	 * Returns {@literal null} if a cache instance was not {@link #setCache(GemFireCache) configured}
	 * on this {@link FactoryBean}.
	 *
	 * @return a {@link CacheResolver} capable of resolving a single, configured cache instance.
	 * @see org.springframework.data.gemfire.support.SingleCacheCacheResolver
	 * @see org.springframework.data.gemfire.CacheResolver
	 * @see #getCache()
	 */
	protected @Nullable CacheResolver newSingleCacheCacheResolver() {

		return getCache()
			.map(this::newSingleCacheCacheResolver)
			.orElse(null);
	}

	private @NonNull CacheResolver newSingleCacheCacheResolver(@NonNull GemFireCache cache) {

		return CacheUtils.isClient(cache)
			? SingleCacheCacheResolver.from((ClientCache) cache)
			: SingleCacheCacheResolver.from((Cache) cache);
	}

	public final void setCache(@Nullable GemFireCache cache) {
		this.cache = cache;
	}

	protected Optional<GemFireCache> getCache() {
		return Optional.ofNullable(this.cache);
	}

	public final void setCacheBeanName(@Nullable String cacheBeanName) {
		this.cacheBeanName = cacheBeanName;
	}

	protected Optional<String> getCacheBeanName() {

		return Optional.ofNullable(this.cacheBeanName)
			.filter(StringUtils::hasText);
	}

	public final void setCompositionStrategy(CompositionStrategy compositionStrategy) {
		this.compositionStrategy = compositionStrategy;
	}

	protected CompositionStrategy getCompositionStrategy() {
		return this.compositionStrategy != null ? this.compositionStrategy : CompositionStrategy.DEFAULT;
	}

	@Autowired(required = false)
	public final void setConfiguredCacheResolvers(List<CacheResolver> cacheResolvers) {

		// NOTE: Be careful not to add the CacheResolverProxy from this SmartCacheResolverFactoryBean
		this.configuredCacheResolvers = CollectionUtils.nullSafeList(cacheResolvers).stream()
			.filter(Objects::nonNull)
			.filter(this::isNotCacheResolverProxyForThisFactoryBean)
			.collect(Collectors.toList());
	}

	protected List<CacheResolver> getConfiguredCacheResolvers() {
		return this.configuredCacheResolvers;
	}

	private boolean isCacheResolverProxyForThisFactoryBean(CacheResolver<?> cacheResolver) {

		return cacheResolver instanceof CacheResolverProxy
			&& ((CacheResolverProxy) cacheResolver).getCacheResolverFactoryBean().equals(this);
	}

	private boolean isNotCacheResolverProxyForThisFactoryBean(CacheResolver<?> cacheResolver) {
		return !isCacheResolverProxyForThisFactoryBean(cacheResolver);
	}

	private List<CacheResolver> resolveCacheResolvers() {

		BeanFactory beanFactory = getBeanFactory();

		Assert.state(beanFactory != null,
			() -> String.format("A Spring context is not present and is required to use [%1$s.%2$s.%3$s]",
				CompositionStrategy.class.getEnclosingClass().getSimpleName(),
				CompositionStrategy.class.getSimpleName(), CompositionStrategy.USER_DEFINED.name()));

		List<CacheResolver> orderedCacheResolvers =
			SpringUtils.getOrderedStreamOfBeansByType(beanFactory, CacheResolver.class)
				.collect(Collectors.toList());

		setConfiguredCacheResolvers(orderedCacheResolvers);

		return orderedCacheResolvers;
	}

	public SmartCacheResolverFactoryBean withCache(GemFireCache cache) {
		setCache(cache);
		return this;
	}

	public SmartCacheResolverFactoryBean withCacheBeanName(String cacheBeanName) {
		setCacheBeanName(cacheBeanName);
		return this;
	}

	public SmartCacheResolverFactoryBean usingCompositionStrategy(CompositionStrategy compositionStrategy) {
		setCompositionStrategy(compositionStrategy);
		return this;
	}

	class CacheResolverProxy implements CacheResolver<GemFireCache> {

		Optional<CacheResolver<GemFireCache>> getCacheResolver() {
			return Optional.ofNullable(getCacheResolverFactoryBean().getCacheResolver());
		}

		SmartCacheResolverFactoryBean getCacheResolverFactoryBean() {
			return SmartCacheResolverFactoryBean.this;
		}

		@Override
		public GemFireCache resolve() {

			return getCacheResolver()
				.map(CacheResolver::resolve)
				.orElseThrow(() -> newIllegalStateException("CacheResolver was not initialized"));
		}
	}

	/**
	 * Specifies the declaration of the algorithm to use to compose {@link CacheResolver CacheResolvers}
	 * into a composition functioning as a single, complete and cohesive cache resolution strategy.
	 */
	enum CompositionStrategy {

		DEFAULT {

			@Override @SuppressWarnings("unchecked")
			<T extends GemFireCache> CacheResolver<T> use(SmartCacheResolverFactoryBean factoryBean) {

				return ComposableCacheResolver.compose(
					factoryBean.newSingleCacheCacheResolver(),
					factoryBean.newBeanFactoryCacheResolver(),
					factoryBean.newClientCacheFactoryCacheResolver(),
					factoryBean.newPeerCacheFactoryCacheResolver());
			}
		},

		GEODE {

			@Override @SuppressWarnings("unchecked")
			<T extends GemFireCache> CacheResolver<T> use(SmartCacheResolverFactoryBean factoryBean) {

				return ComposableCacheResolver.compose(
					factoryBean.newSingleCacheCacheResolver(),
					factoryBean.newClientCacheFactoryCacheResolver(),
					factoryBean.newPeerCacheFactoryCacheResolver()
				);
			}
		},

		SPRING {

			@Override @SuppressWarnings("unchecked")
			<T extends GemFireCache> CacheResolver<T> use(SmartCacheResolverFactoryBean factoryBean) {

				return ComposableCacheResolver.compose(
					factoryBean.newSingleCacheCacheResolver(),
					factoryBean.newBeanFactoryCacheResolver()
				);
			}
		},

		USER_DEFINED {

			@Override @SuppressWarnings("unchecked")
			<T extends GemFireCache> CacheResolver<T> use(SmartCacheResolverFactoryBean factoryBean) {

				List<CacheResolver> cacheResolvers = computeIfAbsent(factoryBean);

				Assert.state(isNotEmpty(cacheResolvers), "No CacheResolver beans were defined");

				CacheResolver<T> current = null;

				for (CacheResolver<T> cacheResolver : cacheResolvers) {
					current = ComposableCacheResolver.compose(current, cacheResolver);
				}

				return current;
			}

			private synchronized List<CacheResolver> computeIfAbsent(SmartCacheResolverFactoryBean factoryBean) {

				return Optional.ofNullable(factoryBean.getConfiguredCacheResolvers())
					.filter(this::isNotEmpty)
					.orElseGet(factoryBean::resolveCacheResolvers);
			}

			private boolean isNotEmpty(List<?> list) {
				return !(list == null || list.isEmpty());
			}
		};

		abstract <T extends GemFireCache> CacheResolver<T> use(SmartCacheResolverFactoryBean factoryBean);

	}
}
