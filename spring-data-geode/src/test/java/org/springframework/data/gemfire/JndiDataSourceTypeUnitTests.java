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

/**
 * Unit Tests for {@link JndiDataSourceType} enum.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.JndiDataSourceType
 * @since 1.7.0
 */
public class JndiDataSourceTypeUnitTests {

	@Test
	public void testNames() {

		assertThat(JndiDataSourceType.MANAGED.getName()).isEqualTo("ManagedDataSource");
		assertThat(JndiDataSourceType.POOLED.getName()).isEqualTo("PooledDataSource");
		assertThat(JndiDataSourceType.SIMPLE.getName()).isEqualTo("SimpleDataSource");
		assertThat(JndiDataSourceType.XA.getName()).isEqualTo("XAPooledDataSource");
	}

	@Test
	public void testValueOfIgnoreCase() {

		assertThat(JndiDataSourceType.valueOfIgnoreCase("managedDataSource  ")).isEqualTo(JndiDataSourceType.MANAGED);
		assertThat(JndiDataSourceType.valueOfIgnoreCase("   ManAGEd")).isEqualTo(JndiDataSourceType.MANAGED);
		assertThat(JndiDataSourceType.valueOfIgnoreCase("POOLedDataSource")).isEqualTo(JndiDataSourceType.POOLED);
		assertThat(JndiDataSourceType.valueOfIgnoreCase("PoolED ")).isEqualTo(JndiDataSourceType.POOLED);
		assertThat(JndiDataSourceType.valueOfIgnoreCase(" SIMPLEDATASOURCE")).isEqualTo(JndiDataSourceType.SIMPLE);
		assertThat(JndiDataSourceType.valueOfIgnoreCase(" SIMPLE ")).isEqualTo(JndiDataSourceType.SIMPLE);
		assertThat(JndiDataSourceType.valueOfIgnoreCase(" xapooleddatasource  ")).isEqualTo(JndiDataSourceType.XA);
		assertThat(JndiDataSourceType.valueOfIgnoreCase("  xa  ")).isEqualTo(JndiDataSourceType.XA);
	}

	@Test
	public void testValueOfIgnoreCaseWithInvalidNames() {

		assertThat(JndiDataSourceType.valueOfIgnoreCase("ManageDataSource")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("ManagedDataSink")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("ManedDataSrc")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("PoolingDataSource")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("ComplexDataSource")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("SimplifiedDataSource")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("XA Pooled DataSource")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("X A")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("XADATASOURCE")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("XAPOOLED")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("XA POOLED")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("  ")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase("")).isNull();
		assertThat(JndiDataSourceType.valueOfIgnoreCase(null)).isNull();
	}
}
