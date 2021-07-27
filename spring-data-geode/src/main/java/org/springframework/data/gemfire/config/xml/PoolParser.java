/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire.config.xml;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.support.ClientRegionPoolBeanFactoryPostProcessor;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.support.ConnectionEndpointList;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

import org.w3c.dom.Element;

/**
 * Spring {@link BeanDefinition} parser for &lt;gfe:pool&gt; SDG XML Namespace (XSD), schema element.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser
 * @see org.springframework.data.gemfire.client.PoolFactoryBean
 */
class PoolParser extends AbstractSingleBeanDefinitionParser {

	static final AtomicBoolean INFRASTRUCTURE_COMPONENTS_REGISTERED = new AtomicBoolean(false);

	static final int DEFAULT_LOCATOR_PORT = GemfireUtils.DEFAULT_LOCATOR_PORT;
	static final int DEFAULT_SERVER_PORT = GemfireUtils.DEFAULT_CACHE_SERVER_PORT;

	static final String DEFAULT_HOST = "localhost";
	static final String HOST_ATTRIBUTE_NAME = "host";
	static final String LOCATOR_ELEMENT_NAME = "locator";
	static final String LOCATORS_ATTRIBUTE_NAME = "locators";
	static final String PORT_ATTRIBUTE_NAME = "port";
	static final String SERVER_ELEMENT_NAME = "server";
	static final String SERVERS_ATTRIBUTE_NAME = "servers";

