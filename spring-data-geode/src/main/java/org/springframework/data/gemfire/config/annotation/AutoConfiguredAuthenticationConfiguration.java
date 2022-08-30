/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import org.apache.geode.security.AuthInitialize;

import org.apache.shiro.util.Assert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.gemfire.GemFireProperties;
import org.springframework.data.gemfire.config.annotation.support.Authentication;
import org.springframework.data.gemfire.config.annotation.support.AutoConfiguredAuthenticationInitializer;
import org.springframework.data.gemfire.config.support.RestTemplateConfigurer;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AutoConfiguredAuthenticationConfiguration} class is a Spring {@link Configuration} class
 * that auto-configures Apache Geode Authentication by providing an implementation of the {@link AuthInitialize}
 * interface along with setting the necessary Apache Geode {@link Properties}.
 *
 * @author John Blum
 * @see java.net.Authenticator
 * @see java.net.PasswordAuthentication
 * @see java.util.Properties
 * @see org.apache.geode.security.AuthInitialize
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Condition
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.ConfigurationCondition
 * @see org.springframework.core.env.Environment
 * @see org.springframework.data.gemfire.GemFireProperties
 * @see org.springframework.data.gemfire.config.annotation.EnableBeanFactoryLocator
 * @see org.springframework.data.gemfire.config.annotation.support.Authentication
 * @see org.springframework.data.gemfire.config.annotation.support.AutoConfiguredAuthenticationInitializer
 * @see org.springframework.data.gemfire.config.support.RestTemplateConfigurer
 * @see org.springframework.data.gemfire.util.PropertiesBuilder
 * @see org.springframework.http.client.ClientHttpRequestInterceptor
 * @since 2.0.0
 */
@Configuration
@EnableBeanFactoryLocator
@Conditional(AutoConfiguredAuthenticationConfiguration.AuthenticationAutoConfigurationEnabledCondition.class)
@SuppressWarnings("unused")
public class AutoConfiguredAuthenticationConfiguration {

	private static final char[] EMPTY_CHAR_ARRAY = {};

	protected static final String AUTO_CONFIGURED_AUTH_INIT_STATIC_FACTORY_METHOD =
		AutoConfiguredAuthenticationInitializer.class.getName().concat(".newAuthenticationInitializer");

	protected static final String DEFAULT_USERNAME = "test";
	protected static final String DEFAULT_PASSWORD = DEFAULT_USERNAME;
	protected static final String HTTP_PROTOCOL = "HTTP";
	protected static final String PROPERTY_SOURCE_NAME = AutoConfiguredAuthenticationConfiguration.class.getName();
	protected static final String SECURITY_CLIENT_AUTH_INIT = GemFireProperties.SECURITY_CLIENT_AUTH_INIT.getName();
	protected static final String SECURITY_PEER_AUTH_INIT = GemFireProperties.SECURITY_PEER_AUTH_INIT.getName();
	protected static final String SECURITY_USERNAME = AutoConfiguredAuthenticationInitializer.SECURITY_USERNAME_PROPERTY;
	protected static final String SECURITY_PASSWORD = AutoConfiguredAuthenticationInitializer.SECURITY_PASSWORD_PROPERTY;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected @NonNull Logger getLogger() {
		return this.logger;
	}

	protected void logDebug(String message, Object... args) {

		Logger logger = getLogger();

		if (logger.isDebugEnabled()) {
			logger.debug(message, args);
		}
	}

	@Bean("GemFireSecurityAuthenticator")
	public @Nullable Authenticator authenticator(
			@Autowired(required = false) @Lazy Authentication<String, String> authentication) {

		return Optional.ofNullable(authentication)
			.filter(Authentication::isRequested)
			.map(this::newAuthenticator)
			.map(this::registerAuthenticator)
			.orElse(null);
	}

