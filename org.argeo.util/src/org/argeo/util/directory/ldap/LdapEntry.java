package org.argeo.util.directory.ldap;

import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

public interface LdapEntry {
	LdapName getDn();

	Attributes getAttributes();

	void publishAttributes(Attributes modifiedAttributes);

	public List<LdapName> getReferences(String attributeId);
}
