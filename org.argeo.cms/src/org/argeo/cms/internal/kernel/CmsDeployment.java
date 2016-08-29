package org.argeo.cms.internal.kernel;

import static org.argeo.node.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.cms.CmsException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.DataModelNamespace;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeState;
import org.argeo.util.naming.AttributesDictionary;
import org.argeo.util.naming.LdifParser;
import org.argeo.util.naming.LdifWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CmsDeployment implements NodeDeployment, SynchronousConfigurationListener {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private Path deployPath = KernelUtils.getOsgiInstancePath(KernelConstants.DEPLOY_PATH);
	private SortedMap<LdapName, Attributes> deployConfigs = new TreeMap<>();

	// private Repository deployedNodeRepository;
	private HomeRepository homeRepository;

	private Long availableSince;

	public CmsDeployment() {
		ConfigurationAdmin configurationAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));
		// FIXME no guarantee this is already available
		NodeState nodeState = bc.getService(bc.getServiceReference(NodeState.class));
		try {
			initDeployConfigs(configurationAdmin, nodeState);
		} catch (IOException e) {
			throw new CmsException("Could not init deploy configs", e);
		}
		bc.registerService(SynchronousConfigurationListener.class, this, null);

		new ServiceTracker<>(bc, RepositoryContext.class, new RepositoryContextStc()).open();
	}

	private void initDeployConfigs(ConfigurationAdmin configurationAdmin, NodeState nodeState) throws IOException {
		if (!Files.exists(deployPath)) {// first init
			Files.createDirectories(deployPath.getParent());
			Files.createFile(deployPath);
			FirstInitProperties firstInitProperties = new FirstInitProperties();

			Dictionary<String, Object> nodeConfig = firstInitProperties.getNodeRepositoryConfig();
			// node repository is mandatory
			putFactoryDeployConfig(NodeConstants.NODE_REPOS_FACTORY_PID, nodeConfig);

			Dictionary<String, Object> webServerConfig = firstInitProperties.getHttpServerConfig();
			if (!webServerConfig.isEmpty())
				putFactoryDeployConfig(KernelConstants.JETTY_FACTORY_PID, webServerConfig);

			saveDeployedConfigs();
		}

		try (InputStream in = Files.newInputStream(deployPath)) {
			deployConfigs = new LdifParser().read(in);
		}
		if (nodeState.isClean()) {
			for (LdapName dn : deployConfigs.keySet()) {
				Rdn lastRdn = dn.getRdn(dn.size() - 1);
				LdapName prefix = (LdapName) dn.getPrefix(dn.size() - 1);
				if (prefix.toString().equals(NodeConstants.DEPLOY_BASEDN)) {
					if (lastRdn.getType().equals(NodeConstants.CN)) {
						// service
						String pid = lastRdn.getValue().toString();
						Configuration conf = configurationAdmin.getConfiguration(pid);
						AttributesDictionary dico = new AttributesDictionary(deployConfigs.get(dn));
						conf.update(dico);
					} else {
						// service factory definition
					}
				} else {
					// service factory service
					Rdn beforeLastRdn = dn.getRdn(dn.size() - 2);
					assert beforeLastRdn.getType().equals(NodeConstants.OU);
					String factoryPid = beforeLastRdn.getValue().toString();
					Configuration conf = configurationAdmin.createFactoryConfiguration(factoryPid.toString(), null);
					AttributesDictionary dico = new AttributesDictionary(deployConfigs.get(dn));
					conf.update(dico);
				}
			}
		}
		// TODO check consistency if not clean
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		try {
			if (ConfigurationEvent.CM_UPDATED == event.getType()) {
				ConfigurationAdmin configurationAdmin = bc.getService(event.getReference());
				Configuration conf = configurationAdmin.getConfiguration(event.getPid(), null);
				LdapName serviceDn = null;
				String factoryPid = conf.getFactoryPid();
				if (factoryPid != null) {
					LdapName serviceFactoryDn = serviceFactoryDn(factoryPid);
					if (deployConfigs.containsKey(serviceFactoryDn)) {
						for (LdapName dn : deployConfigs.keySet()) {
							if (dn.startsWith(serviceFactoryDn)) {
								Rdn lastRdn = dn.getRdn(dn.size() - 1);
								assert lastRdn.getType().equals(NodeConstants.CN);
								Object value = conf.getProperties().get(lastRdn.getType());
								assert value != null;
								if (value.equals(lastRdn.getValue())) {
									serviceDn = dn;
									break;
								}
							}
						}

						Object cn = conf.getProperties().get(NodeConstants.CN);
						if (cn == null)
							throw new IllegalArgumentException("Properties must contain cn");
						if (serviceDn == null) {
							putFactoryDeployConfig(factoryPid, conf.getProperties());
						} else {
							Attributes attrs = deployConfigs.get(serviceDn);
							assert attrs != null;
							AttributesDictionary.copy(conf.getProperties(), attrs);
						}
						saveDeployedConfigs();
						if (log.isDebugEnabled())
							log.debug("Updated deploy config " + serviceDn(factoryPid, cn.toString()));
					} else {
						// ignore non config-registered service factories
					}
				} else {
					serviceDn = serviceDn(event.getPid());
					if (deployConfigs.containsKey(serviceDn)) {
						Attributes attrs = deployConfigs.get(serviceDn);
						assert attrs != null;
						AttributesDictionary.copy(conf.getProperties(), attrs);
						saveDeployedConfigs();
						if (log.isDebugEnabled())
							log.debug("Updated deploy config " + serviceDn);
					} else {
						// ignore non config-registered services
					}
				}
			}
		} catch (Exception e) {
			log.error("Could not handle configuration event", e);
		}
	}

	private void putFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props) {
		Object cn = props.get(NodeConstants.CN);
		if (cn == null)
			throw new IllegalArgumentException("cn must be set in properties");
		LdapName serviceFactorydn = serviceFactoryDn(factoryPid);
		if (!deployConfigs.containsKey(serviceFactorydn))
			deployConfigs.put(serviceFactorydn, new BasicAttributes(NodeConstants.OU, factoryPid));
		LdapName serviceDn = serviceDn(factoryPid, cn.toString());
		Attributes attrs = new BasicAttributes();
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	private void putDeployConfig(String servicePid, Dictionary<String, Object> props) {
		LdapName serviceDn = serviceDn(servicePid);
		Attributes attrs = new BasicAttributes(NodeConstants.CN, servicePid);
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	void saveDeployedConfigs() throws IOException {
		try (Writer writer = Files.newBufferedWriter(deployPath)) {
			new LdifWriter(writer).write(deployConfigs);
		}
	}

	private LdapName serviceFactoryDn(String factoryPid) {
		try {
			return new LdapName(NodeConstants.OU + "=" + factoryPid + "," + NodeConstants.DEPLOY_BASEDN);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + factoryPid, e);
		}
	}

	private LdapName serviceDn(String servicePid) {
		try {
			return new LdapName(NodeConstants.CN + "=" + servicePid + "," + NodeConstants.DEPLOY_BASEDN);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + servicePid, e);
		}
	}

	private LdapName serviceDn(String factoryPid, String cn) {
		try {
			return (LdapName) serviceFactoryDn(factoryPid).add(new Rdn(NodeConstants.CN, cn));
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + factoryPid + " and " + cn, e);
		}
	}

	private void prepareNodeRepository(Repository deployedNodeRepository) {
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

	// public void setDeployedNodeRepository(Repository deployedNodeRepository)
	// {
	// this.deployedNodeRepository = deployedNodeRepository;
	// }

	@Override
	public long getAvailableSince() {
		return availableSince;
	}

	private class RepositoryContextStc implements ServiceTrackerCustomizer<RepositoryContext, RepositoryContext> {

		@Override
		public RepositoryContext addingService(ServiceReference<RepositoryContext> reference) {
			RepositoryContext nodeRepo = bc.getService(reference);
			Object cn = reference.getProperty(NodeConstants.CN);
			if (cn != null && cn.equals(ArgeoJcrConstants.ALIAS_NODE)) {
				prepareNodeRepository(nodeRepo.getRepository());
				// nodeDeployment.setDeployedNodeRepository(nodeRepo.getRepository());
				// Dictionary<String, Object> props =
				// LangUtils.init(Constants.SERVICE_PID,
				// NodeConstants.NODE_DEPLOYMENT_PID);
				// props.put(NodeConstants.CN,
				// nodeRepo.getRootNodeId().toString());
				// register
				// bc.registerService(LangUtils.names(NodeDeployment.class,
				// ManagedService.class), nodeDeployment, props);
			}

			return nodeRepo;
		}

		@Override
		public void modifiedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

		@Override
		public void removedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

	}

}
