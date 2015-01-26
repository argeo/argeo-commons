package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.argeo.cms.CmsException;
import org.osgi.framework.BundleContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class KernelUtils implements KernelConstants {
	final static String OSGI_INSTANCE_AREA = "osgi.instance.area";

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

	static File getOsgiInstanceDir(BundleContext bundleContext) {
		return new File(bundleContext.getProperty(OSGI_INSTANCE_AREA)
				.substring("file:".length())).getAbsoluteFile();
	}

	// Security
	static void anonymousLogin(AuthenticationManager authenticationManager) {
		try {
			List<SimpleGrantedAuthority> anonAuthorities = Collections
					.singletonList(new SimpleGrantedAuthority(ROLE_ANONYMOUS));
			UserDetails anonUser = new User(ANONYMOUS_USER, "", true, true,
					true, true, anonAuthorities);
			AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
					DEFAULT_SECURITY_KEY, anonUser, anonAuthorities);
			Authentication authentication = authenticationManager
					.authenticate(anonToken);
			SecurityContextHolder.getContext()
					.setAuthentication(authentication);
		} catch (Exception e) {
			throw new CmsException("Cannot authenticate", e);
		}
	}

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
