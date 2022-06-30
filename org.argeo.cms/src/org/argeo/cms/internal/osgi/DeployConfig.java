package org.argeo.cms.internal.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.util.directory.DirectoryConf;
import org.argeo.util.directory.ldap.AttributesDictionary;
import org.argeo.util.directory.ldap.LdifParser;
import org.argeo.util.directory.ldap.LdifWriter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

/** Manages the LDIF-based deployment configuration. */
@Deprecated
public class DeployConfig {

	private final CmsLog log = CmsLog.getLog(getClass());
//	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private static Path deployConfigPath;// = KernelUtils.getOsgiInstancePath(KernelConstants.DEPLOY_CONFIG_PATH);
	private SortedMap<LdapName, Attributes> deployConfigs = new TreeMap<>();
//	private final DataModels dataModels;

	private boolean isFirstInit = false;

	private final static String ROLES = "roles";

	private ConfigurationAdmin configurationAdmin;

	private void firstInit() throws IOException {
		log.info("## FIRST INIT ##");
		Files.createDirectories(deployConfigPath.getParent());

		// FirstInit firstInit = new FirstInit();
		//InitUtils.prepareFirstInitInstanceArea();

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
//		List<Dictionary<String, Object>> userDirectoryConfigs = InitUtils.getUserDirectoryConfigs();
//		if (userDirectoryConfigs.size() != 0) {
//			List<String> activeCns = new ArrayList<>();
//			for (int i = 0; i < userDirectoryConfigs.size(); i++) {
//				Dictionary<String, Object> userDirectoryConfig = userDirectoryConfigs.get(i);
//				String baseDn = (String) userDirectoryConfig.get(DirectoryConf.baseDn.name());
//				String cn;
//				if (CmsConstants.ROLES_BASEDN.equals(baseDn))
//					cn = ROLES;
//				else
//					cn = DirectoryConf.baseDnHash(userDirectoryConfig);
//				activeCns.add(cn);
//				userDirectoryConfig.put(CmsConstants.CN, cn);
//				putFactoryDeployConfig(CmsConstants.NODE_USER_ADMIN_PID, userDirectoryConfig);
//			}
//			// disable others
//			LdapName userAdminFactoryName = serviceFactoryDn(CmsConstants.NODE_USER_ADMIN_PID);
//			for (LdapName name : deployConfigs.keySet()) {
//				if (name.startsWith(userAdminFactoryName) && !name.equals(userAdminFactoryName)) {
////					try {
//					Attributes attrs = deployConfigs.get(name);
//					String cn = name.getRdn(name.size() - 1).getValue().toString();
//					if (!activeCns.contains(cn)) {
//						attrs.put(DirectoryConf.disabled.name(), "true");
//					}
////					} catch (Exception e) {
////						throw new CmsException("Cannot disable user directory " + name, e);
////					}
//				}
//			}
//		}

		// http server
//		Dictionary<String, Object> webServerConfig = InitUtils
//				.getHttpServerConfig(getProps(KernelConstants.JETTY_FACTORY_PID, CmsConstants.DEFAULT));
//		if (!webServerConfig.isEmpty()) {
//			// TODO check for other customizers
//			putFactoryDeployConfig(KernelConstants.JETTY_FACTORY_PID, webServerConfig);
//		}

		// SAVE
		save();
		//

	}

	public void start() {
		try {
			if (!isInitialized()) { // first init
				isFirstInit = true;
				firstInit();
			}

			boolean isClean = true;
			if (configurationAdmin != null)
				try {
					Configuration[] confs = configurationAdmin
							.listConfigurations("(service.factoryPid=" + CmsConstants.NODE_USER_ADMIN_PID + ")");
					isClean = confs == null || confs.length == 0;
				} catch (Exception e) {
					throw new IllegalStateException("Cannot analyse clean state", e);
				}

			try (InputStream in = Files.newInputStream(deployConfigPath)) {
				deployConfigs = new LdifParser().read(in);
			}
			if (isClean) {
				if (log.isDebugEnabled())
					log.debug("Clean state, loading from framework properties...");
				setFromFrameworkProperties(isFirstInit);
				if (configurationAdmin != null)
					loadConfigs();
			}
			// TODO check consistency if not clean
		} catch (IOException e) {
			throw new RuntimeException("Cannot load deploy configuration", e);
		}
	}

	public void stop() {

	}

