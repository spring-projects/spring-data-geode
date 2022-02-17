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
package org.springframework.data.gemfire.wan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.apache.geode.cache.wan.GatewaySender;

/**
 * Unit Tests for {@link OrderPolicyConverter}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.apache.geode.cache.util.Gateway.OrderPolicy
 * @see org.springframework.data.gemfire.wan.OrderPolicyConverter
 * @since 1.7.0
 */
public class OrderPolicyConverterUnitTests {

	private final OrderPolicyConverter converter = new OrderPolicyConverter();

	@After
	public void tearDown() {
		converter.setValue(null);
	}

	@Test
	public void convert() {

		assertThat(converter.convert("key")).isEqualTo(GatewaySender.OrderPolicy.KEY);
		assertThat(converter.convert("Partition")).isEqualTo(GatewaySender.OrderPolicy.PARTITION);
		assertThat(converter.convert("THREAD")).isEqualTo(GatewaySender.OrderPolicy.THREAD);
	}

	@Test(expected = IllegalArgumentException.class)
	public void convertIllegalValue() {

		try {
			converter.convert("process");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[process] is not a valid OrderPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAsText() {
		assertThat(converter.getValue()).isNull();
		converter.setAsText("PartItIOn");
		assertThat(converter.getValue()).isEqualTo(GatewaySender.OrderPolicy.PARTITION);
		converter.setAsText("thREAD");
		assertThat(converter.getValue()).isEqualTo(GatewaySender.OrderPolicy.THREAD);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setAsTextWithIllegalValue() {

		try {
			converter.setAsText("value");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("[value] is not a valid OrderPolicy");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}
}
