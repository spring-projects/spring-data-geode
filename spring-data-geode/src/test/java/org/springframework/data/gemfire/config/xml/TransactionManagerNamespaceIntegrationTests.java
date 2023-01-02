/*
 * Copyright 2010-2023 the original author or authors.
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

import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.data.gemfire.transaction.GemfireTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for SDG Transaction Manager configuration in SDG XML namespace configuration metadata.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.transaction.GemfireTransactionManager
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class TransactionManagerNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void basicCacheWithTransactionsIsConfiguredCorrectly() {

		assertThat(requireApplicationContext().containsBean("gemfireTransactionManager")).isTrue();
		assertThat(requireApplicationContext().containsBean("gemfire-transaction-manager")).isTrue();

		GemfireTransactionManager transactionManager =
			requireApplicationContext().getBean("gemfireTransactionManager", GemfireTransactionManager.class);

		assertThat(transactionManager.isCopyOnRead()).isFalse();
	}
}
