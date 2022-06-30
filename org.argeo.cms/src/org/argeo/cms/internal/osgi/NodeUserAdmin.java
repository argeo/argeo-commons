package org.argeo.cms.internal.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.runtime.CmsUserAdmin;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.directory.DirectoryConf;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
@Deprecated
public class NodeUserAdmin extends CmsUserAdmin implements ManagedServiceFactory {
	private final static CmsLog log = CmsLog.getLog(NodeUserAdmin.class);

	// OSGi
	private Map<String, String> pidToBaseDn = new HashMap<>();

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {

		String basePath = (String) properties.get(DirectoryConf.baseDn.name());

		// FIXME make updates more robust
		if (pidToBaseDn.containsValue(basePath)) {
			if (log.isDebugEnabled())
				log.debug("Ignoring user directory update of " + basePath);
			return;
		}

		UserDirectory userDirectory = enableUserDirectory(properties);
		// OSGi
		Hashtable<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		if (isSystemRolesBaseDn(basePath))
			regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		regProps.put(DirectoryConf.baseDn.name(), basePath);

		CmsActivator.getBundleContext().registerService(UserDirectory.class, userDirectory, regProps);
		pidToBaseDn.put(pid, basePath);

		if (isSystemRolesBaseDn(basePath)) {
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
		String basePath = pidToBaseDn.remove(pid);
		removeUserDirectory(basePath);
	}

	@Override
	public String getName() {
		return "Node User Admin";
	}

}
