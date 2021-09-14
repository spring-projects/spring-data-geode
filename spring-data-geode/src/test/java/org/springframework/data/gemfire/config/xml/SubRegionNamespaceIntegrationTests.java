/*
 * Copyright 2010-2021 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link Region} and {@link Region sub-Region} creation in a cache.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings({ "rawtypes", "unused" })
public class SubRegionNamespaceIntegrationTests extends IntegrationTestsSupport {

    @Test
    public void testNestedRegionsCreated() {

        Cache cache = requireApplicationContext().getBean(Cache.class);

        assertThat(cache.getRegion("parent")).isNotNull();
        assertThat(cache.getRegion("/parent/child")).isNotNull();
        assertThat(cache.getRegion("/parent/child/grandchild")).isNotNull();
    }

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedReplicatedRegions() {

        Region parent = requireApplicationContext().getBean("parent", Region.class);
		Region child = requireApplicationContext().getBean("/parent/child", Region.class);
		Region grandchild = requireApplicationContext().getBean("/parent/child/grandchild", Region.class);

		assertThat(child).isNotNull();
		assertThat(child.getName()).isEqualTo("child");
		assertThat(child.getFullPath()).isEqualTo("/parent/child");
		assertThat(parent.getSubregion("child")).isSameAs(child);
		assertThat(grandchild).isNotNull();
		assertThat(grandchild.getName()).isEqualTo("grandchild");
		assertThat(grandchild.getFullPath()).isEqualTo("/parent/child/grandchild");
		assertThat(child.getSubregion("grandchild")).isSameAs(grandchild);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMixedNestedRegions() {

		Region parent = requireApplicationContext().getBean("replicatedParent", Region.class);
		Region child = requireApplicationContext().getBean("/replicatedParent/replicatedChild", Region.class);
		Region grandchild = requireApplicationContext().getBean("/replicatedParent/replicatedChild/partitionedGrandchild", Region.class);

		assertThat(child).isNotNull();
		assertThat(child.getFullPath()).isEqualTo("/replicatedParent/replicatedChild");
		assertThat(parent.getSubregion("replicatedChild")).isEqualTo(child);
		assertThat(grandchild).isNotNull();
		assertThat(grandchild.getFullPath()).isEqualTo("/replicatedParent/replicatedChild/partitionedGrandchild");
		assertThat(child.getSubregion("partitionedGrandchild")).isSameAs(grandchild);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedRegionsWithSiblings() {

		Region parent = requireApplicationContext().getBean("parentWithSiblings", Region.class);
		Region child1 = requireApplicationContext().getBean("/parentWithSiblings/child1", Region.class);

		assertThat(child1.getFullPath()).isEqualTo("/parentWithSiblings/child1");

		Region child2 = requireApplicationContext().getBean("/parentWithSiblings/child2", Region.class);

		assertThat(child2.getFullPath()).isEqualTo("/parentWithSiblings/child2");
		assertThat(parent.getSubregion("child1")).isSameAs(child1);
		assertThat(parent.getSubregion("child2")).isSameAs(child2);

		Region grandchild1 = requireApplicationContext().getBean("/parentWithSiblings/child1/grandChild11", Region.class);

		assertThat(grandchild1.getFullPath()).isEqualTo("/parentWithSiblings/child1/grandChild11");
	}

	@Test
	@SuppressWarnings("unused" )
	public void testComplexNestedRegions() throws Exception {

		Region parent = requireApplicationContext().getBean("complexNested", Region.class);
		Region child1 = requireApplicationContext().getBean("/complexNested/child1", Region.class);
		Region child2 = requireApplicationContext().getBean("/complexNested/child2", Region.class);
		Region grandchild11 = requireApplicationContext().getBean("/complexNested/child1/grandChild11", Region.class);

		ReplicatedRegionFactoryBean grandchild11FactoryBean =
			requireApplicationContext().getBean("&/complexNested/child1/grandChild11",
				ReplicatedRegionFactoryBean.class);

		assertThat(grandchild11FactoryBean).isNotNull();

		CacheLoader expectedCacheLoader = TestUtils.readField("cacheLoader", grandchild11FactoryBean);

		assertThat(expectedCacheLoader).isNotNull();

		CacheLoader actualCacheLoader = grandchild11.getAttributes().getCacheLoader();

		assertThat(actualCacheLoader).isSameAs(expectedCacheLoader);
	}
}
