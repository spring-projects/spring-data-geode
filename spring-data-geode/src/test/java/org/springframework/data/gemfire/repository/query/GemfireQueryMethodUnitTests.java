/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.data.gemfire.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.gemfire.mapping.GemfireMappingContext;
import org.springframework.data.gemfire.repository.Query;
import org.springframework.data.gemfire.repository.query.annotation.Hint;
import org.springframework.data.gemfire.repository.query.annotation.Import;
import org.springframework.data.gemfire.repository.query.annotation.Limit;
import org.springframework.data.gemfire.repository.query.annotation.Trace;
import org.springframework.data.gemfire.repository.sample.Person;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ObjectUtils;

/**
 * Unit tests for {@link GemfireQueryMethod}.
 *
 * @author Oliver Gierke
 * @author John Blum
 */
@RunWith(MockitoJUnitRunner.class)
public class GemfireQueryMethodUnitTests {

	private GemfireMappingContext mappingContext = new GemfireMappingContext();

	private ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	@Mock
	private RepositoryMetadata repositoryMetadata;

	protected void assertQueryHints(GemfireQueryMethod queryMethod, String... expectedHints) {

		assertThat(queryMethod).isNotNull();
		assertThat(queryMethod.hasHint()).isEqualTo(!ObjectUtils.isEmpty(expectedHints));

		String[] actualHints = queryMethod.getHints();

		assertThat(actualHints).isNotNull();
		assertThat(actualHints.length).isEqualTo(expectedHints.length);

		for (int index = 0; index < expectedHints.length; index++) {
			assertThat(actualHints[index]).isEqualTo(expectedHints[index]);
		}
	}

	protected void assertNoQueryHints(GemfireQueryMethod queryMethod) {
		assertQueryHints(queryMethod);
	}

	protected void assertImportStatement(GemfireQueryMethod queryMethod, String expectedImport) {

		assertThat(queryMethod).isNotNull();
		assertThat(queryMethod.hasImport()).isEqualTo(expectedImport != null);

		if (expectedImport != null) {
			assertThat(queryMethod.getImport()).isEqualTo(expectedImport);
		}
		else {
			assertThat(queryMethod.getImport()).isNull();
		}
	}

	protected void assertNoImportStatement(GemfireQueryMethod queryMethod) {
		assertImportStatement(queryMethod, null);
	}

	protected void assertLimitedQuery(GemfireQueryMethod queryMethod, Integer expectedLimit) {

		assertThat(queryMethod).isNotNull();
		assertThat(queryMethod.hasLimit()).isEqualTo(expectedLimit != null);

		if (expectedLimit != null) {
			assertThat(queryMethod.getLimit()).isEqualTo(expectedLimit);
		}
		else {
			assertThat(queryMethod.getLimit()).isEqualTo(Integer.MAX_VALUE);
		}
	}

	protected void assertUnlimitedQuery(GemfireQueryMethod queryMethod) {
		assertLimitedQuery(queryMethod, null);
	}

	@Before
	public void setup() {

		doReturn(Person.class).when(this.repositoryMetadata).getDomainType();
		doReturn(Person.class).when(this.repositoryMetadata).getReturnedDomainClass(any(Method.class));
		doReturn(ClassTypeInformation.from(Object.class)).when(this.repositoryMetadata).getReturnType(any(Method.class));
	}

	@Test
	public void detectsAnnotatedQueryCorrectly() throws Exception {

		GemfireQueryMethod method =
			new GemfireQueryMethod(Sample.class.getMethod("annotated"),
				this.repositoryMetadata, this.projectionFactory, this.mappingContext);

		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.getAnnotatedQuery()).isEqualTo("foo");

		method = new GemfireQueryMethod(Sample.class.getMethod("annotatedButEmpty"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext);

		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.getAnnotatedQuery()).isNull();

		method = new GemfireQueryMethod(Sample.class.getMethod("notAnnotated"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext);

		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.getAnnotatedQuery()).isNull();
	}

	@Test
	public void acceptsQueryMethodWithPageableParameter() throws Exception {
		new GemfireQueryMethod(PageablePojo.class.getMethod("pageableMethod", Pageable.class),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext);
	}

	@Test
	public void detectsQueryHintsCorrectly() throws Exception {

		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasHint()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasHint()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasHint()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("unlimitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasHint()).isFalse();
	}

	@Test
	public void detectsQueryImportsCorrectly() throws Exception {

		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasImport()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasImport()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasImport()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("unlimitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasImport()).isFalse();
	}

	@Test
	public void detectsQueryLimitsCorrectly() throws Exception {

		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasLimit()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasLimit()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasLimit()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("unlimitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasLimit()).isFalse();
	}

	@Test
	public void detectsQueryTracingCorrectly() throws Exception {

		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasTrace()).isTrue();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasTrace()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasTrace()).isFalse();
		assertThat(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("unlimitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext).hasTrace()).isTrue();
	}

	@Test
	public void hintOnQueryWithHint() throws Exception {

		assertQueryHints(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext), "IdIdx", "LastNameIdx");

		assertQueryHints(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext), "BirthDateIdx");
	}

	@Test
	public void hintOnQueryWithNoHints() throws Exception {

		assertNoQueryHints(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext));
	}

	@Test
	public void importOnQueryWithImport() throws Exception {

		assertImportStatement(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithImport"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext), "org.example.app.domain.ExampleType");

		assertImportStatement(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext), "org.example.app.domain.Person");
	}

	@Test
	public void importOnQueryWithNoImports() throws Exception {

		assertNoImportStatement(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("queryWithHint"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext));
	}

	@Test
	public void limitOnQueryWithLimit() throws Exception {

		assertLimitedQuery(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("limitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext), 1024);
	}

	@Test
	public void limitOnQueryWithNoLimits() throws Exception {

		assertUnlimitedQuery(new GemfireQueryMethod(AnnotatedQueryMethods.class.getMethod("unlimitedQuery"),
			this.repositoryMetadata, this.projectionFactory, this.mappingContext));
	}

	@SuppressWarnings("unused")
	interface Sample {

		@Query("foo")
		Object annotated();

		@Query("")
		Object annotatedButEmpty();

		Object notAnnotated();

	}

	@SuppressWarnings("unused")
	interface PageablePojo {

		Page<?> pageableMethod(Pageable pageable);

	}

	@SuppressWarnings("unused")
	interface AnnotatedQueryMethods {

		@Trace
		@Hint({ "IdIdx", "LastNameIdx" })
		Object queryWithHint();

		@Import("org.example.app.domain.ExampleType")
		Object queryWithImport();

		@Hint("BirthDateIdx")
		@Import("org.example.app.domain.Person")
		@Limit(1024)
		Object limitedQuery();

		@Trace
		Object unlimitedQuery();

	}
}
