package org.springframework.data.gemfire.config.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public abstract class AsCacheListenerConfigurationTests {

	protected static final List<Object[]> events = new ArrayList<>();
	protected ConfigurableApplicationContext applicationContext;

	@After public void shutdown() {
		Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
		events.clear();
	}

	protected static void recordEvent(EntryEvent event) {
		events.add(
			new Object[] {
				event.getRegion().getName(),
				event.getOperation(),
				event.getNewValue(),
				event.getOldValue()
			});
	}

	@Test(expected = BeanCreationException.class)
	public void cacheListenerWithIncorrectRegionEventParameter() {
		this.applicationContext = newApplicationContext(getCacheListenerWithIncorrectRegionEventParameterConfiguration());
	}

	@Test(expected = BeanCreationException.class)
	public void cacheListenerWithIncorrectEntryEventParameter() {
		this.applicationContext = newApplicationContext(getCacheListenerWithIncorrectRegionEventParameterConfiguration());
	}

	protected abstract Class<?> getCacheListenerWithIncorrectRegionEventParameterConfiguration();

	@Test public void cacheListenerAnnotationSingleDefaultRegions() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationSingleDefaultRegionsConfiguration());

		Region<String,String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.put("1", "1");
		testRegion.put("1", "2");

		Assertions.assertThat(events.size()).isEqualTo(2);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertUpdateEvent(1, "2", "1", "TestRegion1");
	}

	abstract protected Class<?> getCacheListenerAnnotationSingleDefaultRegionsConfiguration();

	@Test(expected = BeanCreationException.class)
	public void cacheListenerAnnotationWithInvalidRegion() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationWithInvalidRegion());
	}

	abstract protected Class getCacheListenerAnnotationWithInvalidRegion();

	@Test public void cacheListenerAnnotationMultipleRegionsDefault() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationMultipleRegionsDefault());

		Region<String,String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);
		Region<String,String> testRegion2 = applicationContext.getBean("TestRegion2", Region.class);

		testRegion1.put("1", "1");
		testRegion2.put("1", "2");
		testRegion2.put("1", "3");

		Assertions.assertThat(events.size()).isEqualTo(6);

		assertCreateEvent(0, "1", null, "TestRegion1", "TestRegion2");
		assertCreateEvent(1, "1", null, "TestRegion1", "TestRegion2");
		assertCreateEvent(2, "2", null, "TestRegion1", "TestRegion2");
		assertCreateEvent(3, "2", null, "TestRegion1", "TestRegion2");
		assertUpdateEvent(4, "3", "2", "TestRegion2");
		assertUpdateEvent(5, "3", "2", "TestRegion2");
	}

	abstract protected Class getCacheListenerAnnotationMultipleRegionsDefault();

	@Test public void cacheListenerAnnotationSingleRegionAllEvents() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationSingleRegionAllEvents());

		Region<String,String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.put("1", "1");
		testRegion.put("1", "2");
		testRegion.invalidate("1");
		testRegion.destroy("1");

		Assertions.assertThat(events.size()).isEqualTo(4);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertUpdateEvent(1, "2", "1", "TestRegion1");
		assertInvalidateEvent(2, null, "2", "TestRegion1");
		assertDestroyEvent(3, null, null, "TestRegion1");
	}

	abstract protected Class<?> getCacheListenerAnnotationSingleRegionAllEvents();

	@Test public void cacheListenerAnnotationAgainst2NamedRegions() {

		this.applicationContext = newApplicationContext(getCacheListenerAnnotationAgainst2NamedRegions());

		Region<String,String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);
		Region<String,String> testRegion2 = applicationContext.getBean("TestRegion2", Region.class);

		testRegion1.put("1", "1");
		testRegion2.put("1", "2");

		Assertions.assertThat(events.size()).isEqualTo(2);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertCreateEvent(1, "2", null, "TestRegion2");
	}

	abstract protected Class<?> getCacheListenerAnnotationAgainst2NamedRegions();

	private ConfigurableApplicationContext newApplicationContext(Class<?>... annotatedClasses) {

		ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(annotatedClasses);

		applicationContext.registerShutdownHook();

		return applicationContext;
	}

	private void assetCommonEventProperties(int index, String newValue, String oldValue, String[] regions) {
		Assertions.assertThat(events.get(index)[0]).isIn(regions);  //regionName
		Assertions.assertThat(events.get(index)[2]).isEqualTo(newValue);   //newValue
		Assertions.assertThat(events.get(index)[3]).isEqualTo(oldValue);   //oldValue
	}

	private void assertCreateEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isCreate()).isEqualTo(true);  //isCreate
	}

	private void assertUpdateEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isUpdate()).isEqualTo(true);  //isUpdate
	}

	private void assertInvalidateEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isInvalidate()).isEqualTo(true);  //isInvalidate
	}

	private void assertDestroyEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isDestroy()).isEqualTo(true);  //isDestroy
	}
}
