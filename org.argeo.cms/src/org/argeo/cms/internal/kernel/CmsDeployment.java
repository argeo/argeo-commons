package org.argeo.cms.internal.kernel;

import static org.argeo.node.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.cms.CmsException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.DataModelNamespace;
import org.argeo.node.NodeDeployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class CmsDeployment implements NodeDeployment, ManagedService {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private Repository deployedNodeRepository;
	private HomeRepository homeRepository;

	private Long availableSince;

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties == null)
			return;

		if (deployedNodeRepository != null) {
			if (availableSince != null) {
				throw new CmsException("Deployment is already available");
			}

			availableSince = System.currentTimeMillis();

			prepareDataModel(KernelUtils.openAdminSession(deployedNodeRepository));
			Hashtable<String, String> regProps = new Hashtable<String, String>();
			regProps.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, ArgeoJcrConstants.ALIAS_HOME);
			homeRepository = new HomeRepository(deployedNodeRepository);
			// register
			bc.registerService(Repository.class, homeRepository, regProps);

		} else {
			throw new CmsException("No node repository available");
		}
	}

	/** Session is logged out. */
	private void prepareDataModel(Session adminSession) {
		try {
			Set<String> processed = new HashSet<String>();
			bundles: for (Bundle bundle : bc.getBundles()) {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				if (wiring == null) {
					if (log.isTraceEnabled())
						log.error("No wiring for " + bundle.getSymbolicName());
					continue bundles;
				}
				processWiring(adminSession, wiring, processed);
			}
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void processWiring(Session adminSession, BundleWiring wiring, Set<String> processed) {
		// recursively process requirements first
		List<BundleWire> requiredWires = wiring.getRequiredWires(CMS_DATA_MODEL_NAMESPACE);
		for (BundleWire wire : requiredWires) {
			processWiring(adminSession, wire.getProviderWiring(), processed);
			// registerCnd(adminSession, wire.getCapability(), processed);
		}
		List<BundleCapability> capabilities = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
		for (BundleCapability capability : capabilities) {
			registerCnd(adminSession, capability, processed);
		}
	}

	private void registerCnd(Session adminSession, BundleCapability capability, Set<String> processed) {
		Map<String, Object> attrs = capability.getAttributes();
		String name = attrs.get(DataModelNamespace.CAPABILITY_NAME_ATTRIBUTE).toString();
		if (processed.contains(name)) {
			if (log.isTraceEnabled())
				log.trace("Data model " + name + " has already been processed");
			return;
		}
		String path = attrs.get(DataModelNamespace.CAPABILITY_CND_ATTRIBUTE).toString();
		URL url = capability.getRevision().getBundle().getResource(path);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			CndImporter.registerNodeTypes(reader, adminSession, true);
			processed.add(name);
			if (log.isDebugEnabled())
				log.debug("Registered CND " + url);
		} catch (Exception e) {
			throw new CmsException("Cannot import CND " + url, e);
		}

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, name);
		bc.registerService(Repository.class, adminSession.getRepository(), properties);
		if (log.isDebugEnabled())
			log.debug("Published data model " + name);
	}

	public void setDeployedNodeRepository(Repository deployedNodeRepository) {
		this.deployedNodeRepository = deployedNodeRepository;
	}

	@Override
	public long getAvailableSince() {
		return availableSince;
	}

}
