/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.gemfire.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.cache.config.EnableGemfireCaching;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.data.gemfire.tests.support.IdentifierSequence;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Integration Tests testing the contractual behavior and combination of using Spring'a {@link CachePut} annotation
 * followed by a {@link CacheEvict} annotation on an application {@link @Service} component.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.cache.annotation.CacheEvict
 * @see org.springframework.cache.annotation.CachePut
 * @see org.springframework.cache.annotation.Caching
 * @see org.springframework.cache.annotation.EnableCaching
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see GemfireCache#evict(Object)
 * @see GemfireCache#put(Object, Object)
 * @see <a href="https://stackoverflow.com/questions/39830488/gemfire-entrynotfoundexception-for-cacheevict">Gemfire EntryNotFoundException on @CacheEvict</a>
 * @see <a href="https://jira.spring.io/browse/SGF-539">Change GemfireCache.evict(key) to call Region.remove(key)</a>
 * @since 1.9.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CompoundCachePutCacheEvictIntegrationTests.TestConfiguration.class)
@SuppressWarnings("unused")
public class CompoundCachePutCacheEvictIntegrationTests extends IntegrationTestsSupport {

	private Employee janeDoe;
	private Employee jonDoe;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	@Qualifier("Employees")
	private org.apache.geode.cache.Region<Long, Employee> employeesRegion;

	private void assertNoEmployeeInDepartment(Department department) {
		assertPeopleInDepartment(department);
	}

	private void assertPeopleInDepartment(Department department, Employee... people) {

		List<Employee> peopleInDepartment = employeeService.findByDepartment(department);

		assertThat(peopleInDepartment).isNotNull();
		assertThat(peopleInDepartment.size()).isEqualTo(people.length);
		assertThat(peopleInDepartment).contains(people);
	}

	private Employee newEmployee(String name, String mobile, Department department) {
		return newEmployee(IdentifierSequence.nextId(), name, mobile, department);
	}

	private Employee newEmployee(Long id, String name, String mobile, Department department) {
		Employee employee = Employee.newEmployee(department, mobile, name);
		employee.setId(id);
		return employee;
	}

	@Before
	public void setup() {

		janeDoe = employeeRepository.save(newEmployee("Jane Doe", "541-555-1234", Department.MARKETING));
		jonDoe = employeeRepository.save(newEmployee("Jon Doe", "972-555-1248", Department.ENGINEERING));

		assertThat(employeesRegion).containsValue(janeDoe);
		assertThat(employeesRegion).containsValue(jonDoe);
	}

	@Test
	public void janeDoeUpdateSuccessful() {

		assertNoEmployeeInDepartment(Department.DESIGN);
		assertThat(employeeService.isCacheMiss()).isTrue();

		janeDoe.setDepartment(Department.DESIGN);
		employeeService.update(janeDoe);

		assertThat(employeesRegion).containsValue(janeDoe);
		assertPeopleInDepartment(Department.DESIGN, janeDoe);
		assertThat(employeeService.isCacheMiss()).isTrue();

		assertThat(employeesRegion).containsValue(janeDoe);
		assertPeopleInDepartment(Department.DESIGN, janeDoe);
		assertThat(employeeService.isCacheMiss()).isFalse();
	}

	@Test
	public void jonDoeUpdateSuccessful() {

		jonDoe.setDepartment(Department.RESEARCH_DEVELOPMENT);
		employeeService.update(jonDoe);

		assertPeopleInDepartment(Department.RESEARCH_DEVELOPMENT, jonDoe);
		assertThat(employeeService.isCacheMiss()).isTrue();

		assertPeopleInDepartment(Department.RESEARCH_DEVELOPMENT, jonDoe);
		assertThat(employeeService.isCacheMiss()).isFalse();
	}

	@Configuration
	@EnableCaching
	@Import(TestConfiguration.class)
	static class Sgf539WorkaroundConfiguration {

		@Bean
		GemfireCacheManager cacheManager(GemFireCache gemfireCache) {

			GemfireCacheManager cacheManager = new GemfireCacheManager() {

				@Override
				protected org.springframework.cache.Cache decorateCache(org.springframework.cache.Cache cache) {

					return new GemfireCache((org.apache.geode.cache.Region<?, ?>) cache.getNativeCache()) {

						@Override
						public void evict(Object key) {
							getNativeCache().remove(key);
						}
					};
				}
			};

			cacheManager.setCache(gemfireCache);

			return cacheManager;
		}
	}

	@Configuration
	@Import(GemFireConfiguration.class)
	static class TestConfiguration {

		@Bean
		GemfireRepositoryFactoryBean<EmployeeRepository, Employee, Long> personRepository() {

			GemfireRepositoryFactoryBean<EmployeeRepository, Employee, Long> personRepository =
				new GemfireRepositoryFactoryBean<>(EmployeeRepository.class);

			personRepository.setGemfireMappingContext(new GemfireMappingContext());

			return personRepository;
		}

		@Bean
		EmployeeService peopleService(EmployeeRepository personRepository) {
			return new EmployeeService(personRepository);
		}
	}

	@ClientCacheApplication
	@EnableCachingDefinedRegions(clientRegionShortcut = ClientRegionShortcut.LOCAL)
	@EnableEntityDefinedRegions(basePackageClasses = Employee.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
	@EnableGemfireCaching
	static class GemFireConfiguration { }

	public enum Department {

		ACCOUNTING,
		DESIGN,
		ENGINEERING,
		LEGAL,
		MANAGEMENT,
		MARKETING,
		RESEARCH_DEVELOPMENT,
		SALES

	}

	@Data
	@Region("Employees")
	@RequiredArgsConstructor(staticName = "newEmployee")
	public static class Employee implements Serializable {

		@Id
		private Long id;

		@NonNull private Department department;
		@NonNull private String mobile;
		@NonNull private String name;

	}

	@Service
	public static class EmployeeService extends CacheableService {

		private final EmployeeRepository employeeRepository;

		public EmployeeService(EmployeeRepository employeeRepository) {
			this.employeeRepository = employeeRepository;
		}

		@Cacheable("DepartmentEmployees")
		public List<Employee> findByDepartment(Department department) {
			setCacheMiss();
			return employeeRepository.findByDepartment(department);
		}

		@Cacheable("MobileEmployees")
		public Employee findByMobile(String mobile) {
			setCacheMiss();
			return employeeRepository.findByMobile(mobile);
		}

		@Caching(
			evict = @CacheEvict(value = "DepartmentEmployees", key = "#p0.department"),
			put = @CachePut(value = "MobileEmployees", key="#p0.mobile")
		)
		public Employee update(Employee employee) {
			return employeeRepository.save(employee);
		}
	}

	protected static abstract class CacheableService {

		private final AtomicBoolean cacheMiss = new AtomicBoolean(false);

		public boolean isCacheMiss() {
			return cacheMiss.compareAndSet(true, false);
		}

		public boolean isNotCacheMiss() {
			return !isCacheMiss();
		}

		protected void setCacheMiss() {
			this.cacheMiss.set(true);
		}
	}

	public interface EmployeeRepository extends CrudRepository<Employee, Long> {

		List<Employee> findByDepartment(Department department);

		Employee findByMobile(String mobile);

	}
}
