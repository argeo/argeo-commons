package org.argeo.security.ldap;

import org.springframework.ldap.core.ContextSource;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;

/** TODO: notify Spring Security to open this class more. */
public class ArgeoLdapAuthoritiesPopulator extends
		DefaultLdapAuthoritiesPopulator {

	/* Hacked from parent class */
	private String groupRoleAttribute = "cn";
	private final String groupSearchBase;
	private String rolePrefix = "ROLE_";
	private boolean convertToUpperCase = true;

	public ArgeoLdapAuthoritiesPopulator(ContextSource contextSource,
			String groupSearchBase) {
		super(contextSource, groupSearchBase);
		this.groupSearchBase = groupSearchBase;
	}

	@Override
	public void setConvertToUpperCase(boolean convertToUpperCase) {
		super.setConvertToUpperCase(convertToUpperCase);
		this.convertToUpperCase = convertToUpperCase;
	}

	@Override
	public void setGroupRoleAttribute(String groupRoleAttribute) {
		super.setGroupRoleAttribute(groupRoleAttribute);
		this.groupRoleAttribute = groupRoleAttribute;
	}

	@Override
	public void setRolePrefix(String rolePrefix) {
		super.setRolePrefix(rolePrefix);
		this.rolePrefix = rolePrefix;
	}

	/** Hacked from parent class. */
	public String convertGroupToRole(String groupName) {
		if (convertToUpperCase) {
			groupName = groupName.toUpperCase();
		}

		return rolePrefix + groupName;
	}

	public String getGroupRoleAttribute() {
		return groupRoleAttribute;
	}

	public String getGroupSearchBase() {
		return groupSearchBase;
	}

}
