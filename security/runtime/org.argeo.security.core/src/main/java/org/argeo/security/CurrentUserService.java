package org.argeo.security;

import java.util.Map;

@Deprecated
public interface CurrentUserService {
	public ArgeoUser getCurrentUser();

	public void updateCurrentUserPassword(String oldPassword, String newPassword);

	@Deprecated
	public void updateCurrentUserNatures(Map<String, UserNature> userNatures);

}
