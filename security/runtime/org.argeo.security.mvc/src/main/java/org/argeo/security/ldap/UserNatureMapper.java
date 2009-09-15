package org.argeo.security.ldap;

import org.argeo.security.UserNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public interface UserNatureMapper {
	public void mapUserInfoToContext(UserNature userInfo, DirContextAdapter ctx);

	public UserNature mapUserInfoFromContext(DirContextOperations ctx);

	public Boolean supports(UserNature userInfo);
}
