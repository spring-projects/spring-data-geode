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
package org.springframework.data.gemfire.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;
import org.springframework.data.gemfire.test.mock.annotation.EnableGemFireMockObjects;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import example.app.model.User;
import example.app.repo.UserRepository;

/**
 * Integration Tests testing and asserting that Apache Geode-based {@link Repository} factories,
 * implementing the {@link RepositoryFactoryInformation} interface, can in fact be looked up
 * in the Spring {@link ApplicationContext}.
 *
 * @author John Blum
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean
 * @see org.springframework.data.repository.Repository
 * @see org.springframework.data.repository.core.support.RepositoryFactoryInformation
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 1.9.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class GemFireRepositoryFactoryInformationIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void applicationContextContainsUserRepositoryBean() {
		assertThat(this.applicationContext.getBeanNamesForType(UserRepository.class)).contains("userRepository");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void canGetGemfireRepositoryBeansByRepositoryFactoryInformationType() {

		Map<String, RepositoryFactoryInformation> repositoryFactories =
			this.applicationContext.getBeansOfType(RepositoryFactoryInformation.class);

		assertThat(repositoryFactories).isNotNull();
		assertThat(repositoryFactories).isNotEmpty();
		assertThat(repositoryFactories).containsKeys("&userRepository");
		assertThat(repositoryFactories.get("&userRepository")).isInstanceOf(GemfireRepositoryFactoryBean.class);
	}

	@ClientCacheApplication(name = "GemFireRepositoryFactoryInformationIntegrationTests")
	@EnableGemFireMockObjects
	@EnableEntityDefinedRegions(basePackageClasses = User.class)
	@EnableGemfireRepositories(basePackageClasses = UserRepository.class)
	static class TestGeodeConfiguration { }

}
