/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.data.gemfire.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.junit.Test;

/**
 * Test suite of test cases testing the contract and functionality of the {@link PropertiesBuilder} class.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.springframework.data.gemfire.util.PropertiesBuilder
 * @since 1.9.0
 */
public class PropertiesBuilderTests {

	private Properties singletonProperties(String name, String value) {
		Properties properties = new Properties();
		properties.setProperty(name, value);
		return properties;
	}

	@Test
	public void constructDefaultPropertiesBuilder() throws Exception {

		PropertiesBuilder builder = new PropertiesBuilder();

		Properties properties = builder.build();

		assertThat(properties).isNotNull();
		assertThat(builder.getObject()).isSameAs(properties);
		assertThat(builder.getObjectType()).isEqualTo(Properties.class);
		assertThat(properties.isEmpty()).isTrue();
	}

	@Test
	public void constructPropertiesBuilderWithDefaultProperties() {

		Properties defaults = singletonProperties("one", "1");

		PropertiesBuilder builder = new PropertiesBuilder(defaults);

		Properties properties = builder.build();

		assertThat(properties).isNotNull();
		assertThat(properties).isNotSameAs(defaults);
		assertThat(properties).isEqualTo(defaults);
	}

	@Test
	public void constructPropertiesBuilderWithPropertiesBuilder() {

		PropertiesBuilder defaults = new PropertiesBuilder().setProperty("one", "1");
		PropertiesBuilder builder = new PropertiesBuilder(defaults);

		Properties properties = builder.build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(1);
		assertThat(properties.containsKey("one")).isTrue();
		assertThat(properties.getProperty("one")).isEqualTo("1");
	}

	@Test
	public void fromInputStreamIsSuccessful() throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Properties source = singletonProperties("one", "1");

		source.store(out, "fromInputStreamIsSuccessfulTest");

		Properties sink = PropertiesBuilder.from(new ByteArrayInputStream(out.toByteArray())).build();

