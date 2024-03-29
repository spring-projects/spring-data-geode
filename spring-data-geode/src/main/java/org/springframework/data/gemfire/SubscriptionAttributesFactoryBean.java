/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire;

import org.apache.geode.cache.InterestPolicy;
import org.apache.geode.cache.SubscriptionAttributes;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} used for defining and constructing an Apache Geode {@link SubscriptionAttributes} object,
 * which determines the subscription policy used by cache Regions declaring their data interests.
 *
 * @author Lyndon Adams
 * @author John Blum
 * @see org.apache.geode.cache.InterestPolicy
 * @see org.apache.geode.cache.SubscriptionAttributes
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @since 1.3.0
 */
public class SubscriptionAttributesFactoryBean implements FactoryBean<SubscriptionAttributes>, InitializingBean {

	private InterestPolicy interestPolicy;

	private SubscriptionAttributes subscriptionAttributes;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.subscriptionAttributes = new SubscriptionAttributes(getInterestPolicy());
	}

	@Override
	public SubscriptionAttributes getObject() throws Exception {
		return this.subscriptionAttributes;
	}

	@Override
	public Class<?> getObjectType() {

		return this.subscriptionAttributes != null
			? this.subscriptionAttributes.getClass()
			: SubscriptionAttributes.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Sets GemFire's InterestPolicy specified on the SubscriptionAttributes in order to define/declare
	 * the data interests and distribution of changes.
	 *
	 * @param interestPolicy the GemFire InterestsPolicy to set for Subscription.
	 * @see org.apache.geode.cache.InterestPolicy
	 * @see org.apache.geode.cache.SubscriptionAttributes#SubscriptionAttributes(org.apache.geode.cache.InterestPolicy)
	 */
	public void setInterestPolicy(InterestPolicy interestPolicy) {
		this.interestPolicy = interestPolicy;
	}

	/**
	 * Gets GemFire's InterestPolicy specified on the SubscriptionAttributes which defines data interests
	 * and distribution of changes.
	 *
	 * @return the GemFire InterestsPolicy set for Subscription.
	 * @see org.apache.geode.cache.InterestPolicy
	 * @see org.apache.geode.cache.SubscriptionAttributes#getInterestPolicy()
	 */
	public InterestPolicy getInterestPolicy() {
		return this.interestPolicy != null ? this.interestPolicy : InterestPolicy.DEFAULT;
	}
}
