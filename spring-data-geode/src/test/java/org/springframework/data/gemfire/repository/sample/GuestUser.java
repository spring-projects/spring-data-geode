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

package org.springframework.data.gemfire.repository.sample;

import org.springframework.data.gemfire.mapping.annotation.Region;

/**
 * The GuestUser class represents an authorized restricted user of a service or computer system, etc.
 *
 * @author John Blum
 * @see Region
 * @see org.springframework.data.gemfire.repository.sample.User
 * @since 1.4.0
 */
@Region("/Local/Guest/Users")
@SuppressWarnings("unused")
public class GuestUser extends User {

	public GuestUser(final String username) {
		super(username);
	}

	@Override
	public String toString() {
		return String.format("Guest User '%1$s'", getUsername());
	}

}
