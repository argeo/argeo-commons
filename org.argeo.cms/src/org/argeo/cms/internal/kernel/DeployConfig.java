package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.util.naming.AttributesDictionary;
import org.argeo.util.naming.LdifParser;
import org.argeo.util.naming.LdifWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

class DeployConfig implements ConfigurationListener {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private Path deployConfigPath = KernelUtils.getOsgiInstancePath(KernelConstants.DEPLOY_CONFIG_PATH);
	private SortedMap<LdapName, Attributes> deployConfigs = new TreeMap<>();

	public DeployConfig(boolean isClean) {
		ConfigurationAdmin configurationAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));
		try {
			if (!Files.exists(deployConfigPath)) { // first init
				firstInit();
			}
			init(configurationAdmin, isClean);
		} catch (IOException e) {
			throw new CmsException("Could not init deploy configs", e);
		}
		// FIXME check race conditions during initialization
		// bc.registerService(ConfigurationListener.class, this, null);
	}

	private void firstInit() throws IOException {
		Files.createDirectories(deployConfigPath.getParent());

		FirstInitProperties firstInit = new FirstInitProperties();
		firstInit.prepareInstanceArea();

		if (!Files.exists(deployConfigPath))// could have juste been copied
			Files.createFile(deployConfigPath);

		try (InputStream in = Files.newInputStream(deployConfigPath)) {
			deployConfigs = new LdifParser().read(in);
		}

		// node repository
		Dictionary<String, Object> nodeConfig = firstInit
				.getNodeRepositoryConfig(getProps(NodeConstants.NODE_REPOS_FACTORY_PID, ArgeoJcrConstants.ALIAS_NODE));
		// node repository is mandatory
		putFactoryDeployConfig(NodeConstants.NODE_REPOS_FACTORY_PID, nodeConfig);

		// user admin

		List<Dictionary<String, Object>> userDirectoryConfigs = firstInit.getUserDirectoryConfigs();
		for (int i = 0; i < userDirectoryConfigs.size(); i++) {
			Dictionary<String, Object> userDirectoryConfig = userDirectoryConfigs.get(i);
			String cn = Integer.toString(i);
			userDirectoryConfig.put(NodeConstants.CN, cn);
			putFactoryDeployConfig(NodeConstants.NODE_USER_ADMIN_PID, userDirectoryConfig);
		}

		// http server
		Dictionary<String, Object> webServerConfig = firstInit
				.getHttpServerConfig(getProps(KernelConstants.JETTY_FACTORY_PID, NodeConstants.DEFAULT));
		if (!webServerConfig.isEmpty())
			putFactoryDeployConfig(KernelConstants.JETTY_FACTORY_PID, webServerConfig);

		save();
	}

	private void init(ConfigurationAdmin configurationAdmin, boolean isClean) throws IOException {

		try (InputStream in = Files.newInputStream(deployConfigPath)) {
			deployConfigs = new LdifParser().read(in);
		}
		if (isClean) {
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
						save();
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
						save();
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

	void putFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props) {
		Object cn = props.get(NodeConstants.CN);
		if (cn == null)
			throw new IllegalArgumentException("cn must be set in properties");
		LdapName serviceFactoryDn = serviceFactoryDn(factoryPid);
		if (!deployConfigs.containsKey(serviceFactoryDn))
			deployConfigs.put(serviceFactoryDn, new BasicAttributes(NodeConstants.OU, factoryPid));
		LdapName serviceDn = serviceDn(factoryPid, cn.toString());
		Attributes attrs = new BasicAttributes();
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	void putDeployConfig(String servicePid, Dictionary<String, Object> props) {
		LdapName serviceDn = serviceDn(servicePid);
		Attributes attrs = new BasicAttributes(NodeConstants.CN, servicePid);
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	void save() {
		try (Writer writer = Files.newBufferedWriter(deployConfigPath)) {
			new LdifWriter(writer).write(deployConfigs);
		} catch (IOException e) {
			throw new CmsException("Cannot save deploy configs", e);
		}
	}

	/*
	 * UTILITIES
	 */
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

	Dictionary<String, Object> getProps(String factoryPid, String cn) {
		Attributes attrs = deployConfigs.get(serviceDn(factoryPid, cn));
		if (attrs != null)
			return new AttributesDictionary(attrs);
		else
			return null;
	}

}