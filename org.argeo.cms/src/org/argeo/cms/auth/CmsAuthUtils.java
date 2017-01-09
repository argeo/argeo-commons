package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.jackrabbit.core.security.AnonymousPrincipal;
//import org.apache.jackrabbit.core.security.SecurityConstants;
//import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.cms.internal.kernel.WebCmsSessionImpl;
import org.argeo.node.security.AnonymousPrincipal;
import org.argeo.node.security.DataAdminPrincipal;
import org.argeo.node.security.NodeSecurityUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

class CmsAuthUtils {
	private final static Log log = LogFactory.getLog(CmsAuthUtils.class);

	/** Shared HTTP request */
	static final String SHARED_STATE_HTTP_REQUEST = "org.argeo.cms.auth.http.request";
	/** From org.osgi.service.http.HttpContext */
	static final String SHARED_STATE_AUTHORIZATION = "org.osgi.service.useradmin.authorization";
	/** From com.sun.security.auth.module.*LoginModule */
	static final String SHARED_STATE_NAME = "javax.security.auth.login.name";
	/** From com.sun.security.auth.module.*LoginModule */
	static final String SHARED_STATE_PWD = "javax.security.auth.login.password";

	static void addAuthentication(Subject subject, Authorization authorization) {
		assert subject != null;
		checkSubjectEmpty(subject);
		assert authorization != null;

		// required for display name:
		subject.getPrivateCredentials().add(authorization);

		Set<Principal> principals = subject.getPrincipals();
		try {
			String authName = authorization.getName();

			// determine user's principal
			final LdapName name;
			final Principal userPrincipal;
			if (authName == null) {
				name = NodeSecurityUtils.ROLE_ANONYMOUS_NAME;
				userPrincipal = new AnonymousPrincipal();
				principals.add(userPrincipal);
				// principals.add(new AnonymousPrincipal());
			} else {
				name = new LdapName(authName);
				NodeSecurityUtils.checkUserName(name);
				userPrincipal = new X500Principal(name.toString());
				principals.add(userPrincipal);
				principals.add(new ImpliedByPrincipal(NodeSecurityUtils.ROLE_USER_NAME, userPrincipal));
			}

			// Add roles provided by authorization
			for (String role : authorization.getRoles()) {
				LdapName roleName = new LdapName(role);
				if (roleName.equals(name)) {
					// skip
				} else {
					NodeSecurityUtils.checkImpliedPrincipalName(roleName);
					principals.add(new ImpliedByPrincipal(roleName.toString(), userPrincipal));
					if (roleName.equals(NodeSecurityUtils.ROLE_ADMIN_NAME))
						principals.add(new DataAdminPrincipal());
				}
			}

		} catch (InvalidNameException e) {
			throw new CmsException("Cannot commit", e);
		}
	}

	private static void checkSubjectEmpty(Subject subject) {
		if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty())
			throw new IllegalStateException("Already logged in as anonymous: " + subject);
		if (!subject.getPrincipals(X500Principal.class).isEmpty())
			throw new IllegalStateException("Already logged in as user: " + subject);
		if (!subject.getPrincipals(DataAdminPrincipal.class).isEmpty())
			throw new IllegalStateException("Already logged in as data admin: " + subject);
		if (!subject.getPrincipals(ImpliedByPrincipal.class).isEmpty())
			throw new IllegalStateException("Already authorized: " + subject);
	}

	static void cleanUp(Subject subject) {
		// Argeo
		subject.getPrincipals().removeAll(subject.getPrincipals(X500Principal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(ImpliedByPrincipal.class));
		// Jackrabbit
		// subject.getPrincipals().removeAll(subject.getPrincipals(AdminPrincipal.class));
		// subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
	}

	// SHARED STATE KEYS
	// compatible with com.sun.security.auth.module.*LoginModule
	// public static final String SHARED_STATE_USERNAME =
	// "javax.security.auth.login.name";
	// public static final String SHARED_STATE_PASSWORD =
	// "javax.security.auth.login.password";

	static void registerSessionAuthorization(BundleContext bc, HttpServletRequest request, Subject subject,
			Authorization authorization) {
		String httpSessId = request.getSession().getId();
		if (authorization.getName() != null) {
			request.setAttribute(HttpContext.REMOTE_USER, authorization.getName());
			request.setAttribute(HttpContext.AUTHORIZATION, authorization);

			HttpSession httpSession = request.getSession();
			if (httpSession.getAttribute(HttpContext.AUTHORIZATION) == null) {

				Collection<ServiceReference<WebCmsSession>> sr;
				try {
					sr = bc.getServiceReferences(WebCmsSession.class,
							"(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessId + ")");
				} catch (InvalidSyntaxException e) {
					throw new CmsException("Cannot get CMS session for id " + httpSessId, e);
				}
				ServiceReference<WebCmsSession> cmsSessionRef;
				if (sr.size() == 1) {
					cmsSessionRef = sr.iterator().next();
				} else if (sr.size() == 0) {
					WebCmsSessionImpl cmsSessionImpl = new WebCmsSessionImpl(httpSessId, authorization);
					cmsSessionRef = cmsSessionImpl.getServiceRegistration().getReference();
					if (log.isDebugEnabled())
						log.debug("Initialized " + cmsSessionImpl + " for " + authorization.getName());
				} else
					throw new CmsException(sr.size() + " CMS sessions registered for " + httpSessId);

				WebCmsSessionImpl cmsSession = (WebCmsSessionImpl) bc.getService(cmsSessionRef);
				cmsSession.addHttpSession(request);
				if (log.isTraceEnabled())
					log.trace("Added " + request.getServletPath() + " to " + cmsSession + " (" + request.getRequestURI()
							+ ")");
				// httpSession.setAttribute(HttpContext.REMOTE_USER,
				// authorization.getName());
				// httpSession.setAttribute(HttpContext.AUTHORIZATION,
				// authorization);
			}
		}
		HttpSessionId httpSessionId = new HttpSessionId(httpSessId);
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 0)
			subject.getPrivateCredentials().add(httpSessionId);
		else {
			String storedSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
			// if (storedSessionId.equals(httpSessionId.getValue()))
			throw new CmsException(
					"Subject already logged with session " + storedSessionId + " (not " + httpSessionId + ")");
		}
	}

	static boolean logoutSession(BundleContext bc, Subject subject) {
		String httpSessionId;
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 1)
			httpSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
		else
			return false;
		Collection<ServiceReference<WebCmsSession>> srs;
		try {
			srs = bc.getServiceReferences(WebCmsSession.class,
					"(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessionId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot retrieve CMS session #" + httpSessionId, e);
		}

		if (srs.size() == 0) {
			if (log.isTraceEnabled())
				log.warn("No CMS web session found for http session " + httpSessionId);
			return false;
		} else if (srs.size() > 1)
			throw new CmsException(srs.size() + " CMS web sessions found for http session " + httpSessionId);

		WebCmsSessionImpl cmsSession = (WebCmsSessionImpl) bc.getService(srs.iterator().next());
		cmsSession.cleanUp();
		subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(HttpSessionId.class));
		if (log.isDebugEnabled())
			log.debug("Cleaned up " + cmsSession);
		return true;
	}

	private CmsAuthUtils() {

	}

}
