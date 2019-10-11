package org.springframework.data.gemfire.config.support;

import static java.util.Arrays.stream;
import static org.springframework.data.gemfire.util.ArrayUtils.isEmpty;
import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalStateException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.CacheEvent;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.gemfire.eventing.config.CacheListenerEventType;
import org.springframework.data.gemfire.eventing.config.CacheWriterEventType;
import org.springframework.data.gemfire.eventing.config.RegionCacheListenerEventType;
import org.springframework.data.gemfire.eventing.config.RegionCacheWriterEventType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 *
 */
public abstract class CallbackPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	protected ConfigurableListableBeanFactory beanFactory;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		registerAnyDeclaredCallbackAnnotatedMethods(bean, getEventHandlerClass());
		registerAnyDeclaredCallbackAnnotatedMethods(bean, getRegionEventHandlerClass());

		return bean;
	}

	protected abstract Class getRegionEventHandlerClass();

	protected abstract Class getEventHandlerClass();

	protected <T extends Annotation> void registerAnyDeclaredCallbackAnnotatedMethods(Object bean,
		Class<T> annotationClazz) {

		Method[] declaredMethods = nullSafeArray(ReflectionUtils.getAllDeclaredMethods(bean.getClass()), Method.class);

		stream(declaredMethods).forEach(method -> {

			Optional<T> optionalCallbackAnnotation = Optional.ofNullable(AnnotationUtils
				.getAnnotation(method, annotationClazz));

			optionalCallbackAnnotation.ifPresent(callback -> {

				Assert.isTrue(Modifier.isPublic(method.getModifiers()), String
					.format("The bean [%s] method [%s] annotated with [%s] must be public", bean.getClass().getName(),
						method.getName(), annotationClazz.getName()));

				AnnotationAttributes callbackAttributes = resolveAnnotationAttributes(callback);

				registerEventHandlers(bean, annotationClazz, method, callbackAttributes);

			});
		});
	}

	protected abstract <T extends Annotation> void registerEventHandlers(Object bean, Class<T> annotationClazz,
		Method method, AnnotationAttributes callbackAttributes);

	/**
	 * Takes an array of Region names. If empty, returns all configured {@link Region} names, otherwise returns the input
	 * region name array
	 *
	 * @param beanFactory - A {@link org.springframework.data.gemfire.ConfigurableRegionFactoryBean}
	 * @return An array of {@link Region} names. If the input regions array is empty, the result will be an array with all
	 * configured {@link Region} names
	 */
	protected List<String> getRegionsForEventRegistration(ConfigurableListableBeanFactory beanFactory) {
		List<String> regionNames = new ArrayList<>();
		stream(beanFactory.getBeanDefinitionNames()).forEach(beanName -> {
			Object bean = beanFactory.getBean(beanName);
			if (bean instanceof Region) {
				Region region = (Region) bean;
				regionNames.add(region.getName());
			}
		});
		return regionNames;
	}

	private AnnotationAttributes resolveAnnotationAttributes(Annotation annotation) {
		return AnnotationAttributes.fromMap(
			AnnotationUtils.getAnnotationAttributes(annotation, false, true));
	}

	/**
	 * Takes an array of Region names. If empty, returns all configured {@link Region} names, otherwise returns the input
	 * region name array
	 *
	 * @param regions - An Array of {@link Region} names. This can be empty and thus defaults to all configured {@link Region}
	 * @param beanFactory - A {@link org.springframework.data.gemfire.ConfigurableRegionFactoryBean}
	 * @return An array of {@link Region} names. If the input regions array is empty, the result will be an array with all
	 * configured {@link Region} names
	 */
	protected List<String> getRegionsForEventRegistration(String[] regions,
		ConfigurableListableBeanFactory beanFactory) {
		if (isEmpty(regions)) {
			return getRegionsForEventRegistration(beanFactory);
		}
		else {
			return Arrays.asList(regions);
		}
	}

	/**
	 * Sets a reference to the configured Spring {@link BeanFactory}.
	 *
	 * @param beanFactory configured Spring {@link BeanFactory}.
	 * @throws IllegalArgumentException if the given {@link BeanFactory} is not an instance of
	 * {@link ConfigurableListableBeanFactory}.
	 * @see BeanFactoryAware
	 * @see BeanFactory
	 */
	@Override
	@SuppressWarnings("all")
	public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory, String
			.format("BeanFactory [%1$s] must be an instance of %2$s", ObjectUtils.nullSafeClassName(beanFactory),
				ConfigurableListableBeanFactory.class.getSimpleName()));

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * Returns a reference to the containing Spring {@link BeanFactory}.
	 *
	 * @return a reference to the containing Spring {@link BeanFactory}.
	 * @throws IllegalStateException if the {@link BeanFactory} was not configured.
	 * @see BeanFactory
	 */
	protected ConfigurableListableBeanFactory getBeanFactory() {
		return Optional.ofNullable(this.beanFactory)
			.orElseThrow(() -> newIllegalStateException("BeanFactory was not properly configured"));
	}

	/**
	 * Returns the correct Event type, either {@link EntryEvent} or {@link RegionEvent}, dependent on the eventType
	 * of either {@link RegionCacheWriterEventType}, {@link CacheWriterEventType}, {@link CacheListenerEventType}
	 * or {@link RegionCacheListenerEventType}
	 *
	 * @param eventTypes an array of event types
	 * @return a class type associated with the enum event type. Returns {@literal null} if not association is found
	 */
	protected Class<? extends CacheEvent> getEventTypeForMethod(Enum[] eventTypes) {
		for (Enum eventType : eventTypes) {
			if (eventType instanceof CacheWriterEventType || eventType instanceof CacheListenerEventType) {
				return EntryEvent.class;
			}
			if (eventType instanceof RegionCacheWriterEventType || eventType instanceof RegionCacheListenerEventType) {
				return RegionEvent.class;
			}
		}
		return null;
	}
}
