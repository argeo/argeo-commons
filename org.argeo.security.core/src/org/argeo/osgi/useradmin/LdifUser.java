package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.User;

class LdifUser implements User {
	// optimisation
	//List<LdifGroup> directMemberOf = new ArrayList<LdifGroup>();

	private final LdapName dn;
	private Attributes attributes;

	private final AttributeDictionary properties;
	private final AttributeDictionary credentials;

	private List<String> credentialAttributes = Arrays
			.asList(new String[] { "userpassword" });

	LdifUser(LdapName dn, Attributes attributes) {
		this.dn = dn;
		this.attributes = attributes;
		properties = new AttributeDictionary(attributes, credentialAttributes,
				false);
		credentials = new AttributeDictionary(attributes, credentialAttributes,
				true);
	}

	@Override
	public String getName() {
		return dn.toString();
	}

	@Override
	public int getType() {
		return USER;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	@Override
	public Dictionary<String, Object> getCredentials() {
		return credentials;
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		Object storedValue = getCredentials().get(key);
		if (storedValue == null || value == null)
			return false;
		if (!(value instanceof String || value instanceof byte[]))
			return false;
		if (storedValue instanceof String && value instanceof String)
			return storedValue.equals(value);
		if (storedValue instanceof byte[] && value instanceof byte[])
			return Arrays.equals((byte[]) storedValue, (byte[]) value);
		return false;
	}

	protected LdapName getDn() {
		return dn;
	}

	protected Attributes getAttributes() {
		return attributes;
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof LdifUser) {
			LdifUser that = (LdifUser) obj;
			return this.dn.equals(that.dn);
		}
		return false;
	}

	@Override
	public String toString() {
		return dn.toString();
	}
}
