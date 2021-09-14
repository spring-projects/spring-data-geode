/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.IndexFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings({ "deprecation", "unused" })
public class IndexNamespaceIntegrationTests extends IntegrationTestsSupport {

	private static final String TEST_REGION_NAME = "IndexedRegion";

	@Autowired
	private ApplicationContext applicationContext;

	@Resource(name = "basic")
	private Index basic;

	@Resource(name = "complex")
	private Index complex;

	@Test
	public void basicIndexIsCorrect() {

		assertThat(basic.getName()).isEqualTo("basic");
		assertThat(basic.getIndexedExpression()).isEqualTo("status");
		assertThat(basic.getFromClause()).isEqualTo(Region.SEPARATOR + TEST_REGION_NAME);
		assertThat(basic.getRegion().getName()).isEqualTo(TEST_REGION_NAME);
		assertThat(basic.getType()).isEqualTo(org.apache.geode.cache.query.IndexType.FUNCTIONAL);
	}

	@Test
	public void basicIndexFactoryBeanIsCorrect() {

		IndexFactoryBean basicIndexFactoryBean = applicationContext.getBean("&basic", IndexFactoryBean.class);

		assertThat(basicIndexFactoryBean.isIgnoreIfExists()).isFalse();
		assertThat(basicIndexFactoryBean.isOverride()).isFalse();
	}

	@Test
	public void complexIndexIsCorrect() {

		assertThat(complex.getName()).isEqualTo("complex-index");
		assertThat(complex.getIndexedExpression()).isEqualTo("tsi.name");
		assertThat(complex.getFromClause()).isEqualTo(Region.SEPARATOR + TEST_REGION_NAME + " tsi");
		assertThat(complex.getRegion()).isNotNull();
		assertThat(complex.getRegion().getName()).isEqualTo(TEST_REGION_NAME);
		assertThat(complex.getType()).isEqualTo(org.apache.geode.cache.query.IndexType.HASH);
	}

	@Test
	public void indexWithIgnoreAndOverrideIsCorrect() {

		IndexFactoryBean indexFactoryBean =
			applicationContext.getBean("&index-with-ignore-and-override", IndexFactoryBean.class);

		assertThat(indexFactoryBean.isIgnoreIfExists()).isTrue();
		assertThat(indexFactoryBean.isOverride()).isTrue();
	}
}
