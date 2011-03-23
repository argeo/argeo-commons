package org.argeo.security.ldap;

import org.springframework.security.providers.ldap.authenticator.LdapShaPasswordEncoder;

/**
 * {@link LdapShaPasswordEncoder} allowing to configure the usage of salt (APache
 * Directory Server 1.0 does not support bind with SSHA)
 */
public class ArgeoLdapShaPasswordEncoder extends LdapShaPasswordEncoder {
	private Boolean useSalt = true;

	@Override
	public String encodePassword(String rawPass, Object salt) {
		return super.encodePassword(rawPass, useSalt ? salt : null);
	}

	public void setUseSalt(Boolean useSalt) {
		this.useSalt = useSalt;
	}

}
