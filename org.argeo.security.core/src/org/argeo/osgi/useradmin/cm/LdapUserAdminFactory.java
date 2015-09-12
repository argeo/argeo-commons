package org.argeo.osgi.useradmin.cm;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.UserDirectoryException;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.UserAdminAggregator;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class LdapUserAdminFactory implements ManagedServiceFactory {
	private final UserAdminAggregator userAdminAggregator;

	private Map<String, String> index = new HashMap<String, String>();

	public LdapUserAdminFactory(UserAdminAggregator userAdminAggregator) {
		this.userAdminAggregator = userAdminAggregator;
	}

	@Override
	public String getName() {
		return "LDAP/LDIF User Source";
	}

	@Override
	public synchronized void updated(String pid,
			Dictionary<String, ?> properties) throws ConfigurationException {
		String baseDn = properties.get("baseDn").toString();
		String userAdminUri = properties.get("uri").toString();
		AbstractUserDirectory userAdmin;
		if (userAdminUri.startsWith("ldap"))
			userAdmin = new LdapUserAdmin(userAdminUri);
		else
			userAdmin = new LdifUserAdmin(userAdminUri);
		userAdminAggregator.addUserAdmin(baseDn, userAdmin);
		index.put(pid, baseDn);
	}

	@Override
	public synchronized void deleted(String pid) {
		if (index.containsKey(pid))
			userAdminAggregator.removeUserAdmin(index.get(pid));
		else
			throw new UserDirectoryException("No user admin registered for "
					+ pid);
		index.remove(pid);
	}

}
