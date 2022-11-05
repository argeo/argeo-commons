package org.argeo.cms.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.api.acr.ldap.LdapAttrs;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.cms.directory.ldap.AbstractLdapDirectory;
import org.argeo.cms.directory.ldap.AbstractLdapDirectoryDao;
import org.argeo.cms.directory.ldap.LdapEntry;
import org.argeo.cms.directory.ldap.LdapEntryWorkingCopy;

/** Pseudo user directory to be used when logging in as OS user. */
public class OsUserDirectory extends AbstractLdapDirectoryDao {
	private final String osUsername = System.getProperty("user.name");
	private final LdapName osUserDn;
	private final LdapEntry osUser;

	public OsUserDirectory(AbstractLdapDirectory directory) {
		super(directory);
		try {
			osUserDn = new LdapName(LdapAttrs.uid.name() + "=" + osUsername + "," + directory.getUserBaseRdn() + ","
					+ directory.getBaseDn());
//			Attributes attributes = new BasicAttributes();
//			attributes.put(LdapAttrs.uid.name(), osUsername);
			osUser = newUser(osUserDn);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot create system user", e);
		}
	}

	@Override
	public List<LdapName> getDirectGroups(LdapName dn) {
		return new ArrayList<>();
	}

	@Override
	public boolean entryExists(LdapName dn) {
		return osUserDn.equals(dn);
	}

	@Override
	public boolean checkConnection() {
		return true;
	}

	@Override
	public LdapEntry doGetEntry(LdapName key) throws NameNotFoundException {
		if (osUserDn.equals(key))
			return osUser;
		else
			throw new NameNotFoundException("Not an OS role");
	}

	@Override
	public List<LdapEntry> doGetEntries(LdapName searchBase, String f, boolean deep) {
		List<LdapEntry> res = new ArrayList<>();
//		if (f == null || f.match(osUser.getProperties()))
		res.add(osUser);
		return res;
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

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public Attributes doGetAttributes(LdapName name) {
		try {
			return doGetEntry(name).getAttributes();
		} catch (NameNotFoundException e) {
			throw new IllegalStateException(name + " doe not exist in " + getDirectory().getBaseDn(), e);
		}
	}

}
