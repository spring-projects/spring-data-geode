/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.data.gemfire.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newIllegalArgumentException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;

import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.support.ConnectionEndpointList;
import org.springframework.data.gemfire.test.support.MapBuilder;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.mock.GemFireMockObjectsSupport;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the use of property placeholders in nested &lt;gfe:locator&gt; and &lt;gfe:server&gt;
 * elements of the SDG XML Namespace &lt;gfe:pool&gt; element along with testing property placeholders
 * in the &lt;gfe:pool&gt; element <code>locators</code> and <code>servers</code> attributes.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolFactory
 * @see org.springframework.data.gemfire.client.PoolFactoryBean
 * @see org.springframework.data.gemfire.config.xml.PoolParser
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see <a href="https://jira.spring.io/browse/SGF-433">SGF-433</a>
 * @since 1.6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SpELExpressionConfiguredPoolsIntegrationTests extends IntegrationTestsSupport {

	private static final ConnectionEndpointList locatorsOne = new ConnectionEndpointList();
	private static final ConnectionEndpointList locatorsTwo = new ConnectionEndpointList();
	private static final ConnectionEndpointList serversOne = new ConnectionEndpointList();
	private static final ConnectionEndpointList serversTwo = new ConnectionEndpointList();

	private static final Map<String, ConnectionEndpointList> poolToConnectionsMap =
		Collections.unmodifiableMap(MapBuilder.<String, ConnectionEndpointList>newMapBuilder()
			.put("locatorPoolOne", locatorsOne)
			.put("locatorPoolTwo", locatorsTwo)
			.put("serverPoolOne", serversOne)
			.put("serverPoolTwo", serversTwo)
			.build());

	@Autowired
	@Qualifier("locatorPoolOne")
	private Pool locatorPoolOne;

	@Autowired
	@Qualifier("locatorPoolTwo")
	private Pool locatorPoolTwo;

	@Autowired
	@Qualifier("serverPoolOne")
	private Pool serverPoolOne;

	@Autowired
	@Qualifier("serverPoolTwo")
	private Pool serverPoolTwo;

	private static void assertConnectionEndpoints(ConnectionEndpointList connectionEndpoints,
			String... expected) {

		assertThat(connectionEndpoints).isNotNull();
		assertThat(connectionEndpoints.size()).isEqualTo(expected.length);

		Collections.sort(connectionEndpoints);

		int index = 0;

		for (ConnectionEndpoint connectionEndpoint : connectionEndpoints) {
			assertThat(connectionEndpoint.toString()).isEqualTo(expected[index++]);
		}

		assertThat(index).isEqualTo(expected.length);
	}

	private static ConnectionEndpoint newConnectionEndpoint(String host, int port) {
		return new ConnectionEndpoint(host, port);
	}

	@Test
	public void locatorPoolOneFactoryConfiguration() {

		String[] expected = { "backspace[10334]", "jambox[11235]", "mars[30303]", "pluto[20668]", "skullbox[12480]" };

		assertConnectionEndpoints(locatorsOne, expected);
	}

	@Test
	public void locatorPoolTwoFactoryConfiguration() {

		String[] expected = { "cardboardbox[10334]", "localhost[10335]", "pobox[10334]", "safetydepositbox[10336]" };

		assertConnectionEndpoints(locatorsTwo, expected);
	}

	@Test
	public void serverPoolOneFactoryConfiguration() {

		String[] expected = {
			"earth[4554]", "jupiter[40404]", "mars[5112]", "mercury[1234]",
			"neptune[42424]", "saturn[41414]", "uranis[0]", "venus[9876]"
		};

		assertConnectionEndpoints(serversOne, expected);
	}

	@Test
	public void serverPoolTwoFactoryConfiguration() {

		String[] expected = { "boombox[1234]", "jambox[40404]", "toolbox[8181]" };

		assertConnectionEndpoints(serversTwo, expected);
	}

	public static class SpELBoundBean {

		private final Properties clientProperties;

		public SpELBoundBean(Properties clientProperties) {
			this.clientProperties = Optional.ofNullable(clientProperties)
				.orElseThrow(() -> newIllegalArgumentException("clientProperties are required"));
		}

		public String locatorsHostsPorts() {
			return "safetydepositbox[10336], pobox";
		}

		public String serverTwoHost() {
			return this.clientProperties.getProperty("gemfire.cache.client.server.2.host");
		}

		public String serverTwoPort() {
			return this.clientProperties.getProperty("gemfire.cache.client.server.2.port");
		}
	}

	public static class TestBeanPostProcessor implements BeanPostProcessor {

		@Nullable @Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

			if (isPoolFactoryBean(bean, beanName)) {

				PoolFactoryBean poolFactoryBeanSpy = spy((PoolFactoryBean) bean);

				doReturn(true).when(poolFactoryBeanSpy).isClientCachePresent();

				doAnswer(invocation -> {

					ConnectionEndpointList list = poolToConnectionsMap.get(beanName);

					PoolFactory mockPoolFactory = GemFireMockObjectsSupport.mockPoolFactory();

					when(mockPoolFactory.addLocator(anyString(), anyInt())).thenAnswer(newAnswer(mockPoolFactory,
						connectionEndpoint -> list.add(connectionEndpoint)));

					when(mockPoolFactory.addServer(anyString(), anyInt())).thenAnswer(newAnswer(mockPoolFactory,
						connectionEndpoint -> list.add(connectionEndpoint)));

					return mockPoolFactory;

				}).when(poolFactoryBeanSpy).createPoolFactory();

				bean = poolFactoryBeanSpy;
			}

			return bean;
		}

		private boolean isPoolFactoryBean(Object bean, String beanName) {
			return bean instanceof PoolFactoryBean && poolToConnectionsMap.containsKey(beanName);
		}

		private Answer<PoolFactory> newAnswer(PoolFactory mockPoolFactory,
			Consumer<ConnectionEndpoint> connectionEndpointConsumer) {

			return invocation -> {

				String host = invocation.getArgument(0);
				int port = invocation.getArgument(1);

				connectionEndpointConsumer.accept(newConnectionEndpoint(host, port));

				return mockPoolFactory;
			};
		}
	}
}
