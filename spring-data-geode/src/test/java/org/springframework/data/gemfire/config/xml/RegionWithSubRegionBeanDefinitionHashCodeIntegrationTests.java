/*
 * Copyright 2010-2023 the original author or authors.
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
import org.junit.runner.RunWith;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the hashCode() method of a Spring container BeanDefinition representing a Region
 * having SubRegions. This test suite is meant to ensure the correct behavior of and provide regression coverage for,
 * JIRA issue SGF-178, "parent attribute causes endless loop in hashCode".
 *
 * The 'parent' attribute was added to the parent Region's BeanDefinition, referring to the parent Region's
 * BeanDefinition itself, before it recurses to parse the SubRegion elements, which is then used by the
 * AbstractRegionParser.doParseInternal method to set the parent property of the SubRegion's BeanDefinition.
 *
 * Calling hashCode on a parent Region's BeanDefinition that has a parent 'attribute' referring to the parent Region's
 * BeanDefinition itself, causes infinite recursion and an eventual StackOverflowError.
 *
 * <code>
 * java.lang.StackOverflowError
 *   at java.util.LinkedHashMap$LinkedHashIterator.<init>(LinkedHashMap.java:345)
 *   at java.util.LinkedHashMap$LinkedHashIterator.<init>(LinkedHashMap.java:345)
 *   at java.util.LinkedHashMap$EntryIterator.<init>(LinkedHashMap.java:391)
 *   at java.util.LinkedHashMap$EntryIterator.<init>(LinkedHashMap.java:391)
 *   at java.util.LinkedHashMap.newEntryIterator(LinkedHashMap.java:398)
 *   at java.util.HashMap$EntrySet.iterator(HashMap.java:950)
 *   at org.springframework.beans.factory.config.ConstructorArgumentValues.hashCode(ConstructorArgumentValues.java:416)
 *   at org.springframework.util.ObjectUtils.nullSafeHashCode(ObjectUtils.java:336)
 *   at org.springframework.beans.factory.support.AbstractBeanDefinition.hashCode(AbstractBeanDefinition.java:1048)
 *   at org.springframework.beans.factory.config.BeanDefinitionHolder.hashCode(BeanDefinitionHolder.java:179)
 *   at org.springframework.util.ObjectUtils.nullSafeHashCode(ObjectUtils.java:336)
 *   at org.springframework.beans.BeanMetadataAttribute.hashCode(BeanMetadataAttribute.java:93)
 *   at java.util.HashMap$Entry.hashCode(HashMap.java:720)
 *   at java.util.AbstractMap.hashCode(AbstractMap.java:461)
 *   at org.springframework.core.AttributeAccessorSupport.hashCode(AttributeAccessorSupport.java:99)
 *   at org.springframework.beans.factory.support.AbstractBeanDefinition.hashCode(AbstractBeanDefinition.java:1052)
 *   at org.springframework.beans.factory.config.BeanDefinitionHolder.hashCode(BeanDefinitionHolder.java:179)
 *   at org.springframework.util.ObjectUtils.nullSafeHashCode(ObjectUtils.java:336)
 *   at org.springframework.beans.BeanMetadataAttribute.hashCode(BeanMetadataAttribute.java:93)
 *   at java.util.HashMap$Entry.hashCode(HashMap.java:720)
 *   at java.util.AbstractMap.hashCode(AbstractMap.java:461)
 *   ...
 *   at org.springframework.core.AttributeAccessorSupport.hashCode(AttributeAccessorSupport.java:99)
 *   at org.springframework.beans.factory.support.AbstractBeanDefinition.hashCode(AbstractBeanDefinition.java:1052)
 * </code>
 *
 * This also causes problems for tools like Spring Tool Suite, which use the BeanDefinitions from the Spring container
 * context as meta-data in the IDE.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @link https://jira.springsource.org/browse/SGF-178
 * @since 1.3.3
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings("unused")
public class RegionWithSubRegionBeanDefinitionHashCodeIntegrationTests extends IntegrationTestsSupport {

	@Test
	public void testNonParentRegionBeanDefinitionHashCode() {

		BeanDefinition nonParentRegionBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("NON_PARENT");

		assertThat(nonParentRegionBeanDefinition).isNotNull();
		assertThat(nonParentRegionBeanDefinition.hashCode() != 0).isTrue();
	}

	@Test
	public void testParentRegionBeanDefinitionHashCode() {

		BeanDefinition parentRegionBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("PARENT");

		assertThat(parentRegionBeanDefinition).isNotNull();
		assertThat(parentRegionBeanDefinition.hashCode() != 0).isTrue();
	}

	@Test
	public void testChildRegionBeanDefinitionHashCode() {

		BeanDefinition childRegionBeanDefinition =
			requireApplicationContext().getBeanFactory().getBeanDefinition("/PARENT/CHILD");

		assertThat(childRegionBeanDefinition).isNotNull();
		assertThat(childRegionBeanDefinition.hashCode() != 0).isTrue();
	}
}
