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
package org.springframework.data.gemfire.client.function;

import java.util.ArrayList;
import java.util.List;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;

import org.springframework.lang.NonNull;

/**
 * {@link ListRegionsOnServerFunction} is an Apache Geode {@link Function}
 * returning a {@link List} of {@link String names} for all {@link Region Regions}
 * defined in the Apache Geode cache.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.apache.geode.cache.execute.Function
 */
public class ListRegionsOnServerFunction implements Function<Object> {

	private static final long serialVersionUID = 867530169L;

	public static final String ID = ListRegionsOnServerFunction.class.getName();

	@Override
	@SuppressWarnings("unchecked")
	public void execute(@NonNull FunctionContext functionContext) {

		List<String> regionNames = new ArrayList<>();

		for (Region<?, ?> region : getCache().rootRegions()) {
			regionNames.add(region.getName());
		}

		functionContext.getResultSender().lastResult(regionNames);
	}

	Cache getCache() {
		return CacheFactory.getAnyInstance();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String getId() {
		return getClass().getName();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean hasResult() {
		return true;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isHA() {
		return false;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean optimizeForWrite() {
		return false;
	}
}