		assertThat(sink).isNotNull();
		assertThat(sink).isNotSameAs(source);
		assertThat(sink).isEqualTo(source);
	}

	@Test
	public void fromReaderIsSuccessful() throws IOException {

		StringWriter writer = new StringWriter();

		Properties source = singletonProperties("one", "1");

		source.store(writer, "fromReaderIsSuccessfulTest");

		Properties sink = PropertiesBuilder.from(new StringReader(writer.toString())).build();

		assertThat(sink).isNotNull();
		assertThat(sink).isNotSameAs(source);
		assertThat(sink).isEqualTo(source);
	}

	@Test
	public void fromXmlInputStreamIsSuccessful() throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Properties source = singletonProperties("one", "1");

		source.storeToXML(out, "fromXmlInputStreamIsSuccessfulTest");

		Properties sink = PropertiesBuilder.fromXml(new ByteArrayInputStream(out.toByteArray())).build();

		assertThat(sink).isNotNull();
		assertThat(sink).isNotSameAs(source);
		assertThat(sink).isEqualTo(source);
	}

	@Test
	public void propertiesBuilderObjectTypeIsPropertiesClass() {
		assertThat(PropertiesBuilder.create().getObjectType()).isEqualTo(Properties.class);
	}

	@Test
	public void propertiesBuilderIsSingletonIsTrue() {
		assertThat(new PropertiesBuilder().isSingleton()).isTrue();
	}

	@Test
	public void addPropertiesFromPropertiesIsSuccessful() {

		PropertiesBuilder builder = PropertiesBuilder.create()
			.setProperty("one", "1")
			.setProperty("two", "@");

		Properties sink = builder.build();

		assertThat(sink).isNotNull();
		assertThat(sink.size()).isEqualTo(2);
		assertThat(sink.containsKey("one")).isTrue();
		assertThat(sink.containsKey("two")).isTrue();
		assertThat(sink.containsKey("three")).isFalse();
		assertThat(sink.getProperty("one")).isEqualTo("1");
		assertThat(sink.getProperty("two")).isEqualTo("@");

		Properties source = new Properties();

		source.setProperty("two", "2");
		source.setProperty("three", "3");

		builder.add(source);

		sink = builder.build();

		assertThat(sink).isNotNull();
		assertThat(sink.size()).isEqualTo(3);
		assertThat(sink).isNotSameAs(source);
		assertThat(sink.containsKey("one")).isTrue();
		assertThat(sink.containsKey("two")).isTrue();
		assertThat(sink.containsKey("three")).isTrue();
		assertThat(sink.getProperty("one")).isEqualTo("1");
		assertThat(sink.getProperty("two")).isEqualTo("2");
		assertThat(sink.getProperty("three")).isEqualTo("3");
	}

	@Test
	public void addPropertiesFromPropertiesBuilderIsSuccessful() {

		PropertiesBuilder source = PropertiesBuilder.create()
			.setProperty("one", "1")
			.setProperty("two", "2");

		Properties properties = PropertiesBuilder.create().add(source).build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(2);
		assertThat(properties.containsKey("one")).isTrue();
		assertThat(properties.containsKey("two")).isTrue();
		assertThat(properties.getProperty("one")).isEqualTo("1");
		assertThat(properties.getProperty("two")).isEqualTo("2");
	}

	@Test
	public void setObjectPropertyValuesIsSuccessful() {

		Properties properties = PropertiesBuilder.create()
			.setProperty("boolean", Boolean.TRUE)
			.setProperty("character", 'A')
			.setProperty("integer", 1)
			.setProperty("double", Math.PI)
			.setProperty("string", (Object) "test")
			.build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(5);
		assertThat(properties.getProperty("boolean")).isEqualTo(Boolean.TRUE.toString());
		assertThat(properties.getProperty("character")).isEqualTo("A");
		assertThat(properties.getProperty("integer")).isEqualTo("1");
		assertThat(properties.getProperty("double")).isEqualTo(String.valueOf(Math.PI));
		assertThat(properties.getProperty("string")).isEqualTo("test");
	}

	@Test
	public void setObjectArrayPropertyValueIsSuccessful() {

		Properties properties = PropertiesBuilder.create()
			.setProperty("numbers", new Object[] { "one", "two", "three" })
			.build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(1);
		assertThat(properties.containsKey("numbers")).isTrue();
		assertThat(properties.getProperty("numbers")).isEqualTo("one,two,three");
	}

	@Test
	public void setPropertyIgnoresNullObjectValue() {

		Properties properties = PropertiesBuilder.create().setProperty("object", (Object) null).build();

		assertThat(properties).isNotNull();
		assertThat(properties.isEmpty()).isTrue();
	}

	@Test
	public void setPropertyIgnoresEmptyAndNullLiteralStringValues() {

		Properties properties = PropertiesBuilder.create()
			.setProperty("blank", "  ")
			.setProperty("empty", "")
			.setProperty("null", "null")
			.setProperty("nullWithWhiteSpace", " null  ")
			.build();

		assertThat(properties).isNotNull();
		assertThat(properties.isEmpty()).isTrue();
	}

	@Test
	public void setPropertyIgnoresEmptyObjectArray() {

		Properties properties = PropertiesBuilder.create().setProperty("emptyArray", new Object[0]).build();

		assertThat(properties).isNotNull();
		assertThat(properties.isEmpty()).isTrue();
	}

	@Test
	public void setPropertyIgnoresNullObjectArray() {

		Properties properties = PropertiesBuilder.create().setProperty("nullArray", (Object[]) null).build();

		assertThat(properties).isNotNull();
		assertThat(properties.isEmpty()).isTrue();
	}

	@Test
	public void setStringPropertyValuesIsSuccessful() {

		Properties properties = PropertiesBuilder.create()
			.setProperty("one", "1")
			.setProperty("two", "2")
			.build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(2);
		assertThat(properties.containsKey("one")).isTrue();
		assertThat(properties.containsKey("two")).isTrue();
		assertThat(properties.getProperty("one")).isEqualTo("1");
		assertThat(properties.getProperty("two")).isEqualTo("2");
	}

	@Test
	public void unsetPropertyIsSuccessful() {

		Properties properties = PropertiesBuilder.create().unsetProperty("example").build();

		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(1);
		assertThat(properties.containsKey("example")).isTrue();
		assertThat(properties.getProperty("example")).isEqualTo("");
	}

	@Test
	public void stringLiteralIsValuable() {
		assertThat(PropertiesBuilder.create().isValuable("test")).isTrue();
	}

	@Test
	public void nullStringLiteralIsNotValuable() {

		assertThat(PropertiesBuilder.create().isValuable("null")).isFalse();
		assertThat(PropertiesBuilder.create().isValuable("Null")).isFalse();
		assertThat(PropertiesBuilder.create().isValuable("NULL")).isFalse();
	}

	@Test
	public void emptyStringLiteralIsNotValuable() {

		assertThat(PropertiesBuilder.create().isValuable("  ")).isFalse();
		assertThat(PropertiesBuilder.create().isValuable("")).isFalse();
	}
}
