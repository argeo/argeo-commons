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
		basicUserInfo.setDescription(ctx.getStringAttribute("description"));
		basicUserInfo.setMobile(ctx.getStringAttribute("mobile"));
		basicUserInfo.setTelephoneNumber(ctx
				.getStringAttribute("telephoneNumber"));
		return basicUserInfo;
	}

	public void mapUserInfoToContext(UserNature userInfoArg, DirContextAdapter ctx) {
		SimpleUserNature userInfo = (SimpleUserNature) userInfoArg;
		ctx.setAttributeValue("cn", userInfo.getFullName());
		ctx.setAttributeValue("sn", userInfo.getLastName());
		ctx.setAttributeValue("givenName", userInfo.getFirstName());
		ctx.setAttributeValue("mail", userInfo.getEmail());
		if (userInfo.getDescription() != null) {
			ctx.setAttributeValue("description", userInfo.getDescription());
		}
		if (userInfo.getMobile() == null || !userInfo.getMobile().equals("")) {
			ctx.setAttributeValue("mobile", userInfo.getMobile());
		}
		if (userInfo.getTelephoneNumber() == null
				|| !userInfo.getTelephoneNumber().equals("")) {
			ctx.setAttributeValue("telephoneNumber", userInfo
					.getTelephoneNumber());
		}
	}

	public Boolean supports(UserNature userInfo) {
		return userInfo instanceof SimpleUserNature;
	}

}
