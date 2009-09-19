package org.argeo.security.ldap;

import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.userdetails.ldap.LdapUserDetailsManager;

public class ArgeoLdapUserDetailsManager extends LdapUserDetailsManager {

	public ArgeoLdapUserDetailsManager(ContextSource contextSource) {
		super(contextSource);
	}

	@Override
	protected DistinguishedName buildGroupDn(String group) {
		// TODO Auto-generated method stub
		return super.buildGroupDn(group);
	}

}
