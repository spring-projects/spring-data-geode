package org.springframework.data.gemfire.config.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public abstract class AsRegionEventConfigurationTests {

	protected static final List<Object[]> events = new ArrayList<>();
	protected ConfigurableApplicationContext applicationContext;

	protected static void recordEvent(RegionEvent<String, String> event) {
		events.add(
			new Object[] {
				event.getRegion().getName(),
				event.getOperation()
			});
	}

	@After
	public void shutdown() {
		Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
		events.clear();
	}

	@Test(expected = BeanCreationException.class)
	public void cacheListenerWithIncorrectRegionEventParameter() {
		this.applicationContext = newApplicationContext(getRegionEventWithIncorrectRegionEventParameterConfiguration());
	}

	protected abstract Class<?> getRegionEventWithIncorrectRegionEventParameterConfiguration();

	@Test
	public void cacheListenerAnnotationRegionEventClear() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationRegionEventClearConfiguration());

		Region<String, String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.clear();

		Assertions.assertThat(events.size()).isEqualTo(1);

		Assertions.assertThat(((Operation) events.get(0)[1]).isClear()).isEqualTo(true);  //isClear
	}

	abstract protected Class<?> getCacheListenerAnnotationRegionEventClearConfiguration();

	@Test(expected = BeanCreationException.class)
	public void cacheListenerAnnotationWithInvalidRegion() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationWithInvalidRegion());
	}

	abstract protected Class<?> getCacheListenerAnnotationWithInvalidRegion();

	@Test
	public void cacheListenerAnnotationRegionEventInvalidate() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationRegionEventInvalidateConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.invalidateRegion();

		Assertions.assertThat(events.size()).isEqualTo(1);

		Assertions.assertThat(((Operation) events.get(0)[1]).isRegionInvalidate()).isEqualTo(true);  //isInvalid
	}

	abstract protected Class<?> getCacheListenerAnnotationRegionEventInvalidateConfiguration();

	@Test
	public void cacheListenerAnnotationRegionEventDestroy() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationRegionEventDestroyConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.destroyRegion();

		Assertions.assertThat(events.size()).isEqualTo(1);

		Assertions.assertThat(((Operation) events.get(0)[1]).isRegionDestroy()).isEqualTo(true);  //isDestroy
	}

	abstract protected Class<?> getCacheListenerAnnotationRegionEventDestroyConfiguration();

	@Test
	public void cacheWriterAnnotationRegionDestroy() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationRegionDestroyConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.destroyRegion();

		Assertions.assertThat(events.size()).isEqualTo(1);

		Assertions.assertThat(((Operation) events.get(0)[1]).isRegionDestroy()).isEqualTo(true);  //isDestroy
	}

	abstract protected Class<?> getCacheWriterAnnotationRegionDestroyConfiguration();

	@Test
	public void cacheWriterAnnotationRegionClear() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationRegionClearConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.clear();

		Assertions.assertThat(events.size()).isEqualTo(1);

		Assertions.assertThat(((Operation) events.get(0)[1]).isClear()).isEqualTo(true);  //isDestroy
	}

	protected abstract Class<?> getCacheWriterAnnotationRegionClearConfiguration();

	@Test
	public void regionClearWithBothWriterAndListener() {

		this.applicationContext = newApplicationContext(getRegionClearWithBothWriterAndListenerConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.clear();

		Assertions.assertThat(events.size()).isEqualTo(2);

		Assertions.assertThat(((Operation) events.get(0)[1]).isClear()).isEqualTo(true);  //isDestroy
		Assertions.assertThat(((Operation) events.get(1)[1]).isClear()).isEqualTo(true);  //isDestroy
	}

	protected abstract Class<?> getRegionClearWithBothWriterAndListenerConfiguration();

	@Test
	public void regionClearWithNoEventHandlers() {

		this.applicationContext = newApplicationContext(getRegionClearWithNoEventHandlersConfiguration());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);

		testRegion1.clear();

		Assertions.assertThat(events.size()).isEqualTo(0);
	}

	protected abstract Class<?> getRegionClearWithNoEventHandlersConfiguration();

	private ConfigurableApplicationContext newApplicationContext(Class<?>... annotatedClasses) {

		ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(annotatedClasses);

		applicationContext.registerShutdownHook();

		return applicationContext;
	}
}
