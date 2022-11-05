package org.argeo.cms.directory.ldap;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.transaction.AbstractWorkingCopy;

/** Working copy for a user directory being edited. */
public class LdapEntryWorkingCopy extends AbstractWorkingCopy<LdapEntry, Attributes, LdapName> {
	@Override
	protected LdapName getId(LdapEntry entry) {
		return entry.getDn();
	}

	@Override
	protected Attributes cloneAttributes(LdapEntry entry) {
		return (Attributes) entry.getAttributes().clone();
	}
}
