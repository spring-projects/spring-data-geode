/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.gemfire.config.admin.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.data.gemfire.config.admin.remote.RestHttpGemfireAdminTemplate.FollowRedirectsSimpleClientHttpRequestFactory;

import java.util.Arrays;

import org.apache.geode.cache.client.ClientCache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.data.gemfire.tests.util.ReflectionUtils;
import org.springframework.data.gemfire.util.NetworkUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Unit Tests for {@link RestHttpGemfireAdminTemplate.Builder}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.apache.geode.cache.client.ClientCache
 * @see org.springframework.data.gemfire.config.admin.remote.RestHttpGemfireAdminTemplate.Builder
 * @see org.springframework.http.client.ClientHttpRequestFactory
 * @see org.springframework.http.client.ClientHttpRequestInterceptor
 * @see org.springframework.web.client.RestTemplate
 * @since 2.2.0
 */
@RunWith(MockitoJUnitRunner.class)
public class RestHttpGemfireAdminTemplateBuilderUnitTests {

	@Mock
	private ClientCache mockClientCache;

	@Test
	public void buildSuccessfullyBuildsNewRestHttpGemfireAdminTemplate() throws NoSuchFieldException {

		ClientHttpRequestInterceptor mockInterceptorOne = mock(ClientHttpRequestInterceptor.class);
		ClientHttpRequestInterceptor mockInterceptorTwo = mock(ClientHttpRequestInterceptor.class);
		ClientHttpRequestInterceptor mockInterceptorThree = mock(ClientHttpRequestInterceptor.class);
		ClientHttpRequestInterceptor mockInterceptorFour = mock(ClientHttpRequestInterceptor.class);
		ClientHttpRequestInterceptor mockInterceptorFive = mock(ClientHttpRequestInterceptor.class);

		RestHttpGemfireAdminTemplate template = new RestHttpGemfireAdminTemplate.Builder()
			.with(this.mockClientCache)
			.with(mockInterceptorOne, mockInterceptorTwo)
			.with(mockInterceptorThree)
			.with(Arrays.asList(mockInterceptorFour, mockInterceptorFive))
			.using("Http")
			.on("skullbox")
			.listenOn(81)
			.followRedirects(false)
			.build();

		assertThat(template).isNotNull();
		assertThat(template.getClientCache()).isSameAs(this.mockClientCache);
		assertThat(template.getManagementRestApiUrl()).isEqualTo("http://skullbox:81/gemfire/v1");
		assertThat(template.<RestTemplate>getRestOperations().getInterceptors())
			.containsExactly(mockInterceptorOne, mockInterceptorTwo, mockInterceptorThree,
				mockInterceptorFour, mockInterceptorFive);
		assertThat(template.<RestTemplate>getRestOperations().getRequestFactory())
			.isInstanceOf(InterceptingClientHttpRequestFactory.class);

		ClientHttpRequestFactory clientHttpRequestFactory =
				ReflectionUtils.getFieldValue(template.<RestTemplate>getRestOperations().getRequestFactory(), "requestFactory");

		assertThat(clientHttpRequestFactory).isInstanceOf(FollowRedirectsSimpleClientHttpRequestFactory.class);
		assertThat(((FollowRedirectsSimpleClientHttpRequestFactory) clientHttpRequestFactory).isFollowRedirects())
			.isFalse();
	}

	private void testInvalidPortThrowsException(int port) {

		try {
			new RestHttpGemfireAdminTemplate.Builder().listenOn(port);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage(NetworkUtils.INVALID_NO_EPHEMERAL_PORT_MESSAGE, port);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void listenOnOverflowPortThrowsException() {
		testInvalidPortThrowsException(65536);
	}

	@Test(expected = IllegalArgumentException.class)
	public void listenOnUnderflowPortThrowsException() {
		testInvalidPortThrowsException(0);
	}

	private void testInvalidHostnameDefaultsToLocalhost(String hostname) {

		RestHttpGemfireAdminTemplate template = new RestHttpGemfireAdminTemplate.Builder()
			.with(this.mockClientCache)
			.on(hostname)
			.build();

		assertThat(template).isNotNull();
		assertThat(template.getManagementRestApiUrl()).isEqualTo("https://localhost/gemfire/v1");
	}

	@Test
	public void onEmptyHostnameDefaultsToLocalhost() {

		testInvalidHostnameDefaultsToLocalhost("");
		testInvalidHostnameDefaultsToLocalhost("  ");
	}

	@Test
	public void onNoHostnameDefaultsToLocalhost() {
		testInvalidHostnameDefaultsToLocalhost(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void usingInvalidScheme() {

		try {
			new RestHttpGemfireAdminTemplate.Builder().using("ftp");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Scheme [ftp] is not valid; must be 1 of %s",
				RestHttpGemfireAdminTemplate.VALID_SCHEMES);

			assertThat(expected).hasNoCause();

			throw expected;
		}
	}
}
