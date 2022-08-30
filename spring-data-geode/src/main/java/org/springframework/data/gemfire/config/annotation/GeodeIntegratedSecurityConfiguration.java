/*
 * Copyright 2016-2022 the original author or authors.
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
 *
 */
package org.springframework.data.gemfire.config.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;

import org.springframework.data.gemfire.GemFireProperties;
import org.springframework.data.gemfire.config.annotation.support.EmbeddedServiceConfigurationSupport;
import org.springframework.data.gemfire.util.PropertiesBuilder;

/**
 * The {@link GeodeIntegratedSecurityConfiguration} class is a {@link EmbeddedServiceConfigurationSupport} implementation
 * that enables Apache Geode's Integrated Security framework and services.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.config.annotation.support.EmbeddedServiceConfigurationSupport
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class GeodeIntegratedSecurityConfiguration extends EmbeddedServiceConfigurationSupport {

	protected static final String SECURITY_CLIENT_AUTH_INIT = GemFireProperties.SECURITY_CLIENT_AUTH_INIT.getName();
	protected static final String SECURITY_MANAGER = GemFireProperties.SECURITY_MANAGER.getName();
	protected static final String SECURITY_PEER_AUTH_INIT = GemFireProperties.SECURITY_PEER_AUTH_INIT.getName();
	protected static final String SECURITY_POST_PROCESSOR = GemFireProperties.SECURITY_POST_PROCESSOR.getName();
	protected static final String SECURITY_SHIRO_INIT = GemFireProperties.SECURITY_SHIRO_INIT.getName();

	/**
	 * Returns the {@link EnableSecurity} {@link java.lang.annotation.Annotation} {@link Class} type.
	 *
	 * @return the {@link EnableSecurity} {@link java.lang.annotation.Annotation} {@link Class} type.
	 * @see org.springframework.data.gemfire.config.annotation.EnableSecurity
	 */
	@Override
	protected Class<? extends Annotation> getAnnotationType() {
		return EnableSecurity.class;
	}

	/**
	 * Determines whether Apache Geode's Apache Shiro Security Framework support is enabled or available.
	 *
	 * @return a boolean value indicating whether Apache Geode's Apache Shiro Security Framework support
	 * is enabled or available.
	 * @see #isShiroSecurityNotConfigured()
	 */
	protected boolean isShiroSecurityConfigured() {

		try {
			//return resolveBean(ApacheShiroSecurityConfiguration.class).isRealmsPresent();
			return false;
		}
		catch (Exception ignore) {
			return false;
		}
	}

	/**
	 * Determines whether Apache Geode's Apache Shiro Security Framework support is enabled or available.
	 *
	 * @return a boolean value indicating whether Apache Geode's Apache Shiro Security Framework support
	 * is enabled or available.
	 * @see #isShiroSecurityConfigured()
	 */
	protected boolean isShiroSecurityNotConfigured() {
		return !isShiroSecurityConfigured();
	}

	@Override
	protected Properties toGemFireProperties(Map<String, Object> annotationAttributes) {

		PropertiesBuilder gemfireProperties = PropertiesBuilder.create();

		gemfireProperties.setProperty(SECURITY_CLIENT_AUTH_INIT,
			resolveProperty(securityProperty("client.authentication-initializer"),
				(String) annotationAttributes.get("clientAuthenticationInitializer")));

		if (isShiroSecurityNotConfigured()) {

			gemfireProperties.setPropertyIfNotDefault(SECURITY_MANAGER,
				annotationAttributes.get("securityManagerClass"), Void.class);

			gemfireProperties.setProperty(SECURITY_MANAGER,
				resolveProperty(securityProperty("manager.class-name"),
					(String) annotationAttributes.get("securityManagerClassName")));

			gemfireProperties.setProperty(SECURITY_SHIRO_INIT,
				resolveProperty(securityProperty("shiro.ini-resource-path"),
					(String) annotationAttributes.get("shiroIniResourcePath")));
		}

		gemfireProperties.setProperty(SECURITY_PEER_AUTH_INIT,
			resolveProperty(securityProperty("peer.authentication-initializer"),
				(String) annotationAttributes.get("peerAuthenticationInitializer")));

		gemfireProperties.setPropertyIfNotDefault(SECURITY_POST_PROCESSOR,
			annotationAttributes.get("securityPostProcessorClass"), Void.class);

		gemfireProperties.setProperty(SECURITY_POST_PROCESSOR,
			resolveProperty(securityProperty("postprocessor.class-name"),
				(String) annotationAttributes.get("securityPostProcessorClassName")));

		return gemfireProperties.build();
	}
}
