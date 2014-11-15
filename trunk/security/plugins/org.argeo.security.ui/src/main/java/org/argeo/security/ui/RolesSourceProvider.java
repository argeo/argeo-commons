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
package org.argeo.security.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.argeo.security.ui.internal.CurrentUser;
import org.eclipse.ui.AbstractSourceProvider;

/**
 * Provides the roles of the current user as a variable to be used for activity
 * binding
 */
public class RolesSourceProvider extends AbstractSourceProvider {
	public final static String ROLES_VARIABLE = "roles";
	private final static String[] PROVIDED_SOURCE_NAMES = new String[] { ROLES_VARIABLE };

	public Map<String, Set<String>> getCurrentState() {
		Map<String, Set<String>> stateMap = new HashMap<String, Set<String>>();
		stateMap.put(ROLES_VARIABLE, CurrentUser.roles());
		return stateMap;
	}

	public String[] getProvidedSourceNames() {
		return PROVIDED_SOURCE_NAMES;
	}

	public void updateRoles() {
		fireSourceChanged(0, getCurrentState());
	}

	public void dispose() {
	}
}