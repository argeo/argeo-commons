package org.argeo.security;

import java.util.List;

public interface ArgeoUser {
	public String getUsername();

	public List<UserNature> getUserNatures();

	public List<String> getRoles();
	
	public String getPassword();
}
