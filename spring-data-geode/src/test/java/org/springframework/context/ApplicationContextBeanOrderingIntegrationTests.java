/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.data.gemfire.util.StreamUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the bean ordering applied by the Spring {@link ApplicationContext}
 * or Spring {@link BeanFactory} when using the {@link Order} annotation or implementing the {@link Ordered} interface.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class ApplicationContextBeanOrderingIntegrationTests {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Autowired
	private NamedBean[] namedBeans;

	/**
	 * 1. Auto-wiring/Dependency Injection (DI) does exactly what I'd like to do programmatically using some API
	 * on a {@link BeanFactory} or an {@link ApplicationContext}.
	 */
	@Test
	public void autoWiredBeansAreOrderedByOrderAnnotationAndOrderedInterface() {

		List<String> beanNames = Arrays.stream(this.namedBeans)
			.map(Object::toString)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	/**
	 * 2. The Javadoc is pretty precise about bean ordering...
	 *
	 * {@literal The Map returned by this method should always return bean names and corresponding bean instances
	 * in the order of definition in the backend configuration, as far as possible.}
	 *
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 * @see <a href="https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/ListableBeanFactory.html#getBeansOfType-java.lang.Class-">ListableBeanFactory.getBeansOfType(:Class)</a>
	 */
	@Test
	public void beansAreOrderedByBeanDefinitionDeclarationOrder() {

		List<String> beanNames = this.applicationContext.getBeansOfType(NamedBean.class).values().stream()
			.map(Object::toString)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [A, B, C, D, U, X, Y, Z]; but was %s", beanNames)
			.containsExactly("A", "B", "C", "D", "U", "X", "Y", "Z");
	}

	/**
	 * 3. The Javadoc is pretty precise about bean name ordering...
	 *
	 * {@literal Bean names returned by this method should always return bean names in the order of definition
	 * in the backend configuration, as far as possible.}
	 *
	 * @see ListableBeanFactory#getBeanNamesForType(Class)
	 * @see <a href="https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/ListableBeanFactory.html#getBeanNamesForType-java.lang.Class-">ListableBeanFactory.getBeanNamesForType(:Class)</a>
	 */
	@Test
	public void beanNamesAreOrderedByBeanDefinitionDeclarationOrder() {

		List<String> beanNames = Arrays.asList(this.applicationContext.getBeanNamesForType(NamedBean.class));

		assertThat(beanNames)
			.describedAs("Expected [A, B, C, D, U, X, Y, Z]; but was %s", beanNames)
			.containsExactly("A", "B", "C", "D", "U", "X", "Y", "Z");
	}

	@Test
	public void expectBeansToBeOrderedByOrderAnnotationAndOrderedInterfaceUsingBeanProviderOrderedStream() {

		List<String> beanNames = this.applicationContext.getBeanProvider(NamedBean.class).orderedStream()
			.map(Object::toString)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	/**
	 * 4. Test Fails (of course)!
	 *
	 * Like the bean name ordering specification called out in the Javadoc, {@link ListableBeanFactory#getBeansOfType(Class)}
	 * is exactly like {@link ListableBeanFactory#getBeanNamesForType(Class)}.  That is...
	 *
	 * {@literal The Map returned by this method should always return bean names and corresponding bean instances
	 * in the order of definition in the backend configuration, as far as possible.}
	 *
	 * However, is there a programmatical means (i.e. API) to do what Auto-wiring/Dependency Injection (DI)
	 * (i.e. using {@link Autowired}) does as tested in the
	 * {@link #autoWiredBeansAreOrderedByOrderAnnotationAndOrderedInterface()} test case?
	 *
	 * This test case demonstrates what I'd like to happen (using a different API call given the contract
	 * of the existing method).
	 *
	 * @see #autoWiredBeansAreOrderedByOrderAnnotationAndOrderedInterface()
	 */
	//@Test
	public void expectBeansToBeOrderedByOrderAnnotationAndOrderedInterface() {

		List<String> beanNames = this.applicationContext.getBeansOfType(NamedBean.class).values().stream()
			.map(Object::toString)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	/**
	 * 5. Test Fails (of course)!
	 *
	 * This test case demonstrates what I'd like to happen (using a different API call given the contract
	 * of the existing method).
	 *
	 * @see #autoWiredBeansAreOrderedByOrderAnnotationAndOrderedInterface()
 	 */
	//@Test
	public void expectBeanNamesToBeOrderedByOrderAnnotationAndOrderedInterface() {

		List<String> beanNames = Arrays.asList(this.applicationContext.getBeanNamesForType(NamedBean.class));

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	/**
	 * 6. Test almost passes... my hack to simulate what I want.
	 *
	 * The hack does not handle {@link Ordered} interface implementations, though.  That would require an instantiation
	 * unless the bean defined some conventions, such as a {@code public static final int} {@literal ORDER} field
	 * that could be introspected reflectively.  :-P
	 *
	 * I used {@link ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)} to avoid
	 * eager bean initialization as far as possible.
	 *
	 * @see #autoWiredBeansAreOrderedByOrderAnnotationAndOrderedInterface()
	 */
	//@Test
	public void expectBeanNamesToBeOrderedByOrderAnnotationFromBeanDefinitionMetadata() {

		List<String> beanNames = Arrays.stream(this.applicationContext.getBeanNamesForType(NamedBean.class, true, false))
			.map(this::toBeanDefinitionHolder)
			.filter(Objects::nonNull)
			.sorted(OrderAnnotatedBeanDefinitionComparator.INSTANCE)
			.map(BeanDefinitionHolder::getBeanName)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	private BeanDefinitionHolder toBeanDefinitionHolder(String beanName) {

		return Optional.ofNullable(this.applicationContext)
			.map(ConfigurableApplicationContext::getBeanFactory)
			.map(beanFactory -> beanFactory.getBeanDefinition(beanName))
			.map(beanDefinition -> new BeanDefinitionHolder(beanDefinition, beanName))
			.orElse(null);
	}

	@Test
	public void expectBeansToBeOrderedByOrderAnnotationAndOrderedInterfaceUsingSpringUtils() {

		List<NamedBean> orderedBeans = CollectionUtils
			.nullSafeList(SpringUtils.getBeansOfTypeOrdered(this.applicationContext.getBeanFactory(), NamedBean.class));

		Stream<NamedBean> orderedBeanStream = StreamUtils.nullSafeStream(orderedBeans.stream());

		List<String> beanNames = orderedBeanStream
			.map(Object::toString)
			.collect(Collectors.toList());

		assertThat(beanNames)
			.describedAs("Expected [Y, X, Z, B, C, A, D, U]; but was %s", beanNames)
			.containsExactly("Y", "X", "Z", "B", "C", "A", "D", "U");
	}

	@Configuration
	static class TestConfiguration {

		@Bean("A")
		@Order(3)
		NamedBean a() {
			return new NamedBean();
		}

		@Bean("B")
		@Order(1)
		NamedBean b() {
			return new NamedBean();
		}

		@Bean("C")
		@Order(2)
		NamedBean c() {
			return new NamedBean();
		}

		@Bean("D")
		@Order(4)
		NamedBean d() {
			return new NamedBean();
		}

		@Bean("U")
		NamedBean unorderedNamedBean() {
			return new NamedBean();
		}

		@Bean("X")
		X x() {
			return new X();
		}

		@Bean("Y")
		Y y() {
			return new Y();
		}

		@Bean("Z")
		Z z() {
			return new Z();
		}
	}

	static class NamedBean implements BeanNameAware {

		private String name;

		public NamedBean() { }

		public NamedBean(String name) {
			this.name = name;
		}

		@Override
		public void setBeanName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Order(0)
	static abstract class AbstractZeroOrderedNamedBean extends NamedBean {

		AbstractZeroOrderedNamedBean() { }

		AbstractZeroOrderedNamedBean(String name) {
			super(name);
		}
	}

	static class X extends NamedBean implements Ordered {

		X() {
			super(X.class.getSimpleName());
		}

		@Override
		public int getOrder() {
			return -1;
		}
	}

	@Order(-2)
	static class Y extends NamedBean {

		Y() {
			super(Y.class.getSimpleName());
		}
	}

	static class Z extends AbstractZeroOrderedNamedBean {

		Z() {
			super(Z.class.getSimpleName());
		}
	}

	static class OrderAnnotatedBeanDefinitionComparator implements Comparator<BeanDefinitionHolder> {

		static final OrderAnnotatedBeanDefinitionComparator INSTANCE = new OrderAnnotatedBeanDefinitionComparator();

		private final Map<String, Integer> beanNameToOrder = new ConcurrentHashMap<>();

		@Override
		public int compare(BeanDefinitionHolder beanOne, BeanDefinitionHolder beanTwo) {
			return getOrder(beanOne).compareTo(getOrder(beanTwo));
		}

		private Integer getOrder(@Nullable BeanDefinitionHolder bean) {

			return this.beanNameToOrder.computeIfAbsent(bean.getBeanName(), beanName -> {

				Integer order = getOrderFromBeanType(bean);

				return order != null ? order : getOrderFromFactoryMethod(bean);
			});
		}

		private @Nullable Integer getOrderFromBeanType(@Nullable BeanDefinitionHolder bean) {

			return Optional.ofNullable(bean)
				.map(BeanDefinitionHolder::getBeanDefinition)
				.map(BeanDefinition::getResolvableType)
				.map(ResolvableType::resolve)
				.map(beanType -> OrderUtils.getOrder(beanType, Ordered.LOWEST_PRECEDENCE))
				.orElse(null);

			// Why does the following not work given the Javadoc for AnnotatedBeanDefinition.getMetadata() reads...
			// "Obtain the annotation metadata (as well as basic class metadata) for this BEAN DEFINITION's 'BEAN CLASS'"
			// See: https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/annotation/AnnotatedBeanDefinition.html#getMetadata--
			// This does not work for BeanDefinitions from @Bean methods on @Configuration classes and seems like it should!
			/*
			return Optional.of(bean)
				.map(BeanDefinitionHolder::getBeanDefinition)
				.filter(AnnotatedBeanDefinition.class::isInstance)
				.map(AnnotatedBeanDefinition.class::cast)
				.map(AnnotatedBeanDefinition::getMetadata)
				.filter(annotationMetadata -> annotationMetadata.hasAnnotation(Order.class.getName()))
				.map(annotationMetadata -> annotationMetadata.getAnnotationAttributes(Order.class.getName()))
				.map(AnnotationAttributes::fromMap)
				.orElse(null);
			*/
		}

		private @NonNull Integer getOrderFromFactoryMethod(@NonNull BeanDefinitionHolder bean) {

			return Optional.of(bean)
				.map(BeanDefinitionHolder::getBeanDefinition)
				.filter(AnnotatedBeanDefinition.class::isInstance)
				.map(AnnotatedBeanDefinition.class::cast)
				.map(AnnotatedBeanDefinition::getFactoryMethodMetadata)
				.filter(methodMetadata -> methodMetadata.isAnnotated(Order.class.getName()))
				.map(methodMetadata -> methodMetadata.getAnnotationAttributes(Order.class.getName()))
				.map(annotationAttributes -> annotationAttributes.getOrDefault("value", Ordered.LOWEST_PRECEDENCE))
				.map(Integer.class::cast)
				.orElse(Ordered.LOWEST_PRECEDENCE);
		}
	}
}
