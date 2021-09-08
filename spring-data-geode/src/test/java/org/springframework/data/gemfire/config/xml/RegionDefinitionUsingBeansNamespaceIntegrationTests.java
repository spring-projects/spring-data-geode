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

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.PartitionAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the contract and functionality of the SDG {@link PeerRegionFactoryBean} class,
 * and specifically the specification of the Apache Geode {@link Region} {@link DataPolicy} when used as
 * raw bean definition in Spring XML configuration metadata.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.DataPolicy
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.PeerRegionFactoryBean
 * @see org.springframework.data.gemfire.PartitionAttributesFactoryBean
 * @see org.springframework.data.gemfire.RegionAttributesFactoryBean
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class RegionDefinitionUsingBeansNamespaceIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Resource(name = "Example")
	private Region<?, ?> example;

	@Resource(name = "AnotherExample")
	private Region<?, ?> anotherExample;

	@Test
	public void testExampleRegionBeanDefinitionConfiguration() {

		assertThat(example).as("The '/Example' Region was not properly configured and initialized!").isNotNull();
		assertThat(example.getName()).isEqualTo("Example");
		assertThat(example.getFullPath()).isEqualTo("/Example");
		assertThat(example.getAttributes()).isNotNull();
		assertThat(example.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(example.getAttributes().getStatisticsEnabled()).isTrue();
		assertThat(example.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(example.getAttributes().getPartitionAttributes().getRedundantCopies()).isEqualTo(1);
		assertThat(example.getAttributes().getPartitionAttributes().getRecoveryDelay()).isEqualTo(0);
	}

	@Test
	public void testAnotherExampleRegionFactoryBeanConfiguration() throws Exception {

		PeerRegionFactoryBean<?, ?> anotherExampleRegionFactoryBean =
			applicationContext.getBean("&AnotherExample", PeerRegionFactoryBean.class);

		assertThat(anotherExampleRegionFactoryBean).isNotNull();
		assertThat(anotherExampleRegionFactoryBean.getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(Boolean.TRUE.equals(TestUtils.readField("persistent", anotherExampleRegionFactoryBean))).isTrue();

		RegionAttributes<?, ?> anotherExampleRegionAttributes =
			TestUtils.readField("attributes", anotherExampleRegionFactoryBean);

		assertThat(anotherExampleRegionAttributes).isNotNull();
		assertThat(anotherExampleRegionAttributes.getDataPolicy()).isEqualTo(DataPolicy.PARTITION);

		PartitionAttributes<?, ?> anotherExamplePartitionAttributes = anotherExampleRegionAttributes.getPartitionAttributes();

		assertThat(anotherExamplePartitionAttributes).isNotNull();
		assertThat(anotherExamplePartitionAttributes.getRedundantCopies()).isEqualTo(2);
	}

	@Test
	public void testAnotherExampleRegionDefinitionConfiguration() {

		assertThat(anotherExample).as("The '/AnotherExample' Region was not properly configured and initialized!")
			.isNotNull();
		assertThat(anotherExample.getName()).isEqualTo("AnotherExample");
		assertThat(anotherExample.getFullPath()).isEqualTo("/AnotherExample");
		assertThat(anotherExample.getAttributes()).isNotNull();
		assertThat(anotherExample.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PERSISTENT_PARTITION);
		assertThat(anotherExample.getAttributes().getPartitionAttributes()).isNotNull();
		assertThat(anotherExample.getAttributes().getPartitionAttributes().getRedundantCopies()).isEqualTo(2);
	}

	public static final class TestRegionFactoryBean<K, V> extends PeerRegionFactoryBean<K, V> { }

}
