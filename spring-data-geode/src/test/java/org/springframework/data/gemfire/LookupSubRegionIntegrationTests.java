/*
 * Copyright 2010-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the contract and functionality of Region lookups using SDG XML namespace configuration
 * metadata and Apache Geode native cache.xml.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("lookupSubRegion.xml")
@SuppressWarnings("unused")
public class LookupSubRegionIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	private void assertRegionExists(String expectedRegionName, String expectedRegionPath, Region<?, ?> region) {

		assertThat(region)
			.describedAs("The Region with name (%1$s) at path (%2$s) was null!",expectedRegionName, expectedRegionPath)
			.isNotNull();

		assertThat(region.getName())
			.describedAs("Expected Region name of %1$s; but was %2$s!", expectedRegionName, region.getName())
			.isEqualTo(expectedRegionName);

		assertThat(region.getFullPath())
			.describedAs("Expected Region path of %1$s; but was %2$s!", expectedRegionPath, region.getFullPath())
			.isEqualTo(expectedRegionPath);
	}

	@Test
	public void testDirectLookup() {

		Region<?, ?> accounts = applicationContext.getBean("/Customers/Accounts", Region.class);

		assertRegionExists("Accounts", "/Customers/Accounts", accounts);
		assertThat(applicationContext.containsBean("Customers/Accounts")).isFalse();
		assertThat(applicationContext.containsBean("/Customers")).isFalse();
		assertThat(applicationContext.containsBean("Customers")).isFalse();

		Region<?, ?> items = applicationContext.getBean("Customers/Accounts/Orders/Items", Region.class);

		assertRegionExists("Items", "/Customers/Accounts/Orders/Items", items);
		assertThat(applicationContext.containsBean("/Customers/Accounts/Orders/Items")).isFalse();
		assertThat(applicationContext.containsBean("/Customers/Accounts/Orders")).isFalse();
		assertThat(applicationContext.containsBean("Customers/Accounts/Orders")).isFalse();
	}

	@Test
	public void testNestedLookup() {

		Region<?, ?> parent = applicationContext.getBean("Parent", Region.class);

		assertRegionExists("Parent", "/Parent", parent);
		assertThat(applicationContext.containsBean("/Parent")).isFalse();

		Region<?, ?> child = applicationContext.getBean("/Parent/Child", Region.class);

		assertRegionExists("Child", "/Parent/Child", child);
		assertThat(applicationContext.containsBean("Parent/Child")).isFalse();

		Region<?, ?> grandchild = applicationContext.getBean("/Parent/Child/Grandchild", Region.class);

		assertRegionExists("Grandchild", "/Parent/Child/Grandchild", grandchild);
		assertThat(applicationContext.containsBean("Parent/Child/Grandchild")).isFalse();
	}
}
