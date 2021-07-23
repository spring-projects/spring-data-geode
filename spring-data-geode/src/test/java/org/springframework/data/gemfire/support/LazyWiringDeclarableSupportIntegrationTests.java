/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.gemfire.support.WiringDeclarableSupport.TEMPLATE_BEAN_NAME_PROPERTY;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.gemfire.repository.sample.User;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.support.DataSourceAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * Integration Tests for {@link LazyWiringDeclarableSupport}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.3.4
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class LazyWiringDeclarableSupportIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	private static Properties createParameters(String parameter, String value) {
		Properties parameters = new Properties();
		parameters.setProperty(parameter, value);
		return parameters;
	}

	@Test
	public void autoWiringSuccessful() {

		TestDeclarable declarable = new TestDeclarable();

		declarable.init(createParameters("testParam", "testValue"));
		declarable.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
		declarable.assertInitialized();

		assertThat(declarable.getDataSource()).isNull();
		assertThat(declarable.getUser()).isNotNull();
		assertThat(declarable.getUser().getUsername()).isEqualTo("supertool");
	}

	@Test
	public void autoWiringWithBeanTemplateSuccessful() {

		TestDeclarable declarable = new TestDeclarable();

		declarable.init(createParameters(TEMPLATE_BEAN_NAME_PROPERTY, "declarableTemplateBean"));
		declarable.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
		declarable.assertInitialized();

		assertThat(declarable.getDataSource()).isNotNull();
		assertThat(declarable.getUser()).isNotNull();
		assertThat(declarable.getUser().getUsername()).isEqualTo("supertool");
	}

	@Test(expected = IllegalArgumentException.class)
	public void autoWiringWithNonExistingBeanTemplateThrowsIllegalArgumentException() {

		try {
			TestDeclarable declarable = new TestDeclarable();

			declarable.init(createParameters(TEMPLATE_BEAN_NAME_PROPERTY, "nonExistingBeanTemplate"));
			declarable.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessageStartingWith("Cannot find bean with name [nonExistingBeanTemplate]");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	protected static final class TestDataSource extends DataSourceAdapter { }

	protected static final class TestDeclarable extends LazyWiringDeclarableSupport {

		private DataSource dataSource;

		@Autowired
		private User user;

		public final void setDataSource(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		DataSource getDataSource() {
			return dataSource;
		}

		protected User getUser() {
			Assert.state(user != null, "A reference to the User was not properly configured");
			return user;
		}
	}
}