	private @NonNull Authenticator newAuthenticator(@NonNull Authentication<String, String> authentication) {

		Assert.notNull(authentication, "Authentication must not be null");
		Assert.state(authentication.isRequested(), "Authentication was not requested");

		return new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {

				String username = authentication.getPrincipal();
				String password = authentication.getCredentials();

				return new PasswordAuthentication(username, password.toCharArray());
			}
		};
	}

	private @NonNull Authenticator registerAuthenticator(@NonNull Authenticator authenticator) {

		if (authenticator != null) {
			Authenticator.setDefault(authenticator);
		}

		return authenticator;
	}

	@NonNull ClientHttpRequestInterceptor loggingAwareClientHttpRequestInterceptor() {

		return (request, body, execution) -> {

			logDebug("HTTP Request URI [{}]", request.getURI());

			HttpHeaders httpHeaders = request.getHeaders();

			CollectionUtils.nullSafeSet(httpHeaders.keySet()).forEach(httpHeaderName ->
				logDebug("HTTP Request Header Name [{}] Value [{}]",
					httpHeaderName, httpHeaders.get(httpHeaderName)));

			ClientHttpResponse response = execution.execute(request, body);

			try {
				logDebug("HTTP Response Status Code [{}] Message [{}]",
					response.getStatusCode().value(), response.getStatusText());
			}
			catch (IOException cause) {
				logDebug("Error occurred getting HTTP Response Status Code and Message", cause);
			}

			return response;
		};
	}

	@Bean
	public RestTemplateConfigurer loggingAwareRestTemplateConfigurer() {
		return restTemplate -> restTemplate.getInterceptors().add(loggingAwareClientHttpRequestInterceptor());
	}

	@NonNull ClientHttpRequestInterceptor securityAwareClientHttpRequestInterceptor() {

		return (request, body, execution) -> {

			URI uri = request.getURI();

			PasswordAuthentication passwordAuthentication =
				Authenticator.requestPasswordAuthentication(uri.getHost(), null, uri.getPort(),
					HTTP_PROTOCOL, null, uri.getScheme());

			if (passwordAuthentication != null) {

				String username = passwordAuthentication.getUserName();
				char[] password = passwordAuthentication.getPassword();

				if (isAuthenticationCredentialsSet(username, password)) {

					HttpHeaders requestHeaders = request.getHeaders();

					requestHeaders.add(SECURITY_USERNAME, username);
					requestHeaders.add(SECURITY_PASSWORD, String.valueOf(password));
				}
			}

			return execution.execute(request, body);
		};
	}

	@Bean
	public RestTemplateConfigurer securityAwareRestTemplateConfigurer() {
		return restTemplate -> restTemplate.getInterceptors().add(securityAwareClientHttpRequestInterceptor());
	}

	private boolean isAuthenticationCredentialsSet(String username, char[] password) {
		return StringUtils.hasText(username) && nullSafeCharArray(password).length > 0;
	}

	private @NonNull char[] nullSafeCharArray(@Nullable char[] array) {
		return array != null ? array : EMPTY_CHAR_ARRAY;
	}

	@Bean
	public ClientCacheConfigurer authenticationInitializingClientCacheConfigurer(
			@Autowired(required = false) @Lazy Authentication<String, String> authentication) {

		return (beanName, clientCacheFactoryBean) ->
			initializeMemberAuthentication(clientCacheFactoryBean.getProperties(), authentication);
	}

	@Bean
	public LocatorConfigurer authenticationInitializingLocatorConfigurer(
			@Autowired(required = false) @Lazy Authentication<String, String> authentication) {

		return (beanName, locatorFactoryBean) ->
			initializeMemberAuthentication(locatorFactoryBean.getGemFireProperties(), authentication);
	}

	@Bean
	public PeerCacheConfigurer authenticationInitializingPeerCacheConfigurer(
			@Autowired(required = false) @Lazy Authentication<String, String> authentication) {

		return (beanName, cacheFactoryBean) ->
			initializeMemberAuthentication(cacheFactoryBean.getProperties(), authentication);
	}

	private void initializeMemberAuthentication(Properties gemfireProperties,
			@Nullable Authentication<String, String> authentication) {

		Optional.ofNullable(gemfireProperties)
			.filter(properties -> isAuthenticationRequested(authentication))
			.ifPresent(properties -> {
				properties.setProperty(SECURITY_CLIENT_AUTH_INIT, AUTO_CONFIGURED_AUTH_INIT_STATIC_FACTORY_METHOD);
				properties.setProperty(SECURITY_PEER_AUTH_INIT, AUTO_CONFIGURED_AUTH_INIT_STATIC_FACTORY_METHOD);
			});
	}

	private boolean isAuthenticationRequested(@Nullable Authentication<?, ?> authentication) {
		return authentication != null && authentication.isRequested();
	}

	public static class AuthenticationAutoConfigurationEnabledCondition implements ConfigurationCondition {

		public static final boolean DEFAULT_ENABLED = true;

		public static final String SECURITY_AUTH_AUTO_CONFIGURATION_ENABLED =
			"spring.data.gemfire.security.auth.auto-configuration-enabled";

		private static boolean isEnabled(@NonNull Environment environment) {
			return environment.getProperty(SECURITY_AUTH_AUTO_CONFIGURATION_ENABLED, Boolean.class, DEFAULT_ENABLED);
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}

		@Override
		public boolean matches(@NonNull ConditionContext conditionContext,
			@NonNull AnnotatedTypeMetadata annotatedTypeMetadata) {

			return isEnabled(conditionContext.getEnvironment());
		}
	}
}
