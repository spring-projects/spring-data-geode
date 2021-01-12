/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.gemfire.util;

import static org.springframework.data.gemfire.util.ArrayUtils.nullSafeArray;

import java.util.stream.Stream;

/**
 * The {@link StreamUtils} class is a abstract utility class for working with {@link Stream Streams}.
 *
 * @author John Blum
 * @see java.util.stream.Stream
 * @since 2.0.2
 */
public abstract class StreamUtils {

	/**
	 * Concatenates an array of {@link Stream Streams} into a single, continuous {@link Stream}.
	 *
	 * @param <T> {@link Class type} of elements in the {@link Stream Streams}.
	 * @param streams array of {@link Stream Streams} to concatenate.
	 * @return the concatenated array of {@link Stream Streams} as a single, continuous {@link Stream}.
	 * @see java.util.stream.Stream
	 */
	@SuppressWarnings("unchecked")
	public static <T> Stream<T> concat(Stream<T>... streams) {

		Stream<T> concatenatedStream = Stream.empty();

		for (Stream<T> stream : nullSafeArray(streams, Stream.class)) {
			concatenatedStream = Stream.concat(concatenatedStream, stream);
		}

		return concatenatedStream;
	}

	/**
	 * Null-safe utility method used to return a {@link Long count} of the number of {@link Object elements}
	 * in the given {@link Stream}.
	 *
	 * @param stream {@link Stream} of {@link Object elements} to count.
	 * @return a {@link Long count} of the number of {@link Object elements} in the {@link Stream}.
	 * @see java.util.stream.Stream#count()
	 * @see #nullSafeStream(Stream)
	 */
	public static long nullSafeCount(Stream<?> stream) {
		return nullSafeStream(stream).count();
	}

	/**
	 * Null-safe utility method used to determine whether the given {@link Stream} is empty.
	 *
	 * @param stream {@link Stream} to evalute.
	 * @return a boolean value indicating whether the given {@link Stream} is empty.
	 * @see java.util.stream.Stream
	 * @see #nullSafeCount(Stream)
	 */
	public static boolean nullSafeIsEmpty(Stream<?> stream) {
		return nullSafeCount(stream) == 0L;
	}

	/**
	 * Utility method used to guard against {@literal null} {@link Stream Streams}.
	 *
	 * @param <T> {@link Class type} of the {@link Object elements} in the {@link Stream}.
	 * @param stream {@link Stream} to evaluate.
	 * @return the given {@link Stream} if not {@literal null} or an {@link Stream#empty() empty} {@link Stream}.
	 * @see java.util.stream.Stream
	 */
	public static <T> Stream<T> nullSafeStream(Stream<T> stream) {
		return stream != null ? stream : Stream.empty();
	}
}
