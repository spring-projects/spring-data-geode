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
package org.springframework.data.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;

/**
 * Integration Tests testing the incorrect order of Template {@link Region} bean definitions
 * and regular {@link Region} bean definitions referring to the templates.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.5.0
 */
public class TemplateRegionDefinitionOrderErrorNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Test(expected = BeanDefinitionParsingException.class)
	public void incorrectTemplateRegionBeanDefinitionOrderThrowsParseException() {

		try {
			new ClassPathXmlApplicationContext(getContextXmlFileLocation(
				TemplateRegionDefinitionOrderErrorNamespaceIntegrationTests.class));
		}
		catch (BeanDefinitionParsingException expected) {

			assertThat(expected).hasMessageContaining("The Region template [RegionTemplate] must be defined before"
				+ " the Region [TemplateBasedPartitionRegion] referring to the template");

			assertThat(expected).hasNoCause();

			throw expected;
		}
	}
}
