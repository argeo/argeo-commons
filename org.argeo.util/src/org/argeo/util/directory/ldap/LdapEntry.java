package org.argeo.util.directory.ldap;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.naming.LdapAttrs;

/** An LDAP entry. */
public interface LdapEntry {
	LdapName getDn();

	Attributes getAttributes();

	void publishAttributes(Attributes modifiedAttributes);

	List<LdapName> getReferences(String attributeId);

	Dictionary<String, Object> getProperties();

	boolean hasCredential(String key, Object value);

	/*
	 * UTILITIES
	 */
	public static void addObjectClasses(Dictionary<String, Object> properties, Set<String> objectClasses) {
		String value = properties.get(LdapAttrs.objectClasses.name()).toString();
		Set<String> currentObjectClasses = new TreeSet<>(Arrays.asList(value.toString().split("\n")));
		currentObjectClasses.addAll(objectClasses);
		StringJoiner values = new StringJoiner("\n");
		currentObjectClasses.forEach((s) -> values.add(s));
		properties.put(LdapAttrs.objectClasses.name(), values.toString());
	}
}
