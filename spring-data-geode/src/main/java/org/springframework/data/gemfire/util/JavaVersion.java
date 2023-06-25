/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.gemfire.util;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Java class to represent the current version of the JRE or JVM.
 *
 * @author John Blum
 * @since 2.6.0
 */
@SuppressWarnings("unused")
public abstract class JavaVersion implements Comparable<JavaVersion> {

	public static final JavaVersion ONE_ZERO = JavaVersion.of(1, 0, 0);
	public static final JavaVersion ONE_ONE = JavaVersion.of(1, 1, 0);
	public static final JavaVersion ONE_TWO = JavaVersion.of(1, 2, 0);
	public static final JavaVersion ONE_THREE = JavaVersion.of(1, 3, 0);
	public static final JavaVersion ONE_FOUR = JavaVersion.of(1, 4, 0);
	public static final JavaVersion FIVE = JavaVersion.of(1, 5, 0);
	public static final JavaVersion SIX = JavaVersion.of(1, 6, 0);
	public static final JavaVersion SEVEN = JavaVersion.of(1, 7, 0);
	public static final JavaVersion EIGHT = JavaVersion.of(1, 8, 0);
	public static final JavaVersion NINE = JavaVersion.of(9, 0, 0);
	public static final JavaVersion TEN = JavaVersion.of(10, 0, 0);
	public static final JavaVersion ELEVEN = JavaVersion.of(11, 0, 0);
	public static final JavaVersion TWELVE = JavaVersion.of(12, 0, 0);
	public static final JavaVersion THIRTEEN = JavaVersion.of(13, 0, 0);
	public static final JavaVersion FOURTEEN = JavaVersion.of(14, 0, 0);
	public static final JavaVersion FIFTEEN = JavaVersion.of(15, 0, 0);
	public static final JavaVersion SIXTEEN = JavaVersion.of(16, 0, 0);
	public static final JavaVersion SEVENTEEN = JavaVersion.of(17, 0, 0);

	protected static final int DEFAULT_VERSION_NUMBER = 0;
	protected static final int DEFAULT_BUILD_NUMBER = DEFAULT_VERSION_NUMBER;
	protected static final int DEFAULT_PATCH_VERSION = DEFAULT_VERSION_NUMBER;

	protected static final Integer ZERO = 0;

	protected static final String JAVA_VERSION_SYSTEM_PROPERTY = "java.version";

	private static final AtomicReference<JavaVersion> CURRENT = new AtomicReference<>(null);

	public static JavaVersion current() {
		return CURRENT.updateAndGet(currentJavaVersion -> currentJavaVersion != null ? currentJavaVersion
			: determineCurrentJavaVersion());
	}

	protected static JavaVersion of(int major, int minor, int patch) {
		return new JavaVersion(major, minor, patch) { };
	}

	private static JavaVersion determineCurrentJavaVersion() {

		String javaVersion = String.valueOf(System.getProperty(JAVA_VERSION_SYSTEM_PROPERTY));

		String[] javaVersionArray = ArrayUtils.nullSafeArray(javaVersion.split("\\."), String.class);

		int major = 0;
		int minor = 0;
		int patch = 0;

		if (javaVersionArray.length > 0) {
			major = parseInt(javaVersionArray[0]);
			if (javaVersionArray.length > 1) {
				minor = parseInt(javaVersionArray[1]);
				if (javaVersionArray.length > 2) {
					String tempPatch = javaVersionArray[2];
					tempPatch = tempPatch.contains("_")
						? tempPatch.substring(0, tempPatch.indexOf("_"))
						: tempPatch;
					patch = parseInt(tempPatch);
				}
			}
		}

		return JavaVersion.of(major, minor, patch);
	}

	private static String parseDigits(String value) {

		StringBuilder digits = new StringBuilder();

		for (char character : String.valueOf(value).toCharArray()) {
			if (Character.isDigit(character)) {
				digits.append(character);
			}
		}

		return digits.toString();
	}

	private static int parseInt(String value) {

		try {
			return Integer.parseInt(parseDigits(value));
		}
		catch (NumberFormatException ignore) {
			return DEFAULT_VERSION_NUMBER;
		}
	}

	private final Integer buildNumber;
	private final Integer major;
	private final Integer minor;
	private final Integer patch;

	protected JavaVersion(int major, int minor) {
		this(major, minor, DEFAULT_PATCH_VERSION);
	}

	protected JavaVersion(int major, int minor, int patch) {
		this(major, minor, patch, DEFAULT_BUILD_NUMBER);
	}

	protected JavaVersion(int major, int minor, int patch, int buildNumber) {

		this.major = validateVersionNumber(major);
		this.minor = validateVersionNumber(minor);
		this.patch = validateVersionNumber(patch);
		this.buildNumber = buildNumber;
	}

	private int validateVersionNumber(int version) {

		Assert.isTrue(version > -1,
			() -> String.format("Version number [%d] must be greater than equal to 0", version));

		return version;
	}

	public boolean isJava8() {
		return EIGHT.getMajor().equals(getMajor())
			&& EIGHT.getMinor().equals(getMinor());
	}

	public boolean isJava11() {
		return ELEVEN.getMajor().equals(getMajor());
	}

	public boolean isJava17() {
		return SEVENTEEN.getMajor().equals(getMajor());
	}

	public boolean isNewerThanOrEqualTo(@Nullable JavaVersion javaVersion) {
		return javaVersion != null && this.compareTo(javaVersion) >= 0;
	}

	public boolean isOlderThan(@Nullable JavaVersion javaVersion) {
		return javaVersion != null && this.compareTo(javaVersion) < 0;
	}

	public boolean isUndetermined() {

		return ZERO.equals(getMajor())
			&& ZERO.equals(getMinor())
			&& ZERO.equals(getPatch());
	}

	public @NonNull Integer getMajor() {
		return this.major;
	}

	public @NonNull Integer getMinor() {
		return this.minor;
	}

	public @NonNull Integer getPatch() {
		return this.patch;
	}

	public @NonNull Integer getBuildNumber() {
		return this.buildNumber;
	}

	@Override
	public int compareTo(@NonNull JavaVersion version) {

		int result = getMajor().compareTo(version.getMajor());

		result = result != 0 ? result
			: getMinor().compareTo(version.getMinor());

		result = result != 0 ? result
			: getPatch().compareTo(version.getPatch());

		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof JavaVersion)) {
			return false;
		}

		JavaVersion that = (JavaVersion) obj;

		return this.getMajor().equals(that.getMajor())
			&& this.getMinor().equals(that.getMinor())
			&& this.getPatch().equals(that.getPatch());
	}

	@Override
	public int hashCode() {

		int hashValue = 17;

		hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getMajor());
		hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getMinor());
		hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getPatch());

		return hashValue;
	}


	@Override
	public @NonNull String toString() {
		return String.format("%1$s.%2$s.%3$s", getMajor(), getMinor(), getPatch());
	}
}
