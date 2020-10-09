/*
 * Copyright 2010-2020 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.geode.GemFireCheckedException;
import org.apache.geode.GemFireException;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.query.IndexInvalidException;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryInvalidException;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * This Template class simplifies Apache Geode data access operations, converting Apache Geode
 * {@link GemFireCheckedException GemFireCheckedExceptions} and {@link GemFireException GemFireExceptions} into
 * Spring {@link DataAccessException DataAccessExceptions}, following the <code>org.springframework.dao</code>
 * {@link Exception} hierarchy.
 *
 * The central method is <code>execute</code>, supporting Apache Geode data access code implementing the
 * {@link GemfireCallback} interface. It provides dedicated handling such that neither the {@link GemfireCallback}
 * implementation nor the {@literal calling code} needs to explicitly care about handling {@link Region} life-cycle
 * {@link Exception Exceptions}.
 *
 * This template class is typically used to implement data access operations or business logic services using Apache
 * Geode within their implementation but are Geode-agnostic in their interface. The latter or code calling the latter
 * only have to deal with business objects, query objects, and <code>org.springframework.dao</code>
 * {@link Exception Exceptions}.
 *
 * @author Costin Leau
 * @author John Blum
 * @see java.util.Map
 * @see org.apache.geode.GemFireCheckedException
 * @see org.apache.geode.GemFireException
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.query.Query
 * @see org.apache.geode.cache.query.QueryService
 * @see org.apache.geode.cache.query.SelectResults
 * @see org.springframework.data.gemfire.GemfireAccessor
 * @see org.springframework.data.gemfire.GemfireOperations
 */
@SuppressWarnings("unused")
public class GemfireTemplate extends GemfireAccessor implements GemfireOperations {

	private boolean exposeNativeRegion = false;

	private Region<?, ?> regionProxy;

	/**
	 * Constructs a new, uninitialized instance of {@link GemfireTemplate}.
	 *
	 * @see #GemfireTemplate(Region)
	 */
	public GemfireTemplate() { }

