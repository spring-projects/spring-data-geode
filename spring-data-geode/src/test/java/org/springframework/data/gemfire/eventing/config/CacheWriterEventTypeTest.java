package org.springframework.data.gemfire.eventing.config;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CacheWriterEventTypeTest {

	@Test
	public void testNoMaskConflictBetweenEntryAndRegionEvents() {
		HashMap<Integer, Integer> eventCount = new HashMap<>();

		for (RegionCacheWriterEventType value : RegionCacheWriterEventType.values()) {
			Integer maskCount = eventCount.getOrDefault(value.mask, 0);
			eventCount.put(value.mask, maskCount + 1);
		}

		for (CacheWriterEventType value : CacheWriterEventType.values()) {
			Integer maskCount = eventCount.getOrDefault(value.mask, 0);
			eventCount.put(value.mask, maskCount + 1);
		}

		for (Map.Entry<Integer, Integer> maskCountEntry : eventCount.entrySet()) {
			Assertions.assertThat(maskCountEntry.getValue())
				.as("Event mask for value %d. Please check CacheWriterEventType and RegionCacheWriterEventType for conflicts",
					maskCountEntry.getKey()).isEqualTo(1);
		}
	}
}