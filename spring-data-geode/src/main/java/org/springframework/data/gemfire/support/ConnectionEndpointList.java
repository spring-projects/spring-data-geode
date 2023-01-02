/*
 * Copyright 2010-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.support;

import java.net.InetSocketAddress;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * The {@link ConnectionEndpointList} class is an {@link Iterable} collection of {@link ConnectionEndpoint} objects.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @see java.net.InetSocketAddress
 * @see java.util.AbstractList
 * @see java.util.List
 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
 * @since 1.6.3
 */
@SuppressWarnings("unused")
public class ConnectionEndpointList extends AbstractList<ConnectionEndpoint> {

	private final List<ConnectionEndpoint> connectionEndpoints;

	/**
	 * Factory method used to create a {@link ConnectionEndpointList} from an array of
	 * {@link ConnectionEndpoint ConnectionPoints}.
	 *
	 * @param connectionEndpoints array of {@link ConnectionEndpoint ConnectionPoints}
	 * used to initialize a new instance of {@link ConnectionEndpointList}.
	 * @return a {@link ConnectionEndpointList} initialized with the array of
	 * {@link ConnectionEndpoint ConnectionPoints}.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 */
	public static ConnectionEndpointList from(ConnectionEndpoint... connectionEndpoints) {
		return new ConnectionEndpointList(connectionEndpoints);
	}

	/**
	 * Factory method used to create a {@link ConnectionEndpointList} from an array of
	 * {@link InetSocketAddress InetSocketAddresses}.
	 *
	 * @param socketAddresses array of {@link InetSocketAddress InetSocketAddresses}
	 * used to initialize a new instance of {@link ConnectionEndpointList}.
	 * @return a {@link ConnectionEndpointList} initialized from the array of
	 * {@link InetSocketAddress InetSocketAddresses}.
	 * @see java.net.InetSocketAddress
	 * @see #from(Iterable)
	 */
	public static ConnectionEndpointList from(InetSocketAddress... socketAddresses) {
		return from(Arrays.asList(socketAddresses));
	}

	/**
	 * Factory method used to create a {@link ConnectionEndpointList} from an {@link Iterable} of
	 * {@link InetSocketAddress InetSocketAddresses}.
	 *
	 * @param socketAddresses {@link Iterable} of {@link InetSocketAddress InetSocketAddresses}
	 * used to initialize a new instance of {@link ConnectionEndpointList}.
	 * @return a {@link ConnectionEndpointList} initialized from an {@link Iterable} of
	 * {@link InetSocketAddress InetSocketAddresses}.
	 * @see java.net.InetSocketAddress
	 * @see java.lang.Iterable
	 */
	public static ConnectionEndpointList from(Iterable<InetSocketAddress> socketAddresses) {

		List<ConnectionEndpoint> connectionEndpoints = new ArrayList<>();

		for (InetSocketAddress socketAddress : CollectionUtils.nullSafeIterable(socketAddresses)) {
			connectionEndpoints.add(ConnectionEndpoint.from(socketAddress));
		}

		return new ConnectionEndpointList(connectionEndpoints);
	}

	/**
	 * Parses the comma-delimited {@link String hosts and ports} in the format {@literal host[port]}
	 * or {@literal host:port} to convert into an instance of {@link ConnectionEndpointList}.
	 *
	 * @param commaDelimitedHostAndPorts {@link String} containing a comma-delimited {@link String} of hosts and ports.
	 * @param defaultPort {@link Integer default port number} to use if port is not specified in a host and port value.
	 * @return a new {@link ConnectionEndpointList} representing the {@link String hosts and ports}.
	 * @see #parse(int, String...)
	 */
	public static ConnectionEndpointList parse(String commaDelimitedHostAndPorts, int defaultPort) {

		String[] hostsPorts = StringUtils.commaDelimitedListToStringArray(commaDelimitedHostAndPorts);

		return parse(defaultPort, hostsPorts);
	}

	/**
	 * Parses the array of {@link String hosts and ports} in the format {@literal host[port]} or {@literal host:port}
	 * to convert into an instance of {@link ConnectionEndpointList}.
	 *
	 * @param defaultPort {@link Integer default port number} to use if port is not specified in a host and port value.
	 * @param hostsPorts array of {@link String hosts and ports} to parse.
	 * @return a new {@link ConnectionEndpointList} representing the {@link String hosts and ports} in the array.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint#parse(String, int)
	 * @see #ConnectionEndpointList(Iterable)
	 */
	public static ConnectionEndpointList parse(int defaultPort, String... hostsPorts) {

		List<ConnectionEndpoint> connectionEndpoints = new ArrayList<>(ArrayUtils.length(hostsPorts));

		for (String hostPort : ArrayUtils.nullSafeArray(hostsPorts, String.class)) {
			connectionEndpoints.add(ConnectionEndpoint.parse(hostPort, defaultPort));
		}

		return new ConnectionEndpointList(connectionEndpoints);
	}

