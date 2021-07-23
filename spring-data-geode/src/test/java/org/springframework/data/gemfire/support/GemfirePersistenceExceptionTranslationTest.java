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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.QueryException;
import org.apache.geode.cache.query.QueryInvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.GemfireQueryException;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class GemfirePersistenceExceptionTranslationTest extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private GemFireRepo1 gemfireRepo1;

	@Test
	public void test() {

		applicationContext.getBeansOfType(BeanPostProcessor.class);

		try {
			gemfireRepo1.doit(new QueryException());
			fail("should throw a query exception");
		}
		catch (GemfireQueryException ignore){ }

		try {
			gemfireRepo1.doit(new FunctionDomainException("test"));
			fail("should throw a query exception");
		}
		catch (GemfireQueryException ignore) { }

		try {
			gemfireRepo1.doit(new QueryInvocationTargetException("test"));
			fail("should throw a query exception");
		}
		catch (GemfireQueryException ignore) { }
	}

	/**
	 * Wraps GemfireCheckedExceptions in RuntimeException
	 */
	@Repository
	public static class GemFireRepo1 {
		public void doit(Exception e)  {
			throw new RuntimeException(e);
		}
	}
}
