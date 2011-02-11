package org.argeo.security;

import java.util.Map;

public interface CurrentUserService {
	public ArgeoUser getCurrentUser();

	public void updateCurrentUserPassword(String oldPassword, String newPassword);

	public void updateCurrentUserNatures(Map<String, UserNature> userNatures);

}
