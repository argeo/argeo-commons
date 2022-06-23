package org.argeo.util.directory.ldap;

import java.util.Dictionary;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

public interface LdapEntry {
	LdapName getDn();

	Attributes getAttributes();

	void publishAttributes(Attributes modifiedAttributes);

	public List<LdapName> getReferences(String attributeId);
	
	public Dictionary<String, Object> getProperties();

	public boolean hasCredential(String key, Object value) ;

}
