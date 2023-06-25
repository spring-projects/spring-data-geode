/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.gemfire.config.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.Index;

import org.springframework.data.gemfire.config.schema.SchemaObjectDefinition;
import org.springframework.data.gemfire.config.schema.definitions.IndexDefinition;
import org.springframework.data.gemfire.config.schema.definitions.RegionDefinition;

/**
 * Unit Tests for {@link GemfireAdminOperations}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.config.admin.GemfireAdminOperations
 * @since 1.9.0
 */
public class GemfireAdminOperationsUnitTests {

	private GemfireAdminOperations adminOperations;

	@Before
	@SuppressWarnings("deprecation")
	public void setup() {
		this.adminOperations = mock(GemfireAdminOperations.class, withSettings().lenient());
	}

	private Index mockIndex(String name) {

		Index mockIndex = mock(Index.class, name);

		doReturn(name).when(mockIndex).getName();

		return mockIndex;
	}

	@SuppressWarnings("unchecked")
	private <K, V> Region<K, V> mockRegion(String name) {

		Region<K, V> mockRegion = mock(Region.class, name);

		doReturn(name).when(mockRegion).getName();

		return mockRegion;
	}

	private SchemaObjectDefinition newGenericSchemaObjectDefinition(String name) {
		return mock(SchemaObjectDefinition.class, name);
	}

	@Test
	public void createRegionsWithArrayCallsCreateRegion() {

		doCallRealMethod().when(this.adminOperations)
			.createRegions(any(RegionDefinition.class), any(RegionDefinition.class));

		//doCallRealMethod().when(this.adminOperations).createRegions(any(RegionDefinition[].class));

		RegionDefinition definitionOne = RegionDefinition.from(mockRegion("RegionOne"));
		RegionDefinition definitionTwo = RegionDefinition.from(mockRegion("RegionTwo"));

		this.adminOperations.createRegions(definitionOne, definitionTwo);

		verify(this.adminOperations, times(1)).createRegion(eq(definitionOne));
		verify(this.adminOperations, times(1)).createRegion(eq(definitionTwo));
	}

	@Test
	public void createRegionsWithEmptyArray() {

		doCallRealMethod().when(this.adminOperations).createRegions(ArgumentMatchers.<RegionDefinition[]>any());

		this.adminOperations.createRegions();

		verify(this.adminOperations, never()).createRegion(any(RegionDefinition.class));
	}

