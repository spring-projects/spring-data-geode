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
package org.springframework.data.gemfire;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link IndexType} is an enumerated type of Apache Geode {@link org.apache.geode.cache.query.IndexType Index Types}.
 *
 * NOTE: The Apache Geode {@link org.apache.geode.cache.query.IndexType} enum has been deprecated, therefore the SDG
 * {@link IndexType} exists to replace it.
 *
 * @author John Blum
 * @see org.apache.geode.cache.query.IndexType
 * @since 1.5.2
 */
@SuppressWarnings({ "deprecation", "unused" })
public enum IndexType {

	FUNCTIONAL(org.apache.geode.cache.query.IndexType.FUNCTIONAL),
	HASH(org.apache.geode.cache.query.IndexType.HASH),
	PRIMARY_KEY(org.apache.geode.cache.query.IndexType.PRIMARY_KEY),
	KEY(org.apache.geode.cache.query.IndexType.PRIMARY_KEY);

	public static final IndexType DEFAULT = IndexType.FUNCTIONAL;

	private final org.apache.geode.cache.query.IndexType gemfireIndexType;

	/**
	 * Constructs a new instance of the {@link IndexType} enum initialized with the given Apache Geode
	 * {@link org.apache.geode.cache.query.IndexType}.
	 *
	 * @param gemfireIndexType an Apache Geode {@link org.apache.geode.cache.query.IndexType}.
	 * @throws IllegalArgumentException if the Apache Geode {@link org.apache.geode.cache.query.IndexType}
	 * is {@literal  null}.
	 * @see org.apache.geode.cache.query.IndexType
	 */
	IndexType(@NonNull org.apache.geode.cache.query.IndexType gemfireIndexType) {

		Assert.notNull(gemfireIndexType, "The Apache Geode IndexType must not be null");

		this.gemfireIndexType = gemfireIndexType;
	}

	/**
	 * Null-safe operation to determine if the given {@link IndexType} is a {@literal FUNCTIONAL} Index.
	 *
	 * @param indexType {@link IndexType} to evaluate.
	 * @return a boolean value indicating whether the given {@link IndexType} is a {@literal FUNCTIONAL} Index.
	 * @see #isFunctional()
	 */
	public static boolean isFunctional(@Nullable IndexType indexType) {
		return indexType != null && indexType.isFunctional();
	}

	/**
	 * Null-safe operation to determine if the given {@link IndexType} is a {@literal HASH} Index.
	 *
	 * @param indexType {@link IndexType} to evaluate.
	 * @return a boolean value indicating whether the given {@link IndexType} is a {@literal HASH} Index.
	 * @see #isHash()
	 */
	public static boolean isHash(IndexType indexType) {
		return indexType != null && indexType.isHash();
	}

	/**
	 * Null-safe operation to determine if the given {@link IndexType} is a {@literal KEY} Index.
	 *
	 * @param indexType {@link IndexType} to evaluate.
	 * @return a boolean value indicating whether the given {@link IndexType} is a {@literal KEY} Index.
	 * @see #isKey()
	 */
	public static boolean isKey(IndexType indexType) {
		return indexType != null && indexType.isKey();
	}

	/**
	 * Returns an {@link IndexType} given the corresponding Apache Geode {@link org.apache.geode.cache.query.IndexType}
	 * or {@literal null} if no SDG {@link IndexType} corresponds to the given Apache Geode
	 * {@link org.apache.geode.cache.query.IndexType}.
	 *
	 * @param gemfireIndexType Apache Geode {@link org.apache.geode.cache.query.IndexType}.
	 * @return an {@link IndexType} matching the Apache Geode {@link org.apache.geode.cache.query.IndexType}
	 * or {@literal null} if the Apache Geode {@link org.apache.geode.cache.query.IndexType} does not match
	 * any {@literal IndexType} in this enumeration.
	 * @see org.apache.geode.cache.query.IndexType
	 */
	public static IndexType valueOf(org.apache.geode.cache.query.IndexType gemfireIndexType) {

		for (IndexType indexType : values()) {
			if (indexType.getGemfireIndexType().equals(gemfireIndexType)) {
				return indexType;
			}
		}

		return null;
	}

	/**
	 * Return an {@link IndexType} matching the given {@link String}.
	 *
	 * @param value {@link String} value describing {@link IndexType} to match.
	 * @return an {@link IndexType} matching the given {@link String}.
	 * @see java.lang.String#equalsIgnoreCase(String)
	 */
	public static IndexType valueOfIgnoreCase(String value) {

		for (IndexType indexType : values()) {
			if (indexType.name().equalsIgnoreCase(value)) {
				return indexType;
			}
		}

		return null;
	}

	/**
	 * Gets the matching Apache Geode {@link org.apache.geode.cache.query.IndexType} for this {@link IndexType}
	 * enumerated value.
	 *
	 * @return the matching Apache Geode {@link org.apache.geode.cache.query.IndexType}.
	 * @see org.apache.geode.cache.query.IndexType
	 */
	public @NonNull org.apache.geode.cache.query.IndexType getGemfireIndexType() {
		return this.gemfireIndexType;
	}

	/**
	 * Determines whether this {@link IndexType} is a {@literal FUNCTIONAL} Index.
	 *
	 * @return a boolean value indicating whether this {@link IndexType} is a {@literal FUNCTIONAL} Index.
	 */
	public boolean isFunctional() {
		return this.equals(FUNCTIONAL);
	}

	/**
	 * Determines whether this {@link IndexType} is a {@literal HASH} Index.
	 *
	 * @return a boolean value indicating whether this {@literal IndexType} is a {@literal HASH} Index.
	 */
	public boolean isHash() {
		return this.equals(HASH);
	}

	/**
	 * Determines whether this {@literal IndexType} is a {@literal KEY} Index.
	 *
	 * @return a boolean value indicating whether this {@link IndexType} is a {@literal KEY} Index.
	 */
	public boolean isKey() {
		return this.equals(KEY) || this.equals(PRIMARY_KEY);
	}
}
