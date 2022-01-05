package org.argeo.cms.servlet;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import javax.security.auth.Subject;

import org.argeo.api.cms.CmsSession;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequest;
import org.argeo.cms.osgi.CmsOsgiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Authentications utilities when using servlets. */
public class ServletAuthUtils {
	static final String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
	private static BundleContext bundleContext = FrameworkUtil.getBundle(ServletAuthUtils.class).getBundleContext();

	/**
	 * Execute this supplier, using the CMS class loader as context classloader.
	 * Useful to log in to JCR.
	 */
	public final static <T> T doAs(Supplier<T> supplier, HttpRequest req) {
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(ServletAuthUtils.class.getClassLoader());
		try {
			return Subject.doAs(
					Subject.getSubject((AccessControlContext) req.getAttribute(AccessControlContext.class.getName())),
					new PrivilegedAction<T>() {

						@Override
						public T run() {
							return supplier.get();
						}

					});
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}
	}

	public final static void configureRequestSecurity(HttpRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) != null)
			throw new IllegalStateException("Request already authenticated.");
		AccessControlContext acc = AccessController.getContext();
		req.setAttribute(REMOTE_USER, CurrentUser.getUsername());
		req.setAttribute(AccessControlContext.class.getName(), acc);
	}

	public final static void clearRequestSecurity(HttpRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) == null)
			throw new IllegalStateException("Cannot clear non-authenticated request.");
		req.setAttribute(REMOTE_USER, null);
		req.setAttribute(AccessControlContext.class.getName(), null);
	}

	public static CmsSession getCmsSession(HttpRequest req) {
		Subject subject = Subject
				.getSubject((AccessControlContext) req.getAttribute(AccessControlContext.class.getName()));
		CmsSession cmsSession = CmsOsgiUtils.getCmsSession(bundleContext, subject);
		return cmsSession;
	}
}
