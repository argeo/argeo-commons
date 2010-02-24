package org.argeo.security;

import java.util.List;

public interface ArgeoUser {
	public String getUsername();

	public List<UserNature> getUserNatures();

	/** Implementation should refuse to add new user natures via this method. */
	public void updateUserNatures(List<UserNature> userNatures);

	public List<String> getRoles();

	public String getPassword();
}
