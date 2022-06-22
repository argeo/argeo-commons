package org.argeo.osgi.useradmin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;

import org.argeo.util.directory.HierarchyUnit;
import org.argeo.util.directory.ldap.LdapEntry;
import org.argeo.util.directory.ldap.LdapEntryWorkingCopy;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.User;

/** Pseudo user directory to be used when logging in as OS user. */
public class OsUserDirectory extends AbstractUserDirectory {
	private final String osUsername = System.getProperty("user.name");
	private final LdapName osUserDn;
	private final DirectoryUser osUser;

	public OsUserDirectory(URI uriArg, Dictionary<String, ?> props) {
		super(uriArg, props, false);
		try {
			osUserDn = new LdapName(
					LdapAttrs.uid.name() + "=" + osUsername + "," + getUserBaseRdn() + "," + getBaseDn());
			Attributes attributes = new BasicAttributes();
			attributes.put(LdapAttrs.uid.name(), osUsername);
			osUser = newUser(osUserDn, attributes);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot create system user", e);
		}
	}

	@Override
	protected List<LdapName> getDirectGroups(LdapName dn) {
		return new ArrayList<>();
	}

	@Override
	protected Boolean daoHasEntry(LdapName dn) {
		return osUserDn.equals(dn);
	}

	@Override
	protected DirectoryUser daoGetEntry(LdapName key) throws NameNotFoundException {
		if (osUserDn.equals(key))
			return osUser;
		else
			throw new NameNotFoundException("Not an OS role");
	}

	@Override
	protected List<LdapEntry> doGetEntries(LdapName searchBase, Filter f, boolean deep) {
		List<LdapEntry> res = new ArrayList<>();
		if (f == null || f.match(osUser.getProperties()))
			res.add(osUser);
		return res;
	}

	@Override
	protected AbstractUserDirectory scope(User user) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HierarchyUnit doGetHierarchyUnit(LdapName dn) {
		return null;
	}

	@Override
	public Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly) {
		return new ArrayList<>();
	}

	public void prepare(LdapEntryWorkingCopy wc) {

	}

	public void commit(LdapEntryWorkingCopy wc) {

	}

	public void rollback(LdapEntryWorkingCopy wc) {

	}

}
