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

package org.springframework.data.gemfire.config.xml;

import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeList;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean;
import org.springframework.util.xml.DomUtils;

/**
 * Bean definition parser for the &lt;gfe-data:snapshot-service&gt; SDG XML namespace (XSD) element.
 *
 * @author John Blum
 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser
 * @see org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean
 * @since 1.7.0
 */
class SnapshotServiceParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Class<?> getBeanClass(final Element element) {
		return SnapshotServiceFactoryBean.class;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		ParsingUtils.setCacheReference(element, builder);
		ParsingUtils.setRegionReference(element, builder);
		ParsingUtils.setPropertyValue(element, builder, "suppress-import-on-init");
		builder.addPropertyValue("exports", parseExports(element, parserContext));
		builder.addPropertyValue("imports", parseImports(element, parserContext));
	}

	/* (non-Javadoc) */
	private ManagedList<BeanDefinition> parseExports(Element element, ParserContext parserContext) {
		return parseSnapshots(element, parserContext, "snapshot-export");
	}

	/* (non-Javadoc) */
	private ManagedList<BeanDefinition> parseImports(Element element, ParserContext parserContext) {
		return parseSnapshots(element, parserContext, "snapshot-import");
	}

	/* (non-Javadoc) */
	private ManagedList<BeanDefinition> parseSnapshots(Element element, ParserContext parserContext,
			String childTagName) {

		ManagedList<BeanDefinition> snapshotBeans = new ManagedList<>();

		nullSafeList(DomUtils.getChildElementsByTagName(element, childTagName)).forEach(childElement ->
			snapshotBeans.add(parseSnapshotMetadata(childElement, parserContext)));

		return snapshotBeans;
	}

	/* (non-Javadoc) */
	private BeanDefinition parseSnapshotMetadata(Element snapshotMetadataElement, ParserContext parserContext) {

		BeanDefinitionBuilder snapshotMetadataBuilder =
			BeanDefinitionBuilder.genericBeanDefinition(SnapshotServiceFactoryBean.SnapshotMetadata.class);

		snapshotMetadataBuilder.addConstructorArgValue(snapshotMetadataElement.getAttribute("location"));

		snapshotMetadataBuilder.addConstructorArgValue(snapshotMetadataElement.getAttribute("format"));

		if (isSnapshotFilterSpecified(snapshotMetadataElement)) {
			snapshotMetadataBuilder.addConstructorArgValue(ParsingUtils.parseRefOrNestedBeanDeclaration(
				snapshotMetadataElement, parserContext, snapshotMetadataBuilder, "filter-ref",
					true));
		}

		ParsingUtils.setPropertyValue(snapshotMetadataElement, snapshotMetadataBuilder, "invokeCallbacks");
		ParsingUtils.setPropertyValue(snapshotMetadataElement, snapshotMetadataBuilder, "parallel");

		return snapshotMetadataBuilder.getBeanDefinition();
	}

	/* (non-Javadoc) */
	private boolean isSnapshotFilterSpecified(final Element snapshotMetadataElement) {
		return (snapshotMetadataElement.hasAttribute("filter-ref") || snapshotMetadataElement.hasChildNodes());
	}
}
