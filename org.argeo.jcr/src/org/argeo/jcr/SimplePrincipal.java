/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr;

import java.security.Principal;

/** Canonical implementation of a {@link Principal} */
public class SimplePrincipal implements Principal {
	private final String name;

	public SimplePrincipal(String name) {
		if (name == null)
			throw new IllegalArgumentException("Principal name cannot be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Principal)
			return name.equals((((Principal) obj).getName()));
		return name.equals(obj.toString());
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new SimplePrincipal(name);
	}

	@Override
	public String toString() {
		return name;
	}

}
