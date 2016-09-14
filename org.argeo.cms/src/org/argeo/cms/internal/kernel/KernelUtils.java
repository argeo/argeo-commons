package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelConstants.WEBDAV_PRIVATE;
import static org.argeo.cms.internal.kernel.KernelConstants.WEBDAV_PUBLIC;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.jcr.ArgeoJcrConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Package utilities */
class KernelUtils implements KernelConstants {
	final static String OSGI_INSTANCE_AREA = "osgi.instance.area";
	final static String OSGI_CONFIGURATION_AREA = "osgi.configuration.area";

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
			throw new CmsException("Cannot load " + resource + " from classpath", e);
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
			throw new CmsException("Cannot get canonical file", e);
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
		return safeUri(osgiInstanceBaseUri + (relativePath != null ? relativePath : ""));
	}

	// static String getOsgiInstancePath(String relativePath) {
	// try {
	// if (relativePath == null)
	// return getOsgiInstanceDir().getCanonicalPath();
	// else
	// return new File(getOsgiInstanceDir(), relativePath).getCanonicalPath();
	// } catch (IOException e) {
	// throw new CmsException("Cannot get instance path for " + relativePath,
	// e);
	// }
	// }

	static File getOsgiConfigurationFile(String relativePath) {
		try {
			return new File(new URI(getBundleContext().getProperty(OSGI_CONFIGURATION_AREA) + relativePath))
					.getCanonicalFile();
		} catch (Exception e) {
			throw new CmsException("Cannot get configuration file for " + relativePath, e);
		}
	}

	static String getFrameworkProp(String key, String def) {
		String value = getBundleContext().getProperty(key);
		if (value == null)
			return def;
		return value;
	}

	static String getFrameworkProp(String key) {
		return getFrameworkProp(key, null);
	}

	// Security
	static Subject anonymousLogin() {
		Subject subject = new Subject();
		LoginContext lc;
		try {
			lc = new LoginContext(AuthConstants.LOGIN_CONTEXT_ANONYMOUS, subject);
			lc.login();
			return subject;
		} catch (LoginException e) {
			throw new CmsException("Cannot login as anonymous", e);
		}
	}

	// HTTP
	static void logRequestHeaders(Log log, HttpServletRequest request) {
		if (!log.isDebugEnabled())
			return;
		for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			Object headerValue = request.getHeader(headerName);
			log.debug(headerName + ": " + headerValue);
		}
		log.debug(request.getRequestURI() + "\n");
	}

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

	static Session openAdminSession(Repository repository) {
		return openAdminSession(repository, null);
	}

	static Session openAdminSession(final Repository repository, final String workspaceName) {
		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(KernelUtils.class.getClassLoader());
		LoginContext loginContext;
		try {
			loginContext = new LoginContext(AuthConstants.LOGIN_CONTEXT_DATA_ADMIN);
			loginContext.login();
		} catch (LoginException e1) {
			throw new CmsException("Could not login as data admin", e1);
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
		return Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Session>() {

			@Override
			public Session run() {
				try {
					return repository.login(workspaceName);
				} catch (RepositoryException e) {
					throw new CmsException("Cannot open admin session", e);
				}
			}

		});
	}

	/**
	 * @return the {@link BundleContext} of the {@link Bundle} which provided
	 *         this class, never null.
	 * @throws CmsException
	 *             if the related bundle is not active
	 */
	public static BundleContext getBundleContext(Class<?> clzz) {
		Bundle bundle = FrameworkUtil.getBundle(clzz);
		BundleContext bc = bundle.getBundleContext();
		if (bc == null)
			throw new CmsException("Bundle " + bundle.getSymbolicName() + " is not active");
		return bc;
	}

	private static BundleContext getBundleContext() {
		return getBundleContext(KernelUtils.class);
	}

	private static URI safeUri(String uri) {
		if (uri == null)
			throw new CmsException("URI cannot be null");
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new CmsException("Dadly formatted URI " + uri, e);
		}
	}

	// DATA
	public static StringBuilder getServerBaseUrl(HttpServletRequest request) {
		try {
			URL url = new URL(request.getRequestURL().toString());
			StringBuilder buf = new StringBuilder();
			buf.append(url.getProtocol()).append("://").append(url.getHost());
			if (url.getPort() != -1)
				buf.append(':').append(url.getPort());
			return buf;
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot extract server base URL from " + request.getRequestURL(), e);
		}
	}

	public static String getDataUrl(Node node, HttpServletRequest request) throws RepositoryException {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append(getDataPath(node));
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot build data URL for " + node, e);
		}
	}

	public static String getDataPath(Node node) throws RepositoryException {
		assert node != null;
		String userId = node.getSession().getUserID();
//		if (log.isTraceEnabled())
//			log.trace(userId + " : " + node.getPath());
		StringBuilder buf = new StringBuilder();
		boolean isAnonymous = userId.equalsIgnoreCase(AuthConstants.ROLE_ANONYMOUS);
		if (isAnonymous)
			buf.append(WEBDAV_PUBLIC);
		else
			buf.append(WEBDAV_PRIVATE);
		// TODO convey repo alias vie repository properties
		return buf.append('/').append(ArgeoJcrConstants.ALIAS_NODE).append('/').append(node.getSession().getWorkspace().getName())
				.append(node.getPath()).toString();
	}

	public static String getCanonicalUrl(Node node, HttpServletRequest request) throws RepositoryException {
		try {
			StringBuilder buf = getServerBaseUrl(request);
			buf.append('/').append('!').append(node.getPath());
			return new URL(buf.toString()).toString();
		} catch (MalformedURLException e) {
			throw new CmsException("Cannot build data URL for " + node, e);
		}
		// return request.getRequestURL().append('!').append(node.getPath())
		// .toString();
	}

	
	private KernelUtils() {

	}
}
