package org.springframework.data.gemfire.eventing.config;

import java.lang.reflect.Method;

import org.apache.geode.cache.CacheEvent;
import org.springframework.util.ReflectionUtils;

/**
 * A simple wrapper method that wraps a {@link Method} and its target instance for simple usage in the {@link ComposableCacheWriterWrapper}
 * for method execution on a {@link org.apache.geode.cache.Region}'s {@link org.apache.geode.cache.CacheWriter}.
 *
 * @author Udo Kohlmeyer
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.CacheWriter
 * @see ComposableCacheWriterWrapper
 * @see ReflectionUtils
 * @since 2.4.0
 */
public class PojoCacheWriterWrapper {

	private final Method method;
	private final Object targetInvocationObject;

	public PojoCacheWriterWrapper(Method method, Object targetInvocationClass) {
		this.method = method;
		this.targetInvocationObject = targetInvocationClass;
	}

	/**
	 * Using Reflection the stored {@link Method} and target invocation instance is used to reflectively run the method.
	 *
	 * @param event the {@link org.apache.geode.cache.RegionEvent} or {@link org.apache.geode.cache.EntryEvent} that is
	 * required for the method to execute.
	 */
	public void executeCacheWriter(CacheEvent event) {
		ReflectionUtils.invokeMethod(method, targetInvocationObject, event);
	}
}
