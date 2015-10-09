package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;

/** Package utilities */
class KernelUtils implements KernelConstants {
	private final static String OSGI_INSTANCE_AREA = "osgi.instance.area";
	private final static String OSGI_CONFIGURATION_AREA = "osgi.configuration.area";

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
			throw new CmsException("Cannot load " + resource
					+ " from classpath", e);
		}
		return asDictionary(props);
	}

	static File getOsgiInstanceDir() {
		return new File(Activator.getBundleContext()
				.getProperty(OSGI_INSTANCE_AREA).substring("file:".length()))
				.getAbsoluteFile();
	}

	static String getOsgiInstancePath(String relativePath) {
		try {
			if (relativePath == null)
				return getOsgiInstanceDir().getCanonicalPath();
			else
				return new File(getOsgiInstanceDir(), relativePath)
						.getCanonicalPath();
		} catch (IOException e) {
			throw new CmsException("Cannot get instance path for "
					+ relativePath, e);
		}
	}

	static File getOsgiConfigurationFile(String relativePath) {
		try {
			return new File(new URI(Activator.getBundleContext().getProperty(
					OSGI_CONFIGURATION_AREA)
					+ relativePath)).getCanonicalFile();
		} catch (Exception e) {
			throw new CmsException("Cannot get configuration file for "
					+ relativePath, e);
		}
	}

	static String getFrameworkProp(String key, String def) {
		String value = Activator.getBundleContext().getProperty(key);
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
			lc = new LoginContext(AuthConstants.LOGIN_CONTEXT_ANONYMOUS,
					subject);
			lc.login();
			return subject;
		} catch (LoginException e) {
			throw new CmsException("Cannot login as anonymous", e);
		}
	}

	// @Deprecated
	// static void anonymousLogin(AuthenticationManager authenticationManager) {
	// try {
	// List<GrantedAuthorityPrincipal> anonAuthorities = Collections
	// .singletonList(new GrantedAuthorityPrincipal(
	// KernelHeader.ROLE_ANONYMOUS));
	// UserDetails anonUser = new User(KernelHeader.USERNAME_ANONYMOUS,
	// "", true, true, true, true, anonAuthorities);
	// AnonymousAuthenticationToken anonToken = new
	// AnonymousAuthenticationToken(
	// DEFAULT_SECURITY_KEY, anonUser, anonAuthorities);
	// Authentication authentication = authenticationManager
	// .authenticate(anonToken);
	// SecurityContextHolder.getContext()
	// .setAuthentication(authentication);
	// } catch (Exception e) {
	// throw new CmsException("Cannot authenticate", e);
	// }
	// }

	// HTTP
	static void logRequestHeaders(Log log, HttpServletRequest request) {
		if (!log.isDebugEnabled())
			return;
		for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames
				.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			Object headerValue = request.getHeader(headerName);
			log.debug(headerName + ": " + headerValue);
		}
	}

	private KernelUtils() {

	}
}
