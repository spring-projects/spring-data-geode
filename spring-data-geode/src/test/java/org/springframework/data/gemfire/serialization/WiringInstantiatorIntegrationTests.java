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
package org.springframework.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;
import java.awt.Shape;
import java.beans.Beans;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.DataSerializable;
import org.apache.geode.Instantiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link WiringInstantiator}.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.Instantiator
 * @see org.springframework.data.gemfire.serialization.WiringInstantiator
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("simple-config.xml")
@SuppressWarnings("unused")
public class WiringInstantiatorIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WiringInstantiator instantiator;


	public static class AnnotatedBean implements DataSerializable {

		@Autowired
		Point point;
		Shape shape;

		@Autowired
		void initShape(Shape shape) {
			this.shape = shape;
		}

		public void fromData(DataInput in) { }

		public void toData(DataOutput out) { }

	}

	public static class TemplateWiringBean implements DataSerializable {

		Beans beans;
		Point point;

		public void setBeans(Beans bs) {
			this.beans = bs;
		}

		public void fromData(DataInput in) { }

		public void toData(DataOutput out) { }

	}

	public static class TypeA implements DataSerializable {

		public void fromData(DataInput arg0) { }

		public void toData(DataOutput arg0) { }

	}

	public static class TypeB implements DataSerializable {

		public void fromData(DataInput arg0) { }

		public void toData(DataOutput arg0) { }

	}

	@Test
	public void testAutowiredBean() {

		Object instance = instantiator.newInstance();

		assertThat(instance).isNotNull();
		assertThat(instance instanceof AnnotatedBean).isTrue();

		AnnotatedBean bean = (AnnotatedBean) instance;

		assertThat(bean.point).isNotNull();
		assertThat(bean.shape).isNotNull();

		assertThat(applicationContext.getBean("point")).isSameAs(bean.point);
		assertThat(applicationContext.getBean("area")).isSameAs(bean.shape);
	}

	@Test
	public void testTemplateBean() {

		WiringInstantiator instantiator2 =
			new WiringInstantiator(new AsmInstantiatorGenerator().getInstantiator(TemplateWiringBean.class, 99));

		instantiator2.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
		instantiator2.afterPropertiesSet();

		Object instance = instantiator2.newInstance();

		assertThat(instance instanceof TemplateWiringBean).isTrue();
		TemplateWiringBean bean = (TemplateWiringBean) instance;

		assertThat(bean.point).isNull();
		assertThat(bean.beans).isNotNull();

		assertThat(applicationContext.getBean("beans")).isSameAs(bean.beans);
	}

	public void testInstantiatorFactoryBean() {
		@SuppressWarnings("unchecked")
		List<Instantiator> list = (List<Instantiator>) applicationContext.getBean("instantiator-factory");
		assertThat(list).isNotNull();
		assertThat(list.size()).isEqualTo(2);
	}
}
