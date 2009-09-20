package org.argeo.security.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

public class ArgeoUserDetailsContextMapper implements UserDetailsContextMapper {
//	private final static Log log = LogFactory
//			.getLog(ArgeoUserDetailsContextMapper.class);

	private List<UserNatureMapper> userNatureMappers = new ArrayList<UserNatureMapper>();

	public UserDetails mapUserFromContext(DirContextOperations ctx,
			String username, GrantedAuthority[] authorities) {
		byte[] arr = (byte[]) ctx.getAttributeSortedStringSet("userPassword")
				.first();
		String password = new String(arr);

		List<UserNature> userNatures = new ArrayList<UserNature>();
		for (UserNatureMapper userInfoMapper : userNatureMappers) {
			UserNature userNature = userInfoMapper.mapUserInfoFromContext(ctx);
			if (userNature != null)
				userNatures.add(userNature);
		}

		return new ArgeoUserDetails(username, Collections
				.unmodifiableList(userNatures), password, authorities);
	}

	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		ctx.setAttributeValues("objectClass", new String[] { "inetOrgPerson" });
		ctx.setAttributeValue("uid", user.getUsername());
		ctx.setAttributeValue("userPassword", user.getPassword());
		if (user instanceof ArgeoUser) {
			ArgeoUser argeoUser = (ArgeoUser) user;
			for (UserNature userNature : argeoUser.getUserNatures()) {
				for (UserNatureMapper userInfoMapper : userNatureMappers) {
					if (userInfoMapper.supports(userNature)) {
						userInfoMapper.mapUserInfoToContext(userNature, ctx);
						break;// use the first mapper found and no others
					}
				}
			}
		}
	}

	public void setUserNatureMappers(List<UserNatureMapper> userNatureMappers) {
		this.userNatureMappers = userNatureMappers;
	}

}
