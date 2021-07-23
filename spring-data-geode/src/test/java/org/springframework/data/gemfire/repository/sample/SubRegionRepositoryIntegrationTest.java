/*
 * Copyright 2010-2021 the original author or authors.
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
package org.springframework.data.gemfire.repository.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Region;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.repository.Wrapper;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests testing the use of GemFire Repositories on GemFire Cache Subregions.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.repository.GemfireRepository
 * @see org.springframework.data.gemfire.repository.Wrapper
 * @see org.springframework.data.gemfire.repository.sample.Programmer
 * @see org.springframework.data.gemfire.repository.sample.ProgrammerRepository
 * @see org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.apache.geode.cache.Region
 * @link https://jira.springsource.org/browse/SGF-251
 * @link https://jira.springsource.org/browse/SGF-252
 * @since 1.4.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("subregionRepository.xml")
@SuppressWarnings("unused")
public class SubRegionRepositoryIntegrationTest extends IntegrationTestsSupport {

	private static final Map<String, RootUser> ADMIN_USER_DATA = new HashMap<>(5, 0.90f);

	private static final Map<String, GuestUser> GUEST_USER_DATA = new HashMap<>(3, 0.90f);

	private static final Map<String, Programmer> PROGRAMMER_USER_DATA = new HashMap<>(23, 0.90f);

	static {
		createAdminUser("supertool");
		createAdminUser("thor");
		createAdminUser("zeus");
		createGuestUser("bubba");
		createGuestUser("joeblow");
		createProgrammer("AdaLovelace", "Ada");
		createProgrammer("AlanKay", "Smalltalk");
		createProgrammer("BjarneStroustrup", "C++");
		createProgrammer("BrendanEich", "JavaScript");
		createProgrammer("DennisRitchie", "C");
		createProgrammer("GuidoVanRossum", "Python");
		createProgrammer("JamesGosling", "Java");
		createProgrammer("JamesStrachan", "Groovy");
		createProgrammer("JohnBackus", "Fortran");
		createProgrammer("JohnKemeny", "BASIC");
		createProgrammer("JohnMcCarthy", "LISP");
		createProgrammer("JoshuaBloch", "Java");
		createProgrammer("LarryWall", "Perl");
		createProgrammer("MartinOdersky", "Scala");
		createProgrammer("NiklausWirth", "Modula-2");
		createProgrammer("NiklausWirth", "Pascal");
		createProgrammer("ThomasKurtz", "BASIC");
		createProgrammer("YukihiroMatsumoto", "Ruby");
	}

	@Autowired
	private ProgrammerRepository programmersRepo;

	@Resource(name = "/Users/Programmers")
	private Region<String, Programmer> programmers;

	@Resource(name = "/Local/Admin/Users")
	private Region<String, RootUser> adminUsers;

	@Resource(name = "/Local/Guest/Users")
	private Region<String, GuestUser> guestUsers;

	@Autowired
	private GuestUserRepository guestUserRepo;

	@Autowired
	private RootUserRepository adminUserRepo;

	private static RootUser createAdminUser(String username) {
		RootUser user = new RootUser(username);
		ADMIN_USER_DATA.put(username, user);
		return user;
	}

	private static GuestUser createGuestUser(String username) {
		GuestUser user = new GuestUser(username);
		GUEST_USER_DATA.put(username, user);
		return user;
	}

	private static RootUser getAdminUser(String username) {
		List<RootUser> users = getAdminUsers(username);
		return (users.isEmpty() ? null : users.get(0));
	}

	private static List<RootUser> getAdminUsers(String... usernames) {
		return getUsers(ADMIN_USER_DATA, usernames);
	}

	private static GuestUser getGuestUser(String username) {
		List<GuestUser> users = getGuestUsers(username);
		return (users.isEmpty() ? null : users.get(0));
	}

	private static List<GuestUser> getGuestUsers(String... usernames) {
		return getUsers(GUEST_USER_DATA, usernames);
	}

	private static <T extends User> List<T> getUsers(Map<String, T> userData, String... usernames) {

		List<T> users = new ArrayList<>(usernames.length);

		for (String username : usernames) {
			T user = userData.get(username);

			if (user != null) {
				users.add(user);
			}
		}

		Collections.sort(users);

		return users;
	}

	private  static Programmer createProgrammer(String username, String programmingLanguage) {
		Programmer programmer = new Programmer(username);
		programmer.setProgrammingLanguage(programmingLanguage);
		PROGRAMMER_USER_DATA.put(username, programmer);
		return programmer;
	}

	private static List<Programmer> getProgrammers(String... usernames) {

		List<Programmer> programmers = new ArrayList<>(usernames.length);

		for (String username : usernames) {
			Programmer programmer = PROGRAMMER_USER_DATA.get(username);

			if (programmer != null) {
				programmers.add(PROGRAMMER_USER_DATA.get(username));
			}
		}

		Collections.sort(programmers);

		return programmers;
	}

	@Before
	public void setup() {

		assertThat(programmers).as("The /Users/Programmers Subregion was null!").isNotNull();

		if (programmers.isEmpty()) {
			programmers.putAll(PROGRAMMER_USER_DATA);
		}

		assertThat(programmers.size()).isEqualTo(PROGRAMMER_USER_DATA.size());
		assertThat(adminUsers).as("The /Local/Admins/Users Subregion was null!").isNotNull();

		if (adminUsers.isEmpty()) {
			adminUsers.putAll(ADMIN_USER_DATA);
		}

		assertThat(adminUsers.size()).isEqualTo(ADMIN_USER_DATA.size());
		assertThat(guestUsers).as("The /Local/Guest/Users Subregion was null!").isNotNull();

		if (guestUsers.isEmpty()) {
			guestUsers.putAll(GUEST_USER_DATA);
		}

		assertThat(guestUsers.size()).isEqualTo(GUEST_USER_DATA.size());
	}

	@Test
	public void testSubregionRepositoryInteractions() {

		assertThat(programmersRepo.findById("JamesGosling").orElse(null)).isEqualTo(PROGRAMMER_USER_DATA.get("JamesGosling"));

		List<Programmer> javaProgrammers = programmersRepo.findDistinctByProgrammingLanguageOrderByUsernameAsc("Java");

		assertThat(javaProgrammers).isNotNull();
		assertThat(javaProgrammers.isEmpty()).isFalse();
		assertThat(javaProgrammers.size()).isEqualTo(2);
		assertThat(getProgrammers("JamesGosling", "JoshuaBloch")).isEqualTo(javaProgrammers);

		List<Programmer> groovyProgrammers = programmersRepo.findDistinctByProgrammingLanguageLikeOrderByUsernameAsc("Groovy");

		assertThat(groovyProgrammers).isNotNull();
		assertThat(groovyProgrammers.isEmpty()).isFalse();
		assertThat(groovyProgrammers.size()).isEqualTo(1);
		assertThat(getProgrammers("JamesStrachan")).isEqualTo(groovyProgrammers);

		programmersRepo.save(new Wrapper<>(createProgrammer("RodJohnson", "Java"), "RodJohnson"));
		programmersRepo.save(new Wrapper<>(createProgrammer("GuillaumeLaforge", "Groovy"), "GuillaumeLaforge"));
		programmersRepo.save(new Wrapper<>(createProgrammer("GraemeRocher", "Groovy"), "GraemeRocher"));

		javaProgrammers = programmersRepo.findDistinctByProgrammingLanguageOrderByUsernameAsc("Java");

		assertThat(javaProgrammers).isNotNull();
		assertThat(javaProgrammers.isEmpty()).isFalse();
		assertThat(javaProgrammers.size()).isEqualTo(3);
		assertThat(getProgrammers("JamesGosling", "JoshuaBloch", "RodJohnson")).isEqualTo(javaProgrammers);

		groovyProgrammers = programmersRepo.findDistinctByProgrammingLanguageLikeOrderByUsernameAsc("Groovy");

		assertThat(groovyProgrammers).isNotNull();
		assertThat(groovyProgrammers.isEmpty()).isFalse();
		assertThat(groovyProgrammers.size()).isEqualTo(3);
		assertThat(getProgrammers("GraemeRocher", "GuillaumeLaforge", "JamesStrachan")).isEqualTo(groovyProgrammers);

		List<Programmer> javaLikeProgrammers = programmersRepo.findDistinctByProgrammingLanguageLikeOrderByUsernameAsc("Java%");

		assertThat(javaLikeProgrammers).isNotNull();
		assertThat(javaLikeProgrammers.isEmpty()).isFalse();
		assertThat(javaLikeProgrammers.size()).isEqualTo(4);
		assertThat(getProgrammers("BrendanEich", "JamesGosling", "JoshuaBloch", "RodJohnson"))
			.isEqualTo(javaLikeProgrammers);

		List<Programmer> pseudoCodeProgrammers = programmersRepo.findDistinctByProgrammingLanguageLikeOrderByUsernameAsc("PseudoCode");

		assertThat(pseudoCodeProgrammers).isNotNull();
		assertThat(pseudoCodeProgrammers.isEmpty()).isTrue();
	}

	@Test
	public void testIdenticallyNamedSubregionDataAccess() {

		assertThat(adminUserRepo.findById("supertool").orElse(null)).isEqualTo(getAdminUser("supertool"));
		assertThat(guestUserRepo.findById("joeblow").orElse(null)).isEqualTo(getGuestUser("joeblow"));

		List<RootUser> rootUsers = adminUserRepo.findDistinctByUsername("zeus");

		assertThat(rootUsers).isNotNull();
		assertThat(rootUsers.isEmpty()).isFalse();
		assertThat(rootUsers.size()).isEqualTo(1);

		assertThat(rootUsers.get(0)).isEqualTo(getAdminUser("zeus"));

		List<GuestUser> guestUsers = guestUserRepo.findDistinctByUsername("bubba");

		assertThat(guestUsers).isNotNull();
		assertThat(guestUsers.isEmpty()).isFalse();
		assertThat(guestUsers.size()).isEqualTo(1);
		assertThat(guestUsers.get(0)).isEqualTo(getGuestUser("bubba"));
	}
}