	protected void logAllConfigurations() {
		if (!log.isDebugEnabled())
			return;
		try {
			Configuration[] configurations = configurationAdmin.listConfigurations(null);
			if (configurations == null) {
				log.debug("No configuration available");
				return;
			}
			Arrays.sort(configurations, (o1, o2) -> o1.getPid().compareTo(o2.getPid()));
			for (Configuration configuration : configurations) {
				log.debug(configuration.getFactoryPid() + " - " + configuration.getPid() + " - "
						+ configuration.getProperties());
			}
		} catch (IOException | InvalidSyntaxException e) {
			throw new IllegalStateException("Cannot log configurations", e);
		}
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
			Attributes deployConfig = deployConfigs.get(dn);
			Rdn lastRdn = dn.getRdn(dn.size() - 1);
			LdapName prefix = (LdapName) dn.getPrefix(dn.size() - 1);
			if (prefix.toString().equals(CmsConstants.DEPLOY_BASEDN)) {
				if (lastRdn.getType().equals(CmsConstants.CN)) {
					// service
					String pid = lastRdn.getValue().toString();
					Configuration conf = configurationAdmin.getConfiguration(pid);
					AttributesDictionary dico = new AttributesDictionary(deployConfig);
					conf.update(dico);
				} else {
					// service factory definition
				}
			} else {
				Attribute disabled = deployConfig.get(DirectoryConf.disabled.name());
				if (disabled != null)
					continue deployConfigs;
				// service factory service
				if (!lastRdn.getType().equals(CmsConstants.CN))
					throw new IllegalStateException("Only " + CmsConstants.CN + "= is supported: " + dn);
				Rdn beforeLastRdn = dn.getRdn(dn.size() - 2);
				assert beforeLastRdn.getType().equals(CmsConstants.OU);
				String factoryPid = beforeLastRdn.getValue().toString();

				String cn = lastRdn.getValue().toString();
				Configuration conf = getSingleServiceConfiguration(factoryPid, cn);
				if (conf != null) {
					if (systemRolesDn.equals(dn))
						systemRolesConf = conf;
					// TODO deal with modifications
//					boolean modified = false;
//					Dictionary<String, Object> currentProperties = conf.getProperties();
//
//					attrs: for (NamingEnumeration<? extends Attribute> it = deployConfig.getAll(); it
//							.hasMoreElements();) {
//						Attribute attr = (Attribute) it.next();
//						String key = attr.getID();
//						Object currentValue = currentProperties.get(key);
//						if (currentValue == null) {
//							modified = true;
//							break attrs;
//						}
//					}

//					AttributesDictionary dico = new AttributesDictionary(deployConfig);
//					conf.update(dico);
				} else {

					conf = configurationAdmin.createFactoryConfiguration(factoryPid.toString(), null);
					if (systemRolesDn.equals(dn)) {
						systemRolesConf = configurationAdmin.createFactoryConfiguration(factoryPid.toString(), null);
					} else {
						AttributesDictionary dico = new AttributesDictionary(deployConfig);
						conf.update(dico);
					}
				}
			}
		}

		// system roles must be last since it triggers node user admin publication
		if (systemRolesConf == null)
			throw new IllegalStateException("System roles are not configured.");
		systemRolesConf.update(new AttributesDictionary(deployConfigs.get(systemRolesDn)));

//		logAllConfigurations();
	}

	public Set<Dictionary<String, Object>> getUserDirectoryConfigs() {
		// not static because class is not supported by Android
		final LdapName USER_ADMIN_BASE_DN;
		try {
			USER_ADMIN_BASE_DN = new LdapName(
					CmsConstants.OU + "=" + CmsConstants.NODE_USER_ADMIN_PID + "," + CmsConstants.DEPLOY_BASEDN);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException(e);
		}
		Set<Dictionary<String, Object>> res = new HashSet<>();
		for (LdapName dn : deployConfigs.keySet()) {
			if (dn.endsWith(USER_ADMIN_BASE_DN)) {
				Attributes attributes = deployConfigs.get(dn);
				res.add(new AttributesDictionary(attributes));
			}
		}
		return res;
	}