	/**
	 * Constructs a new, empty and uninitialized instance of the {@link ConnectionEndpointList}.
	 *
	 * @see #ConnectionEndpointList(Iterable)
	 */
	public ConnectionEndpointList() {
		this(Collections.emptyList());
	}

	/**
	 * Constructs a new instance of {@link ConnectionEndpointList} initialized with an array
	 * of {@link ConnectionEndpoint ConnectionEndpoints}.
	 *
	 * @param connectionEndpoints array of {@link ConnectionEndpoint ConnectionEndpoints} to add to this list.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see #ConnectionEndpointList(Iterable)
	 */
	public ConnectionEndpointList(@NonNull ConnectionEndpoint... connectionEndpoints) {
		this(Arrays.asList(connectionEndpoints));
	}

	/**
	 * Constructs a new instance of {@link ConnectionEndpointList} initialized with the {@link Iterable} collection
	 * of {@link ConnectionEndpoint ConnectionEndpoints}.
	 *
	 * @param connectionEndpoints {@link Iterable} object containing {@link ConnectionEndpoint ConnectionEndpoints}
	 * to add to this list.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see java.lang.Iterable
	 * @see #add(Iterable)
	 */
	public ConnectionEndpointList(@NonNull Iterable<ConnectionEndpoint> connectionEndpoints) {
		this.connectionEndpoints = new ArrayList<>();
		add(connectionEndpoints);
	}

	/**
	 * Adds the given {@link ConnectionEndpoint} to this list.
	 *
	 * @param connectionEndpoint {@link ConnectionEndpoint} to add to this list.
	 * @return a boolean value indicating whether this list was modified by the add operation.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see #add(ConnectionEndpoint...)
	 */
	@Override
	public boolean add(ConnectionEndpoint connectionEndpoint) {
		return add(ArrayUtils.asArray(connectionEndpoint)) == this;
	}

	/**
	 * Adds the array of {@link ConnectionEndpoint ConnectionEndpoints} to this list.
	 *
	 * @param connectionEndpoints array of {@link ConnectionEndpoint ConnectionEndpoints} to add to this list.
	 * @return this {@link ConnectionEndpointList}.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see #add(Iterable)
	 */
	public final ConnectionEndpointList add(ConnectionEndpoint... connectionEndpoints) {
		Collections.addAll(this.connectionEndpoints, connectionEndpoints);
		return this;
	}

	/**
	 * Adds the {@link Iterable} collection of {@link ConnectionEndpoint ConnectionEndpoints} to this list.
	 *
	 * @param connectionEndpoints {@link Iterable} collection of {@link ConnectionEndpoint ConnectionEndpoints}
	 * to add to this list.
	 * @return this {@link ConnectionEndpointList}.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see #add(ConnectionEndpoint...)
	 */
	public final ConnectionEndpointList add(Iterable<ConnectionEndpoint> connectionEndpoints) {

		for (ConnectionEndpoint connectionEndpoint : CollectionUtils.nullSafeIterable(connectionEndpoints)) {
			this.connectionEndpoints.add(connectionEndpoint);
		}

		return this;
	}

	/**
	 * Clears the current list of {@link ConnectionEndpoint ConnectionEndpoints}.
	 */
	@Override
	public void clear() {
		this.connectionEndpoints.clear();
	}

	private @NonNull ConnectionEndpointList findBy(@NonNull Predicate<ConnectionEndpoint> predicate) {

		List<ConnectionEndpoint> connectionEndpoints = new ArrayList<>(size());

		for (ConnectionEndpoint connectionEndpoint : this) {
			if (predicate.test(connectionEndpoint)) {
				connectionEndpoints.add(connectionEndpoint);
			}
		}

		return new ConnectionEndpointList(connectionEndpoints);
	}

	/**
	 * Finds all {@link ConnectionEndpoint ConnectionEndpoints} in this list with the specified {@link String hostname}.
	 *
	 * @param host {@link String} indicating the hostname to use in the match.
	 * @return a {@link ConnectionEndpointList} (sub-List) containing all the {@link ConnectionEndpoint ConnectionEndpoints}
	 * matching the given {@link String hostname}.
	 * @see #findBy(int)
	 */
	public @NonNull ConnectionEndpointList findBy(String host) {
		return findBy(connectionEndpoint -> connectionEndpoint.getHost().equals(host));
	}

