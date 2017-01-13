package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.ArgeoLogger;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeInstance;
import org.argeo.node.NodeState;
import org.argeo.util.LangUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;

/**
 * Activates the {@link Kernel} from the provided {@link BundleContext}. Gives
 * access to kernel information for the rest of the bundle (and only it)
 */
public class Activator implements BundleActivator {
	private final static Log log = LogFactory.getLog(Activator.class);

	private static Activator instance;

	private BundleContext bc;
	private LogReaderService logReaderService;
	// private ConfigurationAdmin configurationAdmin;

	private NodeLogger logger;
	private CmsState nodeState;
	private CmsDeployment nodeDeployment;
	private CmsInstance nodeInstance;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		instance = this;
		this.bc = bundleContext;
		this.logReaderService = getService(LogReaderService.class);
		// this.configurationAdmin = getService(ConfigurationAdmin.class);

		try {
			initSecurity();// must be first
			initArgeoLogger();
			initNode();
		} catch (Exception e) {
			log.error("## FATAL: CMS activator failed", e);
			// throw new CmsException("Cannot initialize node", e);
		}
	}

	private void initSecurity() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			URL url = getClass().getClassLoader().getResource(KernelConstants.JAAS_CONFIG);
			// URL url =
			// getClass().getClassLoader().getResource(KernelConstants.JAAS_CONFIG_IPA);
			System.setProperty(KernelConstants.JAAS_CONFIG_PROP, url.toExternalForm());
		}
		Configuration.getConfiguration();
	}

	private void initArgeoLogger() {
		logger = new NodeLogger(logReaderService);
		bc.registerService(ArgeoLogger.class, logger, null);
	}

	private void initNode() throws IOException {
		// Node state
		Path stateUuidPath = bc.getDataFile("stateUuid").toPath();
		String stateUuid;
		if (Files.exists(stateUuidPath)) {
			stateUuid = Files.readAllLines(stateUuidPath).get(0);
		} else {
			stateUuid = bc.getProperty(Constants.FRAMEWORK_UUID);
			Files.write(stateUuidPath, stateUuid.getBytes());
		}
		nodeState = new CmsState(stateUuid);
		Dictionary<String, Object> regProps = LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_STATE_PID);
		regProps.put(NodeConstants.CN, stateUuid);
		bc.registerService(NodeState.class, nodeState, regProps);

		// Node deployment
		nodeDeployment = new CmsDeployment();
		bc.registerService(NodeDeployment.class, nodeDeployment, null);

		// Node instance
		nodeInstance = new CmsInstance();
		bc.registerService(NodeInstance.class, nodeInstance, null);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		nodeInstance.shutdown();
		nodeDeployment.shutdown();
		nodeState.shutdown();

		instance = null;
		this.bc = null;
		this.logReaderService = null;
		// this.configurationAdmin = null;
	}

	private <T> T getService(Class<T> clazz) {
		ServiceReference<T> sr = bc.getServiceReference(clazz);
		if (sr == null)
			throw new CmsException("No service available for " + clazz);
		return bc.getService(sr);
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