	/**
	 * Constructs a new instance of the {@link GemfireTemplate} initialized with the given {@link Region} on which
	 * (cache) data access operations will be performed.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @param region {@link Region} on which data access operations will be performed by this template;
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link Region} is {@literal null}.
	 * @see #setRegion(Region)
	 * @see #afterPropertiesSet()
	 */
	public <K, V> GemfireTemplate(Region<K, V> region) {
		setRegion(region);
		afterPropertiesSet();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void afterPropertiesSet() {

		super.afterPropertiesSet();

		this.regionProxy = createRegionProxy(getRegion());
	}

	/**
	 * Configure whether to expose the native {@link Region} to {@link GemfireCallback} code.
	 *
	 * <p>Default is {@literal false}, therefore a {@link Region} {@literal proxy} will be returned,
	 * suppressing <code>close</code> calls.
	 *
	 * <p>As there is often a need to cast to an interface, the exposed proxy implements all interfaces implemented by
	 * the original {@link Region}. If this is not sufficient, turn this flag to {@literal true}.
	 *
	 * @param exposeNativeRegion a boolean value indicating whether the native {@link Region} should be exposed to
	 * the {@link GemfireCallback}.
	 * @see org.springframework.data.gemfire.GemfireCallback
	 */
	public void setExposeNativeRegion(boolean exposeNativeRegion) {
		this.exposeNativeRegion = exposeNativeRegion;
	}

	/**
	 * Determines whether to expose the native {@link Region} or the {@link Region} {@literal proxy}
	 * to {@link GemfireCallback} code.
	 *
	 * @return a boolean value indicating whether the native {@link Region} or the {@link Region} {@literal proxy}
	 * is exposed to {@link GemfireCallback} code.
	 * @see #setExposeNativeRegion(boolean)
	 */
	public boolean isExposeNativeRegion() {
		return this.exposeNativeRegion;
	}

	@Override
	public boolean containsKey(Object key) {
		return getRegion().containsKey(key);
	}

	@Override
	public boolean containsKeyOnServer(Object key) {
		return getRegion().containsKeyOnServer(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return getRegion().containsValue(value);
	}

	@Override
	public boolean containsValueForKey(Object key) {
		return getRegion().containsValueForKey(key);
	}

	@Override
	public <K, V> void create(K key, V value) {

		try {
			getRegion().create(key, value);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> V get(K key) {

		try {
			return this.<K, V>getRegion().get(key);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> Map<K, V> getAll(Collection<?> keys) {

		try {
			return this.<K, V>getRegion().getAll(keys);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> V put(K key, V value) {

		try {
			return this.<K, V>getRegion().put(key, value);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> void putAll(Map<? extends K, ? extends V> map) {

		try {
			this.<K, V>getRegion().putAll(map);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> V putIfAbsent(K key, V value) {

		try {
			return this.<K, V>getRegion().putIfAbsent(key, value);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> V remove(K key) {

		try {
			return this.<K, V>getRegion().remove(key);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> V replace(K key, V value) {

		try {
			return this.<K, V>getRegion().replace(key, value);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <K, V> boolean replace(K key, V oldValue, V newValue) {

		try {
			return this.<K, V>getRegion().replace(key, oldValue, newValue);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
	}

	@Override
	public <E> SelectResults<E> query(String query) {

		try {
			return getRegion().query(query);
		}
		catch (IndexInvalidException | QueryInvalidException cause) {
			throw convertGemFireQueryException(cause);
		}
		catch (GemFireCheckedException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (RuntimeException cause) {

			if (GemfireCacheUtils.isCqInvalidException(cause)) {
				throw GemfireCacheUtils.convertCqInvalidException(cause);
			}

			throw cause;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> SelectResults<E> find(String query, Object... arguments) throws InvalidDataAccessApiUsageException {

		try {

			QueryService queryService = resolveQueryService(getRegion());

			Query compiledQuery = queryService.newQuery(query);

			Object result = compiledQuery.execute(arguments);

			if (result instanceof SelectResults) {
				return (SelectResults<E>) result;
			}
			else {

				String message =
					String.format("The result from executing query [%1$s] was not an instance of SelectResults [%2$s]",
						query, result);

				throw new InvalidDataAccessApiUsageException(message);
			}
		}
		catch (IndexInvalidException | QueryInvalidException cause) {
			throw convertGemFireQueryException(cause);
		}
		catch (GemFireCheckedException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (RuntimeException cause) {

			if (GemfireCacheUtils.isCqInvalidException(cause)) {
				throw GemfireCacheUtils.convertCqInvalidException(cause);
			}

			throw cause;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T findUnique(String query, Object... arguments) throws InvalidDataAccessApiUsageException {

		try {

			QueryService queryService = resolveQueryService(getRegion());

			Query compiledQuery = queryService.newQuery(query);

			Object result = compiledQuery.execute(arguments);

			if (result instanceof SelectResults) {

				SelectResults<T> selectResults = (SelectResults<T>) result;

				List<T> results = selectResults.asList();

				if (results.size() == 1) {
					result = results.get(0);
				}
				else {

					String message = String.format("The result returned from query [%1$s]) was not unique [%2$s]",
						query, result);

					throw new InvalidDataAccessApiUsageException(message);
				}
			}

			return (T) result;
		}
		catch (IndexInvalidException | QueryInvalidException cause) {
			throw convertGemFireQueryException(cause);
		}
		catch (GemFireCheckedException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (RuntimeException cause) {

			if (GemfireCacheUtils.isCqInvalidException(cause)) {
				throw GemfireCacheUtils.convertCqInvalidException(cause);
			}

			throw cause;
		}
	}

	/**
	 * Returns the {@link QueryService} used by this template in its query/finder methods.
	 *
	 * @param region {@link Region} used to acquire the {@link QueryService}.
	 * @return the {@link QueryService} that will perform the {@link Query}.
	 * @see org.apache.geode.cache.Region
	 * @see org.apache.geode.cache.Region#getRegionService()
	 * @see org.apache.geode.cache.RegionService#getQueryService()
	 * @see org.apache.geode.cache.client.ClientCache#getLocalQueryService()
	 */
	protected QueryService resolveQueryService(Region<?, ?> region) {

		return region.getRegionService() instanceof ClientCache
			? resolveClientQueryService(region)
			: queryServiceFrom(region);
	}

	QueryService resolveClientQueryService(Region<?, ?> region) {

		ClientCache clientCache = (ClientCache) region.getRegionService();

		return requiresLocalQueryService(region) ? clientCache.getLocalQueryService()
			: requiresPooledQueryService(region) ? clientCache.getQueryService(poolNameFrom(region))
			: queryServiceFrom(region);
	}

	boolean requiresLocalQueryService(Region<?, ?> region) {
		return Scope.LOCAL.equals(region.getAttributes().getScope()) && isLocalWithNoServerProxy(region);
	}

	boolean isLocalWithNoServerProxy(Region<?, ?> region) {

		if (RegionUtils.isLocal(region)) {

			SpringUtils.ValueReturningThrowableOperation<Boolean> hasServerProxyMethod = () ->
				Optional.ofNullable(ReflectionUtils.findMethod(region.getClass(), "hasServerProxy"))
					.map(method -> ReflectionUtils.invokeMethod(method, region))
					.map(Boolean.FALSE::equals)
					.orElse(false);

			return SpringUtils.safeGetValue(hasServerProxyMethod, false);
		}

		return false;
	}

	boolean requiresPooledQueryService(Region<?, ?> region) {
		return StringUtils.hasText(poolNameFrom(region));
	}

	String poolNameFrom(Region<?, ?> region) {
		return region.getAttributes().getPoolName();
	}

	QueryService queryServiceFrom(Region<?, ?> region) {
		return region.getRegionService().getQueryService();
	}

	/**
	 * Executes the given data access operation defined by the {@link GemfireCallback} in the context of Apache Geode.
	 *
	 * @param <T> {@link Class type} returned by the {@link GemfireCallback}.
	 * @param action {@link GemfireCallback} object defining the Apache Geode action to execute;
	 * must not be {@literal null}.
	 * @return the result of executing the {@link GemfireCallback}.
	 * @throws DataAccessException if an Apache Geode error is thrown by a data access operation.
	 * @throws IllegalArgumentException if {@link GemfireCallback} is {@literal null}.
	 * @see org.springframework.data.gemfire.GemfireCallback
	 * @see #execute(GemfireCallback, boolean)
	 */
	@Override
	public <T> T execute(@NonNull GemfireCallback<T> action) throws DataAccessException {
		return execute(action, isExposeNativeRegion());
	}

	/**
	 * Executes the given data access operation defined by the {@link GemfireCallback} in the context of Apache Geode.
	 *
	 * @param <T> {@link Class type} returned by the {@link GemfireCallback}.
	 * @param action {@link GemfireCallback} object defining the Apache Geode action to execute;
	 * must not be {@literal null}.
	 * @param exposeNativeRegion boolean value indicating whether to pass the native {@link Region}
	 * or the {@link Region} {@literal proxy} to the {@link GemfireCallback}.
	 * @return the result of executing the {@link GemfireCallback}.
	 * @throws DataAccessException if an Apache Geode error is thrown by a data access operation.
	 * @throws IllegalArgumentException if {@link GemfireCallback} is {@literal null}.
	 * @see org.springframework.data.gemfire.GemfireCallback
	 */
	@Override
	public <T> T execute(@NonNull GemfireCallback<T> action, boolean exposeNativeRegion) throws DataAccessException {

		Assert.notNull(action, "GemfireCallback must not be null");

		try {

			Region<?, ?> regionArgument = exposeNativeRegion ? getRegion() : this.regionProxy;

			return action.doInGemfire(regionArgument);
		}
		catch (IndexInvalidException | QueryInvalidException cause) {
			throw convertGemFireQueryException(cause);
		}
		catch (GemFireCheckedException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (GemFireException cause) {
			throw convertGemFireAccessException(cause);
		}
		catch (RuntimeException cause) {

			if (GemfireCacheUtils.isCqInvalidException(cause)) {
				throw GemfireCacheUtils.convertCqInvalidException(cause);
			}

			throw cause;
		}
	}

	/**
	 * Create a close-suppressing proxy for the given Apache Geode cache {@link Region}.
	 *
	 * Called by the <code>execute</code> method.
	 *
	 * @param <K> {@link Class type} of the {@link Region} key.
	 * @param <V> {@link Class type} of the {@link Region} value.
	 * @param region {@link Region} for which a proxy will be created.
	 * @return the Region proxy implementing all interfaces implemented by the passed-in Region object.
	 * @see org.apache.geode.cache.Region#close()
	 * @see #execute(GemfireCallback, boolean)
	 */
	@SuppressWarnings("unchecked")
	@NonNull
	protected <K, V> Region<K, V> createRegionProxy(@NonNull Region<K, V> region) {

		Class<?> regionType = region.getClass();

		return (Region<K, V>) Proxy.newProxyInstance(regionType.getClassLoader(),
			ClassUtils.getAllInterfacesForClass(regionType, getClass().getClassLoader()),
				new RegionCloseSuppressingInvocationHandler(region));
	}

	/**
	 * {@link InvocationHandler} that suppresses the {@link Region#close()} call on a target {@link Region}.
	 *
	 * @see java.lang.reflect.InvocationHandler
	 * @see org.apache.geode.cache.Region#close()
	 */
	private static class RegionCloseSuppressingInvocationHandler implements InvocationHandler {

		private final Region<?, ?> target;

		/**
		 * Constructs a new instance of the {@link RegionCloseSuppressingInvocationHandler} initialized with
		 * the given {@link Region}.
		 *
		 * @param target {@link Region} to proxy; must not be {@literal null}.
		 * @throws IllegalArgumentException if {@link Region} is {@literal null}.
		 * @see org.apache.geode.cache.Region
		 */
		public RegionCloseSuppressingInvocationHandler(@NonNull Region<?, ?> target) {

			Assert.notNull(target, "Target Region must not be null");

			this.target = target;
		}

		/**
		 * @inheritDoc
		 */
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if ("equals".equals(method.getName())) {
				// only consider equals when proxies are identical
				return proxy == args[0];
			}
			else if ("hashCode".equals(method.getName())) {
				// use hashCode of Region proxy
				return System.identityHashCode(proxy);
			}
			else if ("close".equals(method.getName())) {
				// suppress Region.close()
				return null;
			}
			else {
				try {
					return method.invoke(this.target, args);
				}
				catch (InvocationTargetException cause) {
					throw cause.getTargetException();
				}
			}
		}
	}
}