	private static void registerInfrastructureComponents(ParserContext parserContext) {

		if (INFRASTRUCTURE_COMPONENTS_REGISTERED.compareAndSet(false, true)) {

			// TODO: Be careful not to register this infrastructure component just yet (requires more thought).
			/*
			BeanDefinitionReaderUtils.registerWithGeneratedName(
				BeanDefinitionBuilder.rootBeanDefinition(ClientCachePoolBeanFactoryPostProcessor.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition(), parserContext.getRegistry());
			*/

			BeanDefinitionReaderUtils.registerWithGeneratedName(
				BeanDefinitionBuilder.rootBeanDefinition(ClientRegionPoolBeanFactoryPostProcessor.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition(), parserContext.getRegistry());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Class<?> getBeanClass(Element element) {
		return PoolFactoryBean.class;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder poolBuilder) {

		registerInfrastructureComponents(parserContext);

		ParsingUtils.setPropertyValue(element, poolBuilder, "free-connection-timeout");
		ParsingUtils.setPropertyValue(element, poolBuilder, "idle-timeout");
		ParsingUtils.setPropertyValue(element, poolBuilder, "keep-alive");
		ParsingUtils.setPropertyValue(element, poolBuilder, "load-conditioning-interval");
		ParsingUtils.setPropertyValue(element, poolBuilder, "max-connections");
		ParsingUtils.setPropertyValue(element, poolBuilder, "min-connections");
		ParsingUtils.setPropertyValue(element, poolBuilder, "multi-user-authentication");
		ParsingUtils.setPropertyValue(element, poolBuilder, "ping-interval");
		ParsingUtils.setPropertyValue(element, poolBuilder, "pr-single-hop-enabled");
		ParsingUtils.setPropertyValue(element, poolBuilder, "read-timeout");
		ParsingUtils.setPropertyValue(element, poolBuilder, "retry-attempts");
		ParsingUtils.setPropertyValue(element, poolBuilder, "server-group");
		ParsingUtils.setPropertyValue(element, poolBuilder, "socket-buffer-size");
		ParsingUtils.setPropertyValue(element, poolBuilder, "socket-connect-timeout");
		ParsingUtils.setPropertyValue(element, poolBuilder, "statistic-interval");
		ParsingUtils.setPropertyValue(element, poolBuilder, "subscription-ack-interval");
		ParsingUtils.setPropertyValue(element, poolBuilder, "subscription-enabled");
		ParsingUtils.setPropertyValue(element, poolBuilder, "subscription-message-tracking-timeout");
		ParsingUtils.setPropertyValue(element, poolBuilder, "subscription-redundancy");
		ParsingUtils.setPropertyValue(element, poolBuilder, "subscription-timeout-multiplier");
		ParsingUtils.setPropertyValue(element, poolBuilder, "thread-local-connections");

		List<Element> childElements = DomUtils.getChildElements(element);

		ManagedList<BeanDefinition> locators = new ManagedList<>(childElements.size());
		ManagedList<BeanDefinition> servers = new ManagedList<>(childElements.size());

		CollectionUtils.nullSafeList(childElements).forEach(childElement -> {

			String childElementName = childElement.getLocalName();

			if (LOCATOR_ELEMENT_NAME.equals(childElementName)) {
				locators.add(parseLocator(childElement, parserContext));
			}

			if (SERVER_ELEMENT_NAME.equals(childElementName)) {
				servers.add(parseServer(childElement, parserContext));
			}
		});

		boolean hasLocators = parseLocators(element, parserContext, poolBuilder);
		boolean hasServers = parseServers(element, parserContext, poolBuilder);

		boolean noLocatorsOrServers = locators.isEmpty() && servers.isEmpty() && !hasLocators && !hasServers;

		// If neither Locators nor Servers were explicitly configured, then setup a connection to a CacheServer
		// running on localhost, listening on the default CacheServer port 40404.
		if (noLocatorsOrServers) {
			servers.add(buildConnection(DEFAULT_HOST, String.valueOf(DEFAULT_SERVER_PORT), true));
		}

		if (!locators.isEmpty()) {
			poolBuilder.addPropertyValue("locators", locators);
		}

		if (!servers.isEmpty()) {
			poolBuilder.addPropertyValue("servers", servers);
		}
	}

	BeanDefinition buildConnection(String host, String port, boolean server) {

		BeanDefinitionBuilder connectionEndpointBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(ConnectionEndpoint.class);

		connectionEndpointBuilder.addConstructorArgValue(defaultHost(host));
		connectionEndpointBuilder.addConstructorArgValue(defaultPort(port, server));

		return connectionEndpointBuilder.getBeanDefinition();
	}

	BeanDefinition buildConnections(String expression, boolean server) {

		BeanDefinitionBuilder connectionEndpointListBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(ConnectionEndpointList.class);

		connectionEndpointListBuilder.setFactoryMethod("parse");
		connectionEndpointListBuilder.addConstructorArgValue(defaultPort(null, server));
		connectionEndpointListBuilder.addConstructorArgValue(expression);

		return connectionEndpointListBuilder.getBeanDefinition();
	}

	String defaultHost(String host) {
		return StringUtils.hasText(host) ? host : DEFAULT_HOST;
	}

	int defaultPort(boolean server) {
		return server ? DEFAULT_SERVER_PORT : DEFAULT_LOCATOR_PORT;
	}

	String defaultPort(String port, boolean server) {
		return StringUtils.hasText(port) ? port : String.valueOf(defaultPort(server));
	}

	@SuppressWarnings("unused")
	BeanDefinition parseLocator(Element element, ParserContext parserContext) {

		return buildConnection(element.getAttribute(HOST_ATTRIBUTE_NAME),
			element.getAttribute(PORT_ATTRIBUTE_NAME), false);
	}

	@SuppressWarnings("unused")
	boolean parseLocators(Element element, ParserContext parserContext, BeanDefinitionBuilder poolBuilder) {

		String locatorsAttributeValue = element.getAttribute(LOCATORS_ATTRIBUTE_NAME);

		if (StringUtils.hasText(locatorsAttributeValue)) {

			poolBuilder.addPropertyValue("xmlDeclaredLocators",
				buildConnections(locatorsAttributeValue, false));

			return true;
		}

		return false;
	}

	@SuppressWarnings("unused")
	BeanDefinition parseServer(Element element, ParserContext parserContext) {

		return buildConnection(element.getAttribute(HOST_ATTRIBUTE_NAME),
			element.getAttribute(PORT_ATTRIBUTE_NAME), true);
	}

	@SuppressWarnings("unused")
	boolean parseServers(Element element, ParserContext parserContext, BeanDefinitionBuilder poolBuilder) {

		String serversAttributeValue = element.getAttribute(SERVERS_ATTRIBUTE_NAME);

		if (StringUtils.hasText(serversAttributeValue)) {

			poolBuilder.addPropertyValue("xmlDeclaredServers",
				buildConnections(serversAttributeValue, true));

			return true;
		}

		return false;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = super.resolveId(element, definition, parserContext);

		if (!StringUtils.hasText(id)) {
			id = GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME;
			parserContext.getRegistry().registerAlias(GemfireConstants.DEFAULT_GEMFIRE_POOL_NAME, "gemfire-pool");
		}

		return id;
	}
}
