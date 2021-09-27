/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.gemfire.repository.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.gemfire.GemfireAccessor;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactory;
import org.springframework.data.gemfire.repository.support.SimpleGemfireRepository;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * Unit Tests for {@link GemfireRepositoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.data.gemfire.repository.cdi.GemfireRepositoryBean
 * @since 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GemfireRepositoryBeanTest {

	@Mock
	private BeanManager mockBeanManager;

	private CustomRepositoryImplementationDetector newCustomRepositoryImplementationDetector() {
		return new CustomRepositoryImplementationDetector(new StandardEnvironment(), new DefaultResourceLoader());
	}

	private <T> Set<T> toSet(Iterable<T> collection) {
		return CollectionUtils.addAll(new HashSet<>(), collection);
	}

	@Test
	public void getDependencyInstanceGetsReference() {

		Bean<Region> mockRegionBean = mock(Bean.class);

		CreationalContext<Region> mockCreationalContext = mock(CreationalContext.class);

		Region mockRegion = mock(Region.class);

		when(mockBeanManager.createCreationalContext(eq(mockRegionBean))).thenReturn(mockCreationalContext);
		when(mockBeanManager.getReference(eq(mockRegionBean), eq(Region.class), eq(mockCreationalContext)))
			.thenReturn(mockRegion);

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), null, null);

		assertThat(repositoryBean.getDependencyInstance(mockRegionBean, Region.class)).isEqualTo(mockRegion);

		verify(mockBeanManager, times(1)).createCreationalContext(eq(mockRegionBean));
		verify(mockBeanManager, times(1))
			.getReference(eq(mockRegionBean), eq(Region.class), eq(mockCreationalContext));
	}

	@Test
	public void resolveGemfireMappingContextUsesDefault() {

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), null, null);

		assertThat(repositoryBean.resolveGemfireMappingContext())
			.isEqualTo(GemfireRepositoryBean.DEFAULT_GEMFIRE_MAPPING_CONTEXT);
	}

	@Test
	public void resolveGemfireMappingContextUsesQualifiedMappingContext() {

		Bean<GemfireMappingContext> mockMappingContextBean = mock(Bean.class);

		CreationalContext<GemfireMappingContext> mockCreationalContext = mock(CreationalContext.class);

		GemfireMappingContext expectedGemfireMappingContext = new GemfireMappingContext();

		when(mockBeanManager.createCreationalContext(eq(mockMappingContextBean))).thenReturn(mockCreationalContext);
		when(mockBeanManager.getReference(eq(mockMappingContextBean), eq(GemfireMappingContext.class),
			eq(mockCreationalContext))).thenReturn(expectedGemfireMappingContext);

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), mockMappingContextBean, null);

		GemfireMappingContext actualGemfireMappingContext = repositoryBean.resolveGemfireMappingContext();

		assertThat(actualGemfireMappingContext).isEqualTo(expectedGemfireMappingContext);

		verify(mockBeanManager, times(1)).createCreationalContext(eq(mockMappingContextBean));
		verify(mockBeanManager, times(1)).getReference(eq(mockMappingContextBean), eq(GemfireMappingContext.class),
			eq(mockCreationalContext));
	}

	@Test
	public void resolveGemfireRegions() {

		Region mockRegionOne = mock(Region.class);
		Region mockRegionTwo = mock(Region.class);

		CreationalContext<Bean<Region>> mockCreationalContext = mock(CreationalContext.class);

		Bean<Region> mockRegionBeanOne = mock(Bean.class);
		Bean<Region> mockRegionBeanTwo = mock(Bean.class);

		when(mockRegionBeanOne.getTypes()).thenReturn(CollectionUtils.asSet(Region.class));
		when(mockRegionBeanTwo.getTypes()).thenReturn(CollectionUtils.asSet(Region.class));
		when(mockBeanManager.createCreationalContext(any(Bean.class))).thenReturn(mockCreationalContext);
		when(mockBeanManager.getReference(eq(mockRegionBeanOne), eq(Region.class), eq(mockCreationalContext)))
			.thenReturn(mockRegionOne);
		when(mockBeanManager.getReference(eq(mockRegionBeanTwo), eq(Region.class), eq(mockCreationalContext)))
			.thenReturn(mockRegionTwo);

		GemfireRepositoryBean repositoryBean = new GemfireRepositoryBean(this.mockBeanManager, PersonRepository.class,
			Collections.emptySet(), newCustomRepositoryImplementationDetector(), null,
				CollectionUtils.asSet(mockRegionBeanOne, mockRegionBeanTwo));

		Iterable<Region> regions = repositoryBean.resolveGemfireRegions();

		assertThat(regions).isNotNull();
		assertThat(toSet(regions).containsAll(CollectionUtils.asSet(mockRegionOne, mockRegionTwo))).isTrue();

		verify(mockRegionBeanOne, times(1)).getTypes();
		verify(mockRegionBeanTwo, times(1)).getTypes();
		verify(mockBeanManager, times(1)).createCreationalContext(eq(mockRegionBeanOne));
		verify(mockBeanManager, times(1)).createCreationalContext(eq(mockRegionBeanTwo));
		verify(mockBeanManager, times(1)).getReference(eq(mockRegionBeanOne), eq(Region.class),
			eq(mockCreationalContext));
		verify(mockBeanManager, times(1)).getReference(eq(mockRegionBeanTwo), eq(Region.class),
			eq(mockCreationalContext));
	}

	@Test
	public void resolveTypeFindsTargetComponentType() {

		Bean mockBean = mock(Bean.class);

		when(mockBean.getTypes())
			.thenReturn(CollectionUtils.asSet((Type) Object.class, Map.class, ConcurrentMap.class, Region.class));

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), null, null);

		assertThat(repositoryBean.resolveType(mockBean, Region.class)).isEqualTo(Region.class);
		assertThat(repositoryBean.resolveType(mockBean, Map.class)).isIn(Map.class, ConcurrentMap.class, Region.class);

		verify(mockBean, times(2)).getTypes();
	}

	@Test
	public void resolveTypeWithParameterizedType() {

		Bean<Map> mockBean = mock(Bean.class);

		Map<Long, Object> parameterizedTypeMap = Collections.emptyMap();

		ParameterizedType mockParameterizedType = mock(ParameterizedType.class);

		assertThat(parameterizedTypeMap.getClass()).isInstanceOf(Type.class);
		assertThat(parameterizedTypeMap.getClass().getGenericSuperclass()).isInstanceOf(ParameterizedType.class);
		assertThat(parameterizedTypeMap.getClass().getTypeParameters().length).isEqualTo(2);

		when(mockBean.getTypes()).thenReturn(CollectionUtils.asSet(mockParameterizedType));
		when(mockParameterizedType.getRawType()).thenReturn(parameterizedTypeMap.getClass());

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), null, null);

		assertThat(repositoryBean.resolveType(mockBean, Map.class)).isEqualTo(mockParameterizedType);

		verify(mockBean, times(1)).getTypes();
		verify(mockParameterizedType, times(1)).getRawType();
	}

	@Test(expected = IllegalStateException.class)
	public void resolveTypeWithUnresolvableType() {

		Bean mockBean = mock(Bean.class);

		when(mockBean.getTypes()).thenReturn(CollectionUtils.asSet((Type) Map.class, Object.class));

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class, Collections.emptySet(),
				newCustomRepositoryImplementationDetector(), null, null);

		try {
			repositoryBean.resolveType(mockBean, Region.class);
		}
		catch (IllegalStateException expected) {

			assertThat(expected)
				.hasMessage("unable to resolve bean instance of type [%1$s] from bean definition [%2$s]",
					Region.class, mockBean);

			assertThat(expected).hasNoCause();

			throw expected;
		}
		finally {
			verify(mockBean, times(1)).getTypes();
		}
	}

	@Test
	// IntegrationTest
	public void createGemfireRepositoryInstanceSuccessfully() {

		Bean<Region> mockRegionBean = mock(Bean.class);

		CreationalContext<Bean<Region>> mockCreationalContext = mock(CreationalContext.class);

		final Region mockRegion = mock(Region.class);

		RegionAttributes mockRegionAttributes = mock(RegionAttributes.class);

		when(mockRegion.getName()).thenReturn("Person");
		when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
		when(mockRegionAttributes.getKeyConstraint()).thenReturn(Long.class);
		when(mockRegionBean.getTypes()).thenReturn(CollectionUtils.asSet(Region.class));
		when(mockBeanManager.createCreationalContext(any(Bean.class))).thenReturn(mockCreationalContext);
		when(mockBeanManager.getReference(eq(mockRegionBean), eq(Region.class), eq(mockCreationalContext)))
			.thenReturn(mockRegion);

		final AtomicBoolean repositoryProxyPostProcessed = new AtomicBoolean(false);

		GemfireRepositoryBean<PersonRepository> repositoryBean =
			new GemfireRepositoryBean<>(this.mockBeanManager, PersonRepository.class,
				Collections.emptySet(), newCustomRepositoryImplementationDetector(), null,
					CollectionUtils.asSet(mockRegionBean))
		{

			@Override
			GemfireRepositoryFactory newGemfireRepositoryFactory() {

				GemfireRepositoryFactory gemfireRepositoryFactory = super.newGemfireRepositoryFactory();

				gemfireRepositoryFactory.addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {
					try {
						assertThat(repositoryInformation.getRepositoryInterface()).isEqualTo(PersonRepository.class);
						assertThat(repositoryInformation.getRepositoryBaseClass())
							.isEqualTo(SimpleGemfireRepository.class);
						assertThat(repositoryInformation.getDomainType()).isEqualTo(Person.class);
						assertThat(repositoryInformation.getIdType()).isEqualTo(Long.class);
						assertThat(factory.getTargetClass()).isEqualTo(SimpleGemfireRepository.class);

						Object gemfireRepository = factory.getTargetSource().getTarget();

						GemfireAccessor gemfireAccessor = TestUtils.readField("template", gemfireRepository);

						assertThat(gemfireAccessor).isNotNull();
						assertThat(gemfireAccessor.getRegion()).isEqualTo(mockRegion);

						repositoryProxyPostProcessed.set(true);
					}
					catch (Exception cause) {
						throw new RuntimeException(cause);
					}
				});

				return gemfireRepositoryFactory;
			}
		};

		GemfireRepository<Person, Long> gemfireRepository =
			repositoryBean.create(null, PersonRepository.class);

		assertThat(gemfireRepository).isNotNull();
		assertThat(repositoryProxyPostProcessed.get()).isTrue();

		verify(mockBeanManager, times(1)).createCreationalContext(eq(mockRegionBean));
		verify(mockBeanManager, times(1)).getReference(eq(mockRegionBean), eq(Region.class),
			eq(mockCreationalContext));
		verify(mockRegionBean, times(1)).getTypes();
		verify(mockRegion, times(1)).getName();
		verify(mockRegion, times(1)).getAttributes();
		verify(mockRegionAttributes, times(1)).getKeyConstraint();
	}

	@SuppressWarnings("unused")
	static class TestMap extends AbstractMap<Long, Object> {

		@Override
		public Set<Entry<Long, Object>> entrySet() {
			return Collections.emptySet();
		}
	}

	static class Person {}

	interface PersonRepository extends GemfireRepository<Person, Long> { }

}
