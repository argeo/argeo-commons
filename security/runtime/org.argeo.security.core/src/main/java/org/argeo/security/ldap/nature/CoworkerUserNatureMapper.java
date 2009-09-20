package org.argeo.security.ldap.nature;

import org.argeo.security.UserNature;
import org.argeo.security.ldap.UserNatureMapper;
import org.argeo.security.nature.CoworkerNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public class CoworkerUserNatureMapper implements UserNatureMapper {

	public UserNature mapUserInfoFromContext(DirContextOperations ctx) {
		CoworkerNature nature = new CoworkerNature();
		nature.setDescription(ctx.getStringAttribute("description"));
		nature.setMobile(ctx.getStringAttribute("mobile"));
		nature.setTelephoneNumber(ctx.getStringAttribute("telephoneNumber"));

		if (nature.getDescription() == null && nature.getMobile() == null
				&& nature.getTelephoneNumber() == null)
			return null;
		else
			return nature;
	}

	public void mapUserInfoToContext(UserNature userInfoArg,
			DirContextAdapter ctx) {
		CoworkerNature nature = (CoworkerNature) userInfoArg;
		if (nature.getDescription() != null) {
			ctx.setAttributeValue("description", nature.getDescription());
		}
		if (nature.getMobile() == null || !nature.getMobile().equals("")) {
			ctx.setAttributeValue("mobile", nature.getMobile());
		}
		if (nature.getTelephoneNumber() == null
				|| !nature.getTelephoneNumber().equals("")) {
			ctx.setAttributeValue("telephoneNumber", nature
					.getTelephoneNumber());
		}
	}

	public Boolean supports(UserNature userNature) {
		return userNature instanceof CoworkerNature;
	}

}
