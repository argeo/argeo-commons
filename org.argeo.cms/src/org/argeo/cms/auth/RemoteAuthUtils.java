package org.argeo.cms.auth;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import javax.security.auth.Subject;

import org.argeo.api.cms.CmsSession;
import org.argeo.cms.internal.runtime.CmsContextImpl;

/** Remote authentication utilities. */
public class RemoteAuthUtils {
	static final String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
//	private static BundleContext bundleContext = FrameworkUtil.getBundle(RemoteAuthUtils.class).getBundleContext();

	/**
	 * Execute this supplier, using the CMS class loader as context classloader.
	 * Useful to log in to JCR.
	 */
	public final static <T> T doAs(Supplier<T> supplier, RemoteAuthRequest req) {
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(RemoteAuthUtils.class.getClassLoader());
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

	public final static void configureRequestSecurity(RemoteAuthRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) != null)
			throw new IllegalStateException("Request already authenticated.");
		AccessControlContext acc = AccessController.getContext();
		req.setAttribute(REMOTE_USER, CurrentUser.getUsername());
		req.setAttribute(AccessControlContext.class.getName(), acc);
	}

	public final static void clearRequestSecurity(RemoteAuthRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) == null)
			throw new IllegalStateException("Cannot clear non-authenticated request.");
		req.setAttribute(REMOTE_USER, null);
		req.setAttribute(AccessControlContext.class.getName(), null);
	}

	public static CmsSession getCmsSession(RemoteAuthRequest req) {
		Subject subject = Subject
				.getSubject((AccessControlContext) req.getAttribute(AccessControlContext.class.getName()));
		CmsSession cmsSession = CmsContextImpl.getCmsContext().getCmsSession(subject);
		return cmsSession;
	}
}
