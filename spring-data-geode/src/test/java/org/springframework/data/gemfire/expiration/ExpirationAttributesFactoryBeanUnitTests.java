/*
 * Copyright 2016-2023 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.expiration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;

/**
 * Unit Tests for {@link ExpirationAttributesFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.ExpirationAttributes
 * @see org.springframework.data.gemfire.expiration.ExpirationAttributesFactoryBean
 * @since 1.6.0
 */
public class ExpirationAttributesFactoryBeanUnitTests {

	@Test
	public void testIsSingleton() {
		assertThat(new ExpirationAttributesFactoryBean().isSingleton()).isTrue();
	}

	@Test
	public void testSetAndGetAction() {

		ExpirationAttributesFactoryBean expirationAttributesFactoryBean = new ExpirationAttributesFactoryBean();

		assertThat(expirationAttributesFactoryBean.getAction())
			.isEqualTo(ExpirationAttributesFactoryBean.DEFAULT_EXPIRATION_ACTION);

		expirationAttributesFactoryBean.setAction(ExpirationAction.LOCAL_DESTROY);

		assertThat(expirationAttributesFactoryBean.getAction()).isEqualTo(ExpirationAction.LOCAL_DESTROY);

		expirationAttributesFactoryBean.setAction(null);

		assertThat(expirationAttributesFactoryBean.getAction())
			.isEqualTo(ExpirationAttributesFactoryBean.DEFAULT_EXPIRATION_ACTION);
	}

	@Test
	public void testSetAndGetTimeout() {

		ExpirationAttributesFactoryBean expirationAttributesFactoryBean = new ExpirationAttributesFactoryBean();

		assertThat(expirationAttributesFactoryBean.getTimeout()).isEqualTo(0);

		expirationAttributesFactoryBean.setTimeout(60000);

		assertThat(expirationAttributesFactoryBean.getTimeout()).isEqualTo(60000);

		expirationAttributesFactoryBean.setTimeout(null);

		assertThat(expirationAttributesFactoryBean.getTimeout()).isEqualTo(0);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {

		ExpirationAttributesFactoryBean expirationAttributesFactoryBean = new ExpirationAttributesFactoryBean();

		assertThat(expirationAttributesFactoryBean.getObject()).isNull();
		assertThat(expirationAttributesFactoryBean.getObjectType()).isEqualTo(ExpirationAttributes.class);

		expirationAttributesFactoryBean.setAction(ExpirationAction.DESTROY);
		expirationAttributesFactoryBean.setTimeout(8192);
		expirationAttributesFactoryBean.afterPropertiesSet();

		ExpirationAttributes expirationAttributes = expirationAttributesFactoryBean.getObject();

		assertThat(expirationAttributes).isNotNull();
		assertThat(expirationAttributes.getAction()).isEqualTo(ExpirationAction.DESTROY);
		assertThat(expirationAttributes.getTimeout()).isEqualTo(8192);
		assertThat(expirationAttributesFactoryBean.getObjectType()).isEqualTo(expirationAttributes.getClass());
	}
}
