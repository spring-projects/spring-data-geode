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

import org.apache.geode.cache.LossAction;
import org.apache.geode.cache.MembershipAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.ResumptionAction;
import org.apache.geode.distributed.Role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.context.GemFireMockObjectsApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Turanski
 * @author John Blum
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/data/gemfire/config/xml/membership-attributes-ns.xml",
	initializers = GemFireMockObjectsApplicationContextInitializer.class)
@SuppressWarnings("unused")
public class MembershipAttributesIntegrationTests extends IntegrationTestsSupport {

    @Autowired
	private ApplicationContext applicationContext;

	@Test
	public void membershipAttributesConfigurationIsCorrect() {

		Region<?, ?> simple = applicationContext.getBean("simple", Region.class);

		MembershipAttributes membershipAttributes = simple.getAttributes().getMembershipAttributes();

		assertThat(membershipAttributes.hasRequiredRoles()).isFalse();

		Region<?, ?> secure = applicationContext.getBean("secure", Region.class);

		membershipAttributes = secure.getAttributes().getMembershipAttributes();

		assertThat(membershipAttributes.hasRequiredRoles()).isTrue();
		assertThat(membershipAttributes.getResumptionAction()).isEqualTo(ResumptionAction.REINITIALIZE);
		assertThat(membershipAttributes.getLossAction()).isEqualTo(LossAction.LIMITED_ACCESS);

		for (Role role : membershipAttributes.getRequiredRoles()) {
			assertThat("ROLE1".equals(role.getName()) || "ROLE2".equals(role.getName())).isTrue();
		}
	}
}
