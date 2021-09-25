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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link MembershipAttributes}.
 *
 * @author David Turanski
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.MembershipAttributes
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.data.gemfire.tests.unit.annotation.GemFireUnitTest
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@GemFireUnitTest
@SuppressWarnings({ "deprecation", "unused" })
public class MembershipAttributesIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("secure")
	private Region<?, ?> secure;

	@Autowired
	@Qualifier("simple")
	private Region<?, ?> simple;

	@Test
	public void secureRegionMembershipAttributesConfigurationIsCorrect() {

		MembershipAttributes membershipAttributes = secure.getAttributes().getMembershipAttributes();

		assertThat(membershipAttributes).isNotNull();
		assertThat(membershipAttributes.getLossAction()).isEqualTo(LossAction.LIMITED_ACCESS);
		assertThat(membershipAttributes.hasRequiredRoles()).isTrue();
		assertThat(membershipAttributes.getRequiredRoles().stream().map(Role::getName))
			.containsExactlyInAnyOrder("ROLE1", "ROLE2");
		assertThat(membershipAttributes.getResumptionAction()).isEqualTo(ResumptionAction.REINITIALIZE);
	}

	@Test
	public void simpleRegionMembershipAttributesConfigurationIsCorrect() {

		MembershipAttributes membershipAttributes = simple.getAttributes().getMembershipAttributes();

		assertThat(membershipAttributes).isNotNull();
		assertThat(membershipAttributes.getLossAction()).isEqualTo(LossAction.FULL_ACCESS);
		assertThat(membershipAttributes.hasRequiredRoles()).isFalse();
		assertThat(membershipAttributes.getRequiredRoles()).isEmpty();
		assertThat(membershipAttributes.getResumptionAction()).isEqualTo(ResumptionAction.NONE);
	}
}
