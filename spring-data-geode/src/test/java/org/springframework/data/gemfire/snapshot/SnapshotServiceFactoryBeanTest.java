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
package org.springframework.data.gemfire.snapshot;

import static org.apache.geode.cache.snapshot.SnapshotOptions.SnapshotFormat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.ArchiveFileFilter;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.CacheSnapshotServiceAdapter;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.RegionSnapshotServiceAdapter;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.SnapshotMetadata;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.SnapshotServiceAdapter;
import static org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean.SnapshotServiceAdapterSupport;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.snapshot.CacheSnapshotService;
import org.apache.geode.cache.snapshot.RegionSnapshotService;
import org.apache.geode.cache.snapshot.SnapshotFilter;
import org.apache.geode.cache.snapshot.SnapshotOptions;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.snapshot.event.ExportSnapshotApplicationEvent;
import org.springframework.data.gemfire.snapshot.event.ImportSnapshotApplicationEvent;
import org.springframework.data.gemfire.snapshot.event.SnapshotApplicationEvent;
import org.springframework.data.gemfire.tests.util.FileSystemUtils;

import org.slf4j.Logger;

/**
 * The SnapshotServiceFactoryBeanTest class is a test suite of test cases testing the contract and functionality
 * of the SnapshotServiceFactoryBean class.
 *
 * @author John Blum
 * @see org.junit.Rule
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.gemfire.snapshot.SnapshotServiceFactoryBean
 * @see org.apache.geode.cache.snapshot.CacheSnapshotService
 * @see org.apache.geode.cache.snapshot.RegionSnapshotService
 * @since 1.7.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SnapshotServiceFactoryBeanTest {

	private static File snapshotDat;

	private final SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

	protected static File mockFile(String filename) {

		File mockFile = mock(File.class, filename);

		when(mockFile.isFile()).thenReturn(true);
		when(mockFile.getAbsolutePath()).thenReturn(String.format("/path/to/%s", filename));
		when(mockFile.getName()).thenReturn(filename);

		return mockFile;
	}

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata() {
		return newSnapshotMetadata(FileSystemUtils.WORKING_DIRECTORY);
	}

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata(File location) {
		return newSnapshotMetadata(location, null, false, false, SnapshotFormat.GEMFIRE);
	}

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata(SnapshotFilter<K, V> filter,
			boolean invokeCallbacks, boolean parallel) {

		return newSnapshotMetadata(FileSystemUtils.WORKING_DIRECTORY, filter, invokeCallbacks, parallel);
	}

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata(File location, SnapshotFilter<K, V> filter,
			boolean invokeCallbacks, boolean parallel) {

		return newSnapshotMetadata(location, filter, invokeCallbacks, parallel, SnapshotFormat.GEMFIRE);
	}

	protected <K, V> SnapshotMetadata<K, V> newSnapshotMetadata(File location, SnapshotFilter<K, V> filter,
			boolean invokeCallbacks, boolean parallel, SnapshotFormat format) {

		SnapshotMetadata<K, V> snapshotMetadata = new SnapshotMetadata<>(location, format, filter);

		snapshotMetadata.setInvokeCallbacks(invokeCallbacks);
		snapshotMetadata.setParallel(parallel);

		return snapshotMetadata;
	}

	protected <K, V> SnapshotMetadata<K, V>[] toArray(SnapshotMetadata<K, V>... metadata) {
		return metadata;
	}

	protected String toPathname(String... pathElements) {

		StringBuilder pathname = new StringBuilder();

		for (String pathElement : pathElements) {
			pathname.append(File.separator).append(pathElement);
		}

		return pathname.toString();
	}

	@BeforeClass
	public static void setupBeforeClass() {
		snapshotDat = mockFile("snapshot.dat");
	}

	@After
	public void tearDown() {

		factoryBean.setExports(null);
		factoryBean.setImports(null);
		factoryBean.setRegion(null);
	}

	@Test
	public void nullSafeIsDirectoryWithDirectory() {
		assertThat(SnapshotServiceFactoryBean.nullSafeIsDirectory(new File(System.getProperty("user.dir")))).isTrue();
	}

	@Test
	public void nullSafeIsDirectoryWithNonDirectories() {

		assertThat(SnapshotServiceFactoryBean.nullSafeIsDirectory(new File("path/to/non-existing/directory")))
			.isFalse();

		assertThat(SnapshotServiceFactoryBean.nullSafeIsDirectory(FileSystemUtils.JAVA_EXE)).isFalse();
	}

	@Test
	public void nullSafeIsFileWithFile() {

		assertThat(SnapshotServiceFactoryBean.nullSafeIsFile(FileSystemUtils.JAVA_EXE))
			.isEqualTo(FileSystemUtils.JAVA_EXE.isFile());
	}

	@Test
	public void nullSafeIsFileWithNonFiles() {

		assertThat(SnapshotServiceFactoryBean.nullSafeIsFile(new File("/path/to/non-existing/file.ext"))).isFalse();
		assertThat(SnapshotServiceFactoryBean.nullSafeIsFile(new File(System.getProperty("user.dir")))).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void setCacheToNull() {

		try {
			factoryBean.setCache(null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("The GemFire Cache must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void getCacheWhenUninitialized() {

		try {
			factoryBean.getCache();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("The GemFire Cache was not properly initialized");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void setAndGetCacheSuccessfully() {

		Cache mockCache = mock(Cache.class, "MockCache");

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);

		assertThat(factoryBean.getCache()).isSameAs(mockCache);
	}

	@Test
	public void setAndGetExports() {

		SnapshotMetadata[] actualExports = factoryBean.getExports();

		assertThat(actualExports).isNotNull();
		assertThat(actualExports.length).isEqualTo(0);

		SnapshotMetadata[] expectedExports = toArray(newSnapshotMetadata());

		factoryBean.setExports(expectedExports);
		actualExports = factoryBean.getExports();

		assertThat(actualExports).isSameAs(expectedExports);

		factoryBean.setExports(null);
		actualExports = factoryBean.getExports();

		assertThat(actualExports).isNotSameAs(expectedExports);
		assertThat(actualExports).isNotNull();
		assertThat(actualExports.length).isEqualTo(0);
	}

	@Test
	public void setAndGetImports() {

		SnapshotMetadata[] actualImports = factoryBean.getImports();

		assertThat(actualImports).isNotNull();
		assertThat(actualImports.length).isEqualTo(0);

		SnapshotMetadata[] expectedImports = toArray(newSnapshotMetadata());

		factoryBean.setImports(expectedImports);
		actualImports = factoryBean.getImports();

		assertThat(actualImports).isSameAs(expectedImports);

		factoryBean.setImports(null);
		actualImports = factoryBean.getImports();

		assertThat(actualImports).isNotSameAs(expectedImports);
		assertThat(actualImports).isNotNull();
		assertThat(actualImports.length).isEqualTo(0);
	}

	@Test
	public void setAndGetRegionSuccessfully() {

		assertThat(factoryBean.getRegion()).isNull();

		Region mockRegion = mock(Region.class, "MockRegion");

		factoryBean.setRegion(mockRegion);

		assertThat(factoryBean.getRegion()).isSameAs(mockRegion);

		factoryBean.setRegion(null);

		assertThat(factoryBean.getRegion()).isNull();
	}

	@Test
	public void setAndGetSuppressImportOnInitSuccessfully() {

		assertThat(factoryBean.getSuppressImportOnInit()).isFalse();

		factoryBean.setSuppressImportOnInit(true);

		assertThat(factoryBean.getSuppressImportOnInit()).isTrue();

		factoryBean.setSuppressImportOnInit(false);

		assertThat(factoryBean.getSuppressImportOnInit()).isFalse();

		factoryBean.setSuppressImportOnInit(null);

		assertThat(factoryBean.getSuppressImportOnInit()).isFalse();
	}

	@Test
	public void isSingletonIsTrue() {
		assertThat(factoryBean.isSingleton()).isTrue();
	}

	@Test
	public void afterPropertiesSetCreatesSnapshotServiceAdapterAndDoesImportWithConfiguredImports() throws Exception {

		SnapshotMetadata expectedSnapshotMetadata = newSnapshotMetadata();

		SnapshotServiceAdapter mockSnapshotService = mock(SnapshotServiceAdapter.class,
			"MockSnapshotServiceAdapter");

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			protected SnapshotServiceAdapter create() {
				return mockSnapshotService;
			}
		};

		factoryBean.setImports(toArray(expectedSnapshotMetadata));
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.getImports()[0]).isEqualTo(expectedSnapshotMetadata);

		verify(mockSnapshotService, times(1)).doImport(eq(expectedSnapshotMetadata));
	}

	@Test
	public void afterPropertiesSetCreatesSnapshotServiceAdapterButSuppressesImportOnInit() throws Exception {

		SnapshotServiceAdapter mockSnapshotService = mock(SnapshotServiceAdapter.class,
			"MockSnapshotServiceAdapter");

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			protected SnapshotServiceAdapter create() {
				return mockSnapshotService;
			}
		};

		factoryBean.setSuppressImportOnInit(true);
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.getSuppressImportOnInit()).isTrue();

		verify(mockSnapshotService, never()).doImport(any(SnapshotMetadata[].class));
	}

	@Test
	public void createCacheSnapshotService() {

		Cache mockCache = mock(Cache.class, "MockCache");

		CacheSnapshotService mockCacheSnapshotService = mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		when(mockCache.getSnapshotService()).thenReturn(mockCacheSnapshotService);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);

		SnapshotServiceAdapter adapter = factoryBean.create();

		assertThat(adapter).isInstanceOf(CacheSnapshotServiceAdapter.class);

		verify(mockCache, times(1)).getSnapshotService();
	}

	@Test
	public void createRegionSnapshotService() {

		Region mockRegion = mock(Region.class, "MockRegion");

		RegionSnapshotService mockRegionSnapshotService = mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		when(mockRegion.getSnapshotService()).thenReturn(mockRegionSnapshotService);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setRegion(mockRegion);

		SnapshotServiceAdapter adapter = factoryBean.create();

		assertThat(adapter).isInstanceOf(RegionSnapshotServiceAdapter.class);

		verify(mockRegion, times(1)).getSnapshotService();
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrapNullCacheSnapshotService() {

		try {
			factoryBean.wrap((CacheSnapshotService) null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("The backing CacheSnapshotService must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrapNullRegionSnapshotService() {

		try {
			factoryBean.wrap((RegionSnapshotService) null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("The backing RegionSnapshotService must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void destroyPerformsExportWithConfiguredExports() throws Exception {

		SnapshotMetadata expectedSnapshotMetadata = newSnapshotMetadata();

		SnapshotServiceAdapter mockSnapshotService = mock(SnapshotServiceAdapter.class,
			"MockSnapshotServiceAdapter");

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			public SnapshotServiceAdapter getObject() {
				return mockSnapshotService;
			}
		};

		factoryBean.setExports(toArray(expectedSnapshotMetadata));
		factoryBean.destroy();

		assertThat(factoryBean.getExports()[0]).isEqualTo(expectedSnapshotMetadata);

		verify(mockSnapshotService, times(1)).doExport(eq(expectedSnapshotMetadata));
	}

	@Test
	public void onApplicationEventWhenMatchUsingEventSnapshotMetadataPerformsExport() {

		Region mockRegion = mock(Region.class, "MockRegion");

		SnapshotApplicationEvent mockSnapshotEvent = mock(ExportSnapshotApplicationEvent.class,
			"MockExportSnapshotApplicationEvent");

		SnapshotMetadata eventSnapshotMetadata = newSnapshotMetadata(snapshotDat);

		SnapshotServiceAdapter mockSnapshotService =
			mock(SnapshotServiceAdapter.class, "MockSnapshotServiceAdapter");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(false);
		when(mockSnapshotEvent.matches(eq(mockRegion))).thenReturn(true);
		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(toArray(eventSnapshotMetadata));

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			public SnapshotServiceAdapter getObject() {
				return mockSnapshotService;
			}
		};

		factoryBean.setExports(toArray(newSnapshotMetadata()));
		factoryBean.setRegion(mockRegion);

		assertThat(factoryBean.getExports()[0]).isNotSameAs(eventSnapshotMetadata);
		assertThat(factoryBean.getRegion()).isSameAs(mockRegion);

		factoryBean.onApplicationEvent(mockSnapshotEvent);

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, times(1)).matches(eq(mockRegion));
		verify(mockSnapshotEvent, times(1)).getSnapshotMetadata();
		verify(mockSnapshotService, times(1)).doExport(eq(eventSnapshotMetadata));
	}

	@Test
	public void onApplicationEventWhenMatchUsingFactorySnapshotMetadataPerformsImport() {

		SnapshotApplicationEvent mockSnapshotEvent = mock(ImportSnapshotApplicationEvent.class,
			"MockImportSnapshotApplicationEvent");

		SnapshotMetadata factorySnapshotMetadata = newSnapshotMetadata(snapshotDat);

		SnapshotServiceAdapter mockSnapshotService =
			mock(SnapshotServiceAdapter.class, "MockSnapshotServiceAdapter");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(true);
		when(mockSnapshotEvent.matches(any(Region.class))).thenReturn(false);
		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(null);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			public SnapshotServiceAdapter getObject() {
				return mockSnapshotService;
			}
		};

		factoryBean.setImports(toArray(factorySnapshotMetadata));

		assertThat(factoryBean.getImports()[0]).isEqualTo(factorySnapshotMetadata);
		assertThat(factoryBean.getRegion()).isNull();

		factoryBean.onApplicationEvent(mockSnapshotEvent);

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, never()).matches(any(Region.class));
		verify(mockSnapshotEvent, times(1)).getSnapshotMetadata();
		verify(mockSnapshotService, times(1)).doImport(eq(factorySnapshotMetadata));
	}

	@Test
	public void onApplicationEventWhenNoMatchDoesNotPerformExport() {

		SnapshotApplicationEvent mockSnapshotEvent = mock(ExportSnapshotApplicationEvent.class,
			"MockExportSnapshotApplicationEvent");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(false);
		when(mockSnapshotEvent.matches(any(Region.class))).thenReturn(false);
		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(null);

		SnapshotServiceAdapter mockSnapshotService =
			mock(SnapshotServiceAdapter.class, "MockSnapshotServiceAdapter");

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			public SnapshotServiceAdapter getObject() {
				return mockSnapshotService;
			}
		};

		factoryBean.setExports(toArray(newSnapshotMetadata()));

		assertThat(factoryBean.getExports()[0]).isInstanceOf(SnapshotMetadata.class);
		assertThat(factoryBean.getRegion()).isNull();

		factoryBean.onApplicationEvent(mockSnapshotEvent);

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, times(1)).matches(ArgumentMatchers.<Region>isNull());
		verify(mockSnapshotEvent, never()).getSnapshotMetadata();
		verify(mockSnapshotService, never()).doExport(any(SnapshotMetadata.class));
	}

	@Test
	public void onApplicationEventWhenNoMatchDoesNotPerformImport() {

		Region mockRegion = mock(Region.class, "MockRegion");

		SnapshotApplicationEvent mockSnapshotEvent = mock(ImportSnapshotApplicationEvent.class,
			"MockImportSnapshotApplicationEvent");

		SnapshotServiceAdapter mockSnapshotService =
			mock(SnapshotServiceAdapter.class, "MockSnapshotServiceAdapter");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(false);
		when(mockSnapshotEvent.matches(any(Region.class))).thenReturn(false);
		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(null);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean() {

			@Override
			public SnapshotServiceAdapter getObject() {
				return mockSnapshotService;
			}
		};

		factoryBean.setImports(toArray(newSnapshotMetadata()));
		factoryBean.setRegion(mockRegion);

		assertThat(factoryBean.getImports()[0]).isInstanceOf(SnapshotMetadata.class);
		assertThat(factoryBean.getRegion()).isEqualTo(mockRegion);

		factoryBean.onApplicationEvent(mockSnapshotEvent);

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, times(1)).matches(eq(mockRegion));
		verify(mockSnapshotEvent, never()).getSnapshotMetadata();
		verify(mockSnapshotService, never()).doImport(any(SnapshotMetadata.class));
	}

	@Test
	public void resolveSnapshotMetadataFromEvent() {

		SnapshotMetadata eventSnapshotMetadata = newSnapshotMetadata(snapshotDat);
		SnapshotMetadata factoryExportSnapshotMetadata = newSnapshotMetadata();
		SnapshotMetadata factoryImportSnapshotMetadata = newSnapshotMetadata(FileSystemUtils.USER_HOME);

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(SnapshotApplicationEvent.class, "MockSnapshotApplicationEvent");

		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(toArray(eventSnapshotMetadata));

		factoryBean.setExports(toArray(factoryExportSnapshotMetadata));
		factoryBean.setImports(toArray(factoryImportSnapshotMetadata));

		assertThat(factoryBean.getExports()[0]).isEqualTo(factoryExportSnapshotMetadata);
		assertThat(factoryBean.getImports()[0]).isEqualTo(factoryImportSnapshotMetadata);
		assertThat(factoryBean.resolveSnapshotMetadata(mockSnapshotEvent)[0]).isEqualTo(eventSnapshotMetadata);

		verify(mockSnapshotEvent, times(1)).getSnapshotMetadata();
	}

	@Test
	public void resolveExportSnapshotMetadataFromFactory() {

		SnapshotMetadata factoryExportSnapshotMetadata = newSnapshotMetadata();
		SnapshotMetadata factoryImportSnapshotMetadata = newSnapshotMetadata(FileSystemUtils.USER_HOME);

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(ExportSnapshotApplicationEvent.class,"MockExportSnapshotApplicationEvent");

		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(null);

		factoryBean.setExports(toArray(factoryExportSnapshotMetadata));
		factoryBean.setImports(toArray(factoryImportSnapshotMetadata));

		assertThat(factoryBean.getExports()[0]).isEqualTo(factoryExportSnapshotMetadata);
		assertThat(factoryBean.getImports()[0]).isEqualTo(factoryImportSnapshotMetadata);
		assertThat(factoryBean.resolveSnapshotMetadata(mockSnapshotEvent)[0]).isEqualTo(factoryExportSnapshotMetadata);

		verify(mockSnapshotEvent, times(1)).getSnapshotMetadata();
	}

	@Test
	public void resolveImportSnapshotMetadataFromFactory() {

		SnapshotMetadata factoryExportSnapshotMetadata = newSnapshotMetadata();
		SnapshotMetadata factoryImportSnapshotMetadata = newSnapshotMetadata(FileSystemUtils.USER_HOME);

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(ImportSnapshotApplicationEvent.class, "MockImportSnapshotApplicationEvent");

		when(mockSnapshotEvent.getSnapshotMetadata()).thenReturn(toArray());

		factoryBean.setExports(toArray(factoryExportSnapshotMetadata));
		factoryBean.setImports(toArray(factoryImportSnapshotMetadata));

		assertThat(factoryBean.getExports()[0]).isEqualTo(factoryExportSnapshotMetadata);
		assertThat(factoryBean.getImports()[0]).isEqualTo(factoryImportSnapshotMetadata);
		assertThat(factoryBean.resolveSnapshotMetadata(mockSnapshotEvent)[0]).isEqualTo(factoryImportSnapshotMetadata);

		verify(mockSnapshotEvent, times(1)).getSnapshotMetadata();
	}

	@Test
	public void withCacheBasedSnapshotServiceOnCacheSnapshotEventIsMatch() {

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(SnapshotApplicationEvent.class, "MockSnapshotApplicationEvent");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(true);

		assertThat(factoryBean.getRegion()).isNull();
		assertThat(factoryBean.isMatch(mockSnapshotEvent)).isTrue();

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, never()).matches(any(Region.class));
	}

	@Test
	public void withCacheBasedSnapshotServiceOnRegionSnapshotEventIsNotAMatch() {

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(SnapshotApplicationEvent.class, "MockSnapshotApplicationEvent");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(false);
		when(mockSnapshotEvent.matches(any(Region.class))).thenReturn(false);

		assertThat(factoryBean.getRegion()).isNull();
		assertThat(factoryBean.isMatch(mockSnapshotEvent)).isFalse();

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, times(1)).matches(ArgumentMatchers.<Region>isNull());
	}

	@Test
	public void withRegionBasedSnapshotServiceOnCacheSnapshotEventIsMatch() {

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(SnapshotApplicationEvent.class, "MockSnapshotApplicationEvent");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(true);

		factoryBean.setRegion(mock(Region.class, "MockRegion"));

		assertThat(factoryBean.getRegion()).isNotNull();
		assertThat(factoryBean.isMatch(mockSnapshotEvent)).isTrue();

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, never()).matches(any(Region.class));
	}

	@Test
	public void withRegionBasedSnapshotServiceOnRegionSnapshotEventIsMatch() {

		Region mockRegion = mock(Region.class, "MockRegion");

		SnapshotApplicationEvent mockSnapshotEvent =
			mock(SnapshotApplicationEvent.class, "MockSnapshotApplicationEvent");

		when(mockSnapshotEvent.isCacheSnapshotEvent()).thenReturn(false);
		when(mockSnapshotEvent.matches(eq(mockRegion))).thenReturn(true);

		factoryBean.setRegion(mockRegion);

		assertThat(factoryBean.getRegion()).isSameAs(mockRegion);
		assertThat(factoryBean.isMatch(mockSnapshotEvent)).isTrue();

		verify(mockSnapshotEvent, times(1)).isCacheSnapshotEvent();
		verify(mockSnapshotEvent, times(1)).matches(eq(mockRegion));
	}

	@Test
	public void importCacheSnapshotOnInitialization() throws Exception {

		Cache mockCache = mock(Cache.class, "MockCache");

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		SnapshotFilter mockSnapshotFilterOne = mock(SnapshotFilter.class, "MockSnapshotFilterOne");
		SnapshotFilter mockSnapshotFilterTwo = mock(SnapshotFilter.class, "MockSnapshotFilterTwo");

		SnapshotOptions mockSnapshotOptionsOne = mock(SnapshotOptions.class, "MockSnapshotOptionsOne");
		SnapshotOptions mockSnapshotOptionsTwo = mock(SnapshotOptions.class, "MockSnapshotOptionsTwo");

		when(mockCache.getSnapshotService()).thenReturn(mockCacheSnapshotService);
		when(mockCacheSnapshotService.createOptions()).thenReturn(mockSnapshotOptionsOne).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsOne.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setFilter(eq(mockSnapshotFilterOne))).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsTwo.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setFilter(eq(mockSnapshotFilterTwo))).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);

		SnapshotMetadata[] expectedImports = toArray(
			newSnapshotMetadata(FileSystemUtils.TEMPORARY_DIRECTORY, mockSnapshotFilterOne, false, true),
			newSnapshotMetadata(mockSnapshotFilterTwo, true, false)
		);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);
		factoryBean.setExports(null);
		factoryBean.setImports(expectedImports);
		factoryBean.setRegion(null);

		assertThat(factoryBean.getObject()).isNull();
		assertThat((Class<SnapshotServiceAdapter>) factoryBean.getObjectType()).isEqualTo(SnapshotServiceAdapter.class);

		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.getObject()).isInstanceOf(CacheSnapshotServiceAdapter.class);
		assertThat((Class<CacheSnapshotServiceAdapter>) factoryBean.getObjectType())
			.isEqualTo(CacheSnapshotServiceAdapter.class);

		verify(mockCache, times(1)).getSnapshotService();
		verify(mockCacheSnapshotService, times(2)).createOptions();
		verify(mockCacheSnapshotService, times(1))
			.load(eq(FileSystemUtils.safeListFiles(FileSystemUtils.TEMPORARY_DIRECTORY, FileSystemUtils.FileOnlyFilter.INSTANCE)),
				eq(SnapshotFormat.GEMFIRE), eq(mockSnapshotOptionsOne));
		verify(mockCacheSnapshotService, times(1))
			.load(eq(FileSystemUtils.safeListFiles(FileSystemUtils.WORKING_DIRECTORY, FileSystemUtils.FileOnlyFilter.INSTANCE)),
				eq(SnapshotFormat.GEMFIRE), eq(mockSnapshotOptionsTwo));
		verify(mockSnapshotOptionsOne, times(1)).invokeCallbacks(eq(false));
		verify(mockSnapshotOptionsOne, times(1)).setFilter(eq(mockSnapshotFilterOne));
		verify(mockSnapshotOptionsOne, times(1)).setParallelMode(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).invokeCallbacks(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).setFilter(eq(mockSnapshotFilterTwo));
		verify(mockSnapshotOptionsTwo, times(1)).setParallelMode(eq(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void importRegionSnapshotOnInitialization() throws Exception {

		Cache mockCache = mock(Cache.class, "MockCache");

		Region mockRegion = mock(Region.class, "MockRegion");

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		SnapshotFilter mockSnapshotFilterOne = mock(SnapshotFilter.class, "MockSnapshotFilterOne");
		SnapshotFilter mockSnapshotFilterTwo = mock(SnapshotFilter.class, "MockSnapshotFilterTwo");

		SnapshotOptions mockSnapshotOptionsOne = mock(SnapshotOptions.class, "MockSnapshotOptionsOne");
		SnapshotOptions mockSnapshotOptionsTwo = mock(SnapshotOptions.class, "MockSnapshotOptionsTwo");

		when(mockCache.getSnapshotService()).thenThrow(new UnsupportedOperationException("operation not supported"));
		when(mockRegion.getSnapshotService()).thenReturn(mockRegionSnapshotService);
		when(mockRegionSnapshotService.createOptions()).thenReturn(mockSnapshotOptionsOne)
			.thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsOne.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setFilter(eq(mockSnapshotFilterOne))).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsTwo.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setFilter(eq(mockSnapshotFilterTwo))).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);

		File snapshotDatTwo = mockFile("snapshot-2.dat");

		SnapshotMetadata[] expectedImports = toArray(
			newSnapshotMetadata(snapshotDat, mockSnapshotFilterOne, false, true),
			newSnapshotMetadata(snapshotDatTwo, mockSnapshotFilterTwo, true, false)
		);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);
		factoryBean.setExports(null);
		factoryBean.setImports(expectedImports);
		factoryBean.setRegion(mockRegion);

		assertThat(factoryBean.getObject()).isNull();
		assertThat((Class<SnapshotServiceAdapter>) factoryBean.getObjectType()).isEqualTo(SnapshotServiceAdapter.class);

		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.getObject()).isInstanceOf(RegionSnapshotServiceAdapter.class);
		assertThat((Class<RegionSnapshotServiceAdapter>) factoryBean.getObjectType())
			.isEqualTo(RegionSnapshotServiceAdapter.class);

		verify(mockCache, never()).getSnapshotService();
		verify(mockRegion, times(1)).getSnapshotService();
		verify(mockRegionSnapshotService, times(2)).createOptions();
		verify(mockRegionSnapshotService, times(1))
			.load(eq(snapshotDat), eq(SnapshotFormat.GEMFIRE), eq(mockSnapshotOptionsOne));
		verify(mockRegionSnapshotService, times(1))
			.load(eq(snapshotDatTwo), eq(SnapshotFormat.GEMFIRE), eq(mockSnapshotOptionsTwo));
		verify(mockSnapshotOptionsOne, times(1)).invokeCallbacks(eq(false));
		verify(mockSnapshotOptionsOne, times(1)).setFilter(eq(mockSnapshotFilterOne));
		verify(mockSnapshotOptionsOne, times(1)).setParallelMode(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).invokeCallbacks(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).setFilter(eq(mockSnapshotFilterTwo));
		verify(mockSnapshotOptionsTwo, times(1)).setParallelMode(eq(false));
	}

	@Test
	public void exportCacheSnapshotOnDestroy() throws Exception {

		Cache mockCache = mock(Cache.class, "MockCache");

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		SnapshotFilter mockSnapshotFilterOne = mock(SnapshotFilter.class, "MockSnapshotFilterOne");
		SnapshotFilter mockSnapshotFilterTwo = mock(SnapshotFilter.class, "MockSnapshotFilterTwo");

		SnapshotOptions mockSnapshotOptionsOne = mock(SnapshotOptions.class, "MockSnapshotOptionsOne");
		SnapshotOptions mockSnapshotOptionsTwo = mock(SnapshotOptions.class, "MockSnapshotOptionsTwo");

		when(mockCache.getSnapshotService()).thenReturn(mockCacheSnapshotService);
		when(mockCacheSnapshotService.createOptions()).thenReturn(mockSnapshotOptionsOne)
			.thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsOne.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setFilter(eq(mockSnapshotFilterOne))).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsTwo.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setFilter(eq(mockSnapshotFilterTwo))).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);

		SnapshotMetadata[] expectedExports = toArray(
			newSnapshotMetadata(mockSnapshotFilterOne, false, true),
			newSnapshotMetadata(mockSnapshotFilterTwo, true, false)
		);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);
		factoryBean.setExports(expectedExports);
		factoryBean.setImports(null);
		factoryBean.setRegion(null);
		factoryBean.afterPropertiesSet();
		factoryBean.destroy();

		assertThat(factoryBean.getObject()).isInstanceOf(CacheSnapshotServiceAdapter.class);

		verify(mockCache, times(1)).getSnapshotService();
		verify(mockCacheSnapshotService, times(2)).createOptions();
		verify(mockCacheSnapshotService, times(1)).save(eq(expectedExports[0].getLocation()),
			eq(expectedExports[0].getFormat()), eq(mockSnapshotOptionsOne));
		verify(mockCacheSnapshotService, times(1)).save(eq(expectedExports[1].getLocation()),
			eq(expectedExports[1].getFormat()), eq(mockSnapshotOptionsTwo));
		verify(mockSnapshotOptionsOne, times(1)).invokeCallbacks(eq(false));
		verify(mockSnapshotOptionsOne, times(1)).setFilter(eq(mockSnapshotFilterOne));
		verify(mockSnapshotOptionsOne, times(1)).setParallelMode(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).invokeCallbacks(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).setFilter(eq(mockSnapshotFilterTwo));
		verify(mockSnapshotOptionsTwo, times(1)).setParallelMode(eq(false));
	}

	@Test
	public void exportRegionSnapshotOnDestroy() throws Exception {

		Cache mockCache = mock(Cache.class, "MockCache");

		Region mockRegion = mock(Region.class, "MockRegion");

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		SnapshotFilter mockSnapshotFilterOne = mock(SnapshotFilter.class, "MockSnapshotFilterOne");
		SnapshotFilter mockSnapshotFilterTwo = mock(SnapshotFilter.class, "MockSnapshotFilterTwo");

		SnapshotOptions mockSnapshotOptionsOne = mock(SnapshotOptions.class, "MockSnapshotOptionsOne");
		SnapshotOptions mockSnapshotOptionsTwo = mock(SnapshotOptions.class, "MockSnapshotOptionsTwo");

		when(mockCache.getSnapshotService()).thenThrow(new UnsupportedOperationException("operation not supported"));
		when(mockRegion.getSnapshotService()).thenReturn(mockRegionSnapshotService);
		when(mockRegionSnapshotService.createOptions()).thenReturn(mockSnapshotOptionsOne)
			.thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsOne.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setFilter(eq(mockSnapshotFilterOne))).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsOne.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsOne);
		when(mockSnapshotOptionsTwo.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setFilter(eq(mockSnapshotFilterTwo))).thenReturn(mockSnapshotOptionsTwo);
		when(mockSnapshotOptionsTwo.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptionsTwo);

		SnapshotMetadata[] expectedExports = toArray(
			newSnapshotMetadata(mockSnapshotFilterOne, false, true),
			newSnapshotMetadata(mockSnapshotFilterTwo, true, false)
		);

		SnapshotServiceFactoryBean factoryBean = new SnapshotServiceFactoryBean();

		factoryBean.setCache(mockCache);
		factoryBean.setExports(expectedExports);
		factoryBean.setImports(null);
		factoryBean.setRegion(mockRegion);
		factoryBean.afterPropertiesSet();
		factoryBean.destroy();

		assertThat(factoryBean.getObject()).isInstanceOf(RegionSnapshotServiceAdapter.class);

		verify(mockCache, never()).getSnapshotService();
		verify(mockRegion, times(1)).getSnapshotService();
		verify(mockRegionSnapshotService, times(2)).createOptions();
		verify(mockRegionSnapshotService, times(1)).save(eq(expectedExports[0].getLocation()),
			eq(expectedExports[0].getFormat()), eq(mockSnapshotOptionsOne));
		verify(mockRegionSnapshotService, times(1)).save(eq(expectedExports[1].getLocation()),
			eq(expectedExports[1].getFormat()), eq(mockSnapshotOptionsTwo));
		verify(mockSnapshotOptionsOne, times(1)).invokeCallbacks(eq(false));
		verify(mockSnapshotOptionsOne, times(1)).setFilter(eq(mockSnapshotFilterOne));
		verify(mockSnapshotOptionsOne, times(1)).setParallelMode(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).invokeCallbacks(eq(true));
		verify(mockSnapshotOptionsTwo, times(1)).setFilter(eq(mockSnapshotFilterTwo));
		verify(mockSnapshotOptionsTwo, times(1)).setParallelMode(eq(false));
	}

	@Test
	public void createOptionsWithParallelModeInvokeCallbacksAndFilterOnSnapshotServiceAdapterSupport() {

		SnapshotFilter mockSnapshotFilter = mock(SnapshotFilter.class, "MockSnapshotFilter");

		SnapshotOptions mockSnapshotOptions = mock(SnapshotOptions.class, "MockSnapshotOptions");

		when(mockSnapshotOptions.invokeCallbacks(anyBoolean())).thenReturn(mockSnapshotOptions);
		when(mockSnapshotOptions.setFilter(any(SnapshotFilter.class))).thenReturn(mockSnapshotOptions);
		when(mockSnapshotOptions.setParallelMode(anyBoolean())).thenReturn(mockSnapshotOptions);

		TestSnapshotServiceAdapter snapshotService = new TestSnapshotServiceAdapter() {

			@Override
			public SnapshotOptions<Object, Object> createOptions() {
				return mockSnapshotOptions;
			}
		};

		SnapshotMetadata<Object, Object> snapshotMetadata =
			new SnapshotMetadata<>(mockFile("snapshot.gfd"), SnapshotMetadata.DEFAULT_SNAPSHOT_FORMAT,
				mockSnapshotFilter);

		snapshotMetadata.setInvokeCallbacks(true);
		snapshotMetadata.setParallel(true);

		assertThat(snapshotService.createOptions(snapshotMetadata)).isEqualTo(mockSnapshotOptions);

		verify(mockSnapshotOptions, times(1)).invokeCallbacks(eq(true));
		verify(mockSnapshotOptions, times(1)).setFilter(eq(mockSnapshotFilter));
		verify(mockSnapshotOptions, times(1)).setParallelMode(eq(true));
	}

	@Test
	public void invokeExceptionSuppressingCloseOnSnapshotServiceAdapterSupportIsSuccessful() throws Exception {

		Closeable mockCloseable = mock(Closeable.class, "MockCloseable");

		assertThat(new TestSnapshotServiceAdapter().exceptionSuppressingClose(mockCloseable)).isTrue();

		verify(mockCloseable, times(1)).close();
	}

	@Test
	public void invokeExceptionSuppressingCloseOnSnapshotServiceAdapterSupportIsUnsuccessful() throws Exception {

		Closeable mockCloseable = mock(Closeable.class, "MockCloseable");

		doThrow(new IOException("TEST")).when(mockCloseable).close();

		assertThat(new TestSnapshotServiceAdapter().exceptionSuppressingClose(mockCloseable)).isFalse();

		verify(mockCloseable, times(1)).close();
	}

	@Test
	public void logDebugWhenDebugging() {

		Logger mockLog = mock(Logger.class, "MockLog");

		when(mockLog.isDebugEnabled()).thenReturn(true);

		TestSnapshotServiceAdapter snapshotService = new TestSnapshotServiceAdapter() {

			@Override
			Logger createLog() {
				return mockLog;
			}
		};

		Exception expectedException = new Exception("test");

		snapshotService.logDebug(expectedException, "Log message with argument (%1$s)", "test");

		verify(mockLog, times(1)).isDebugEnabled();
		verify(mockLog, times(1)).debug(eq("Log message with argument (test)"), eq(expectedException));
	}

	@Test
	public void logDebugWhenNotDebugging() {

		Logger mockLog = mock(Logger.class, "MockLog");

		when(mockLog.isDebugEnabled()).thenReturn(false);

		TestSnapshotServiceAdapter snapshotService = new TestSnapshotServiceAdapter() {

			@Override
			Logger createLog() {
				return mockLog;
			}
		};

		snapshotService.logDebug(null, "Log message with argument (%1$s)", "test");

		verify(mockLog, times(1)).isDebugEnabled();
		verify(mockLog, never()).debug(any(String.class), any(Throwable.class));
	}

	@Test
	public void toSimpleFilenameUsingVariousPathnames() {

		TestSnapshotServiceAdapter snapshotService = new TestSnapshotServiceAdapter();

		assertThat(snapshotService.toSimpleFilename(toPathname("path", "to", "file.ext"))).isEqualTo("file.ext");
		assertThat(snapshotService.toSimpleFilename(toPathname("path", "to", "file   "))).isEqualTo("file");
		assertThat(snapshotService.toSimpleFilename(toPathname("  file.ext "))).isEqualTo("file.ext");
		assertThat(snapshotService.toSimpleFilename("  file.ext ")).isEqualTo("file.ext");
		assertThat(snapshotService.toSimpleFilename(File.separator.concat(" "))).isEqualTo("");
		assertThat(snapshotService.toSimpleFilename("  ")).isEqualTo("");
		assertThat(snapshotService.toSimpleFilename("")).isEqualTo("");
		assertThat(snapshotService.toSimpleFilename(null)).isNull();
	}

	@Test(expected = ImportSnapshotException.class)
	public void loadCacheSnapshotWithDirectoryAndFormatHandlesExceptionAppropriately() throws Exception {

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		doThrow(new IOException("TEST")).when(mockCacheSnapshotService).load(any(File.class), any(SnapshotFormat.class));

		CacheSnapshotServiceAdapter adapter = new CacheSnapshotServiceAdapter(mockCacheSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockCacheSnapshotService);

		try {
			adapter.load(FileSystemUtils.WORKING_DIRECTORY, SnapshotFormat.GEMFIRE);
		}
		catch (ImportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to load snapshots from directory [%1$s] in format [GEMFIRE]",
				FileSystemUtils.WORKING_DIRECTORY));
			assertThat(expected.getCause()).isInstanceOf(IOException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockCacheSnapshotService, times(1)).load(eq(FileSystemUtils.WORKING_DIRECTORY),
				eq(SnapshotFormat.GEMFIRE));
		}
	}

	@Test(expected = ImportSnapshotException.class)
	public void loadCacheSnapshotWithFormatOptionsAndSnapshotFilesHandlesExceptionAppropriately() throws Exception {

		SnapshotOptions mockSnapshotOptions = mock(SnapshotOptions.class, "MockSnapshotOptions");

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		doThrow(new ClassCastException("TEST")).when(mockCacheSnapshotService).load(any(File[].class),
			any(SnapshotFormat.class), any(SnapshotOptions.class));

		CacheSnapshotServiceAdapter adapter = new CacheSnapshotServiceAdapter(mockCacheSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockCacheSnapshotService);

		try {
			adapter.load(SnapshotFormat.GEMFIRE, mockSnapshotOptions, snapshotDat);
		}
		catch (ImportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to load snapshots [%1$s] in format [GEMFIRE] using options [%2$s]",
				Arrays.toString(new File[] { snapshotDat }), mockSnapshotOptions));
			assertThat(expected.getCause()).isInstanceOf(ClassCastException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockCacheSnapshotService, times(1)).load(eq(new File[] { snapshotDat }),
				eq(SnapshotFormat.GEMFIRE), ArgumentMatchers.isA(SnapshotOptions.class));
		}
	}

	@Test(expected = ExportSnapshotException.class)
	public void saveCacheSnapshotWithDirectoryAndFormatHandlesExceptionAppropriately() throws Exception {

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		doThrow(new IOException("TEST")).when(mockCacheSnapshotService).save(any(File.class), any(SnapshotFormat.class));

		CacheSnapshotServiceAdapter adapter = new CacheSnapshotServiceAdapter(mockCacheSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockCacheSnapshotService);

		try {
			adapter.save(FileSystemUtils.WORKING_DIRECTORY, SnapshotFormat.GEMFIRE);
		}
		catch (ExportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to save snapshots to directory [%1$s] in format [GEMFIRE]",
				FileSystemUtils.WORKING_DIRECTORY));
			assertThat(expected.getCause()).isInstanceOf(IOException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockCacheSnapshotService, times(1)).save(eq(FileSystemUtils.WORKING_DIRECTORY),
				eq(SnapshotFormat.GEMFIRE));
		}
	}

	@Test(expected = ExportSnapshotException.class)
	public void saveCacheSnapshotWithDirectoryFormatAndOptionsHandlesExceptionAppropriately() throws Exception {

		SnapshotOptions mockSnapshotOptions = mock(SnapshotOptions.class, "MockSnapshotOptions");

		CacheSnapshotService mockCacheSnapshotService =
			mock(CacheSnapshotService.class, "MockCacheSnapshotService");

		doThrow(new ClassCastException("TEST")).when(mockCacheSnapshotService).save(any(File.class),
			any(SnapshotFormat.class), any(SnapshotOptions.class));

		CacheSnapshotServiceAdapter adapter = new CacheSnapshotServiceAdapter(mockCacheSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockCacheSnapshotService);

		try {
			adapter.save(FileSystemUtils.USER_HOME, SnapshotFormat.GEMFIRE, mockSnapshotOptions);
		}
		catch (ExportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to save snapshots to directory [%1$s] in format [GEMFIRE] using options [%2$s]",
				FileSystemUtils.USER_HOME, mockSnapshotOptions));
			assertThat(expected.getCause()).isInstanceOf(ClassCastException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockCacheSnapshotService, times(1)).save(eq(FileSystemUtils.USER_HOME), eq(SnapshotFormat.GEMFIRE),
				ArgumentMatchers.isA(SnapshotOptions.class));
		}
	}

	@Test(expected = ImportSnapshotException.class)
	public void loadRegionSnapshotWithSnapshotFileAndFormatHandlesExceptionAppropriately() throws Exception {

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		doThrow(new IOException("TEST")).when(mockRegionSnapshotService).load(any(File.class),
			any(SnapshotFormat.class));

		RegionSnapshotServiceAdapter adapter = new RegionSnapshotServiceAdapter(mockRegionSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockRegionSnapshotService);

		try {
			adapter.load(snapshotDat, SnapshotFormat.GEMFIRE);
		}
		catch (ImportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to load snapshot from file [%1$s] in format [GEMFIRE]", snapshotDat));
			assertThat(expected.getCause()).isInstanceOf(IOException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockRegionSnapshotService, times(1)).load(eq(snapshotDat), eq(SnapshotFormat.GEMFIRE));
		}
	}

	@Test(expected = ImportSnapshotException.class)
	public void loadRegionSnapshotWithFormatOptionsAndSnapshotFilesHandlesExceptionAppropriately() throws Exception {

		SnapshotOptions mockSnapshotOptions = mock(SnapshotOptions.class, "MockSnapshotOptions");

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		doThrow(new ClassCastException("TEST")).when(mockRegionSnapshotService).load(
			any(File.class), any(SnapshotFormat.class), any(SnapshotOptions.class));

		RegionSnapshotServiceAdapter adapter = new RegionSnapshotServiceAdapter(mockRegionSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockRegionSnapshotService);

		try {
			adapter.load(SnapshotFormat.GEMFIRE, mockSnapshotOptions, snapshotDat);
		}
		catch (ImportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to load snapshots [%1$s] in format [GEMFIRE] using options [%2$s]",
				Arrays.toString(new File[] { snapshotDat }), mockSnapshotOptions));
			assertThat(expected.getCause()).isInstanceOf(ClassCastException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockRegionSnapshotService, times(1)).load(eq(snapshotDat), eq(SnapshotFormat.GEMFIRE),
				eq(mockSnapshotOptions));
		}
	}

	@Test(expected = ExportSnapshotException.class)
	public void saveRegionSnapshotWithSnapshotFileAndFormatHandlesExceptionAppropriately() throws Exception {

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		doThrow(new IOException("TEST")).when(mockRegionSnapshotService).save(any(File.class),
			any(SnapshotFormat.class));

		RegionSnapshotServiceAdapter adapter = new RegionSnapshotServiceAdapter(mockRegionSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockRegionSnapshotService);

		try {
			adapter.save(snapshotDat, SnapshotFormat.GEMFIRE);
		}
		catch (ExportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to save snapshot to file [%1$s] in format [GEMFIRE]", snapshotDat));
			assertThat(expected.getCause()).isInstanceOf(IOException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockRegionSnapshotService, times(1)).save(eq(snapshotDat), eq(SnapshotFormat.GEMFIRE));
		}
	}

	@Test(expected = ExportSnapshotException.class)
	public void saveRegionSnapshotWithSnapshotFileFormatAndOptionsHandlesExceptionAppropriately() throws Exception {

		SnapshotOptions mockSnapshotOptions =
			mock(SnapshotOptions.class, "MockSnapshotOptions");

		RegionSnapshotService mockRegionSnapshotService =
			mock(RegionSnapshotService.class, "MockRegionSnapshotService");

		doThrow(new ClassCastException("TEST")).when(mockRegionSnapshotService).save(any(File.class),
			any(SnapshotFormat.class), any(SnapshotOptions.class));

		RegionSnapshotServiceAdapter adapter = new RegionSnapshotServiceAdapter(mockRegionSnapshotService);

		assertThat(adapter.getSnapshotService()).isEqualTo(mockRegionSnapshotService);

		try {
			adapter.save(snapshotDat, SnapshotFormat.GEMFIRE, mockSnapshotOptions);
		}
		catch (ExportSnapshotException expected) {

			assertThat(expected.getMessage()).isEqualTo(String.format(
				"Failed to save snapshot to file [%1$s] in format [GEMFIRE] using options [%2$s]",
				snapshotDat, mockSnapshotOptions));
			assertThat(expected.getCause()).isInstanceOf(ClassCastException.class);
			assertThat(expected.getCause().getMessage()).isEqualTo("TEST");

			throw expected;
		}
		finally {
			verify(mockRegionSnapshotService, times(1))
				.save(eq(snapshotDat), eq(SnapshotFormat.GEMFIRE), eq(mockSnapshotOptions));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void createSnapshotMetadataWithNullLocation() {

		try {
			new SnapshotMetadata(null, SnapshotFormat.GEMFIRE, mock(SnapshotFilter.class));
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Location is required");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void createSnapshotMetadataWithFileGemFireFormatAndNullFilter() {

		SnapshotMetadata snapshotMetadata = new SnapshotMetadata(snapshotDat, SnapshotFormat.GEMFIRE, null);

		assertThat(snapshotMetadata.getLocation()).isEqualTo(snapshotDat);
		assertThat(snapshotMetadata.isDirectory()).isFalse();
		assertThat(snapshotMetadata.isFile()).isTrue();
		assertThat(snapshotMetadata.getFormat()).isEqualTo(SnapshotFormat.GEMFIRE);
		assertThat(snapshotMetadata.isFilterPresent()).isFalse();
		assertThat(snapshotMetadata.getFilter()).isNull();
		assertThat(snapshotMetadata.isParallel()).isFalse();
	}

	@Test
	public void createSnapshotMetadataWithDirectoryNullFormatAndFilter() {

		SnapshotFilter mockSnapshotFilter = mock(SnapshotFilter.class, "MockSnapshotFilter");

		SnapshotMetadata snapshotMetadata =
			new SnapshotMetadata(FileSystemUtils.WORKING_DIRECTORY, null, mockSnapshotFilter);

		assertThat(snapshotMetadata.getLocation()).isEqualTo(FileSystemUtils.WORKING_DIRECTORY);
		assertThat(snapshotMetadata.isDirectory()).isTrue();
		assertThat(snapshotMetadata.isFile()).isFalse();
		assertThat(snapshotMetadata.getFormat()).isEqualTo(SnapshotFormat.GEMFIRE);
		assertThat(snapshotMetadata.isFilterPresent()).isTrue();
		assertThat(snapshotMetadata.getFilter()).isEqualTo(mockSnapshotFilter);
		assertThat(snapshotMetadata.isParallel()).isFalse();
	}

	@Test
	public void isJarFileIsTrue() {

		// JRE
		File runtimeDotJar = new File(new File(FileSystemUtils.JAVA_HOME, "lib"), "rt.jar");

		// JDK
		if (!runtimeDotJar.isFile()) {
			runtimeDotJar = new File(new File(new File(FileSystemUtils.JAVA_HOME, "jre"), "lib"), "rt.jar");
			assumeThat(runtimeDotJar.isFile()).isTrue();
		}

		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(runtimeDotJar)).isTrue();
	}

	@Test
	public void isJarFileIsFalse() throws Exception {

		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new File("/path/to/non-existing/file.jar"))).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new ClassPathResource("/cluster_config.zip").getFile())).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new File("to/file.tar"))).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new File("jar.file"))).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new File("  "))).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(new File(""))).isFalse();
		assertThat(ArchiveFileFilter.INSTANCE.isJarFile(null)).isFalse();
	}

	@Test
	public void getFileExtensionOfVariousFiles() throws Exception {

		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(new ClassPathResource("/cluster_config.zip").getFile())).isEqualTo("zip");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(new File("/path/to/non-existing/file.jar"))).isEqualTo("");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(new File("to/non-existing/file.tar"))).isEqualTo("");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(FileSystemUtils.WORKING_DIRECTORY)).isEqualTo("");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(new File("  "))).isEqualTo("");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(new File(""))).isEqualTo("");
		assertThat(ArchiveFileFilter.INSTANCE.getFileExtension(null)).isEqualTo("");
	}

	@Test
	public void archiveFileFilterAcceptsJarOrZipFile() throws Exception {
		assertThat(ArchiveFileFilter.INSTANCE.accept(new ClassPathResource("/cluster_config.zip").getFile())).isTrue();
	}

	@Test
	public void archiveFileFilterRejectsTarFile() {
		assertThat(ArchiveFileFilter.INSTANCE.accept(new File("/path/to/file.tar"))).isFalse();
	}

	protected static class TestSnapshotServiceAdapter extends SnapshotServiceAdapterSupport<Object, Object> {

		@Override
		protected File[] handleLocation(final SnapshotMetadata<Object, Object> configuration) {
			throw new UnsupportedOperationException("not implemented");
		}
	}
}
