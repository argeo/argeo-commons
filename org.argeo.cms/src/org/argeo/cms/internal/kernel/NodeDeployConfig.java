package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.SortedMap;
import java.util.function.Function;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.cms.CmsException;
import org.argeo.naming.AttributesDictionary;
import org.argeo.naming.LdifParser;
import org.argeo.naming.LdifWriter;
import org.argeo.node.NodeConstants;

class NodeDeployConfig {
	private final String BASE = "ou=deploy,ou=node";
	private final Path path;
	private final Function<String, String> getter;

	private final SortedMap<LdapName, Attributes> configurations;

	public NodeDeployConfig(Function<String, String> getter) {
		String osgiConfigurationArea = getter.apply(KernelUtils.OSGI_CONFIGURATION_AREA);
		try {
			this.path = Paths.get(new URI(osgiConfigurationArea));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot parse " + getter.apply(KernelUtils.OSGI_CONFIGURATION_AREA), e);
		}
		this.getter = getter;

		if (!Files.exists(path))
			try (Writer writer = Files.newBufferedWriter(path)) {
				Files.createFile(path);
				LdifWriter ldifWriter = new LdifWriter(writer);
			} catch (IOException e) {
				throw new CmsException("Cannot create " + path, e);
			}
		
		try (InputStream in = Files.newInputStream(path)) {
			configurations = new LdifParser().read(in);
		} catch (IOException e) {
			throw new CmsException("Cannot read " + path, e);
		}
	}

	public Dictionary<String, Object> getConfiguration(String servicePid) {
		LdapName dn;
		try {
			dn = new LdapName("ou=" + servicePid + "," + BASE);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot parse DN", e);
		}
		if (configurations.containsKey(dn))
			return new AttributesDictionary(configurations.get(dn));
		else
			return null;
	}
	
	static Dictionary<String, Object> getStatePropertiesFromEnvironment(Function<String, String> getter) {
		Hashtable<String, Object> props = new Hashtable<>();
		// i18n
		copyFrameworkProp(getter, NodeConstants.I18N_DEFAULT_LOCALE, props);
		copyFrameworkProp(getter, NodeConstants.I18N_LOCALES, props);
		// user admin
		copyFrameworkProp(getter, NodeConstants.ROLES_URI, props);
		copyFrameworkProp(getter, NodeConstants.USERADMIN_URIS, props);
		// data
		for (RepoConf repoConf : RepoConf.values())
			copyFrameworkProp(getter, NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name(), props);
		// TODO add other environment sources
		return props;
	}

	static Dictionary<String, Object> getUserAdminPropertiesFromEnvironment(Function<String, String> getter) {
		Hashtable<String, Object> props = new Hashtable<>();
		copyFrameworkProp(getter, NodeConstants.ROLES_URI, props);
		copyFrameworkProp(getter, NodeConstants.USERADMIN_URIS, props);
		return props;
	}

	private static void copyFrameworkProp(Function<String, String> getter, String key,
			Dictionary<String, Object> props) {
		String value = getter.apply(key);
		if (value != null)
			props.put(key, value);
	}

}
