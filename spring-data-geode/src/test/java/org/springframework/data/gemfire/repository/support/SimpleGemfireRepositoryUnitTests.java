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
package org.springframework.data.gemfire.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.query.SelectResults;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.repository.Wrapper;
import org.springframework.data.gemfire.repository.sample.Animal;
import org.springframework.data.gemfire.repository.sample.Identifiable;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.slf4j.Logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Unit Tests for {@link SimpleGemfireRepository}.
 *
 * @author John Blum
 * @see java.util.function.Function
 * @see java.util.stream.Stream
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.Region
 * @see org.springframework.data.domain.Page
 * @see org.springframework.data.domain.Pageable
 * @see org.springframework.data.domain.Sort
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.Wrapper
 * @see org.springframework.data.gemfire.repository.support.SimpleGemfireRepository
 * @see org.springframework.data.repository.core.EntityInformation
 * @since 1.4.5
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleGemfireRepositoryUnitTests {

	private final AtomicLong idSequence = new AtomicLong(0L);

	private @NonNull Map<Long, Animal> asMap(@Nullable Iterable<Animal> animals) {

		Map<Long, Animal> animalMap = new HashMap<>();

		for (Animal animal : CollectionUtils.nullSafeIterable(animals)) {
			animalMap.put(animal.getId(), animal);
		}

		return animalMap;
	}

	private Animal newAnimal(String name) {

		Animal animal = new Animal();

		animal.setId(this.idSequence.incrementAndGet());
		animal.setName(name);

		return animal;
	}

	private Animal newAnimal(Long id, String name) {

		Animal animal = newAnimal(name);

		animal.setId(id);

		return animal;
	}

	private GemfireTemplate newGemfireTemplate(Region<?, ?> region) {
		return new GemfireTemplate(region);
	}

	private <ID, T extends Identifiable<ID>> Wrapper<T, ID> newWrapper(T entity) {
		return new Wrapper<>(entity, entity.getId());
	}

	private Cache mockCache(String name, boolean transactionExists) {

		Cache mockCache = mock(Cache.class, String.format("%s.MockCache", name));

		CacheTransactionManager mockCacheTransactionManager = mock(CacheTransactionManager.class,
			String.format("%s.MockCacheTransactionManager", name));

		doReturn(mockCacheTransactionManager).when(mockCache).getCacheTransactionManager();
		doReturn(transactionExists).when(mockCacheTransactionManager).exists();

		return mockCache;
	}

	private EntityInformation<Animal, Long> mockEntityInformation() {

		EntityInformation<Animal, Long> mockEntityInformation = mock(EntityInformation.class);

		doAnswer(new Answer<Long>() {

			private final AtomicLong idSequence = new AtomicLong(0L);

			@Override
			public Long answer(InvocationOnMock invocation) {
				Animal argument = invocation.getArgument(0);
				argument.setId(resolveId(argument.getId()));
				return argument.getId();

			}

			private Long resolveId(Long id) {
				return id != null ? id : idSequence.incrementAndGet();
			}

		}).when(mockEntityInformation).getRequiredId(any(Animal.class));

		return mockEntityInformation;
	}

	private Region mockRegion() {
		return mockRegion("MockRegion");
	}

	private Region mockRegion(String name) {

		Region mockRegion = mock(Region.class, String.format("%s.MockRegion", name));

		doReturn(name).when(mockRegion).getName();
		doReturn(RegionUtils.toRegionPath(name)).when(mockRegion).getFullPath();

		return mockRegion;
	}

	private Region mockRegion(String name, Cache mockCache, DataPolicy dataPolicy) {

		Region mockRegion = mockRegion(name);

		doReturn(mockCache).when(mockRegion).getRegionService();

		RegionAttributes mockRegionAttributes = mock(RegionAttributes.class,
			String.format("%s.MockRegionAttributes", name));

		doReturn(mockRegionAttributes).when(mockRegion).getAttributes();
		doReturn(dataPolicy).when(mockRegionAttributes).getDataPolicy();

		return mockRegion;
	}

	@Test
	public void constructSimpleGemfireRepositorySuccessfully() {

		Region mockRegion = mockRegion();

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		EntityInformation mockEntityInformation = mockEntityInformation();

		SimpleGemfireRepository repository = new SimpleGemfireRepository(template, mockEntityInformation);

		assertThat(repository).isNotNull();
		assertThat(repository.getEntityInformation()).isEqualTo(mockEntityInformation);
		assertThat(repository.getLogger()).isNotNull();
		assertThat(repository.getTemplate()).isEqualTo(template);

		verifyNoInteractions(template, mockRegion, mockEntityInformation);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructSimpleGemfireRepositoryWithNullTemplateThrowsIllegalArgumentException() {

		try {
			new SimpleGemfireRepository<>(null, mockEntityInformation());
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("GemfireTemplate must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructSimpleGemfireRepositoryWithNullEntityInformationThrowsIllegalArgumentException() {

		try {
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("EntityInformation must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void getRegionFromTemplate() {

		Region<Long, Animal> mockRegion = mockRegion();

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		EntityInformation<Animal, Long> mockEntityInformation = mockEntityInformation();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation);

		assertThat(repository).isNotNull();
		assertThat(repository.getTemplate()).isEqualTo(template);
		assertThat(repository.getRegion()).isEqualTo(mockRegion);

		verify(template, times(1)).getRegion();
		verifyNoInteractions(mockRegion, mockEntityInformation);
		verifyNoMoreInteractions(template);
	}

	@Test
	public void saveEntitySuccessfully() {

		Animal cat = newAnimal(1L, "cat");

		Logger mockLogger = mock(Logger.class);

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			spy(new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation()));

		doReturn(mockLogger).when(repository).getLogger();
		doReturn(true).when(mockLogger).isDebugEnabled();
		doReturn(cat).when(mockRegion).put(anyLong(), any());

		Animal dog = repository.save(newAnimal(1L, "dog"));

		assertThat(dog).isNotNull();
		assertThat(dog.getId().longValue()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("dog");

		verify(mockLogger, times(1)).isDebugEnabled();
		verify(mockLogger, times(1))
			.debug(eq("Overwrote existing value [{}] for ID [{}]"), eq(cat), eq(1L));
		verify(mockRegion, times(1)).put(eq(1L), eq(dog));
		verifyNoMoreInteractions(mockLogger, mockRegion);
	}

	@Test
	public void saveEntitiesSuccessfully() {

		List<Animal> animals = new ArrayList<>(3);

		animals.add(newAnimal(1L, "bird"));
		animals.add(newAnimal(2L, "cat"));
		animals.add(newAnimal(3L, "dog"));

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> savedAnimals = repository.saveAll(animals);

		assertThat(savedAnimals).isNotNull();
		assertThat(savedAnimals).isNotSameAs(animals);
		assertThat(savedAnimals).containsAll(animals);

		verify(mockRegion, times(1)).putAll(eq(asMap(savedAnimals)));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void saveWrapperSuccessfully() {

		Animal dog = newAnimal(2L, "dog");

		Logger mockLogger = mock(Logger.class);

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			spy(new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation()));

		doReturn(mockLogger).when(repository).getLogger();
		doReturn(true).when(mockLogger).isDebugEnabled();
		doReturn(dog).when(mockRegion).put(anyLong(), any());

		Animal cat = repository.save(newWrapper(newAnimal(2L, "cat")));

		assertThat(cat).isNotNull();
		assertThat(cat.getId()).isEqualTo(2L);
		assertThat(cat.getName()).isEqualTo("cat");

		verify(mockLogger, times(1)).isDebugEnabled();
		verify(mockLogger, times(1))
			.debug(eq("Overwrote existing value [{}] for ID [{}]"), eq(dog), eq(2L));
		verify(mockRegion, times(1)).put(eq(2L), eq(cat));
		verifyNoMoreInteractions(mockLogger, mockRegion);
	}

	@Test
	public void saveAllEntitiesWithIterableContainingNullEntitiesIsNullSafe() {

		List<Animal> animals = new ArrayList<>();

		animals.add(newAnimal(1L, "cat"));
		animals.add(null);
		animals.add(newAnimal(2L, "dog"));

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> savedAnimals = repository.saveAll(animals);

		assertThat(savedAnimals).isNotNull();
		assertThat(savedAnimals).isNotSameAs(animals);
		assertThat(savedAnimals).hasSize(2);
		assertThat(savedAnimals).containsAll(Arrays.asList(animals.get(0), animals.get(2)));

		verify(mockRegion, times(1))
			.putAll(eq(asMap(Arrays.asList(animals.get(0), animals.get(2)))));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void saveAllEntitiesWithNullIterableIsNullSafe() {

		Region<Long, Animal> mockRegion = mock(Region.class);

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<?> iterable = repository.saveAll(null);

		assertThat(iterable).isNotNull();
		assertThat(iterable).isEmpty();

		verifyNoInteractions(mockRegion);
	}

	@Test
	public void countReturnsNumberOfRegionEntries() {

		SelectResults mockSelectResults = mock(SelectResults.class);

		Region mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		doReturn(mockSelectResults).when(template).find(eq("SELECT count(*) FROM /Example"));
		doReturn(Collections.singletonList(21).iterator()).when(mockSelectResults).iterator();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.count()).isEqualTo(21L);

		verify(template, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(template, times(1)).find(eq("SELECT count(*) FROM /Example"));
		verify(mockSelectResults, times(1)).iterator();
		verifyNoMoreInteractions(mockRegion, mockSelectResults, template);
	}

	@Test
	public void countWhenSelectResultsAreNullIsNullSafeAndReturnsZero() {

		Region mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		doReturn(null).when(template).find(anyString());

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.count()).isEqualTo(0L);

		verify(template, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(template, times(1)).find(eq("SELECT count(*) FROM /Example"));
		verifyNoMoreInteractions(mockRegion, template);
	}

	@Test
	public void countWhenSelectResultsIteratorIsNullIsNullSafeAndReturnsZero() {

		SelectResults mockSelectResults = mock(SelectResults.class);

		Region mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		doReturn(mockSelectResults).when(template).find(anyString());
		doReturn(null).when(mockSelectResults).iterator();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.count()).isEqualTo(0L);

		verify(template, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(template, times(1)).find(eq("SELECT count(*) FROM /Example"));
		verify(mockSelectResults, times(1)).iterator();
		verifyNoMoreInteractions(mockRegion, mockSelectResults, template);
	}

	@Test
	public void countWhenSelectResultsIteratorIsEmptyReturnsZero() {

		SelectResults mockSelectResults = mock(SelectResults.class);

		Region mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		doReturn(mockSelectResults).when(template).find(anyString());
		doReturn(Collections.emptyIterator()).when(mockSelectResults).iterator();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.count()).isEqualTo(0L);

		verify(template, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(template, times(1)).find(eq("SELECT count(*) FROM /Example"));
		verify(mockSelectResults, times(1)).iterator();
		verifyNoMoreInteractions(mockRegion, mockSelectResults, template);
	}

	@Test
	public void countWhenSelectResultsIteratorContainsNullIsNullSafeReturnsZero() {

		SelectResults mockSelectResults = mock(SelectResults.class);

		Region mockRegion = mockRegion("Example");

		Iterator mockIterator = mock(Iterator.class);

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		doReturn(mockSelectResults).when(template).find(anyString());
		doReturn(mockIterator).when(mockSelectResults).iterator();
		doReturn(true).when(mockIterator).hasNext();
		doReturn(null).when(mockIterator).next();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.count()).isEqualTo(0L);

		verify(template, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(template, times(1)).find(eq("SELECT count(*) FROM /Example"));
		verify(mockSelectResults, times(1)).iterator();
		verify(mockIterator, times(1)).hasNext();
		verify(mockIterator, times(1)).next();
		verifyNoMoreInteractions(mockIterator, mockRegion, mockSelectResults, template);
	}

	@Test
	public void existsByIdCallsFindById() {

		Animal dog = newAnimal(1L, "dog");

		Region<Long, Animal> mockRegion = mockRegion();

		doAnswer(invocation -> dog.getId().equals(invocation.getArgument(0)) ? dog : null)
			.when(mockRegion).get(anyLong());

		SimpleGemfireRepository<Animal, Long> repository =
			spy(new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation()));

		assertThat(repository.existsById(1L)).isTrue();
		assertThat(repository.existsById(2L)).isFalse();
		assertThat(repository.existsById(10L)).isFalse();

		verify(repository, times(1)).findById(eq(1L));
		verify(mockRegion, times(1)).get(eq(1L));
		verify(repository, times(1)).findById(eq(2L));
		verify(mockRegion, times(1)).get(eq(2L));
		verify(repository, times(1)).findById(eq(10L));
		verify(mockRegion, times(1)).get(eq(10L));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void findAllSuccessfully() {

		GemfireTemplate mockTemplate = mock(GemfireTemplate.class);

		List<Object> results = Arrays.asList("test", "mock", "fake");

		Region mockRegion = mockRegion("Example");

		SelectResults mockSelectResults = mock(SelectResults.class);

		doReturn(mockRegion).when(mockTemplate).getRegion();
		doReturn(mockSelectResults).when(mockTemplate).find(anyString());
		doReturn(results).when(mockSelectResults).asList();

		SimpleGemfireRepository repository = new SimpleGemfireRepository(mockTemplate, mockEntityInformation());

		Iterable<Object> returnValue = repository.findAll();

		assertThat(returnValue).isNotNull();
		assertThat(returnValue).containsAll(results);

		verify(mockTemplate, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(mockTemplate, times(1)).find(eq("SELECT * FROM /Example"));
		verify(mockSelectResults, times(1)).asList();
		verifyNoMoreInteractions(mockRegion, mockSelectResults, mockTemplate);
	}

	@Test
	public void findAllOrderedBySortSuccessfully() {

		GemfireTemplate mockTemplate = mock(GemfireTemplate.class);

		List<Object> results = Arrays.asList("test", "mock", "fake");

		Region mockRegion = mockRegion("Example");

		SelectResults mockSelectResults = mock(SelectResults.class);

		doReturn(mockRegion).when(mockTemplate).getRegion();
		doReturn(mockSelectResults).when(mockTemplate).find(anyString());
		doReturn(results).when(mockSelectResults).asList();

		SimpleGemfireRepository repository = new SimpleGemfireRepository(mockTemplate, mockEntityInformation());

		Iterable<Object> returnValue = repository.findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.desc("birthDate")));

		assertThat(returnValue).isNotNull();
		assertThat(returnValue).containsAll(results);

		verify(mockTemplate, times(1)).getRegion();
		verify(mockRegion, times(1)).getFullPath();
		verify(mockTemplate, times(1))
			.find(eq("SELECT DISTINCT * FROM /Example ORDER BY name ASC, birthDate DESC"));
		verify(mockSelectResults, times(1)).asList();
		verifyNoMoreInteractions(mockRegion, mockSelectResults, mockTemplate);
	}

	// Page Numbers are 0 based indexed
	private void assertPage(Page page, int pageNumber, int pageSize, int total, Sort orderBy, User... content) {

		int totalPages = (total / pageSize) + (total % pageSize > 0 ? 1 : 0);

		assertThat(page).isNotNull();
		assertThat(page).isNotEmpty();
		assertThat(page.getNumber()).isEqualTo(pageNumber);
		assertThat(page.getNumberOfElements()).isEqualTo(content.length);
		assertThat(page.getSize()).isEqualTo(pageSize);
		assertThat(page.getSort()).isEqualTo(orderBy);
		assertThat(page.getTotalElements()).isEqualTo(total);
		assertThat(page.getTotalPages()).isEqualTo(totalPages);
		assertThat(page.getContent()).containsExactly(content);
	}

	@Test
	public void findAllPagedSuccessfully() {

		Sort orderBy = Sort.by("name");

		Region mockRegion = mockRegion();

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber(); // page 1
		doReturn(5).when(mockPageable).getPageSize();
		doReturn(orderBy).when(mockPageable).getSort();

		EntityInformation mockEntityInformation = mockEntityInformation();

		SimpleGemfireRepository repository =
			spy(new SimpleGemfireRepository(newGemfireTemplate(mockRegion), mockEntityInformation));

		List<User> users = Arrays.asList(
			User.newUser("Jon Doe"),
			User.newUser("Jane Doe"),
			User.newUser("Cookie Doe"),
			User.newUser("Fro Doe"),
			User.newUser("Lan Doe"),
			User.newUser("Pie Doe"),
			User.newUser("Sour Doe")
		);

		doReturn(users).when(repository).findAll(eq(orderBy));

		Page pageOne = repository.findAll(mockPageable);

		assertPage(pageOne, 0, 5, users.size(), orderBy,
			User.newUser("Jon Doe"),
			User.newUser("Jane Doe"),
			User.newUser("Cookie Doe"),
			User.newUser("Fro Doe"),
			User.newUser("Lan Doe")
		);

		doReturn(1).when(mockPageable).getPageNumber(); // page 2

		Page pageTwo = repository.findAll(mockPageable);

		assertPage(pageTwo, 1, 5, users.size(), orderBy,
			User.newUser("Pie Doe"),
			User.newUser("Sour Doe")
		);

		doReturn(2).when(mockPageable).getPageNumber(); // page 2

		Page pageThree = repository.findAll(mockPageable);

		assertThat(pageThree).isNotNull();
		assertThat(pageThree).isEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void findAllPagedWithNullPageableThrowsIllegalArgumentException() {

		try {
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
				.findAll((Pageable) null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pageable must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void findAllByIdSuccessfully() {

		Map<Long, Animal> animals = Stream.of(
			newAnimal(1L, "bird"),
			newAnimal(2L, "cat"),
			newAnimal(3L, "dog")
		).collect(Collectors.toMap(Animal::getId, Function.identity()));

		Region<Long, Animal> mockRegion = mockRegion();

		doAnswer(invocation -> {

			Collection<Long> keys = invocation.getArgument(0);

			return animals.values().stream()
				.filter((animal -> keys.contains(animal.getId())))
				.collect(Collectors.toMap(Animal::getId, Function.identity()));

		}).when(mockRegion).getAll(any(Collection.class));

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> animalsFound = repository.findAllById(Arrays.asList(1L, 3L));

		assertThat(animalsFound).isNotNull();
		assertThat(animalsFound).hasSize(2);
		assertThat(animalsFound).contains(animals.get(1L), animals.get(3L));

		verify(mockRegion, times(1)).getAll(eq(Arrays.asList(1L, 3L)));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void findAllByIdReturnsNoMatches() {

		Region<Long, Animal> mockRegion = mockRegion();

		doReturn(Collections.emptyMap()).when(mockRegion).getAll(any(Collection.class));

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> animalsFound = repository.findAllById(Arrays.asList(1L, null, 2L, null, 3L));

		assertThat(animalsFound).isNotNull();
		assertThat(animalsFound).isEmpty();

		verify(mockRegion, times(1)).getAll(eq(Arrays.asList(1L, 2L, 3L)));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void findAllByIdReturnsPartialMatches() {

		Map<Long, Animal> animals = Stream.of(
			newAnimal(1L, "bird"),
			newAnimal(2L, "cat"),
			newAnimal(3L, "dog")
		).collect(Collectors.toMap(Animal::getId, Function.identity()));

		Region<Long, Animal> mockRegion = mockRegion();

		doAnswer(invocation -> {

			Collection<Long> keys = invocation.getArgument(0);

			return keys.stream()
				.filter(animals::containsKey)
				.collect(Collectors.toMap(Function.identity(), animals::get));

		}).when(mockRegion).getAll(any(Collection.class));

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> animalsFound = repository.findAllById(Arrays.asList(null, 0L, null, 1L, 2L, 4L, null));

		assertThat(animalsFound).isNotNull();
		assertThat(animalsFound).hasSize(2);
		assertThat(animalsFound).contains(animals.get(1L), animals.get(2L));

		verify(mockRegion, times(1)).getAll(eq(Arrays.asList(0L, 1L, 2L, 4L)));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void findAllByIdWithNullIterableIsNullSafe() {

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository(newGemfireTemplate(mockRegion), mockEntityInformation());

		Iterable<Animal> animals = repository.findAllById(null);

		assertThat(animals).isNotNull();
		assertThat(animals).isEmpty();

		verifyNoInteractions(mockRegion);
	}

	@Test
	public void findByIdSuccessfully() {

		Animal dog = newAnimal(1L, "dog");

		Region<Long, Animal> mockRegion = mockRegion();

		doAnswer(invocation -> dog.getId().equals(invocation.getArgument(0)) ? dog : null)
			.when(mockRegion).get(any(Long.class));

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		assertThat(repository.findById(1L).orElse(null)).isEqualTo(dog);
		assertThat(repository.findById(2L).isPresent()).isFalse();
		assertThat(repository.findById(10L).isPresent()).isFalse();

		verify(mockRegion, times(1)).get(eq(1L));
		verify(mockRegion, times(1)).get(eq(2L));
		verify(mockRegion, times(1)).get(eq(10L));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void findByIdWithNullIdIsNullSafe() {

		Region mockRegion = mockRegion();

		SimpleGemfireRepository repository =
			new SimpleGemfireRepository(newGemfireTemplate(mockRegion), mockEntityInformation());

		assertThat(repository.findById(null).isPresent()).isFalse();

		verifyNoInteractions(mockRegion);
	}

	@Test
	public void deleteByIdSuccessfully() {

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		repository.deleteById(1L);

		verify(mockRegion, times(1)).remove(eq(1L));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteEntitySuccessfully() {

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			spy(new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation()));

		repository.delete(newAnimal(1L, "dog"));

		verify(repository, times(1)).deleteById(eq(1L));
		verify(mockRegion, times(1)).remove(eq(1L));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteEntitiesSuccessfully() {

		Region<Long, Animal> mockRegion = mockRegion();

		SimpleGemfireRepository<Animal, Long> repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		repository.deleteAll(Arrays.asList(
			newAnimal(1L, "bird"),
			newAnimal(2L, "cat"),
			newAnimal(3L, "dog")
		));

		verify(mockRegion, times(1)).remove(eq(1L));
		verify(mockRegion, times(1)).remove(eq(2L));
		verify(mockRegion, times(1)).remove(eq(3L));
		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteAllWithClear() {

		Cache mockCache = mockCache("MockCache", false);

		Region<Long, Animal> mockRegion = mockRegion("MockRegion", mockCache, DataPolicy.REPLICATE);

		SimpleGemfireRepository<Animal, Long> gemfireRepository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		gemfireRepository.deleteAll();

		verify(mockCache, times(1)).getCacheTransactionManager();
		verify(mockRegion, times(2)).getAttributes();
		verify(mockRegion, times(2)).getRegionService();
		verify(mockRegion, times(1)).clear();
		verify(mockRegion, never()).keySet();
		verify(mockRegion, never()).keySetOnServer();
		verify(mockRegion, never()).removeAll(any(Collection.class));

		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteAllWithKeysWhenClearThrowsException() {

		Cache mockCache = mockCache("MockCache", false);

		Region<Long, Animal> mockRegion = mockRegion("MockRegion", mockCache, DataPolicy.PERSISTENT_REPLICATE);

		Set<Long> keys = new HashSet<>(Arrays.asList(1L, 2L, 3L));

		doThrow(new UnsupportedOperationException("TEST")).when(mockRegion).clear();
		when(mockRegion.keySet()).thenReturn(keys);

		SimpleGemfireRepository<Animal, Long> gemfireRepository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		gemfireRepository.deleteAll();

		verify(mockCache, times(1)).getCacheTransactionManager();
		verify(mockRegion, times(4)).getAttributes();
		verify(mockRegion, times(2)).getRegionService();
		verify(mockRegion, times(1)).clear();
		verify(mockRegion, times(1)).keySet();
		verify(mockRegion, never()).keySetOnServer();
		verify(mockRegion, times(1)).removeAll(eq(keys));
	}

	@Test
	public void deleteAllWithKeysWhenPartitionRegion() {

		Cache mockCache = mockCache("MockCache", false);

		Region<Long, Animal> mockRegion = mockRegion("MockRegion", mockCache, DataPolicy.PERSISTENT_PARTITION);

		Set<Long> keys = new HashSet<>(Arrays.asList(1L, 2L, 3L));

		when(mockRegion.keySet()).thenReturn(keys);

		SimpleGemfireRepository<Animal, Long> gemfireRepository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		gemfireRepository.deleteAll();

		verify(mockCache, times(0)).getCacheTransactionManager();
		verify(mockRegion, times(4)).getAttributes();
		verify(mockRegion, never()).getRegionService();
		verify(mockRegion, never()).clear();
		verify(mockRegion, times(1)).keySet();
		verify(mockRegion, never()).keySetOnServer();
		verify(mockRegion, times(1)).removeAll(eq(keys));
	}

	@Test
	public void deleteAllWithKeysWhenTransactionPresent() {

		Cache mockCache = mockCache("MockCache", true);

		Region<Long, Animal> mockRegion = mockRegion("MockRegion", mockCache, DataPolicy.REPLICATE);

		Set<Long> keys = new HashSet<>(Arrays.asList(1L, 2L, 3L));

		when(mockRegion.keySet()).thenReturn(keys);

		SimpleGemfireRepository<Animal, Long> gemfireRepository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		gemfireRepository.deleteAll();

		verify(mockCache, times(1)).getCacheTransactionManager();
		verify(mockRegion, times(4)).getAttributes();
		verify(mockRegion, times(2)).getRegionService();
		verify(mockRegion, never()).clear();
		verify(mockRegion, times(1)).keySet();
		verify(mockRegion, never()).keySetOnServer();
		verify(mockRegion, times(1)).removeAll(eq(keys));
	}

	@Test
	public void deleteAllWithKeySetOnServerWhenClientRegion() {

		Cache mockCache = mockCache("MockCache", false);

		Region<Long, Animal> mockRegion = mockRegion("MockRegion", mockCache, DataPolicy.EMPTY);

		RegionAttributes<Long, Animal> mockRegionAttributes = mockRegion.getAttributes();

		Set<Long> keys = new HashSet<>(Arrays.asList(1L, 2L, 3L));

		doThrow(new UnsupportedOperationException("TEST")).when(mockRegion).clear();
		doReturn("TestPool").when(mockRegionAttributes).getPoolName();
		doReturn(keys).when(mockRegion).keySetOnServer();

		SimpleGemfireRepository<Animal, Long> gemfireRepository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion), mockEntityInformation());

		gemfireRepository.deleteAll();

		verify(mockCache, times(1)).getCacheTransactionManager();
		verify(mockRegion, times(4)).getAttributes();
		verify(mockRegion, times(2)).getRegionService();
		verify(mockRegion, times(1)).clear();
		verify(mockRegion, times(1)).keySetOnServer();
		verify(mockRegion, never()).keySet();
		verify(mockRegion, times(1)).removeAll(eq(keys));

		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteAllByIdWithKeys() {

		Collection ids = Arrays.asList(1, 2, 3);
		Collection keys = new HashSet(ids);

		Region<?, ?> mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		SimpleGemfireRepository repository = new SimpleGemfireRepository(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.getTemplate()).isEqualTo(template);
		assertThat(template.getRegion()).isEqualTo(mockRegion);

		repository.deleteAllById(ids);

		InOrder order = inOrder(mockRegion, template);

		order.verify(template, times(1)).removeAll(eq(keys));
		order.verify(mockRegion, times(1)).removeAll(eq(keys));

		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteAllByIdWithMixedKeys() {

		Collection ids = Arrays.asList(1, null, 3);
		Collection keys = CollectionUtils.asSet(1, 3);

		Region<?, ?> mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		SimpleGemfireRepository repository = new SimpleGemfireRepository(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.getTemplate()).isEqualTo(template);
		assertThat(template.getRegion()).isEqualTo(mockRegion);

		repository.deleteAllById(ids);

		InOrder order = inOrder(mockRegion, template);

		order.verify(template, times(1)).removeAll(eq(keys));
		order.verify(mockRegion, times(1)).removeAll(eq(keys));

		verifyNoMoreInteractions(mockRegion);
	}

	@Test
	public void deleteAllByIdWithNullCollectionOfKeys() {

		Region<?, ?> mockRegion = mockRegion("Example");

		GemfireTemplate template = spy(newGemfireTemplate(mockRegion));

		SimpleGemfireRepository repository = new SimpleGemfireRepository(template, mockEntityInformation());

		assertThat(repository).isNotNull();
		assertThat(repository.getTemplate()).isEqualTo(template);
		assertThat(template.getRegion()).isEqualTo(mockRegion);

		repository.deleteAllById(null);

		verifyNoInteractions(mockRegion);
	}

	@Test
	@SuppressWarnings("all")
	public void toListFromIterable() {

		Set<User> userSet = CollectionUtils.asSet(User.newUser("Jon Doe"), User.newUser("Jane Doe"));

		List<User> userList = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((Iterable) userSet);

		assertThat(userList).isNotNull();
		assertThat(userList).isNotSameAs(userSet);
		assertThat(userList).hasSize(userSet.size());
		assertThat(userList).containsExactly(userSet.toArray(new User[0]));
	}

	@Test
	public void toListFromEmptyIterable() {

		List<User> users = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((Iterable) Collections::emptyIterator);

		assertThat(users).isNotNull();
		assertThat(users).isEmpty();
	}

	@Test
	public void toListFromList() {

		List<User> users = Collections.singletonList(User.newUser("Jon Doe"));

		List<User> userList = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((Iterable) users);

		assertThat(userList).isSameAs(users);
	}

	@Test
	public void toListFromNullIterableIsNullSafe() {

		List<User> users = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((Iterable) null);

		assertThat(users).isNotNull();
		assertThat(users).isEmpty();
	}

	@Test
	public void toListFromSelectResults() {

		List<User> users = Arrays.asList(User.newUser("Jon Doe"), User.newUser("Jane Doe"));

		SelectResults<User> mockSelectResults = mock(SelectResults.class);

		doReturn(users).when(mockSelectResults).asList();

		List<User> userList = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((SelectResults) mockSelectResults);

		assertThat(userList).isSameAs(users);

		verify(mockSelectResults, times(1)).asList();
		verifyNoMoreInteractions(mockSelectResults);
	}

	@Test
	public void toListFromNullSelectResultsIsNullSafe() {

		List<User> users = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mockEntityInformation())
			.toList((SelectResults) null);

		assertThat(users).isNotNull();
		assertThat(users).isEmpty();
	}

	@Test
	public void toPageFromIterable() {

		List<User> users = Arrays.asList(User.newUser("Jon Doe"), User.newUser("Jane Doe"));

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber(); // page 1
		doReturn(1).when(mockPageable).getPageSize();

		SimpleGemfireRepository repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class));

		Page<User> pageOne = repository.toPage(users, mockPageable);

		assertPage(pageOne, 0, 1, 2, null, users.get(0));

		doReturn(1).when(mockPageable).getPageNumber(); // page 2

		Page<User> pageTwo = repository.toPage(users, mockPageable);

		assertPage(pageTwo, 1, 1, 2, null, users.get(1));

		doReturn(2).when(mockPageable).getPageNumber(); // page 3

		Page<User> pageThree = repository.toPage(users, mockPageable);

		assertThat(pageThree).isNotNull();
		assertThat(pageThree).isEmpty();
	}

	@Test
	public void toPageFromEmptyIterable() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		Page<User> page = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class))
			.toPage(Collections.emptyList(), mockPageable);

		assertThat(page).isNotNull();
		assertThat(page).isEmpty();
	}

	@Test
	public void toPageFromNullIterableIsNullSafe() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(0).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		Page<User> page = new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class))
			.toPage(null, mockPageable);

		assertThat(page).isNotNull();
		assertThat(page).isEmpty();
	}

	@Test
	public void toPageWithOverflowFromIterable() {

		List<User> users = Arrays.asList(User.newUser("Jon Doe"), User.newUser("Jane Doe"));

		Pageable mockPageable = mock(Pageable.class);

		doReturn(true).when(mockPageable).isPaged();
		doReturn(0).when(mockPageable).getPageNumber(); // page 1
		doReturn(5).when(mockPageable).getPageSize();

		SimpleGemfireRepository repository =
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class));

		Page<User> pageOne = repository.toPage(users, mockPageable);

		assertPage(pageOne, 0, 5, 2, null, users.toArray(new User[0]));

		doReturn(1).when(mockPageable).getPageNumber(); // page 2

		Page<User> pageTwo = repository.toPage(users, mockPageable);

		assertThat(pageTwo).isNotNull();
		assertThat(pageTwo).isEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void toPageWithNullPageableThrowsIllegalArgumentException() {

		try {
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class))
				.toPage(Collections.emptyList(), null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Pageable must not be null");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void toPageWithInvalidPageNumberThrowsIllegalArgumentException() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(-1).when(mockPageable).getPageNumber();
		doReturn(5).when(mockPageable).getPageSize();

		try {
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class))
				.toPage(Collections.singletonList(User.newUser("Jon Doe")), mockPageable);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Page Number [-1] must be greater than equal to 0");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void toPageWithInvalidPageSizeThrowsIllegalArgumentException() {

		Pageable mockPageable = mock(Pageable.class);

		doReturn(1).when(mockPageable).getPageNumber();
		doReturn(-5).when(mockPageable).getPageSize();

		try {
			new SimpleGemfireRepository<>(newGemfireTemplate(mockRegion()), mock(EntityInformation.class))
				.toPage(Collections.singletonList(User.newUser("Jon Doe")), mockPageable);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Page Size [-5] must be greater than equal to 1");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Getter
	@ToString(of = "name")
	@EqualsAndHashCode(of = "name")
	@RequiredArgsConstructor(staticName = "newUser")
	@org.springframework.data.gemfire.mapping.annotation.Region("Users")
	static class User {

		@Id
		Long id;

		@NonNull
		final String name;

	}
}
