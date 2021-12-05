package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.naming.AttributesDictionary;
import org.argeo.naming.LdifParser;
import org.argeo.naming.LdifWriter;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/** Manages the LDIF-based deployment configuration. */
class DeployConfig implements ConfigurationListener {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private static Path deployConfigPath = KernelUtils.getOsgiInstancePath(KernelConstants.DEPLOY_CONFIG_PATH);
	private SortedMap<LdapName, Attributes> deployConfigs = new TreeMap<>();
//	private final DataModels dataModels;

	private boolean isFirstInit = false;

	private final static String ROLES = "roles";
	
	private ConfigurationAdmin configurationAdmin;

	public DeployConfig(ConfigurationAdmin configurationAdmin, boolean isClean) {
//		this.dataModels = dataModels;
		// ConfigurationAdmin configurationAdmin =
		// bc.getService(bc.getServiceReference(ConfigurationAdmin.class));
		try {
			if (!isInitialized()) { // first init
				isFirstInit = true;
				firstInit();
			}
			this.configurationAdmin = configurationAdmin;
			init(configurationAdmin, isClean, isFirstInit);
		} catch (IOException e) {
			throw new RuntimeException("Could not init deploy configs", e);
		}
		// FIXME check race conditions during initialization
		// bc.registerService(ConfigurationListener.class, this, null);
	}

	private void firstInit() throws IOException {
		log.info("## FIRST INIT ##");
		Files.createDirectories(deployConfigPath.getParent());

		// FirstInit firstInit = new FirstInit();
		InitUtils.prepareFirstInitInstanceArea();

		if (!Files.exists(deployConfigPath))
			deployConfigs = new TreeMap<>();
		else// config file could have juste been copied by preparation
			try (InputStream in = Files.newInputStream(deployConfigPath)) {
				deployConfigs = new LdifParser().read(in);
			}
		save();
	}

	private void setFromFrameworkProperties(boolean isFirstInit) {

		// user admin
		List<Dictionary<String, Object>> userDirectoryConfigs = InitUtils.getUserDirectoryConfigs();
		if (userDirectoryConfigs.size() != 0) {
			List<String> activeCns = new ArrayList<>();
			for (int i = 0; i < userDirectoryConfigs.size(); i++) {
				Dictionary<String, Object> userDirectoryConfig = userDirectoryConfigs.get(i);
				String baseDn = (String) userDirectoryConfig.get(UserAdminConf.baseDn.name());
				String cn;
				if (NodeConstants.ROLES_BASEDN.equals(baseDn))
					cn = ROLES;
				else
					cn = UserAdminConf.baseDnHash(userDirectoryConfig);
				activeCns.add(cn);
				userDirectoryConfig.put(NodeConstants.CN, cn);
				putFactoryDeployConfig(NodeConstants.NODE_USER_ADMIN_PID, userDirectoryConfig);
			}
			// disable others
			LdapName userAdminFactoryName = serviceFactoryDn(NodeConstants.NODE_USER_ADMIN_PID);
			for (LdapName name : deployConfigs.keySet()) {
				if (name.startsWith(userAdminFactoryName) && !name.equals(userAdminFactoryName)) {
//					try {
					Attributes attrs = deployConfigs.get(name);
					String cn = name.getRdn(name.size() - 1).getValue().toString();
					if (!activeCns.contains(cn)) {
						attrs.put(UserAdminConf.disabled.name(), "true");
					}
//					} catch (Exception e) {
//						throw new CmsException("Cannot disable user directory " + name, e);
//					}
				}
			}
		}

		// http server
//		Dictionary<String, Object> webServerConfig = InitUtils
//				.getHttpServerConfig(getProps(KernelConstants.JETTY_FACTORY_PID, NodeConstants.DEFAULT));
//		if (!webServerConfig.isEmpty()) {
//			// TODO check for other customizers
//			webServerConfig.put("customizer.class", "org.argeo.equinox.jetty.CmsJettyCustomizer");
//			putFactoryDeployConfig(KernelConstants.JETTY_FACTORY_PID, webServerConfig);
//		}
		LdapName defaultHttpServiceDn = serviceDn(KernelConstants.JETTY_FACTORY_PID, NodeConstants.DEFAULT);
		if (deployConfigs.containsKey(defaultHttpServiceDn)) {
			// remove old default configs since we have now to start Jetty servlet bridge
			// indirectly
			deployConfigs.remove(defaultHttpServiceDn);
		}

		// SAVE
		save();
		//

		// Explicitly configures Jetty so that the default server is not started by the
		// activator of the Equinox Jetty bundle.
		Dictionary<String, Object> webServerConfig = InitUtils
				.getHttpServerConfig(getProps(KernelConstants.JETTY_FACTORY_PID, NodeConstants.DEFAULT));
//		if (!webServerConfig.isEmpty()) {
//			webServerConfig.put("customizer.class", KernelConstants.CMS_JETTY_CUSTOMIZER_CLASS);
//
//			// TODO centralise with Jetty extender
//			Object webSocketEnabled = webServerConfig.get(InternalHttpConstants.WEBSOCKET_ENABLED);
//			if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
//				bc.registerService(ServerEndpointConfig.Configurator.class, new CmsWebSocketConfigurator(), null);
//				webServerConfig.put(InternalHttpConstants.WEBSOCKET_ENABLED, "true");
//			}
//		}

		int tryCount = 60;
		try {
			tryGettyJetty: while (tryCount > 0) {
				try {
					JettyConfigurator.startServer(KernelConstants.DEFAULT_JETTY_SERVER, webServerConfig);
					// Explicitly starts Jetty OSGi HTTP bundle, so that it gets triggered if OSGi
					// configuration is not cleaned
					FrameworkUtil.getBundle(JettyConfigurator.class).start();
					break tryGettyJetty;
				} catch (IllegalStateException e) {
					// Jetty may not be ready
					try {
						Thread.sleep(1000);
					} catch (Exception e1) {
						// silent
					}
					tryCount--;
				}
			}
		} catch (Exception e) {
			log.error("Cannot start default Jetty server with config " + webServerConfig, e);
		}

	}