	@Test
	public void createRegionsWithNullArray() {

		doCallRealMethod().when(this.adminOperations).createRegions(ArgumentMatchers.<RegionDefinition[]>any());

		this.adminOperations.createRegions((RegionDefinition[]) null);

		verify(this.adminOperations, never()).createRegion(any(RegionDefinition.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createRegionsWithIterableCallsCreateRegion() {

		doCallRealMethod().when(this.adminOperations).createRegions(any(Iterable.class));

		RegionDefinition definitionOne = RegionDefinition.from(mockRegion("RegionOne"));
		RegionDefinition definitionTwo = RegionDefinition.from(mockRegion("RegionTwo"));

		this.adminOperations.createRegions(Arrays.asList(definitionOne, definitionTwo));

		verify(this.adminOperations, times(1)).createRegion(eq(definitionOne));
		verify(this.adminOperations, times(1)).createRegion(eq(definitionTwo));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createRegionsWithEmptyIterableCallsCreateRegion() {

		doCallRealMethod().when(this.adminOperations).createRegions(any(Iterable.class));

		this.adminOperations.createRegions(Collections.emptyList());

		verify(this.adminOperations, never()).createRegion(any(RegionDefinition.class));
	}

	@Test
	public void createRegionsWithNullIterableCallsCreateRegion() {

		this.adminOperations.createRegions((Iterable<RegionDefinition>) null);

		verify(this.adminOperations, never()).createRegion(any(RegionDefinition.class));
	}

	@Test
	public void createLuceneIndexesWithArrayCallsCreateLuceneIndex() {

		doCallRealMethod().when(this.adminOperations)
			.createLuceneIndexes(any(SchemaObjectDefinition.class), any(SchemaObjectDefinition.class));

		//doCallRealMethod().when(this.adminOperations).createLuceneIndexes(any(SchemaObjectDefinition[].class));

		SchemaObjectDefinition definitionOne = newGenericSchemaObjectDefinition("LucenIndexOne");
		SchemaObjectDefinition definitionTwo = newGenericSchemaObjectDefinition("LucenIndexOne");

		this.adminOperations.createLuceneIndexes(definitionOne, definitionTwo);

		verify(this.adminOperations, times(1)).createLuceneIndex(eq(definitionOne));
		verify(this.adminOperations, times(1)).createLuceneIndex(eq(definitionTwo));
	}

	@Test
	public void createLuceneIndexesWithEmptyArray() {

		doCallRealMethod().when(this.adminOperations)
			.createLuceneIndexes(ArgumentMatchers.<SchemaObjectDefinition[]>any());

		this.adminOperations.createLuceneIndexes();

		verify(this.adminOperations, never()).createLuceneIndex(any(SchemaObjectDefinition.class));
	}

	@Test
	public void createLuceneIndexesWithNullArray() {

		doCallRealMethod().when(this.adminOperations)
			.createLuceneIndexes(ArgumentMatchers.<SchemaObjectDefinition[]>any());

		this.adminOperations.createLuceneIndexes((SchemaObjectDefinition[]) null);

		verify(this.adminOperations, never()).createLuceneIndex(any(SchemaObjectDefinition.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createLuceneIndexesWithIterableCallsCreateLuceneIndex() {

		doCallRealMethod().when(this.adminOperations).createLuceneIndexes(any(Iterable.class));

		SchemaObjectDefinition definitionOne = newGenericSchemaObjectDefinition("LucenIndexOne");
		SchemaObjectDefinition definitionTwo = newGenericSchemaObjectDefinition("LucenIndexOne");

		this.adminOperations.createLuceneIndexes(Arrays.asList(definitionOne, definitionTwo));

		verify(this.adminOperations, times(1)).createLuceneIndex(eq(definitionOne));
		verify(this.adminOperations, times(1)).createLuceneIndex(eq(definitionTwo));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createLuceneIndexesWithEmptyIterableCallsCreateLuceneIndex() {

		doCallRealMethod().when(this.adminOperations).createLuceneIndexes(any(Iterable.class));

		this.adminOperations.createLuceneIndexes(Collections.emptyList());

		verify(this.adminOperations, never()).createLuceneIndex(any(SchemaObjectDefinition.class));
	}

	@Test
	public void createLuceneIndexesWithNullIterableCallsCreateLuceneIndex() {

		this.adminOperations.createLuceneIndexes((Iterable<SchemaObjectDefinition>) null);

		verify(this.adminOperations, never()).createLuceneIndex(any(SchemaObjectDefinition.class));
	}

	@Test
	public void createIndexesWithArrayCallsCreateIndex() {

		doCallRealMethod().when(this.adminOperations)
			.createIndexes(any(IndexDefinition.class), any(IndexDefinition.class));

		//doCallRealMethod().when(this.adminOperations).createIndexes(any(IndexDefinition[].class));

		IndexDefinition definitionOne = IndexDefinition.from(mockIndex("IndexOne"));
		IndexDefinition definitionTwo = IndexDefinition.from(mockIndex("IndexTwo"));

		this.adminOperations.createIndexes(definitionOne, definitionTwo);

		verify(this.adminOperations, times(1)).createIndex(eq(definitionOne));
		verify(this.adminOperations, times(1)).createIndex(eq(definitionTwo));
	}

	@Test
	public void createIndexesWithEmptyArray() {

		doCallRealMethod().when(this.adminOperations).createIndexes(ArgumentMatchers.<IndexDefinition[]>any());

		this.adminOperations.createIndexes();

		verify(this.adminOperations, never()).createIndex(any(IndexDefinition.class));
	}

	@Test
	public void createIndexesWithNullArray() {

		doCallRealMethod().when(this.adminOperations).createIndexes(ArgumentMatchers.<IndexDefinition[]>any());

		this.adminOperations.createIndexes();

		verify(this.adminOperations, never()).createIndex(any(IndexDefinition.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createIndexesWithIterableCallsCreateIndex() {

		doCallRealMethod().when(this.adminOperations).createIndexes(any(Iterable.class));

		IndexDefinition definitionOne = IndexDefinition.from(mockIndex("IndexOne"));
		IndexDefinition definitionTwo = IndexDefinition.from(mockIndex("IndexTwo"));

		this.adminOperations.createIndexes(Arrays.asList(definitionOne, definitionTwo));

		verify(this.adminOperations, times(1)).createIndex(eq(definitionOne));
		verify(this.adminOperations, times(1)).createIndex(eq(definitionTwo));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createIndexesWithEmptyIterable() {

		doCallRealMethod().when(this.adminOperations).createIndexes(any(Iterable.class));

		this.adminOperations.createIndexes(Collections.emptyList());

		verify(this.adminOperations, never()).createIndex(any(IndexDefinition.class));
	}

	@Test
	public void createIndexesWithNullIterable() {

		this.adminOperations.createIndexes((Iterable<IndexDefinition>) null);

		verify(this.adminOperations, never()).createIndex(any(IndexDefinition.class));
	}
}
