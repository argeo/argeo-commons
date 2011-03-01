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