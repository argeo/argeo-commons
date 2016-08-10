package org.argeo.osgi.useradmin.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class UserDirectoryFactory implements ManagedServiceFactory {
	private final BundleContext bc = FrameworkUtil.getBundle(UserDirectoryFactory.class).getBundleContext();

	private Map<String, UserDirectory> userDirectories = new HashMap<>();

	@Override
	public String getName() {
		return "User Directories Factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		String uri = (String) properties.get(UserAdminConf.uri.name());
		UserDirectory userDirectory = uri.startsWith("ldap:") ? new LdapUserAdmin(properties)
				: new LdifUserAdmin(properties);
		Dictionary<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		regProps.put(UserAdminConf.uri.name(), uri);
		bc.registerService(UserDirectory.class, userDirectory, regProps);
		userDirectories.put(pid, userDirectory);
	}

	@Override
	public void deleted(String pid) {
		userDirectories.remove(pid);
	}

}
