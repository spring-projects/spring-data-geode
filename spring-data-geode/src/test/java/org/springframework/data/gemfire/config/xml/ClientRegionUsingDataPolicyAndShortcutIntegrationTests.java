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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;

/**
 * Integration Tests for client {@link Region} bean definition with both {@literal data-policy}(i.e. {@link DataPolicy})
 * and {@literal shortcut} {@link ClientRegionShortcut} attributes specified.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DataPolicy
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.client.ClientRegionShortcut
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 * @see org.springframework.data.gemfire.client.ClientCacheFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @since 1.3.3
 */
public class ClientRegionUsingDataPolicyAndShortcutIntegrationTests extends IntegrationTestsSupport {

	@Test(expected = BeanDefinitionParsingException.class)
	public void testClientRegionBeanDefinitionWithDataPolicyAndShortcut() {

		try {
			new ClassPathXmlApplicationContext(getContextXmlFileLocation(ClientRegionUsingDataPolicyAndShortcutIntegrationTests.class));
		}
		catch (BeanDefinitionParsingException expected) {

			assertThat(expected).hasMessageContaining("Only one of [data-policy, shortcut] may be specified with element");

			throw expected;
		}
	}
}
