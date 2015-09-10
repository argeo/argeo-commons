package org.argeo.osgi.useradmin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public abstract class AbstractLdapUserAdmin implements UserAdmin {
	private boolean isReadOnly;
	private URI uri;

	private UserAdmin externalRoles;
	private List<String> indexedUserProperties = Arrays.asList(new String[] {
			"uid", "mail", "cn" });

	public AbstractLdapUserAdmin() {
	}

	public AbstractLdapUserAdmin(URI uri, boolean isReadOnly) {
		this.uri = uri;
		this.isReadOnly = isReadOnly;
	}

	public void init() {

	}

	public void destroy() {

	}

	/** Returns the {@link Group}s this user is a direct member of. */
	protected abstract List<? extends Group> getDirectGroups(User user);

	List<Role> getAllRoles(User user) {
		List<Role> allRoles = new ArrayList<Role>();
		if (user != null) {
			collectRoles(user, allRoles);
			allRoles.add(user);
		} else
			collectAnonymousRoles(allRoles);
		return allRoles;
	}

	private void collectRoles(User user, List<Role> allRoles) {
		for (Group group : getDirectGroups(user)) {
			// TODO check for loops
			allRoles.add(group);
			collectRoles(group, allRoles);
		}
	}

	private void collectAnonymousRoles(List<Role> allRoles) {
		// TODO gather anonymous roles
	}

	protected URI getUri() {
		return uri;
	}

	protected void setUri(URI uri) {
		this.uri = uri;
	}

	protected List<String> getIndexedUserProperties() {
		return indexedUserProperties;
	}

	protected void setIndexedUserProperties(List<String> indexedUserProperties) {
		this.indexedUserProperties = indexedUserProperties;
	}

	protected void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	UserAdmin getExternalRoles() {
		return externalRoles;
	}

	public void setExternalRoles(UserAdmin externalRoles) {
		this.externalRoles = externalRoles;
	}

}
