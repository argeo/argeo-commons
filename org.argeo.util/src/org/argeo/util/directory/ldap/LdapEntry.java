package org.argeo.util.directory.ldap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
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
	/**
	 * Convert a collection of object classes to the format expected by an LDAP
	 * backend.
	 */
	public static void addObjectClasses(Dictionary<String, Object> properties, Collection<String> objectClasses) {
		String value = properties.get(LdapAttrs.objectClasses.name()).toString();
		Set<String> currentObjectClasses = new TreeSet<>(Arrays.asList(value.toString().split("\n")));
		currentObjectClasses.addAll(objectClasses);
		StringJoiner values = new StringJoiner("\n");
		currentObjectClasses.forEach((s) -> values.add(s));
		properties.put(LdapAttrs.objectClasses.name(), values.toString());
	}

	public static Object getLocalized(Dictionary<String, Object> properties, String key, Locale locale) {
		if (locale == null)
			return null;
		Object value = null;
		value = properties.get(key + ";lang-" + locale.getLanguage() + "-" + locale.getCountry());
		if (value == null)
			value = properties.get(key + ";lang-" + locale.getLanguage());
		return value;
	}

	public static String toLocalizedKey(String key, Locale locale) {
		String country = locale.getCountry();
		if ("".equals(country)) {
			return key + ";lang-" + locale.getLanguage();
		} else {
			return key + ";lang-" + locale.getLanguage() + "-" + locale.getCountry();
		}
	}
}
