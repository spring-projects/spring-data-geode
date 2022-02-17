/*
 * Copyright 2010-2022 the original author or authors.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import org.apache.geode.cache.AttributesFactory;
import org.apache.geode.cache.RegionFactory;

/**
 * Unit Tests for {@link IndexMaintenancePolicyType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.IndexMaintenancePolicyType
 * @since 1.6.0
 */
public class IndexMaintenancePolicyTypeTest {

	@Test
	public void testDefault() {
		assertThat(IndexMaintenancePolicyType.DEFAULT).isSameAs(IndexMaintenancePolicyType.SYNCHRONOUS);
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("SYNCHRONOUS"))
			.isEqualTo(IndexMaintenancePolicyType.SYNCHRONOUS);
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("Synchronous"))
			.isEqualTo(IndexMaintenancePolicyType.SYNCHRONOUS);
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("synchronous"))
			.isEqualTo(IndexMaintenancePolicyType.SYNCHRONOUS);
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("SynCHrOnOus"))
			.isEqualTo(IndexMaintenancePolicyType.SYNCHRONOUS);
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("ASYNChronous"))
			.isEqualTo(IndexMaintenancePolicyType.ASYNCHRONOUS);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidValues() {

		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("synchronicity")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("SYNC")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("ASYNC")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("Concurrent")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("parallel")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("  ")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase("")).isNull();
		assertThat(IndexMaintenancePolicyType.valueOfIgnoreCase(null)).isNull();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testAttributesFactorySetIndexMaintenanceAsynchronous() {

		AttributesFactory<?, ?> mockAttributesFactory = mock(AttributesFactory.class,
			"testAttributesFactorySetIndexMaintenanceAsynchronous");

		IndexMaintenancePolicyType.ASYNCHRONOUS.setIndexMaintenance(mockAttributesFactory);

		verify(mockAttributesFactory).setIndexMaintenanceSynchronous(eq(false));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testAttributesFactorySetIndexMaintenanceSynchronous() {

		AttributesFactory<?, ?> mockAttributesFactory =
			mock(AttributesFactory.class, "testAttributesFactorySetIndexMaintenanceAsynchronous");

		IndexMaintenancePolicyType.SYNCHRONOUS.setIndexMaintenance(mockAttributesFactory);

		verify(mockAttributesFactory).setIndexMaintenanceSynchronous(eq(true));
	}

	@Test
	public void testRegionFactorySetIndexMaintenanceAsynchronous() {

		RegionFactory<?, ?> mockRegionFactory =
			mock(RegionFactory.class, "testRegionFactorySetIndexMaintenanceAsynchronous");

		IndexMaintenancePolicyType.ASYNCHRONOUS.setIndexMaintenance(mockRegionFactory);

		verify(mockRegionFactory).setIndexMaintenanceSynchronous(eq(false));
	}

	@Test
	public void testRegionFactorySetIndexMaintenanceSynchronous() {

		RegionFactory<?, ?> mockRegionFactory =
			mock(RegionFactory.class, "testRegionFactorySetIndexMaintenanceSynchronous");

		IndexMaintenancePolicyType.SYNCHRONOUS.setIndexMaintenance(mockRegionFactory);

		verify(mockRegionFactory).setIndexMaintenanceSynchronous(eq(true));
	}

}
