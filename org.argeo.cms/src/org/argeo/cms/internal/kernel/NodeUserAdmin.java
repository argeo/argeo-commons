package org.argeo.cms.internal.kernel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.ldap.LdapName;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
class NodeUserAdmin extends AggregatingUserAdmin implements ManagedServiceFactory, KernelConstants {
	private final static Log log = LogFactory.getLog(NodeUserAdmin.class);
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	// OSGi
	private Map<String, LdapName> pidToBaseDn = new HashMap<>();
	private Map<String, ServiceRegistration<UserDirectory>> pidToServiceRegs = new HashMap<>();
	private ServiceRegistration<UserAdmin> userAdminReg;

	// JTA
	private final ServiceTracker<TransactionManager, TransactionManager> tmTracker;
	private final String cacheName = UserDirectory.class.getName();

	public NodeUserAdmin(String systemRolesBaseDn) {
		super(systemRolesBaseDn);
		tmTracker = new ServiceTracker<>(bc, TransactionManager.class, null);
		tmTracker.open();
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		String uri = (String) properties.get(UserAdminConf.uri.name());
		URI u;
		try {
			u = new URI(uri);
		} catch (URISyntaxException e) {
			throw new CmsException("Badly formatted URI " + uri, e);
		}

		// Create
		AbstractUserDirectory userDirectory = u.getScheme().equals("ldap") ? new LdapUserAdmin(properties)
				: new LdifUserAdmin(properties);
		addUserDirectory(userDirectory);

		// OSGi
		LdapName baseDn = userDirectory.getBaseDn();
		Dictionary<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		if(isSystemRolesBaseDn(baseDn))
			regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		regProps.put(UserAdminConf.baseDn.name(), baseDn);
		ServiceRegistration<UserDirectory> reg = bc.registerService(UserDirectory.class, userDirectory, regProps);
		pidToBaseDn.put(pid, baseDn);
		pidToServiceRegs.put(pid, reg);

		if (log.isDebugEnabled())
			log.debug("User directory " + userDirectory.getBaseDn() + " [" + u.getScheme() + "] enabled.");

		if (!isSystemRolesBaseDn(baseDn)) {
			if (userAdminReg != null)
				userAdminReg.unregister();
			// register self as main user admin
			Dictionary<String, Object> userAdminregProps = currentState();
			userAdminregProps.put(NodeConstants.CN, NodeConstants.DEFAULT);
			userAdminregProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			userAdminReg = bc.registerService(UserAdmin.class, this, userAdminregProps);
		}
	}

	@Override
	public void deleted(String pid) {
		assert pidToServiceRegs.get(pid) != null;
		assert pidToBaseDn.get(pid) != null;
		pidToServiceRegs.remove(pid).unregister();
		LdapName baseDn = pidToBaseDn.remove(pid);
		removeUserDirectory(baseDn);
	}

	@Override
	public String getName() {
		return "Node User Admin";
	}

	protected void postAdd(AbstractUserDirectory userDirectory) {
		// JTA
		TransactionManager tm = tmTracker.getService();
		if (tm == null)
			throw new CmsException("A JTA transaction manager must be available.");
		userDirectory.setTransactionManager(tm);
		if (tmTracker.getService() instanceof BitronixTransactionManager)
			EhCacheXAResourceProducer.registerXAResource(cacheName, userDirectory.getXaResource());
	}

	protected void preDestroy(UserDirectory userDirectory) {
		if (tmTracker.getService() instanceof BitronixTransactionManager)
			EhCacheXAResourceProducer.unregisterXAResource(cacheName, userDirectory.getXaResource());
	}

}
