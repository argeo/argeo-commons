/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.security.ldap;

import static org.argeo.security.core.ArgeoUserDetails.createSimpleArgeoUser;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ldap.DefaultLdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.ldap.authenticator.LdapShaPasswordEncoder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.ldap.LdapUserDetailsManager;
import org.springframework.security.userdetails.ldap.LdapUserDetailsService;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

public class ArgeoSecurityDaoLdap implements ArgeoSecurityDao, InitializingBean {
	// private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private LdapAuthoritiesPopulator authoritiesPopulator;
	private String userBase = "ou=People";
	private String usernameAttributeName = "uid";
	private String groupBase = "ou=Roles";
	private String[] groupClasses = { "top", "groupOfNames" };
	private String groupRoleAttributeName = "cn";
	private String groupMemberAttributeName = "member";
	private String defaultRole = "ROLE_USER";
	private String rolePrefix = "ROLE_";

	private final BaseLdapPathContextSource contextSource;
	private final LdapTemplate ldapTemplate;

	private LdapUsernameToDnMapper usernameMapper = null;

	private UserDetailsContextMapper userDetailsMapper;
	private LdapUserDetailsService ldapUserDetailsService;
	private List<UserNatureMapper> userNatureMappers;

	private LdapShaPasswordEncoder ldapShaPasswordEncoder = new LdapShaPasswordEncoder();
	private Random random;

