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
	private List<UserNatureMapper> userInfoMappers = new ArrayList<UserNatureMapper>();

	public UserDetails mapUserFromContext(DirContextOperations ctx,
			String username, GrantedAuthority[] authorities) {
		byte[] arr = (byte[]) ctx.getAttributeSortedStringSet("userPassword")
				.first();
		String password = new String(arr);

		List<UserNature> userInfos = new ArrayList<UserNature>();
		for (UserNatureMapper userInfoMapper : userInfoMappers) {
			userInfos.add(userInfoMapper.mapUserInfoFromContext(ctx));
		}

		return new ArgeoUserDetails(username, Collections
				.unmodifiableList(userInfos), password, authorities);
	}

	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		ctx.setAttributeValues("objectClass", new String[] { "inetOrgPerson" });
		ctx.setAttributeValue("uid", user.getUsername());
		ctx.setAttributeValue("userPassword", user.getPassword());
		if (user instanceof ArgeoUser) {
			ArgeoUser argeoUser = (ArgeoUser) user;
			for (UserNature userInfo : argeoUser.getUserNatures()) {
				for (UserNatureMapper userInfoMapper : userInfoMappers) {
					if (userInfoMapper.supports(userInfo)) {
						userInfoMapper.mapUserInfoToContext(userInfo, ctx);
						break;// use the first mapper found an no others
					}
				}
			}
		}
	}

	public void setUserInfoMappers(List<UserNatureMapper> userInfoMappers) {
		this.userInfoMappers = userInfoMappers;
	}

}
