/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.test;

/**
 * @author David Turanski
 *
 */

import static org.junit.Assert.assertEquals;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.Scope;

import org.junit.Test;
public class MockRegionFactoryTest {
	@Test
	public void testBasicAttributes() {
		RegionFactory<?,?> rf = new MockRegionFactory<Object,Object>(new StubCache()).createMockRegionFactory();
		rf.setScope(Scope.LOCAL);
		Region<?,?> foo = rf.create("foo");
		assertEquals(Scope.LOCAL,foo.getAttributes().getScope());
	}
}