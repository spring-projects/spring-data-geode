/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.gemfire.function.execution;

import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.util.Optional;

import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;

import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.util.CacheUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for Apache Geode client-side {@link Function} {@link Execution}.
 *
 * @author John Blum
 * @see org.apache.geode.cache.RegionService
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.execute.Execution
 * @see org.apache.geode.cache.execute.Function
 * @since 2.3.0
 */
@SuppressWarnings("unused")
public abstract class AbstractClientFunctionTemplate extends AbstractFunctionTemplate {

	protected static final PoolResolver DEFAULT_POOL_RESOLVER = new PoolManagerPoolResolver();

	private Pool pool;

	private PoolResolver poolResolver = DEFAULT_POOL_RESOLVER;

	private RegionService regionService;

	private String poolName;

	public AbstractClientFunctionTemplate(RegionService regionService) {
		this.regionService = regionService;
	}

	public AbstractClientFunctionTemplate(Pool pool) {
		this.pool = pool;
	}

	public AbstractClientFunctionTemplate(String poolName) {
		this.poolName = poolName;
	}

	public void setPool(Pool pool) {
		this.pool = pool;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public void setPoolResolver(PoolResolver poolResolver) {
		this.poolResolver = poolResolver;
	}

	protected PoolResolver getPoolResolver() {

		PoolResolver poolResolver = this.poolResolver;

		return poolResolver != null ? poolResolver : DEFAULT_POOL_RESOLVER;
	}

	protected Object resolveRequiredGemFireObject() {
		return Optional.<Object>ofNullable(resolvePool()).orElseGet(this::resolveClientCache);
	}

	/**
	 * @deprecated as of 2.3.0; Use {@link #resolveRegionService()}.
	 */
	@Deprecated
	protected ClientCache resolveClientCache() {
		return (ClientCache) resolveRegionService();
	}

	protected Pool resolvePool() {

		if (this.pool == null) {
			this.pool = resolveNamedPool();
		}

		return this.pool;
	}

	protected Pool resolveDefaultPool() {

		return Optional.ofNullable(getPoolResolver().resolve(GemfireUtils.DEFAULT_POOL_NAME))
			.orElseThrow(() -> newIllegalStateException("DEFAULT Pool is not present"));
	}

	protected Pool resolveNamedPool() {

		if (StringUtils.hasText(this.poolName)) {
			this.pool = Optional.ofNullable(getPoolResolver().resolve(this.poolName))
				.orElseThrow(() -> newIllegalStateException("Pool with name [%s] is not present",
					this.poolName));
		}

		return this.pool;
	}

	protected RegionService resolveRegionService() {

		RegionService resolvedRegionService = this.regionService != null
			? this.regionService
			: CacheUtils.getClientCache();

		return Optional.ofNullable(resolvedRegionService)
			.orElseThrow(() -> newIllegalStateException("ClientCache is not present"));
	}

	@Override
	protected AbstractFunctionExecution getFunctionExecution() {

		Object gemfireObject = resolveRequiredGemFireObject();

		return gemfireObject instanceof Pool
			? newFunctionExecutionUsingPool((Pool) gemfireObject)
			: newFunctionExecutionUsingRegionService((RegionService) gemfireObject);
	}

	protected abstract AbstractFunctionExecution newFunctionExecutionUsingPool(Pool pool);

	protected abstract AbstractFunctionExecution newFunctionExecutionUsingRegionService(RegionService regionService);

}
