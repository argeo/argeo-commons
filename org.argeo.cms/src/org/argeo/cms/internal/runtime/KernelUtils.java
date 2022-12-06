package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.URIParameter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.osgi.CmsActivator;

/** Package utilities */
class KernelUtils implements KernelConstants {
	final static String OSGI_INSTANCE_AREA = "osgi.instance.area";
	final static String OSGI_CONFIGURATION_AREA = "osgi.configuration.area";

	static void setJaasConfiguration(URL jaasConfigurationUrl) {
		try {
			URIParameter uriParameter = new URIParameter(jaasConfigurationUrl.toURI());
			javax.security.auth.login.Configuration jaasConfiguration = javax.security.auth.login.Configuration
					.getInstance("JavaLoginConfig", uriParameter);
			javax.security.auth.login.Configuration.setConfiguration(jaasConfiguration);
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot set configuration " + jaasConfigurationUrl, e);
		}
	}

	static Dictionary<String, ?> asDictionary(Properties props) {
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			hashtable.put(key.toString(), props.get(key));
		}
		return hashtable;
	}

	static Dictionary<String, ?> asDictionary(ClassLoader cl, String resource) {
		Properties props = new Properties();
		try {
			props.load(cl.getResourceAsStream(resource));
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load " + resource + " from classpath", e);
		}
		return asDictionary(props);
	}

	static Path getExecutionDir(String relativePath) {
		Path executionDir = Paths.get(getFrameworkProp("user.dir"));
		if (relativePath == null)
			return executionDir;
		return executionDir.resolve(relativePath);
	}

	public static Path getOsgiInstancePath(String relativePath) {
		URI uri = getOsgiInstanceUri(relativePath);
		if (uri == null) // no data area available
			return null;
		return Paths.get(uri);
	}

	public static URI getOsgiInstanceUri(String relativePath) {
		String osgiInstanceBaseUri = getFrameworkProp(OSGI_INSTANCE_AREA);
		if (osgiInstanceBaseUri == null) // no data area available
			return null;

		if (!osgiInstanceBaseUri.endsWith("/"))
			osgiInstanceBaseUri = osgiInstanceBaseUri + "/";
		return safeUri(osgiInstanceBaseUri + (relativePath != null ? relativePath : ""));
	}

	static String getFrameworkProp(String key, String def) {
		String value;
		if (CmsActivator.getBundleContext() != null)
			value = CmsActivator.getBundleContext().getProperty(key);
		else
			value = System.getProperty(key);
		if (value == null)
			return def;
		return value;
	}

	static String getFrameworkProp(String key) {
		return getFrameworkProp(key, null);
	}

	static void logFrameworkProperties(CmsLog log) {
		for (Object sysProp : new TreeSet<Object>(System.getProperties().keySet())) {
			log.debug(sysProp + "=" + getFrameworkProp(sysProp.toString()));
		}
	}

	static void printSystemProperties(PrintStream out) {
		TreeMap<String, String> display = new TreeMap<>();
		for (Object key : System.getProperties().keySet())
			display.put(key.toString(), System.getProperty(key.toString()));
		for (String key : display.keySet())
			out.println(key + "=" + display.get(key));
	}

	static boolean asBoolean(String value) {
		if (value == null)
			return false;
		switch (value) {
		case "true":
			return true;
		case "false":
			return false;
		default:
			throw new IllegalArgumentException("Unsupported value for boolean attribute : " + value);
		}
	}

	private static URI safeUri(String uri) {
		if (uri == null)
			throw new IllegalArgumentException("URI cannot be null");
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Badly formatted URI " + uri, e);
		}
	}

	private KernelUtils() {

	}
}
