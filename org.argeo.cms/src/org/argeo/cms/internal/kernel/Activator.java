package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.ArgeoLogger;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeState;
import org.argeo.node.RepoConf;
import org.argeo.util.LangUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.log.LogReaderService;

/**
 * Activates the {@link Kernel} from the provided {@link BundleContext}. Gives
 * access to kernel information for the rest of the bundle (and only it)
 */
public class Activator implements BundleActivator {
	// public final static String SYSTEM_KEY_PROPERTY =
	// "argeo.security.systemKey";
	private final Log log = LogFactory.getLog(Activator.class);

	// private final static String systemKey;
	// static {
	// System.setProperty(SYSTEM_KEY_PROPERTY, systemKey);
	// }

	// private static Kernel kernel;
	private static Activator instance;

	private BundleContext bc;
	private ConditionalPermissionAdmin permissionAdmin;
	private LogReaderService logReaderService;
	private ConfigurationAdmin configurationAdmin;

	private NodeLogger logger;
	private CmsState nodeState;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		// try {
		// kernel = new Kernel();
		// kernel.init();
		// } catch (Exception e) {
		// log.error("Cannot boot kernel", e);
		// }

		instance = this;
		this.bc = bundleContext;
		this.permissionAdmin = getService(ConditionalPermissionAdmin.class);
		this.logReaderService = getService(LogReaderService.class);
		this.configurationAdmin = getService(ConfigurationAdmin.class);

		initSecurity();// must be first
		initArgeoLogger();
		initNodeState();
	}

	private void initSecurity() {
		URL url = getClass().getClassLoader().getResource(KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config", url.toExternalForm());
	}

	private void initArgeoLogger() {
		logger = new NodeLogger(logReaderService);

		// register
		bc.registerService(ArgeoLogger.class, logger, null);
	}

	private void initNodeState() throws IOException {
		nodeState = new CmsState();

		Object cn;
		Configuration nodeConf = configurationAdmin.getConfiguration(NodeConstants.NODE_STATE_PID);
		Dictionary<String, Object> props = nodeConf.getProperties();
		if (props == null) {
			if (log.isDebugEnabled())
				log.debug("Clean node state");
			Dictionary<String, Object> envProps = getStatePropertiesFromEnvironment();
			// Use the UUID of the first framework run as state UUID
			cn = bc.getProperty(Constants.FRAMEWORK_UUID);
			envProps.put(NodeConstants.CN, cn);
			nodeConf.update(envProps);
		} else {
			// Check if state is in line with environment
			Dictionary<String, Object> envProps = getStatePropertiesFromEnvironment();
			for (String key : LangUtils.keys(envProps)) {
				Object envValue = envProps.get(key);
				Object storedValue = props.get(key);
				if (storedValue == null)
					throw new CmsException("No state value for env " + key + "=" + envValue
							+ ", please clean the OSGi configuration.");
				if (!storedValue.equals(envValue))
					throw new CmsException("State value for " + key + "=" + storedValue
							+ " is different from env value =" + envValue + ", please clean the OSGi configuration.");
			}
			cn = props.get(NodeConstants.CN);
			if (cn == null)
				throw new CmsException("No state UUID available");
		}

		Dictionary<String, Object> regProps = LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_STATE_PID);
		regProps.put(NodeConstants.CN, cn);
		bc.registerService(LangUtils.names(NodeState.class, ManagedService.class), nodeState, regProps);

	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		nodeState.shutdown();

		instance = null;
		this.bc = null;
		this.permissionAdmin = null;
		this.logReaderService = null;
		this.configurationAdmin = null;

		// if (kernel != null) {
		// kernel.destroy();
		// kernel = null;
		// }

	}

	private <T> T getService(Class<T> clazz) {
		ServiceReference<T> sr = bc.getServiceReference(clazz);
		if (sr == null)
			throw new CmsException("No service available for " + clazz);
		return bc.getService(sr);
	}

	protected Dictionary<String, Object> getStatePropertiesFromEnvironment() {
		Hashtable<String, Object> props = new Hashtable<>();
		// i18n
		copyFrameworkProp(NodeConstants.I18N_DEFAULT_LOCALE, props);
		copyFrameworkProp(NodeConstants.I18N_LOCALES, props);
		// user admin
		copyFrameworkProp(NodeConstants.ROLES_URI, props);
		copyFrameworkProp(NodeConstants.USERADMIN_URIS, props);
		// data
		for (RepoConf repoConf : RepoConf.values())
			copyFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name(), props);
		// TODO add other environment sources
		return props;
	}

	private void copyFrameworkProp(String key, Dictionary<String, Object> props) {
		String value = bc.getProperty(key);
		if (value != null)
			props.put(key, value);
	}

	public static NodeState getNodeState() {
		return instance.nodeState;
	}

	public String[] getLocales() {
		// TODO optimize?
		List<Locale> locales = getNodeState().getLocales();
		String[] res = new String[locales.size()];
		for (int i = 0; i < locales.size(); i++)
			res[i] = locales.get(i).toString();
		return res;
	}

}
