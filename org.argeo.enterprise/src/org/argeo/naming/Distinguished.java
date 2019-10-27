package org.argeo.naming;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * An object that can be identified with an X.500 distinguished name.
 * 
 * @see https://tools.ietf.org/html/rfc1779
 */
public interface Distinguished {
	/** The related distinguished name. */
	String dn();

	/** The related distinguished name as an {@link LdapName}. */
	default LdapName distinguishedName() {
		try {
			return new LdapName(dn());
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Distinguished name " + dn() + " is not properly formatted.", e);
		}
	}
}
