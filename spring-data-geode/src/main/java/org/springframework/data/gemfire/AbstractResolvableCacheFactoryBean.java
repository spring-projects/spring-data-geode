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
package org.springframework.data.gemfire;

import static org.springframework.data.gemfire.GemfireUtils.apacheGeodeProductName;
import static org.springframework.data.gemfire.GemfireUtils.apacheGeodeVersion;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.util.Optional;
import java.util.Properties;

import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.distributed.DistributedSystem;

import org.springframework.lang.NonNull;

/**
 * Abstract base class encapsulating logic to resolve or create a {@link GemFireCache cache} instance.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.apache.geode.distributed.DistributedSystem
 * @see org.springframework.data.gemfire.AbstractBasicCacheFactoryBean
 * @since 2.5.0
 */
public abstract class AbstractResolvableCacheFactoryBean extends AbstractBasicCacheFactoryBean {

	private volatile String cacheResolutionMessagePrefix;

	/**
	 * @inheritDoc
	 */
	@Override
	protected GemFireCache doGetObject() {
		return init();
	}

	/**
	 * Initializes a {@link GemFireCache}.
	 *
	 * @return a reference to the initialized {@link GemFireCache}.
	 * @see org.apache.geode.cache.GemFireCache
	 * @see #setCache(GemFireCache)
	 * @see #resolveCache()
	 * @see #getCache()
	 */
	protected GemFireCache init() {

		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			// Use Spring Bean ClassLoader to load Spring configured Apache Geode classes
			Thread.currentThread().setContextClassLoader(getBeanClassLoader());

			setCache(resolveCache());

			logCacheInitialization();

			return getCache();
		}
		catch (Exception cause) {
			throw newRuntimeException(cause, "Error occurred while initializing the cache");
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}
	}

	@SuppressWarnings("deprecation")
	private void logCacheInitialization() {

		getOptionalCache().ifPresent(cache -> {

			Optional.ofNullable(cache.getDistributedSystem())
				.map(DistributedSystem::getDistributedMember)
				.ifPresent(member -> {

					String message = "Connected to Distributed System [%1$s] as Member [%2$s] in Group(s) [%3$s]"
						+ " with Role(s) [%4$s] on Host [%5$s] having PID [%6$d]";

					logInfo(() -> String.format(message,
						cache.getDistributedSystem().getName(), member.getId(), member.getGroups(),
						member.getRoles(), member.getHost(), member.getProcessId()));
				});

			logInfo(() -> String.format("%1$s %2$s version [%3$s] Cache [%4$s]", this.cacheResolutionMessagePrefix,
				apacheGeodeProductName(), apacheGeodeVersion(), cache.getName()));

		});
	}

	/**
	 * Resolves a {@link GemFireCache} by attempting to lookup an existing {@link GemFireCache} instance in the JVM,
	 * first. If an existing {@link GemFireCache} could not be found, then this method proceeds in attempting to
	 * create a new {@link GemFireCache} instance.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @return the resolved {@link GemFireCache}.
	 * @see org.apache.geode.cache.client.ClientCache
	 * @see org.apache.geode.cache.GemFireCache
	 * @see org.apache.geode.cache.Cache
	 * @see #fetchCache()
	 * @see #resolveProperties()
	 * @see #createFactory(java.util.Properties)
	 * @see #initializeFactory(Object)
	 * @see #configureFactory(Object)
	 * @see #postProcess(Object)
	 * @see #createCache(Object)
	 * @see #postProcess(GemFireCache)
	 */
	protected <T extends GemFireCache> T resolveCache() {

		try {

			this.cacheResolutionMessagePrefix = "Found existing";

			T cache = fetchCache();

			cache = postProcess(cache);

			return cache;
		}
		catch (CacheClosedException cause) {

			this.cacheResolutionMessagePrefix = "Created new";

			Properties gemfireProperties = resolveProperties();

			Object factory = createFactory(gemfireProperties);

			factory = initializeFactory(factory);
			factory = configureFactory(factory);
			factory = postProcess(factory);

			T cache = createCache(factory);

			cache = postProcess(cache);

			return cache;
		}
	}

	/**
	 * Constructs a new cache factory initialized with the given Apache Geode {@link Properties}
	 * used to construct, configure and initialize a new {@link GemFireCache}.
	 *
	 * @param gemfireProperties {@link Properties} used by the cache factory to configure the {@link GemFireCache};
	 * must not be {@literal null}
	 * @return a new cache factory initialized with the given Apache Geode {@link Properties}.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.CacheFactory
	 * @see java.util.Properties
	 * @see #resolveProperties()
	 */
	protected abstract @NonNull Object createFactory(@NonNull Properties gemfireProperties);

	/**
	 * Configures the cache factory used to create the {@link GemFireCache}.
	 *
	 * @param factory cache factory to configure; must not be {@literal null}.
	 * @return the given cache factory.
	 * @see #createFactory(Properties)
	 */
	protected @NonNull Object configureFactory(@NonNull Object factory) {
		return factory;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	protected Object initializeFactory(Object factory) {
		return super.initializeFactory(factory);
	}

	/**
	 * Post process the cache factory used to create the {@link GemFireCache}.
	 *
	 * @param factory cache factory to post process; must not be {@literal null}.
	 * @return the post processed cache factory.
	 * @see org.apache.geode.cache.client.ClientCacheFactory
	 * @see org.apache.geode.cache.CacheFactory
	 * @see #createFactory(Properties)
	 */
	protected @NonNull Object postProcess(@NonNull Object factory) {
		return factory;
	}

	/**
	 * Creates a new {@link GemFireCache} instance using the provided {@link Object factory}.
	 *
	 * @param <T> {@link Class Subtype} of {@link GemFireCache}.
	 * @param factory factory used to create the {@link GemFireCache}.
	 * @return a new instance of {@link GemFireCache} created by the provided {@link Object factory}.
	 * @see org.apache.geode.cache.client.ClientCacheFactory#create()
	 * @see org.apache.geode.cache.CacheFactory#create()
	 * @see org.apache.geode.cache.GemFireCache
	 */
	protected abstract @NonNull <T extends GemFireCache> T createCache(@NonNull Object factory);

	/**
	 * Post process the {@link GemFireCache} by loading any {@literal cache.xml} file, applying custom settings
	 * specified in SDG XML configuration metadata, and registering appropriate Transaction Listeners, Writer
	 * and JVM Heap configuration.
	 *
	 * @param <T> parameterized {@link Class} type extending {@link GemFireCache}.
	 * @param cache {@link GemFireCache} to post process.
	 * @return the given {@link GemFireCache}.
	 * @see #loadCacheXml(GemFireCache)
	 * @see org.apache.geode.cache.Cache#loadCacheXml(java.io.InputStream)
	 * @see #configureHeapPercentages(org.apache.geode.cache.GemFireCache)
	 * @see #configureOffHeapPercentages(GemFireCache)
	 * @see #registerTransactionListeners(org.apache.geode.cache.GemFireCache)
	 * @see #registerTransactionWriter(org.apache.geode.cache.GemFireCache)
	 */
	protected @NonNull <T extends GemFireCache> T postProcess(@NonNull T cache) {

		loadCacheXml(cache);

		Optional.ofNullable(getCopyOnRead()).ifPresent(cache::setCopyOnRead);

		configureHeapPercentages(cache);
		configureOffHeapPercentages(cache);
		registerTransactionListeners(cache);
		registerTransactionWriter(cache);

		return cache;
	}
}