	/**
	 * Finds all {@link ConnectionEndpoint ConnectionEndpoints} in this list with the specified port number.
	 *
	 * @param port {@link Integer} value indicating the port number to use in the match.
	 * @return a {@link ConnectionEndpointList} (sub-List) containing all the {@link ConnectionEndpoint ConnectionEndpoints}
	 * matching the given port number.
	 * @see #findBy(String)
	 */
	public @NonNull ConnectionEndpointList findBy(int port) {
		return findBy(connectionEndpoint -> connectionEndpoint.getPort() == port);
	}

	private @Nullable ConnectionEndpoint findOne(@NonNull ConnectionEndpointList list) {
		return list == null || list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Finds the first {@link ConnectionEndpoint} in the collection with the given host.
	 *
	 * @param host a String indicating the hostname of the {@link ConnectionEndpoint} to find.
	 * @return the first {@link ConnectionEndpoint} in this collection with the given host,
	 * or null if no {@link ConnectionEndpoint} exists with the given hostname.
	 * @see #findBy(String)
	 */
	public @Nullable ConnectionEndpoint findOne(String host) {
		return findOne(findBy(host));
	}

	/**
	 * Finds the first {@link ConnectionEndpoint} in the collection with the given port.
	 *
	 * @param port an integer indicating the port number of the {@link ConnectionEndpoint} to find.
	 * @return the first {@link ConnectionEndpoint} in this collection with the given port,
	 * or null if no {@link ConnectionEndpoint} exists with the given port number.
	 * @see #findBy(int)
	 */
	public @Nullable ConnectionEndpoint findOne(int port) {
		return findOne(findBy(port));
	}

	/**
	 * Gets the {@link ConnectionEndpoint} at the given index in this list.
	 *
	 * @param index an integer value indicating the index of the {@link ConnectionEndpoint} of interest.
	 * @return the {@link ConnectionEndpoint} at index in this list.
	 * @throws IndexOutOfBoundsException if the index is not within the bounds of this list.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see java.util.AbstractList#get(int)
	 */
	@Override
	public ConnectionEndpoint get(int index) {
		return this.connectionEndpoints.get(index);
	}

	/**
	 * Sets the element at the given index in this list to the given {@link ConnectionEndpoint}.
	 *
	 * @param index the index in the list at which to set the {@link ConnectionEndpoint}.
	 * @param element the {@link ConnectionEndpoint} to set in this list at the given index.
	 * @return the old {@link ConnectionEndpoint} at index in this list or null if no {@link ConnectionEndpoint}
	 * at index existed.
	 * @throws IndexOutOfBoundsException if the index is not within the bounds of this list.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint
	 * @see java.util.AbstractList#set(int, Object)
	 */
	@Override
	public ConnectionEndpoint set(int index, ConnectionEndpoint element) {
		return this.connectionEndpoints.set(index, element);
	}

	/**
	 * Determines whether this collection contains any ConnectionEndpoints.
	 *
	 * @return a boolean value indicating whether this collection contains any ConnectionEndpoints.
	 */
	@Override
	public boolean isEmpty() {
		return this.connectionEndpoints.isEmpty();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public @NonNull Iterator<ConnectionEndpoint> iterator() {
		return Collections.unmodifiableList(this.connectionEndpoints).iterator();
	}

	/**
	 * Determines the number of ConnectionEndpoints contained in this collection.
	 *
	 * @return an integer value indicating the number of ConnectionEndpoints contained in this collection.
	 */
	@Override
	public int size() {
		return this.connectionEndpoints.size();
	}

	/**
	 * Converts this collection of {@link ConnectionEndpoint}s into an array of {@link ConnectionEndpoint}s.
	 *
	 * @return an array of {@link ConnectionEndpoint}s representing this collection.
	 */
	@Override
	public ConnectionEndpoint[] toArray() {
		return this.connectionEndpoints.toArray(new ConnectionEndpoint[0]);
	}

	/**
	 * Converts this collection of {@link ConnectionEndpoint}s into a {@link List} of {@link InetSocketAddress}es.
	 *
	 * @return a {@link List} of {@link InetSocketAddress}es representing this collection of {@link ConnectionEndpoint}s.
	 * @see org.springframework.data.gemfire.support.ConnectionEndpoint#toInetSocketAddress()
	 * @see java.net.InetSocketAddress
	 * @see java.util.List
	 */
	public List<InetSocketAddress> toInetSocketAddresses() {

		List<InetSocketAddress> inetSocketAddresses = new ArrayList<>(size());

		for (ConnectionEndpoint connectionEndpoint : this) {
			inetSocketAddresses.add(connectionEndpoint.toInetSocketAddress());
		}

		return inetSocketAddresses;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String toString() {
		return connectionEndpoints.toString();
	}
}