//	@Override
	public void configurationEvent(ConfigurationEvent event) {
		try {
			if (ConfigurationEvent.CM_UPDATED == event.getType()) {
				Configuration conf = configurationAdmin.getConfiguration(event.getPid(), null);
				LdapName serviceDn = null;
				String factoryPid = conf.getFactoryPid();
				if (factoryPid != null) {
					LdapName serviceFactoryDn = serviceFactoryDn(factoryPid);
					if (deployConfigs.containsKey(serviceFactoryDn)) {
						for (LdapName dn : deployConfigs.keySet()) {
							if (dn.startsWith(serviceFactoryDn)) {
								Rdn lastRdn = dn.getRdn(dn.size() - 1);
								assert lastRdn.getType().equals(CmsConstants.CN);
								Object value = conf.getProperties().get(lastRdn.getType());
								assert value != null;
								if (value.equals(lastRdn.getValue())) {
									serviceDn = dn;
									break;
								}
							}
						}

						Object cn = conf.getProperties().get(CmsConstants.CN);
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

	public void putFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props) {
		Object cn = props.get(CmsConstants.CN);
		if (cn == null)
			throw new IllegalArgumentException("cn must be set in properties");
		LdapName serviceFactoryDn = serviceFactoryDn(factoryPid);
		if (!deployConfigs.containsKey(serviceFactoryDn))
			deployConfigs.put(serviceFactoryDn, new BasicAttributes(CmsConstants.OU, factoryPid));
		LdapName serviceDn = serviceDn(factoryPid, cn.toString());
		Attributes attrs = new BasicAttributes();
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	void putDeployConfig(String servicePid, Dictionary<String, Object> props) {
		LdapName serviceDn = serviceDn(servicePid);
		Attributes attrs = new BasicAttributes(CmsConstants.CN, servicePid);
		AttributesDictionary.copy(props, attrs);
		deployConfigs.put(serviceDn, attrs);
	}

	public void save() {
		try (Writer writer = Files.newBufferedWriter(deployConfigPath)) {
			new LdifWriter(writer).write(deployConfigs);
		} catch (IOException e) {
			// throw new CmsException("Cannot save deploy configs", e);
			log.error("Cannot save deploy configs", e);
		}
	}

	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

	public boolean hasDomain() {
		// FIXME lookup deploy configs directly
		if (configurationAdmin == null)
			return false;

		Configuration[] configs = listConfigurationsByFactory(CmsConstants.NODE_USER_ADMIN_PID);

		boolean hasDomain = false;
		for (Configuration config : configs) {
			Object realm = config.getProperties().get(DirectoryConf.realm.name());
			if (realm != null) {
				log.debug("Found realm: " + realm);
				hasDomain = true;
			}
		}
		return hasDomain;
	}

	private Configuration[] listConfigurationsByFactory(String factoryPid) {
		try {
			Configuration[] configs = configurationAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")");
			if (configs == null)
				configs = new Configuration[0];
			return configs;
		} catch (IOException | InvalidSyntaxException e) {
			throw new IllegalStateException("Cannot list configurations with factoryPid " + factoryPid, e);
		}

	}

	private Configuration getSingleServiceConfiguration(String factoryPid, String cn) {
		Configuration[] configs = listConfigurationsByFactory(factoryPid);
		List<Configuration> res = new ArrayList<>();
		for (Configuration config : configs) {
			Object currentCn = config.getProperties().get(CmsConstants.CN);
			if (currentCn != null && cn.equals(currentCn.toString()))
				res.add(config);
		}
		if (res.size() == 0)
			return null;
		if (res.size() > 1)
			throw new IllegalStateException(
					"More than one " + factoryPid + " configuration returned for " + CmsConstants.CN + "=" + cn);
		return res.get(0);
	}

	/*
	 * UTILITIES
	 */
	private LdapName serviceFactoryDn(String factoryPid) {
		try {
			return new LdapName(CmsConstants.OU + "=" + factoryPid + "," + CmsConstants.DEPLOY_BASEDN);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + factoryPid, e);
		}
	}

	private LdapName serviceDn(String servicePid) {
		try {
			return new LdapName(CmsConstants.CN + "=" + servicePid + "," + CmsConstants.DEPLOY_BASEDN);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + servicePid, e);
		}
	}

	private LdapName serviceDn(String factoryPid, String cn) {
		try {
			return (LdapName) serviceFactoryDn(factoryPid).add(new Rdn(CmsConstants.CN, cn));
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot generate DN from " + factoryPid + " and " + cn, e);
		}
	}

	public Dictionary<String, Object> getProps(String factoryPid, String cn) {
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
