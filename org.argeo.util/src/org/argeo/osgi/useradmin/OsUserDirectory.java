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
	protected Boolean daoHasRole(LdapName dn) {
		return osUserDn.equals(dn);
	}

	@Override
	protected DirectoryUser daoGetRole(LdapName key) throws NameNotFoundException {
		if (osUserDn.equals(key))
			return osUser;
		else
			throw new NameNotFoundException("Not an OS role");
	}

	@Override
	protected List<DirectoryUser> doGetRoles(LdapName searchBase, Filter f, boolean deep) {
		List<DirectoryUser> res = new ArrayList<>();
		if (f == null || f.match(osUser.getProperties()))
			res.add(osUser);
		return res;
	}

	@Override
	protected AbstractUserDirectory scope(User user) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected HierarchyUnit doGetHierarchyUnit(LdapName dn) {
		return null;
	}

	@Override
	protected Iterable<HierarchyUnit> doGetDirectHierarchyUnits(LdapName searchBase, boolean functionalOnly) {
		return new ArrayList<>();
	}

	public void prepare(DirectoryUserWorkingCopy wc) {

	}

	public void commit(DirectoryUserWorkingCopy wc) {

	}

	public void rollback(DirectoryUserWorkingCopy wc) {

	}


}
