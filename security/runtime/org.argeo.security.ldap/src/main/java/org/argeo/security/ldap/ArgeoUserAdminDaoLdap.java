/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;

/**
 * Wraps low-level LDAP operation on user and roles, used by
 * {@link ArgeoLdapUserDetailsManager}
 */
public class ArgeoUserAdminDaoLdap {
	private String userBase;
	private String usernameAttribute;
	private String groupBase;
	private String[] groupClasses;

	private String groupRoleAttribute;
	private String groupMemberAttribute;
	private String defaultRole;
	private String rolePrefix;

	private final LdapTemplate ldapTemplate;
	private LdapUsernameToDnMapper usernameMapper;

	/**
	 * Standard constructor, using the LDAP context source shared with Spring
	 * Security components.
	 */
	public ArgeoUserAdminDaoLdap(BaseLdapPathContextSource contextSource) {
		this.ldapTemplate = new LdapTemplate(contextSource);
	}

	@SuppressWarnings("unchecked")
	public synchronized Set<String> listUsers() {
		List<String> usernames = (List<String>) ldapTemplate.listBindings(
				new DistinguishedName(userBase), new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						DirContextAdapter ctx = (DirContextAdapter) ctxArg;
						return ctx.getStringAttribute(usernameAttribute);
					}
				});

		return Collections
				.unmodifiableSortedSet(new TreeSet<String>(usernames));
	}

	@SuppressWarnings("unchecked")
	public Set<String> listEditableRoles() {
		return Collections.unmodifiableSortedSet(new TreeSet<String>(
				ldapTemplate.listBindings(groupBase, new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						String groupName = ((DirContextAdapter) ctxArg)
								.getStringAttribute(groupRoleAttribute);
						String roleName = convertGroupToRole(groupName);
						return roleName;
					}
				})));
	}

	@SuppressWarnings("unchecked")
	public Set<String> listUsersInRole(String role) {
		return (Set<String>) ldapTemplate.lookup(
				buildGroupDn(convertRoleToGroup(role)), new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						DirContextAdapter ctx = (DirContextAdapter) ctxArg;
						String[] userDns = ctx
								.getStringAttributes(groupMemberAttribute);
						TreeSet<String> set = new TreeSet<String>();
						for (String userDn : userDns) {
							DistinguishedName dn = new DistinguishedName(userDn);
							String username = dn.getValue(usernameAttribute);
							set.add(username);
						}
						return Collections.unmodifiableSortedSet(set);
					}
				});
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
		context.setAttributeValue(groupMemberAttribute, superuserDn.toString());
		ldapTemplate.bind(groupDn, context, null);
	}

	public void deleteRole(String role) {
		String group = convertRoleToGroup(role);
		Name dn = buildGroupDn(group);
		ldapTemplate.unbind(dn);
	}

	/** Maps a role (ROLE_XXX) to the related LDAP group (xxx) */
	protected String convertRoleToGroup(String role) {
		String group = role;
		if (group.startsWith(rolePrefix)) {
			group = group.substring(rolePrefix.length());
			group = group.toLowerCase();
		}
		return group;
	}

	/** Maps anLDAP group (xxx) to the related role (ROLE_XXX) */
	protected String convertGroupToRole(String groupName) {
		groupName = groupName.toUpperCase();

		return rolePrefix + groupName;
	}

	protected Name buildGroupDn(String name) {
		return new DistinguishedName(groupRoleAttribute + "=" + name + ","
				+ groupBase);
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	public void setUsernameAttribute(String usernameAttribute) {
		this.usernameAttribute = usernameAttribute;
	}

	public void setGroupBase(String groupBase) {
		this.groupBase = groupBase;
	}

	public void setGroupRoleAttribute(String groupRoleAttributeName) {
		this.groupRoleAttribute = groupRoleAttributeName;
	}

	public void setGroupMemberAttribute(String groupMemberAttributeName) {
		this.groupMemberAttribute = groupMemberAttributeName;
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

	public String getDefaultRole() {
		return defaultRole;
	}

	public void setGroupClasses(String[] groupClasses) {
		this.groupClasses = groupClasses;
	}
}
