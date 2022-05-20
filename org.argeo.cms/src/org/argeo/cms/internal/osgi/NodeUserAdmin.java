package org.argeo.cms.internal.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.runtime.CmsUserAdmin;
import org.argeo.cms.internal.runtime.KernelConstants;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
public class NodeUserAdmin extends CmsUserAdmin implements ManagedServiceFactory, KernelConstants {
	private final static CmsLog log = CmsLog.getLog(NodeUserAdmin.class);

	// OSGi
	private Map<String, LdapName> pidToBaseDn = new HashMap<>();

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {

		LdapName baseDn;
		try {
			baseDn = new LdapName((String) properties.get(UserAdminConf.baseDn.name()));
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException(e);
		}

		// FIXME make updates more robust
		if (pidToBaseDn.containsValue(baseDn)) {
			if (log.isDebugEnabled())
				log.debug("Ignoring user directory update of " + baseDn);
			return;
		}

		UserDirectory userDirectory = enableUserDirectory(properties);
		// OSGi
		Hashtable<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		if (isSystemRolesBaseDn(baseDn))
			regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		regProps.put(UserAdminConf.baseDn.name(), baseDn);

		CmsActivator.getBundleContext().registerService(UserDirectory.class, userDirectory, regProps);
		pidToBaseDn.put(pid, baseDn);

		if (isSystemRolesBaseDn(baseDn)) {
			// publishes itself as user admin only when system roles are available
			Dictionary<String, Object> userAdminregProps = new Hashtable<>();
			userAdminregProps.put(CmsConstants.CN, CmsConstants.DEFAULT);
			userAdminregProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			CmsActivator.getBundleContext().registerService(UserAdmin.class, this, userAdminregProps);
		}
	}

	@Override
	public void deleted(String pid) {
		// assert pidToServiceRegs.get(pid) != null;
		assert pidToBaseDn.get(pid) != null;
		// pidToServiceRegs.remove(pid).unregister();
		LdapName baseDn = pidToBaseDn.remove(pid);
		removeUserDirectory(baseDn);
	}

	@Override
	public String getName() {
		return "Node User Admin";
	}

}
