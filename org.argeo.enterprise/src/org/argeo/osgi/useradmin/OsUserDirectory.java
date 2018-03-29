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

import org.argeo.naming.LdapAttrs;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.User;

public class OsUserDirectory extends AbstractUserDirectory {
	private final String osUsername = System.getProperty("user.name");
	private final LdapName osUserDn;
	private final LdifUser osUser;

	public OsUserDirectory(URI uriArg, Dictionary<String, ?> props) {
		super(uriArg, props);
		try {
			osUserDn = new LdapName(LdapAttrs.uid.name() + "=" + osUsername + "," + getUserBase() + "," + getBaseDn());
			Attributes attributes = new BasicAttributes();
			attributes.put(LdapAttrs.uid.name(), osUsername);
			osUser = new LdifUser(this, osUserDn, attributes);
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot create system user", e);
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
	protected List<DirectoryUser> doGetRoles(Filter f) {
		List<DirectoryUser> res = new ArrayList<>();
		if (f==null || f.match(osUser.getProperties()))
			res.add(osUser);
		return res;
	}

	@Override
	protected AbstractUserDirectory scope(User user) {
		throw new UnsupportedOperationException();
	}

}
