package org.argeo.security.ldap.nature;

import org.argeo.security.UserNature;
import org.argeo.security.ldap.UserNatureMapper;
import org.argeo.security.nature.SimpleUserNature;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;

public class SimpleUserNatureMapper implements UserNatureMapper {

	public UserNature mapUserInfoFromContext(DirContextOperations ctx) {
		SimpleUserNature nature = new SimpleUserNature();
		nature.setLastName(ctx.getStringAttribute("sn"));
		nature.setFirstName(ctx.getStringAttribute("givenName"));
		nature.setEmail(ctx.getStringAttribute("mail"));
		nature.setDescription(ctx.getStringAttribute("description"));
		return nature;
	}

	public void mapUserInfoToContext(UserNature userInfoArg,
			DirContextAdapter ctx) {
		SimpleUserNature nature = (SimpleUserNature) userInfoArg;
		ctx.setAttributeValue("cn", nature.getFirstName() + " "
				+ nature.getLastName());
		ctx.setAttributeValue("sn", nature.getLastName());
		ctx.setAttributeValue("givenName", nature.getFirstName());
		ctx.setAttributeValue("mail", nature.getEmail());
		if (nature.getDescription() != null) {
			ctx.setAttributeValue("description", nature.getDescription());
		}
	}

	public Boolean supports(UserNature userNature) {
		return userNature instanceof SimpleUserNature;
	}

}
