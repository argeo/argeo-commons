package org.argeo.util.directory.ldap;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

public interface LdapEntry {
	LdapName getDn();

	Attributes getAttributes();

	void publishAttributes(Attributes modifiedAttributes);

}
