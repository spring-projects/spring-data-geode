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
package org.springframework.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.RegionFactory;

/**
 * Unit Tests for {@link ReplicatedRegionFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.DataPolicy
 * @see org.apache.geode.cache.RegionFactory
 * @see org.springframework.data.gemfire.ReplicatedRegionFactoryBean
 * @since 1.3.3
 */
public class ReplicatedRegionFactoryBeanUnitTests {

	private final ReplicatedRegionFactoryBean<Object, Object> factoryBean = new ReplicatedRegionFactoryBean<>();

	@SuppressWarnings("unchecked")
	private RegionFactory<Object, Object> createMockRegionFactory() {
		return mock(RegionFactory.class);
	}

	@Test
	public void testResolveDataPolicyWithPersistentUnspecifiedAndDataPolicyUnspecified() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.resolveDataPolicy(mockRegionFactory, null, (String) null);

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.REPLICATE));
	}

	@Test
	public void testResolveDataPolicyWhenNotPersistentAndDataPolicyUnspecified() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.setPersistent(false);
		factoryBean.resolveDataPolicy(mockRegionFactory, false, (String) null);

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.REPLICATE));
	}

	@Test
	public void testResolveDataPolicyWhenPersistentAndDataPolicyUnspecified() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.setPersistent(true);
		factoryBean.resolveDataPolicy(mockRegionFactory, true, (String) null);

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWithBlankDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.resolveDataPolicy(mockRegionFactory, null, "  ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [  ] is invalid.");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.NORMAL));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PRELOADED));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWithEmptyDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.resolveDataPolicy(mockRegionFactory, null, "");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [] is invalid.");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.NORMAL));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PRELOADED));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWhenPersistentUnspecifiedAndInvalidDataPolicyName() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.resolveDataPolicy(mockRegionFactory, null, "INVALID_DATA_POLICY_NAME");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [INVALID_DATA_POLICY_NAME] is invalid.");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.EMPTY));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.REPLICATE));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWhenPersistentUnspecifiedAndInvalidDataPolicyType() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.resolveDataPolicy(mockRegionFactory, null, "PARTITION");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [PARTITION] is not supported in Replicated Regions.");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PARTITION));
		}
	}

	@Test
	public void testResolveDataPolicyWhenPersistentUnspecifiedAndEmptyDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.resolveDataPolicy(mockRegionFactory, null, "EMPTY");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.EMPTY));
	}

	@Test
	public void testResolveDataPolicyWhenNotPersistentAndEmptyDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.setPersistent(false);
		factoryBean.resolveDataPolicy(mockRegionFactory, false, "empty");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.EMPTY));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWhenPersistentAndEmptyDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.setPersistent(true);
			factoryBean.resolveDataPolicy(mockRegionFactory, true, "empty");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [EMPTY] is not valid when persistent is true");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.EMPTY));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
		}
	}

	@Test
	public void testResolveDataPolicyWhenPersistentUnspecifiedAndReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.resolveDataPolicy(mockRegionFactory, null, "REPLICATE");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.REPLICATE));
	}

	@Test
	public void testResolveDataPolicyWhenNotPersistentAndReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.setPersistent(false);
		factoryBean.resolveDataPolicy(mockRegionFactory, false, "REPLICATE");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.REPLICATE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWhenPersistentAndReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.setPersistent(true);
			factoryBean.resolveDataPolicy(mockRegionFactory, true, "REPLICATE");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Data Policy [REPLICATE] is not valid when persistent is true");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.REPLICATE));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
		}
	}

	@Test
	public void testResolveDataPolicyWhenPersistentUnspecifiedAndPersistentReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.resolveDataPolicy(mockRegionFactory, null, "PERSISTENT_REPLICATE");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveDataPolicyWhenNotPersistentAndPersistentReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		try {
			factoryBean.setPersistent(false);
			factoryBean.resolveDataPolicy(mockRegionFactory, false, "PERSISTENT_REPLICATE");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
				.isEqualTo("Data Policy [PERSISTENT_REPLICATE] is not valid when persistent is false");
			throw e;
		}
		finally {
			verify(mockRegionFactory, never()).setDataPolicy(null);
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.REPLICATE));
			verify(mockRegionFactory, never()).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
		}
	}

	@Test
	public void testResolveDataPolicyWhenPersistentAndPersistentReplicateDataPolicy() {

		RegionFactory<Object, Object> mockRegionFactory = createMockRegionFactory();

		factoryBean.setPersistent(true);
		factoryBean.resolveDataPolicy(mockRegionFactory, true, "PERSISTENT_REPLICATE");

		verify(mockRegionFactory).setDataPolicy(eq(DataPolicy.PERSISTENT_REPLICATE));
	}
}
