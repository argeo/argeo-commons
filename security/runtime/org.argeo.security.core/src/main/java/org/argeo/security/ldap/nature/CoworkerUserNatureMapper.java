package org.argeo.security.ldap.nature;

import org.argeo.security.UserNature;
import org.argeo.security.ldap.UserNatureMapper;
import org.argeo.security.nature.CoworkerNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public class CoworkerUserNatureMapper implements UserNatureMapper {

	public UserNature mapUserInfoFromContext(DirContextOperations ctx) {
		CoworkerNature basicUserInfo = new CoworkerNature();
		basicUserInfo.setDescription(ctx.getStringAttribute("description"));
		basicUserInfo.setMobile(ctx.getStringAttribute("mobile"));
		basicUserInfo.setTelephoneNumber(ctx
				.getStringAttribute("telephoneNumber"));
		basicUserInfo.setUuid(ctx.getStringAttribute("employeeNumber"));
		return basicUserInfo;
	}

	public void mapUserInfoToContext(UserNature userInfoArg,
			DirContextAdapter ctx) {
		CoworkerNature userInfo = (CoworkerNature) userInfoArg;
		ctx.setAttributeValue("employeeNumber", userInfo.getUuid());
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
		return userInfo instanceof CoworkerNature;
	}

}
