package org.argeo.security.ldap;

import org.argeo.security.UserNature;
import org.argeo.security.nature.SimpleUserNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public class SimpleUserNatureMapper implements UserNatureMapper {

	public UserNature mapUserInfoFromContext(DirContextOperations ctx) {
		SimpleUserNature basicUserInfo = new SimpleUserNature();
		basicUserInfo.setLastName(ctx.getStringAttribute("sn"));
		basicUserInfo.setFirstName(ctx.getStringAttribute("givenName"));
		basicUserInfo.setEmail(ctx.getStringAttribute("mail"));
		return basicUserInfo;
	}

	public void mapUserInfoToContext(UserNature userInfoArg,
			DirContextAdapter ctx) {
		SimpleUserNature userInfo = (SimpleUserNature) userInfoArg;
		ctx.setAttributeValue("cn", userInfo.getFirstName() + " "
				+ userInfo.getLastName());
		ctx.setAttributeValue("sn", userInfo.getLastName());
		ctx.setAttributeValue("givenName", userInfo.getFirstName());
		ctx.setAttributeValue("mail", userInfo.getEmail());
	}

	public Boolean supports(UserNature userInfo) {
		return userInfo instanceof SimpleUserNature;
	}

}
