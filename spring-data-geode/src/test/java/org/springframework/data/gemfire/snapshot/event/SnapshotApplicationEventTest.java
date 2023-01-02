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
package org.springframework.data.gemfire.snapshot.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.SnapshotMetadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.snapshot.SnapshotOptions;

import org.springframework.data.gemfire.tests.util.FileSystemUtils;

/**
 * Unit Tests testing the contract and functionality of the {@link SnapshotApplicationEvent} class.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.data.gemfire.snapshot.event.SnapshotApplicationEvent
 * @since 1.7.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class SnapshotApplicationEventTest {

	@Mock
	@SuppressWarnings("unused")
	private Region<Object, Object> mockRegion;

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata() {
		return new SnapshotMetadata<>(FileSystemUtils.WORKING_DIRECTORY, SnapshotOptions.SnapshotFormat.GEMFIRE);
	}

	@Test
	public void constructSnapshotApplicationEventWithoutRegionOrSnapshotMetadata() {

		SnapshotApplicationEvent<?, ?> event = new TestSnapshotApplicationEvent<>(this);

		assertThat(event.getSource()).isEqualTo(this);
		assertThat(event.getRegionPath()).isNull();
		assertThat(event.getSnapshotMetadata()).isNotNull();
		assertThat(event.getSnapshotMetadata().length).isEqualTo(0);
		assertThat(event.isCacheSnapshotEvent()).isTrue();
		assertThat(event.isRegionSnapshotEvent()).isFalse();
	}

	@Test
	public void constructSnapshotApplicationEventWithRegionAndSnapshotMetadata() {

		SnapshotMetadata<?, ?> eventSnapshotMetadata = newSnapshotMetadata();

		SnapshotApplicationEvent<?, ?> event =
			new TestSnapshotApplicationEvent<>(this, "/Example", eventSnapshotMetadata);

		assertThat(event.getSource()).isEqualTo(this);
		assertThat(event.getRegionPath()).isEqualTo("/Example");
		assertThat(event.getSnapshotMetadata()[0]).isSameAs(eventSnapshotMetadata);
		assertThat(event.isCacheSnapshotEvent()).isFalse();
		assertThat(event.isRegionSnapshotEvent()).isTrue();
	}

	@Test
	public void constructSnapshotApplicationEventWithRegionButNoSnapshotMetadata() {

		SnapshotApplicationEvent<?, ?> event = new TestSnapshotApplicationEvent<>(this, "/Example");

		assertThat(event.getSource()).isEqualTo(this);
		assertThat(event.getRegionPath()).isEqualTo("/Example");
		assertThat(event.getSnapshotMetadata()).isNotNull();
		assertThat(event.getSnapshotMetadata().length).isEqualTo(0);
		assertThat(event.isCacheSnapshotEvent()).isFalse();
		assertThat(event.isRegionSnapshotEvent()).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void constructSnapshotApplicationEventWithSnapshotMetadataButNoRegion() {

		SnapshotMetadata<?, ?> eventSnapshotMetadataOne = newSnapshotMetadata();
		SnapshotMetadata<?, ?> eventSnapshotMetadataTwo = newSnapshotMetadata();

		SnapshotApplicationEvent event =
			new TestSnapshotApplicationEvent(this, eventSnapshotMetadataOne, eventSnapshotMetadataTwo);

		assertThat(event.getSource()).isEqualTo(this);
		assertThat(event.getRegionPath()).isNull();
		assertThat(event.getSnapshotMetadata()[0]).isEqualTo(eventSnapshotMetadataOne);
		assertThat(event.getSnapshotMetadata()[1]).isEqualTo(eventSnapshotMetadataTwo);
		assertThat(event.isCacheSnapshotEvent()).isTrue();
		assertThat(event.isRegionSnapshotEvent()).isFalse();
	}

	@Test
	public void matchesNullRegionIsFalse() {
		assertThat(new TestSnapshotApplicationEvent<>(this).matches((Region<?, ?>) null)).isFalse();
	}

	@Test
	public void matchesNonMatchingRegionIsFalse() {

		Region<?, ?> mockRegion = mock(Region.class, "MockRegion");

		when(mockRegion.getFullPath()).thenReturn("/Example");

		assertThat(new TestSnapshotApplicationEvent<>(this, "/Prototype").matches(mockRegion)).isFalse();

		verify(mockRegion, times(1)).getFullPath();
	}

	@Test
	public void matchesMatchingRegionIsTrue() {

		Region<?, ?> mockRegion = mock(Region.class, "MockRegion");

		when(mockRegion.getFullPath()).thenReturn("/Example");

		assertThat(new TestSnapshotApplicationEvent<>(this, "/Example").matches(mockRegion)).isTrue();

		verify(mockRegion, times(1)).getFullPath();
	}

	@Test
	public void matchesNonMatchingRegionPathsIsFalse() {

		SnapshotApplicationEvent<?, ?> event = new TestSnapshotApplicationEvent<>(this, "/Example");

		assertThat(event.getRegionPath()).isEqualTo("/Example");
		assertThat(event.matches("Example")).isFalse();
		assertThat(event.matches("/Sample")).isFalse();
		assertThat(event.matches("/Prototype")).isFalse();
		assertThat(event.matches("/example")).isFalse();
		assertThat(event.matches("/Exam")).isFalse();
		assertThat(event.matches("/Xmpl")).isFalse();
		assertThat(event.matches("/Ex.")).isFalse();
		assertThat(event.matches("/E.g.")).isFalse();
		assertThat(event.matches("/")).isFalse();
		assertThat(event.matches("  ")).isFalse();
		assertThat(event.matches("")).isFalse();
		assertThat(event.matches((String) null)).isFalse();
	}

	@Test
	public void matchesMatchingRegionPathsIsTrue() {

		assertThat(new TestSnapshotApplicationEvent<>(this, "/Example").matches("/Example")).isTrue();
		assertThat(new TestSnapshotApplicationEvent<>(this, "/").matches("/")).isTrue();
		assertThat(new TestSnapshotApplicationEvent<>(this, "   ").matches(" ")).isTrue();
		assertThat(new TestSnapshotApplicationEvent<>(this, "").matches(" ")).isTrue();
		assertThat(new TestSnapshotApplicationEvent<>(this, "").matches("")).isTrue();
		assertThat(new TestSnapshotApplicationEvent<>(this, (String) null).matches((String) null)).isTrue();
	}

	protected static final class TestSnapshotApplicationEvent<K, V> extends SnapshotApplicationEvent<K, V> {

		public TestSnapshotApplicationEvent(Object source, SnapshotMetadata<K, V>... snapshotMetadata) {
			super(source, snapshotMetadata);
		}

		public TestSnapshotApplicationEvent(Object source, String regionPath,
				SnapshotMetadata<K, V>... snapshotMetadata) {
			super(source, regionPath, snapshotMetadata);
		}
	}
}
