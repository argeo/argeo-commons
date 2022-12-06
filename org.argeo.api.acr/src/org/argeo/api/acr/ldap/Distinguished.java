package org.argeo.api.acr.ldap;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * An object that can be identified with an X.500 distinguished name.
 * 
 * @see "https://tools.ietf.org/html/rfc1779"
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

	/** List all DNs of an enumeration as strings. */
	static Set<String> enumToDns(EnumSet<? extends Distinguished> enumSet) {
		Set<String> res = new TreeSet<>();
		for (Enum<? extends Distinguished> enm : enumSet) {
			res.add(((Distinguished) enm).dn());
		}
		return res;
	}
}
