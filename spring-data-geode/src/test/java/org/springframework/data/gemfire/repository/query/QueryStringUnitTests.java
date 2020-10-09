/*
 * Copyright 2012-2020 the original author or authors.
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.gemfire.repository.query.QueryString.HINT_PATTERN;
import static org.springframework.data.gemfire.repository.query.QueryString.IMPORT_PATTERN;
import static org.springframework.data.gemfire.repository.query.QueryString.LIMIT_PATTERN;
import static org.springframework.data.gemfire.repository.query.QueryString.TRACE_PATTERN;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.geode.cache.Region;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.repository.sample.RootUser;
import org.springframework.data.gemfire.test.model.Person;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Unit Tests for {@link QueryString}.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see java.util.regex.Pattern
 * @see org.junit.Test
 * @see org.mockito.Mock
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.Region
 * @see org.mockito.junit.MockitoJUnitRunner
 * @see org.springframework.data.domain.Sort
 * @see org.springframework.data.gemfire.repository.query.QueryString
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryStringUnitTests {

	@Mock
	@SuppressWarnings("rawtypes")
	private Region region;

	private Sort.Order newSortOrder(String property) {
		return newSortOrder(property, Sort.Direction.ASC);
	}

	private Sort.Order newSortOrder(String property, Sort.Direction direction) {
		return new Sort.Order(direction, property);
	}

	private Sort newSort(Sort.Order... orders) {
		return Sort.by(orders);
	}

	@Test
	public void constructQueryStringWithAtRegionAnnotatedDomainType() {
		assertThat(new QueryString(Person.class).toString()).isEqualTo("SELECT * FROM /People");
	}

	@Test
	public void constructQueryStringWithDomainType() {
		assertThat(new QueryString(User.class).toString()).isEqualTo("SELECT * FROM /User");
	}

	@Test
	public void constructQueryStringWithDomainTypeAsCount() {
		assertThat(new QueryString(User.class, true).toString()).isEqualTo("SELECT count(*) FROM /User");
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructQueryStringWithNullDomainType() {

		try {
			new QueryString((Class<?>) null);
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("Domain type is required");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void constructQueryStringWithQuery() {

		String query = "SELECT * FROM /Example";

		assertThat(new QueryString(query).toString()).isEqualTo(query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructQueryStringWithBlankQueryThrowsIllegalArgumentException() {
		assertInvalidQueryThrowsIllegalArgumentException("  ");
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructQueryStringWithEmptyQueryThrowsIllegalArgumentException() {
		assertInvalidQueryThrowsIllegalArgumentException("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructQueryStringWithNullQueryThrowsIllegalArgumentException() {
		assertInvalidQueryThrowsIllegalArgumentException(null);
	}

	private void assertInvalidQueryThrowsIllegalArgumentException(String query) {

		try {
			new QueryString(query);
		}
		catch (Exception expected) {

			assertThat(expected).hasMessage("Query [%s] is required", query);
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void queryStringOfQuery() {

		QueryString query = QueryString.of("SELECT * FROM /Test");

		assertThat(query).isNotNull();
		assertThat(query.toString()).isEqualTo("SELECT * FROM /Test");
	}

	@Test
	public void queryStringFromAtRegionAnnotatedDomainType() {

		QueryString query = QueryString.from(Person.class);

		assertThat(query).isNotNull();
		assertThat(query.toString()).isEqualTo("SELECT * FROM /People");
	}

	@Test
	public void queryStringFromDomainType() {

		QueryString query = QueryString.from(User.class);

		assertThat(query).isNotNull();
		assertThat(query.toString()).isEqualTo("SELECT * FROM /User");
	}

	@Test
	public void queryStringCountingObjectsOfAtAnnotatedDomainType() {

		QueryString query = QueryString.count(Person.class);

		assertThat(query).isNotNull();
		assertThat(query.toString()).isEqualTo("SELECT count(*) FROM /People");
	}

	@Test
	public void queryStringCountingObjectsOfDomainType() {

		QueryString query = QueryString.count(User.class);

		assertThat(query).isNotNull();
		assertThat(query.toString()).isEqualTo("SELECT count(*) FROM /User");
	}

	@Test
	public void getDigitsOnlyIsCorrect() {

		assertThat(QueryString.getDigitsOnly("1")).isEqualTo("1");
		assertThat(QueryString.getDigitsOnly(" 2")).isEqualTo("2");
		assertThat(QueryString.getDigitsOnly(" 2 34  ")).isEqualTo("234");
		assertThat(QueryString.getDigitsOnly("abc123")).isEqualTo("123");
		assertThat(QueryString.getDigitsOnly("O1E2l4")).isEqualTo("124");
		assertThat(QueryString.getDigitsOnly("lOlO")).isEqualTo("");
		assertThat(QueryString.getDigitsOnly("  ")).isEqualTo("");
		assertThat(QueryString.getDigitsOnly("")).isEqualTo("");
		assertThat(QueryString.getDigitsOnly(null)).isEqualTo("");
	}

	@Test
	public void isLimitedWithLimitBasedQueryReturnsTrue() {
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT 50").isLimited()).isTrue();
	}

	@Test
	public void isLimitedWithUnlimitedQueryReturnsFalse() {
		assertThat(QueryString.of("SELECT * FROM /Test").isLimited()).isFalse();
	}

	@Test
	public void isLimitedWithQueryHavingInvalidLimitSyntaxReturnsFalse() {

		assertThat(QueryString.of("SELECT * FROM /Test LIMIT").isLimited()).isFalse();
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT abc").isLimited()).isFalse();
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT abc123").isLimited()).isFalse();
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT lO").isLimited()).isFalse();
		assertThat(QueryString.of("SELECT * FROM /Test LMT 10").isLimited()).isFalse();
		assertThat(QueryString.of("SELECT * FROM /Test 10").isLimited()).isFalse();
	}

	@Test
	public void getLimitReturnsIntegerValue() {

		assertThat(QueryString.of("SELECT * FROM /Test LIMIT 1").getLimit()).isEqualTo(1);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT 10").getLimit()).isEqualTo(10);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT 21").getLimit()).isEqualTo(21);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT 421").getLimit()).isEqualTo(421);
	}

	@Test
	public void getLimitReturnsIntegerMaxValue() {

		assertThat(QueryString.of("SELECT * FROM /Test LIMIT").getLimit()).isEqualTo(Integer.MAX_VALUE);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT abc").getLimit()).isEqualTo(Integer.MAX_VALUE);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT abc123").getLimit()).isEqualTo(Integer.MAX_VALUE);
		assertThat(QueryString.of("SELECT * FROM /Test LIMIT lO").getLimit()).isEqualTo(Integer.MAX_VALUE);
		assertThat(QueryString.of("SELECT * FROM /Test LMT 10").getLimit()).isEqualTo(Integer.MAX_VALUE);
		assertThat(QueryString.of("SELECT * FROM /Test 10").getLimit()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	public void hintPatternMatches() {

		assertThat(matches(HINT_PATTERN, "<HINT 'ExampleIndex'>")).isTrue();
		assertThat(matches(HINT_PATTERN, "<HINT 'IdIdx'> SELECT * FROM /Example WHERE id = $1")).isTrue();
		assertThat(matches(HINT_PATTERN, "<HINT 'LastNameIdx', 'BirthDateIdx'> SELECT * FROM /Person WHERE lastName = $1 AND birthDate = $2")).isTrue();
	}

	@Test
	public void hintPatternNoMatches() {

		assertThat(matches(HINT_PATTERN, "HINT")).isFalse();
		assertThat(matches(HINT_PATTERN, "<HINT>")).isFalse();
		assertThat(matches(HINT_PATTERN, "<HINT ''>")).isFalse();
		assertThat(matches(HINT_PATTERN, "<HINT '  '>")).isFalse();
		assertThat(matches(HINT_PATTERN, "<HINT IdIdx>")).isFalse();
		assertThat(matches(HINT_PATTERN, "<HINT LastNameIdx, FirstNameIdx>")).isFalse();
		assertThat(matches(HINT_PATTERN, "SELECT * FROM /Example")).isFalse();
		assertThat(matches(HINT_PATTERN, "SELECT * FROM /Hint")).isFalse();
		assertThat(matches(HINT_PATTERN, "SELECT x.hint FROM /Clues x WHERE x.hint > $1")).isFalse();
	}

	@Test
	public void importPatternMatches() {

		assertThat(matches(IMPORT_PATTERN, "IMPORT *;")).isTrue();
		assertThat(matches(IMPORT_PATTERN, "IMPORT org.example.*;")).isTrue();
		assertThat(matches(IMPORT_PATTERN, "IMPORT org.example.Type;")).isTrue();
		assertThat(matches(IMPORT_PATTERN, "IMPORT org.example.app.domain.DomainType; SELECT * FROM /DomainType")).isTrue();
	}

	@Test
	public void importPatternNoMatches() {

		assertThat(matches(IMPORT_PATTERN, "IMPORT")).isFalse();
		assertThat(matches(IMPORT_PATTERN, "IMPORT ;")).isFalse();
		assertThat(matches(IMPORT_PATTERN, "IMPORT *")).isFalse();
		assertThat(matches(IMPORT_PATTERN, "IMPORT *:")).isFalse();
		assertThat(matches(IMPORT_PATTERN, "SELECT * FROM /Example")).isFalse();
	}

	@Test
	public void limitPatternMatches() {

		assertThat(matches(LIMIT_PATTERN, "LIMIT 0")).isTrue();
		assertThat(matches(LIMIT_PATTERN, "LIMIT 1")).isTrue();
		assertThat(matches(LIMIT_PATTERN, "LIMIT 10")).isTrue();
		assertThat(matches(LIMIT_PATTERN, "SELECT * FROM /Example LIMIT 10")).isTrue();
		assertThat(matches(LIMIT_PATTERN, "SELECT * FROM /Example WHERE id = $1 LIMIT 10")).isTrue();
	}

	@Test
	public void limitPatternNoMatches() {

		assertThat(matches(LIMIT_PATTERN, "LIMIT")).isFalse();
		assertThat(matches(LIMIT_PATTERN, "LIMIT lO")).isFalse();
		assertThat(matches(LIMIT_PATTERN, "LIMIT AF")).isFalse();
		assertThat(matches(LIMIT_PATTERN, "LIMIT ten")).isFalse();
		assertThat(matches(LIMIT_PATTERN, "SELECT * FROM /Example LIMIT")).isFalse();
		assertThat(matches(LIMIT_PATTERN, "SELECT * FROM /Example WHERE id = $1 LIM 10")).isFalse();
	}

	@Test
	public void tracePatternMatches() {

		assertThat(matches(TRACE_PATTERN, "<TRACE>")).isTrue();
		assertThat(matches(TRACE_PATTERN, "<TRACE> SELECT * FROM /Example")).isTrue();
		assertThat(matches(TRACE_PATTERN, "<TRACE>SELECT * FROM /Example")).isTrue();
		assertThat(matches(TRACE_PATTERN, "SELECT * FROM /Example<TRACE>")).isTrue();
	}

	@Test
	public void tracePatternNoMatches() {

		assertThat(matches(TRACE_PATTERN, "TRACE")).isFalse();
		assertThat(matches(TRACE_PATTERN, "TRACE SELECT * FROM /Example")).isFalse();
		assertThat(matches(TRACE_PATTERN, "<TRACE SELECT * FROM /Example>")).isFalse();
	}

	private boolean matches(Pattern pattern, String value) {
		return pattern.matcher(value).find();
	}

	@Test
	public void adjustLimitWithQueryHavingLimit() {

		QueryString original = QueryString.of("SELECT * FROM /Test LIMIT 10");
		QueryString adjusted = original.adjustLimit(20);

		assertThat(adjusted).isNotNull();
		assertThat(adjusted).isNotSameAs(original);
		assertThat(adjusted.toString()).isEqualTo("SELECT * FROM /Test LIMIT 20");
	}

	@Test
	public void adjustLimitWithQueryHavingNoLimit() {

		QueryString original = QueryString.of("SELECT * FROM /Test");
		QueryString adjusted = original.adjustLimit(25);

		assertThat(adjusted).isNotNull();
		assertThat(adjusted).isNotSameAs(original);
		assertThat(adjusted.toString()).isEqualTo("SELECT * FROM /Test LIMIT 25");
	}

	@Test
	public void adjustLimitWithNullLimit() {

		QueryString original = QueryString.of("SELECT * FROM /Test LIMIT 10");
		QueryString adjusted = original.adjustLimit(null);

		assertThat(adjusted).isSameAs(original);
		assertThat(adjusted.toString()).isEqualTo("SELECT * FROM /Test LIMIT 10");
	}

	@Test
	public void asDistinctQuery() {

		QueryString query = QueryString.of("SELECT * FROM /Test");

		assertThat(query.asDistinct().toString()).isEqualTo("SELECT DISTINCT * FROM /Test");
	}

	@Test
	public void asDistinctWithDistinctQuery() {

		QueryString query = QueryString.of("SELECT DISTINCT * FROM /Test");

		assertThat(query.asDistinct().toString()).isEqualTo("SELECT DISTINCT * FROM /Test");
	}

	// SGF-251
	@Test
	public void replacesDomainTypeSimpleNameWithRegionPathCorrectly() {

		QueryString query = QueryString.from(Person.class);

		when(this.region.getFullPath()).thenReturn("/foo/bar");

		assertThat(query.toString()).isEqualTo("SELECT * FROM /People");
		assertThat(query.fromRegion(this.region, Person.class).toString()).isEqualTo("SELECT * FROM /foo/bar");

		verify(this.region, times(1)).getFullPath();
		verifyNoMoreInteractions(this.region);
	}

	// SGF-156, SGF-251
	@Test
	public void replacesFromClauseWithRegionPathCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM /Persons p WHERE p.lastname = $1");

		when(this.region.getFullPath()).thenReturn("/People");

		assertThat(query.fromRegion(this.region, Person.class).toString())
			.isEqualTo("SELECT * FROM /People p WHERE p.lastname = $1");

		verify(this.region, times(1)).getFullPath();
		verifyNoMoreInteractions(this.region);
	}

	// SGF-252
	@Test
	public void replacesFullyQualifiedSubRegionPathWithRegionPathCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM //Local/Root/Users u WHERE u.username = $1");

		when(this.region.getFullPath()).thenReturn("/Remote/Root/Users");

		assertThat(query.fromRegion(this.region, RootUser.class).toString())
			.isEqualTo("SELECT * FROM /Remote/Root/Users u WHERE u.username = $1");

		verify(this.region, times(1)).getFullPath();
		verifyNoMoreInteractions(this.region);
	}

	@Test
	public void bindsInValuesCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM /Collection WHERE elements IN SET $1");

		assertThat(query.bindIn(Arrays.asList(1, 2, 3)).toString())
			.isEqualTo("SELECT * FROM /Collection WHERE elements IN SET ('1', '2', '3')");
	}

	@Test
	public void detectsInParameterIndexesCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM /Example WHERE values IN SET $1 OR IN SET $2");

		assertThat(query.getInParameterIndexes()).isEqualTo(Arrays.asList(1, 2));
	}

	@Test
	public void addsNoOrderByClauseCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM /People p").orderBy(null);

		assertThat(query.toString()).isEqualTo("SELECT * FROM /People p");
	}

	@Test
	public void addsOrderByClauseCorrectly() {

		QueryString query = QueryString.of("SELECT * FROM /People p WHERE p.lastName = $1")
			.orderBy(newSort(newSortOrder("lastName", Sort.Direction.DESC),
				newSortOrder("firstName")));

		assertThat(query.toString())
			.isEqualTo("SELECT DISTINCT * FROM /People p WHERE p.lastName = $1 ORDER BY lastName DESC, firstName ASC");
	}

	@Test
	public void addsSingleOrderByClauseCorrectly() {

		QueryString query = QueryString.of("SELECT DISTINCT p.lastName FROM /People p WHERE p.firstName = $1")
			.orderBy(newSort(newSortOrder("lastName")));

		assertThat(query.toString())
			.isEqualTo("SELECT DISTINCT p.lastName FROM /People p WHERE p.firstName = $1 ORDER BY lastName ASC");
	}

	@Test
	public void withHints() {

		assertThat(QueryString.of("SELECT * FROM /Example").withHints("IdIdx").toString())
			.isEqualTo("<HINT 'IdIdx'> SELECT * FROM /Example");

		assertThat(QueryString.of("SELECT * FROM /Example").withHints("IdIdx", "SpatialIdx", "TxDateIdx").toString())
			.isEqualTo("<HINT 'IdIdx', 'SpatialIdx', 'TxDateIdx'> SELECT * FROM /Example");
	}

	@Test
	public void withoutHints() {

		QueryString expectedQueryString = QueryString.of("SELECT * FROM /Example");

		assertThat(expectedQueryString.withHints()).isSameAs(expectedQueryString);
		assertThat(expectedQueryString.withHints((String[]) null)).isSameAs(expectedQueryString);
	}

	@Test
	public void withImport() {
		assertThat(QueryString.of("SELECT * FROM /People").withImport("org.example.app.domain.Person").toString())
			.isEqualTo("IMPORT org.example.app.domain.Person; SELECT * FROM /People");
	}

	@Test
	public void withoutImport() {

		QueryString expectedQueryString = QueryString.of("SELECT * FROM /Example");

		assertThat(expectedQueryString.withImport(null)).isSameAs(expectedQueryString);
		assertThat(expectedQueryString.withImport("")).isSameAs(expectedQueryString);
		assertThat(expectedQueryString.withImport("  ")).isSameAs(expectedQueryString);
	}

	@Test
	public void withLimit() {

		assertThat(QueryString.of("SELECT * FROM /Example").withLimit(10).toString())
			.isEqualTo("SELECT * FROM /Example LIMIT 10");

		assertThat(QueryString.of("SELECT * FROM /Example").withLimit(0).toString())
			.isEqualTo("SELECT * FROM /Example LIMIT 0");

		assertThat(QueryString.of("SELECT * FROM /Example").withLimit(-5).toString())
			.isEqualTo("SELECT * FROM /Example LIMIT -5");
	}

	@Test
	public void withoutLimit() {

		QueryString expectedQueryString = QueryString.of("SELECT * FROM /Example");

		assertThat(expectedQueryString.withLimit(null)).isSameAs(expectedQueryString);
	}

	@Test
	public void withTrace() {
		assertThat(QueryString.of("SELECT * FROM /Example").withTrace().toString())
			.isEqualTo("<TRACE> SELECT * FROM /Example");
	}

	@Test
	public void withHintAndTrace() {
		assertThat(QueryString.of("SELECT * FROM /Example").withHints("IdIdx").withTrace().toString())
			.isEqualTo("<TRACE> <HINT 'IdIdx'> SELECT * FROM /Example");
	}

	@Test
	public void withHintImportLimitAndTrace() {

		QueryString query = QueryString.of("SELECT * FROM /Example")
			.withImport("org.example.domain.Type")
			.withHints("IdIdx", "NameIdx")
			.withLimit(20)
			.withTrace();

		assertThat(query.toString())
			.isEqualTo("<TRACE> <HINT 'IdIdx', 'NameIdx'> IMPORT org.example.domain.Type; SELECT * FROM /Example LIMIT 20");
	}

	@Getter
	@ToString(of = "name")
	@EqualsAndHashCode(of = "name")
	@RequiredArgsConstructor(staticName = "newUser")
	@SuppressWarnings("unused")
	static class User {

		@Id
		private Long id;

		@NonNull
		private final String name;
	}
}
