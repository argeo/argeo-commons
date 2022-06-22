package org.argeo.util.directory.ldap;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

public abstract class AbstractLdapEntry implements LdapEntry {
	private final AbstractLdapDirectory directory;

	private final LdapName dn;

	private Attributes publishedAttributes;

	protected AbstractLdapEntry(AbstractLdapDirectory userAdmin, LdapName dn, Attributes attributes) {
		this.directory = userAdmin;
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

	protected AbstractLdapDirectory getDirectory() {
		return directory;
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
