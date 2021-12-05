package org.argeo.cms.jcr.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.security.URIParameter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.argeo.api.DataModelNamespace;
import org.argeo.api.NodeConstants;
import org.argeo.cms.jcr.internal.osgi.CmsJcrActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

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

	static File getExecutionDir(String relativePath) {
		File executionDir = new File(getFrameworkProp("user.dir"));
		if (relativePath == null)
			return executionDir;
		try {
			return new File(executionDir, relativePath).getCanonicalFile();
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot get canonical file", e);
		}
	}

	static File getOsgiInstanceDir() {
		return new File(getBundleContext().getProperty(OSGI_INSTANCE_AREA).substring("file:".length()))
				.getAbsoluteFile();
	}

	static Path getOsgiInstancePath(String relativePath) {
		return Paths.get(getOsgiInstanceUri(relativePath));
	}

	static URI getOsgiInstanceUri(String relativePath) {
		String osgiInstanceBaseUri = getFrameworkProp(OSGI_INSTANCE_AREA);
		if (osgiInstanceBaseUri != null)
			return safeUri(osgiInstanceBaseUri + (relativePath != null ? relativePath : ""));
		else
			return Paths.get(System.getProperty("user.dir")).toUri();
	}

	static File getOsgiConfigurationFile(String relativePath) {
		try {
			return new File(new URI(getBundleContext().getProperty(OSGI_CONFIGURATION_AREA) + relativePath))
					.getCanonicalFile();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot get configuration file for " + relativePath, e);
		}
	}

	static String getFrameworkProp(String key, String def) {
		BundleContext bundleContext = CmsJcrActivator.getBundleContext();
		String value;
		if (bundleContext != null)
			value = bundleContext.getProperty(key);
		else
			value = System.getProperty(key);
		if (value == null)
			return def;
		return value;
	}

	static String getFrameworkProp(String key) {
		return getFrameworkProp(key, null);
	}

	// Security
	// static Subject anonymousLogin() {
	// Subject subject = new Subject();
	// LoginContext lc;
	// try {
	// lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject);
	// lc.login();
	// return subject;
	// } catch (LoginException e) {
	// throw new CmsException("Cannot login as anonymous", e);
	// }
	// }

	static void logFrameworkProperties(Log log) {
		BundleContext bc = getBundleContext();
		for (Object sysProp : new TreeSet<Object>(System.getProperties().keySet())) {
			log.debug(sysProp + "=" + bc.getProperty(sysProp.toString()));
		}
		// String[] keys = { Constants.FRAMEWORK_STORAGE,
		// Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION,
		// Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_SECURITY,
		// Constants.FRAMEWORK_TRUST_REPOSITORIES,
		// Constants.FRAMEWORK_WINDOWSYSTEM, Constants.FRAMEWORK_VENDOR,
		// Constants.FRAMEWORK_VERSION, Constants.FRAMEWORK_STORAGE_CLEAN,
		// Constants.FRAMEWORK_LANGUAGE, Constants.FRAMEWORK_UUID };
		// for (String key : keys)
		// log.debug(key + "=" + bc.getProperty(key));
	}

	static void printSystemProperties(PrintStream out) {
		TreeMap<String, String> display = new TreeMap<>();
		for (Object key : System.getProperties().keySet())
			display.put(key.toString(), System.getProperty(key.toString()));
		for (String key : display.keySet())
			out.println(key + "=" + display.get(key));
	}

	static Session openAdminSession(Repository repository) {
		return openAdminSession(repository, null);
	}

	static Session openAdminSession(final Repository repository, final String workspaceName) {
		LoginContext loginContext = loginAsDataAdmin();
		return Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Session>() {

			@Override
			public Session run() {
				try {
					return repository.login(workspaceName);
				} catch (RepositoryException e) {
					throw new IllegalStateException("Cannot open admin session", e);
				} finally {
					try {
						loginContext.logout();
					} catch (LoginException e) {
						throw new IllegalStateException(e);
					}
				}
			}

		});
	}

	static LoginContext loginAsDataAdmin() {
		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(KernelUtils.class.getClassLoader());
		LoginContext loginContext;
		try {
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_DATA_ADMIN);
			loginContext.login();
		} catch (LoginException e1) {
			throw new IllegalStateException("Could not login as data admin", e1);
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
		return loginContext;
	}

	static void doAsDataAdmin(Runnable action) {
		LoginContext loginContext = loginAsDataAdmin();
		Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					action.run();
					return null;
				} finally {
					try {
						loginContext.logout();
					} catch (LoginException e) {
						throw new IllegalStateException(e);
					}
				}
			}

		});
	}

	static void asyncOpen(ServiceTracker<?, ?> st) {
		Runnable run = new Runnable() {

			@Override
			public void run() {
				st.open();
			}
		};
//		Activator.getInternalExecutorService().execute(run);
		new Thread(run, "Open service tracker " + st).start();
	}

	static BundleContext getBundleContext() {
		return CmsJcrActivator.getBundleContext();
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
			throw new IllegalArgumentException(
					"Unsupported value for attribute " + DataModelNamespace.ABSTRACT + ": " + value);
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
