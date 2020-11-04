package org.argeo.cms.servlet;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.argeo.cms.auth.CurrentUser;
import org.osgi.service.http.HttpContext;

/** Authentications utilities when using servlets. */
public class ServletAuthUtils {
	/**
	 * Execute this supplier, using the CMS class loader as context classloader.
	 * Useful to log in to JCR.
	 */
	public final static <T> T doAs(Supplier<T> supplier, HttpServletRequest req) {
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

	public final static void configureRequestSecurity(HttpServletRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) != null)
			throw new IllegalStateException("Request already authenticated.");
		AccessControlContext acc = AccessController.getContext();
		req.setAttribute(HttpContext.REMOTE_USER, CurrentUser.getUsername());
		req.setAttribute(AccessControlContext.class.getName(), acc);
	}

	public final static void clearRequestSecurity(HttpServletRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) == null)
			throw new IllegalStateException("Cannot clear non-authenticated request.");
		req.setAttribute(HttpContext.REMOTE_USER, null);
		req.setAttribute(AccessControlContext.class.getName(), null);
	}
}
