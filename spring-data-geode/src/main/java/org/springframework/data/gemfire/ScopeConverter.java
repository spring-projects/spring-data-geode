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

import java.beans.PropertyEditor;

import org.apache.geode.cache.Scope;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.gemfire.support.AbstractPropertyEditorConverterSupport;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * The {@link ScopeConverter} class is a Spring {@link Converter} and JavaBeans {@link PropertyEditor}
 * that converts a {@link String} into a {@link Scope}.
 *
 * @author John Blum
 * @see java.beans.PropertyEditor
 * @see org.apache.geode.cache.Scope
 * @see org.springframework.core.convert.converter.Converter
 * @see org.springframework.data.gemfire.support.AbstractPropertyEditorConverterSupport
 * @since 1.6.0
 */
@SuppressWarnings("unused")
public class ScopeConverter extends AbstractPropertyEditorConverterSupport<Scope> {

	/**
	 * Converts the given {@link String} into an instance of {@link Scope}.
	 *
	 * @param source the String to convert into a GemFire Scope.
	 * @return a GemFire Scope for the given String.
	 * @throws java.lang.IllegalArgumentException if the String is not a valid GemFire Scope.
	 * @see org.apache.geode.cache.Scope#fromString(String)
	 * @see org.springframework.data.gemfire.ScopeType#valueOfIgnoreCase(String)
	 * @see org.springframework.data.gemfire.ScopeType#getScope(ScopeType)
	 */
	@Override
	public @NonNull Scope convert(@Nullable String source) {

		try {
			return Scope.fromString(source);
		}
		catch (IllegalArgumentException cause) {
			return assertConverted(source, ScopeType.getScope(ScopeType.valueOfIgnoreCase(source)), Scope.class);
		}
	}
}
