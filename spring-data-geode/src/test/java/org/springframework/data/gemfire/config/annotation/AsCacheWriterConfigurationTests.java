package org.springframework.data.gemfire.config.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionEvent;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public abstract class AsCacheWriterConfigurationTests {

	protected static final List<Object[]> events = new ArrayList<>();
	protected ConfigurableApplicationContext applicationContext;

	@After
	public void shutdown() {
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

	protected static void recordEvent(RegionEvent<String, String> event) {
		events.add(
			new Object[] {
				event.getRegion().getName(),
				event.getOperation()
			});
	}

	@Test(expected = BeanCreationException.class)
	public void cacheWriterWithIncorrectRegionEventParameter() {
		this.applicationContext = newApplicationContext(getCacheWriterWithIncorrectRegionEventParameterConfiguration());
	}

	@Test(expected = BeanCreationException.class)
	public void cacheWriterrWithIncorrectEntryEventParameter() {
		this.applicationContext = newApplicationContext(getCacheWriterWithIncorrectRegionEventParameterConfiguration());
	}

	protected abstract Class<?> getCacheWriterWithIncorrectRegionEventParameterConfiguration();

	@Test
	public void cacheWriterAnnotationSingleDefaultRegions() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationSingleDefaultRegionsConfiguration());

		Region<String, String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.put("1", "1");
		testRegion.put("1", "2");

		Assertions.assertThat(events.size()).isEqualTo(2);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertUpdateEvent(1, "2", "1", "TestRegion1");
	}

	abstract protected Class<?> getCacheWriterAnnotationSingleDefaultRegionsConfiguration();

	@Test(expected = BeanCreationException.class)
	public void cacheWriterAnnotationWithInvalidRegion() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationWithInvalidRegion());
	}

	abstract protected Class getCacheWriterAnnotationWithInvalidRegion();

	@Test
	public void cacheWriterAnnotationMultipleRegionsDefault() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationMultipleRegionsDefault());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);
		Region<String, String> testRegion2 = applicationContext.getBean("TestRegion2", Region.class);

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

	abstract protected Class getCacheWriterAnnotationMultipleRegionsDefault();

	@Test
	public void cacheWriterAnnotationSingleRegionAllEvents() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationSingleRegionAllEvents());

		Region<String, String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.put("1", "1");
		testRegion.put("1", "2");
		testRegion.destroy("1");

		Assertions.assertThat(events.size()).isEqualTo(3);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertUpdateEvent(1, "2", "1", "TestRegion1");
		assertDestroyEvent(2, null, "2", "TestRegion1");
	}

	abstract protected Class<?> getCacheWriterAnnotationSingleRegionAllEvents();

	@Test
	public void cacheWriterAnnotationAgainst2NamedRegions() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationAgainst2NamedRegions());

		Region<String, String> testRegion1 = applicationContext.getBean("TestRegion1", Region.class);
		Region<String, String> testRegion2 = applicationContext.getBean("TestRegion2", Region.class);

		testRegion1.put("1", "1");
		testRegion2.put("1", "2");

		Assertions.assertThat(events.size()).isEqualTo(2);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertCreateEvent(1, "2", null, "TestRegion2");
	}

	abstract protected Class<?> getCacheWriterAnnotationAgainst2NamedRegions();

	@Test
	public void cacheWriterAnnotationWithRegionEventSingleRegionAllEvents() {

		this.applicationContext = newApplicationContext(getCacheWriterAnnotationWithRegionEventAndCacheWriter());

		Region<String, String> testRegion = applicationContext.getBean("TestRegion1", Region.class);

		testRegion.put("1", "1");
		testRegion.clear();

		Assertions.assertThat(events.size()).isEqualTo(2);

		assertCreateEvent(0, "1", null, "TestRegion1");
		assertRegionClearEvent(1, "TestRegion1");
	}

	abstract protected Class<?> getCacheWriterAnnotationWithRegionEventAndCacheWriter();

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

	private void assetCommonRegionEventProperties(int index, String[] regions) {
		Assertions.assertThat(events.get(index)[0]).isIn(regions);  //regionName
	}

	private void assertCreateEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isCreate()).isEqualTo(true);  //isCreate
	}

	private void assertUpdateEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isUpdate()).isEqualTo(true);  //isUpdate
	}

	private void assertDestroyEvent(int index, String newValue, String oldValue, String... regions) {
		assetCommonEventProperties(index, newValue, oldValue, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isDestroy()).isEqualTo(true);  //isDestroy
	}

	private void assertRegionClearEvent(int index, String... regions) {
		assetCommonRegionEventProperties(index, regions);
		Assertions.assertThat(((Operation) events.get(index)[1]).isClear()).isEqualTo(true);  //isClear
	}
}