	private void init(ConfigurationAdmin configurationAdmin, boolean isClean, boolean isFirstInit) throws IOException {

		try (InputStream in = Files.newInputStream(deployConfigPath)) {
			deployConfigs = new LdifParser().read(in);
		}
		if (isClean) {
			if (log.isDebugEnabled())
				log.debug("Clean state, loading from framework properties...");
			setFromFrameworkProperties(isFirstInit);
			loadConfigs();
		}
		// TODO check consistency if not clean
	}
	
	public void loadConfigs() throws IOException {
		// FIXME make it more robust
		Configuration systemRolesConf = null;
		LdapName systemRolesDn;
		try {
			// FIXME make it more robust
			systemRolesDn = new LdapName("cn=roles,ou=org.argeo.api.userAdmin,ou=deploy,ou=node");
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException(e);
		}
		deployConfigs: for (LdapName dn : deployConfigs.keySet()) {
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
				Attributes config = deployConfigs.get(dn);
				Attribute disabled = config.get(UserAdminConf.disabled.name());
				if (disabled != null)
					continue deployConfigs;
				// service factory service
				Rdn beforeLastRdn = dn.getRdn(dn.size() - 2);
				assert beforeLastRdn.getType().equals(NodeConstants.OU);
				String factoryPid = beforeLastRdn.getValue().toString();
				Configuration conf = configurationAdmin.createFactoryConfiguration(factoryPid.toString(), null);
				if (systemRolesDn.equals(dn)) {
					systemRolesConf = configurationAdmin.createFactoryConfiguration(factoryPid.toString(), null);
				} else {
					AttributesDictionary dico = new AttributesDictionary(config);
					conf.update(dico);
				}
			}
		}

		// system roles must be last since it triggers node user admin publication
		if (systemRolesConf == null)
			throw new IllegalStateException("System roles are not configured.");
		systemRolesConf.update(new AttributesDictionary(deployConfigs.get(systemRolesDn)));
		
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
			// throw new CmsException("Cannot save deploy configs", e);
			log.error("Cannot save deploy configs", e);
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

public	Dictionary<String, Object> getProps(String factoryPid, String cn) {
		Attributes attrs = deployConfigs.get(serviceDn(factoryPid, cn));
		if (attrs != null)
			return new AttributesDictionary(attrs);
		else
			return null;
	}

	private static boolean isInitialized() {
		return Files.exists(deployConfigPath);
	}

	public boolean isFirstInit() {
		return isFirstInit;
	}

}
