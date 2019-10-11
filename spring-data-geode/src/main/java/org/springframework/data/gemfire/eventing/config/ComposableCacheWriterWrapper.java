package org.springframework.data.gemfire.eventing.config;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.geode.cache.CacheEvent;
import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ComposableCacheWriterWrapper} is a {@link org.apache.geode.cache.CacheWriter} that enables the ability to
 * include multiple {@link PojoCacheWriterWrapper}s into a single {@link org.apache.geode.cache.CacheWriter}.
 * {@link org.apache.geode.cache.Region}s don't allow for multiple {@link org.apache.geode.cache.CacheWriter}s to be
 * registered against the same {@link org.apache.geode.cache.Region}.
 *
 * Given the callback event handling approach, using method annotations for {@link org.apache.geode.cache.CacheWriter} events of
 * {@link RegionEvent} and {@link EntryEvent}, could possibly result into the creation of multiple {@link org.apache.geode.cache.CacheWriter}
 * instances for the same region for different event types.
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.CacheWriter
 * @see CacheWriterAdapter
 * @see PojoCacheWriterWrapper
 * @since 2.4.0
 */
public class ComposableCacheWriterWrapper<E extends Enum<E>> extends CacheWriterAdapter {

	private static final String BEFORE_CREATE = "beforeCreate";
	private static final String BEFORE_UPDATE = "beforeUpdate";
	private static final String BEFORE_DESTROY = "beforeDestroy";
	private static final String BEFORE_REGION_DESTROY = "beforeRegionDestroy";
	private static final String BEFORE_REGION_CLEAR = "beforeRegionClear";

	private static Logger logger = LoggerFactory.getLogger(ComposableCacheWriterWrapper.class);

	protected Map<Integer, List<PojoCacheWriterWrapper>> cacheWriterWrapperList = new HashMap<>();

	/**
	 * Registers a {@link org.springframework.data.gemfire.config.annotation.AsCacheWriter} or
	 * {@link org.springframework.data.gemfire.config.annotation.AsRegionEventHandler} method,
	 * as a {@link PojoCacheWriterWrapper} for execution.
	 *
	 * In the event that the events are not {@link org.apache.geode.cache.CacheWriter} events, no event handler will
	 * be registered	 *
	 *
	 * @param method the method that needs to be executed in an event handling callback
	 * @param targetInvocationClass the target class on which the method was registered
	 * @param eventTypes the event types that the method needs to be registered for.
	 */
	public void addCacheWriter(Method method, Object targetInvocationClass, E[] eventTypes) {
		int eventMask = createRegionEventTypeMask(eventTypes);
		if (eventMask > 0) {
			List<PojoCacheWriterWrapper> regionEventCacheWriterList = cacheWriterWrapperList
				.getOrDefault(eventMask, new LinkedList<>());

			regionEventCacheWriterList.add(new PojoCacheWriterWrapper(method, targetInvocationClass));

			cacheWriterWrapperList.put(eventMask, regionEventCacheWriterList);
		}
	}

	/**
	 * Takes either a {@link RegionCacheWriterEventType} or {@link CacheWriterEventType} as an input.
	 * These enums contain an int that represents a filtering mask that will be applied. In order to create only 1
	 * {@link PojoCacheWriterWrapper} instance for each method, the masks need to be combined into a single value to
	 * represent all the events that the single event handling callback method needs cover.
	 *
	 * @param cacheWriterEventTypes an array of Enum EventTypes. Either {@link RegionCacheWriterEventType} or {@link CacheWriterEventType}
	 * @return an integer representing the combined masks of all registered event types
	 * @author Udo Kohlmeyer
	 * @since 2.4.0
	 */
	public int createRegionEventTypeMask(E[] cacheWriterEventTypes) {
		int mask = 0x0;
		for (E cacheWriterEventType : cacheWriterEventTypes) {
			if (cacheWriterEventType instanceof CacheWriterEventType) {
				CacheWriterEventType writerEventType = (CacheWriterEventType) cacheWriterEventType;
				mask |= writerEventType.mask;
			}
			else if (cacheWriterEventType instanceof RegionCacheWriterEventType) {
				RegionCacheWriterEventType writerEventType = (RegionCacheWriterEventType) cacheWriterEventType;
				mask |= writerEventType.mask;
			}
		}
		return mask;
	}

	@Override
	public void beforeCreate(EntryEvent event) {
		logDebug(BEFORE_CREATE);

		executeEventHandler(event, CacheWriterEventType.BEFORE_CREATE.mask);
	}

	@Override
	public void beforeUpdate(EntryEvent event) {
		logDebug(BEFORE_UPDATE);

		executeEventHandler(event, CacheWriterEventType.BEFORE_UPDATE.mask);
	}

	@Override
	public void beforeDestroy(EntryEvent event) {
		logDebug(BEFORE_DESTROY);

		executeEventHandler(event, CacheWriterEventType.BEFORE_DESTROY.mask);
	}

	@Override
	public void beforeRegionDestroy(RegionEvent event) throws CacheWriterException {
		logDebug(BEFORE_REGION_DESTROY);

		executeEventHandler(event, RegionCacheWriterEventType.BEFORE_REGION_DESTROY.mask);
	}

	@Override
	public void beforeRegionClear(RegionEvent event) throws CacheWriterException {
		logDebug(BEFORE_REGION_CLEAR);

		executeEventHandler(event, RegionCacheWriterEventType.BEFORE_REGION_CLEAR.mask);
	}

	/**
	 * This method takes a {@link CacheEvent}, which will either be a {@link RegionEvent} or a {@link EntryEvent},
	 * an eventType mask integer and tries to execute the event with registered {@link PojoCacheWriterWrapper}.
	 *
	 * Given that the registered {@link PojoCacheWriterWrapper} might be registered under a combined event mask, like
	 * {@link RegionCacheWriterEventType#ALL} or {@link CacheWriterEventType#ALL} each event received needs to be compared
	 * against each registered {@link PojoCacheWriterWrapper} entry to determine if the event needs to be executed.
	 *
	 * @param event either a {@link RegionEvent} or {@link EntryEvent} that needs to be actioned
	 * @param eventTypeInt a mask for the event that has just fired and needs to be handled
	 * @author Udo Kohlmeyer
	 * @see CacheEvent
	 * @see EntryEvent
	 * @see RegionEvent
	 * @since 2.4.0
	 */
	private void executeEventHandler(CacheEvent event, int eventTypeInt) {

		for (Map.Entry<Integer, List<PojoCacheWriterWrapper>> cacheWriterWrappers : cacheWriterWrapperList
			.entrySet()) {
			if ((cacheWriterWrappers.getKey() & eventTypeInt) == eventTypeInt) {

				for (PojoCacheWriterWrapper pojoCacheWriterWrapper : cacheWriterWrappers.getValue()) {
					pojoCacheWriterWrapper.executeCacheWriter(event);
				}
			}
		}
	}

	private void logDebug(String eventType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + eventType);
		}
	}
}
