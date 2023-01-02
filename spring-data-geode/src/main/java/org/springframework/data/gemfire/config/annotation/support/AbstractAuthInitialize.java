/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation.support;

import java.util.Optional;
import java.util.Properties;

import org.apache.geode.LogWriter;
import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AuthInitialize;
import org.apache.geode.security.AuthenticationFailedException;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.support.WiringDeclarableSupport;
import org.springframework.lang.Nullable;

/**
 * Abstract class and basic implementation of the {@link AuthInitialize} interface used to authenticate a client
 * or peer with a secure Apache Geode cluster.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.distributed.DistributedMember
 * @see org.apache.geode.security.AuthInitialize
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.core.env.Environment
 * @see org.springframework.data.gemfire.support.WiringDeclarableSupport
 * @since 2.0.0
 */
public abstract class AbstractAuthInitialize extends WiringDeclarableSupport
		implements AuthInitialize, EnvironmentAware {

	@Nullable
	private Environment environment;

	/**
	 * Sets a reference to the configured Spring {@link Environment}.
	 *
	 * @param environment reference to the configured Spring {@link Environment}.
	 * @see org.springframework.core.env.Environment
	 */
	@Override
	@SuppressWarnings("all")
	public void setEnvironment(@Nullable Environment environment) {
		this.environment = environment;
	}

	/**
	 * Get an {@link Optional} reference to the configured Spring {@link Environment}.
	 *
	 * @return an {@link Optional} reference to the configured Spring {@link Environment}.
	 * @see org.springframework.core.env.Environment
	 * @see java.util.Optional
	 */
	protected Optional<Environment> getEnvironment() {
		return Optional.ofNullable(this.environment);
	}

	/**
	 * Initializes this Apache Geode component by auto-wiring (configuring) any dependencies managed by
	 * the Spring container required during authentication.
	 *
	 * @param systemLogWriter {@link LogWriter} for system output.
	 * @param securityLogWriter {@link LogWriter} for security output.
	 * @throws AuthenticationFailedException if this Apache Geode node could not be authenticated with
	 * the Apache Geode distributed system (cluster).
	 * @see #initialize(Cache, Properties)
	 * @see #doInit()
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final void init(LogWriter systemLogWriter, LogWriter securityLogWriter) throws AuthenticationFailedException {
		doInit();
	}

	protected void doInit() { }

	/**
	 * Gets the security credentials used to authenticate this Apache Geode node
	 * with the Apache Geode distributed system (cluster).
	 *
	 * @param properties Apache Geode {@link Properties} to configure with authentication credentials
	 * used during the authentication request.
	 * @param distributedMember {@link DistributedMember} representing this Apache Geode node.
	 * @param isPeer boolean value indicating whether this Apache Geode node is joining the cluster
	 * as a peer or a client.
	 * @return the given Apache Geode {@link Properties}.
	 * @throws AuthenticationFailedException if this Apache Geode node could not be authenticated with
	 * the Apache Geode distributed system (cluster).
	 * @see org.apache.geode.distributed.DistributedMember
	 * @see #doGetCredentials(Properties)
	 * @see java.util.Properties
	 */
	@Override
	public final Properties getCredentials(Properties properties, DistributedMember distributedMember, boolean isPeer)
			throws AuthenticationFailedException {

		return doGetCredentials(properties);
	}

	protected abstract Properties doGetCredentials(Properties properties);

}