	public ArgeoSecurityDaoLdap(BaseLdapPathContextSource contextSource) {
		this.contextSource = contextSource;
		ldapTemplate = new LdapTemplate(this.contextSource);
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			random = new Random(System.currentTimeMillis());
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (usernameMapper == null)
			usernameMapper = new DefaultLdapUsernameToDnMapper(userBase,
					usernameAttributeName);

		if (authoritiesPopulator == null) {
			DefaultLdapAuthoritiesPopulator ap = new DefaultLdapAuthoritiesPopulator(
					ldapTemplate.getContextSource(), groupBase);
			ap.setDefaultRole(defaultRole);
			ap.setGroupSearchFilter(groupMemberAttributeName + "={0}");
			authoritiesPopulator = ap;
		}

		if (userDetailsMapper == null) {
			ArgeoUserDetailsContextMapper audm = new ArgeoUserDetailsContextMapper();
			audm.setUserNatureMappers(userNatureMappers);
			userDetailsMapper = audm;
		}

		if (userDetailsManager == null) {
			LdapUserDetailsManager ludm = new LdapUserDetailsManager(
					ldapTemplate.getContextSource());
			ludm.setGroupSearchBase(groupBase);
			ludm.setUserDetailsMapper(userDetailsMapper);
			ludm.setUsernameMapper(usernameMapper);
			ludm.setGroupMemberAttributeName(groupMemberAttributeName);
			userDetailsManager = ludm;
		}

		if (ldapUserDetailsService == null) {
			FilterBasedLdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(
					userBase, "(" + usernameAttributeName + "={0})",
					contextSource);
			ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch,
					authoritiesPopulator);
			ldapUserDetailsService.setUserDetailsMapper(userDetailsMapper);
		}
	}

	public synchronized void create(ArgeoUser user) {
		userDetailsManager.createUser(new ArgeoUserDetails(user));
	}

	public synchronized ArgeoUser getUser(String uname) {
		SimpleArgeoUser user = createSimpleArgeoUser(getDetails(uname));
		user.setPassword(null);
		return user;
	}

	public synchronized ArgeoUser getUserWithPassword(String uname) {
		return createSimpleArgeoUser(getDetails(uname));
	}

	// public ArgeoUser getCurrentUser() {
	// ArgeoUser argeoUser = ArgeoUserDetails.securityContextUser();
	// if (argeoUser == null)
	// return null;
	// if (argeoUser.getRoles().contains(defaultRole))
	// argeoUser.getRoles().remove(defaultRole);
	// return argeoUser;
	// }

	@SuppressWarnings("unchecked")
	public synchronized List<ArgeoUser> listUsers() {
		List<String> usernames = (List<String>) ldapTemplate.listBindings(
				new DistinguishedName(userBase), new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						DirContextAdapter ctx = (DirContextAdapter) ctxArg;
						return ctx.getStringAttribute(usernameAttributeName);
					}
				});

		List<ArgeoUser> lst = new ArrayList<ArgeoUser>();
		for (String username : usernames) {
			lst.add(createSimpleArgeoUser(getDetails(username)));
		}
		return lst;
	}

	@SuppressWarnings("unchecked")
	public List<String> listEditableRoles() {
		return (List<String>) ldapTemplate.listBindings(groupBase,
				new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						String groupName = ((DirContextAdapter) ctxArg)
								.getStringAttribute(groupRoleAttributeName);
						String roleName = convertGroupToRole(groupName);
						return roleName;
					}
				});
	}

	public synchronized void update(ArgeoUser user) {
		ArgeoUserDetails argeoUserDetails = new ArgeoUserDetails(user);
		userDetailsManager.updateUser(new ArgeoUserDetails(user));
		// refresh logged in user
		if (ArgeoUserDetails.securityContextUser().getUsername()
				.equals(argeoUserDetails.getUsername())) {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(argeoUserDetails,
							null, argeoUserDetails.getAuthorities()));
		}
	}

	public synchronized void delete(String username) {
		userDetailsManager.deleteUser(username);
	}

	public synchronized Boolean userExists(String username) {
		return userDetailsManager.userExists(username);
	}

	public void createRole(String role, final String superuserName) {
		String group = convertRoleToGroup(role);
		DistinguishedName superuserDn = (DistinguishedName) ldapTemplate
				.executeReadWrite(new ContextExecutor() {
					public Object executeWithContext(DirContext ctx)
							throws NamingException {
						return LdapUtils.getFullDn(
								usernameMapper.buildDn(superuserName), ctx);
					}
				});

		Name groupDn = buildGroupDn(group);
		DirContextAdapter context = new DirContextAdapter();
		context.setAttributeValues("objectClass", groupClasses);
		context.setAttributeValue("cn", group);

		// Add superuser because cannot create empty group
		context.setAttributeValue(groupMemberAttributeName,
				superuserDn.toString());

		ldapTemplate.bind(groupDn, context, null);
	}

	public void deleteRole(String role) {
		String group = convertRoleToGroup(role);
		Name dn = buildGroupDn(group);
		ldapTemplate.unbind(dn);
	}

	public Boolean isPasswordValid(String encoded, String raw) {
		return ldapShaPasswordEncoder.isPasswordValid(encoded, raw, null);
	}

	public String encodePassword(String raw) {
		byte[] salt = null;
		// TODO: check that Linux auth supports SSHA
		// byte[] salt = new byte[16];
		// random.nextBytes(salt);
		return ldapShaPasswordEncoder.encodePassword(raw, salt);
	}

	protected String convertRoleToGroup(String role) {
		String group = role;
		if (group.startsWith(rolePrefix)) {
			group = group.substring(rolePrefix.length());
			group = group.toLowerCase();
		}
		return group;
	}

	public String convertGroupToRole(String groupName) {
		groupName = groupName.toUpperCase();

		return rolePrefix + groupName;
	}

	protected Name buildGroupDn(String name) {
		return new DistinguishedName(groupRoleAttributeName + "=" + name + ","
				+ groupBase);
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	public void setUsernameAttributeName(String usernameAttribute) {
		this.usernameAttributeName = usernameAttribute;
	}

	public void setAuthoritiesPopulator(
			LdapAuthoritiesPopulator authoritiesPopulator) {
		this.authoritiesPopulator = authoritiesPopulator;
	}

	protected UserDetails getDetails(String username) {
		return userDetailsManager.loadUserByUsername(username);
	}

	public void setGroupBase(String groupBase) {
		this.groupBase = groupBase;
	}

	public void setGroupRoleAttributeName(String groupRoleAttributeName) {
		this.groupRoleAttributeName = groupRoleAttributeName;
	}

	public void setGroupMemberAttributeName(String groupMemberAttributeName) {
		this.groupMemberAttributeName = groupMemberAttributeName;
	}

	public void setDefaultRole(String defaultRole) {
		this.defaultRole = defaultRole;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
	}

	public void setUsernameMapper(LdapUsernameToDnMapper usernameMapper) {
		this.usernameMapper = usernameMapper;
	}

	public void setUserDetailsMapper(UserDetailsContextMapper userDetailsMapper) {
		this.userDetailsMapper = userDetailsMapper;
	}

	public LdapAuthoritiesPopulator getAuthoritiesPopulator() {
		return authoritiesPopulator;
	}

	public UserDetailsContextMapper getUserDetailsMapper() {
		return userDetailsMapper;
	}

	public void setUserNatureMappers(List<UserNatureMapper> userNatureMappers) {
		this.userNatureMappers = userNatureMappers;
	}

	public String getDefaultRole() {
		return defaultRole;
	}

	public void setGroupClasses(String[] groupClasses) {
		this.groupClasses = groupClasses;
	}

	public UserDetailsService getUserDetailsService() {
		return ldapUserDetailsService;
	}

}
