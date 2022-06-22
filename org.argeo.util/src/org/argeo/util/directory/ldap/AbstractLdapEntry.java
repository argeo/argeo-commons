package org.argeo.util.directory.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

/** An entry in an LDAP (or LDIF) directory. */
public abstract class AbstractLdapEntry implements LdapEntry {
	private final AbstractLdapDirectory directory;

	private final LdapName dn;

	private Attributes publishedAttributes;

	protected AbstractLdapEntry(AbstractLdapDirectory directory, LdapName dn, Attributes attributes) {
		Objects.requireNonNull(directory);
		Objects.requireNonNull(dn);
		this.directory = directory;
		this.dn = dn;
		this.publishedAttributes = attributes;
	}

	@Override
	public LdapName getDn() {
		return dn;
	}

	public synchronized Attributes getAttributes() {
		return isEditing() ? getModifiedAttributes() : publishedAttributes;
	}
	
	@Override
	public List<LdapName> getReferences(String attributeId){
		Attribute memberAttribute = getAttributes().get(attributeId);
		if (memberAttribute == null)
			return new ArrayList<LdapName>();
		try {
			List<LdapName> roles = new ArrayList<LdapName>();
			NamingEnumeration<?> values = memberAttribute.getAll();
			while (values.hasMore()) {
				LdapName dn = new LdapName(values.next().toString());
				roles.add(dn);
			}
			return roles;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get members", e);
		}
		
	}

	/** Should only be called from working copy thread. */
	protected synchronized Attributes getModifiedAttributes() {
		assert getWc() != null;
		return getWc().getModifiedData().get(getDn());
	}

	protected synchronized boolean isEditing() {
		return getWc() != null && getModifiedAttributes() != null;
	}

	private synchronized LdapEntryWorkingCopy getWc() {
		return directory.getWorkingCopy();
	}

	protected synchronized void startEditing() {
//		if (frozen)
//			throw new IllegalStateException("Cannot edit frozen view");
		if (directory.isReadOnly())
			throw new IllegalStateException("User directory is read-only");
		assert getModifiedAttributes() == null;
		getWc().startEditing(this);
		// modifiedAttributes = (Attributes) publishedAttributes.clone();
	}

	public synchronized void publishAttributes(Attributes modifiedAttributes) {
		publishedAttributes = modifiedAttributes;
	}

	public AbstractLdapDirectory getDirectory() {
		return directory;
	}

	public LdapDirectoryDao getDirectoryDao() {
		return directory.getDirectoryDao();
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof LdapEntry) {
			LdapEntry that = (LdapEntry) obj;
			return this.dn.equals(that.getDn());
		}
		return false;
	}

	@Override
	public String toString() {
		return dn.toString();
	}

}
