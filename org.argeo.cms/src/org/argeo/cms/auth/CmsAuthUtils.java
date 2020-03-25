package org.argeo.cms.auth;

import java.security.Principal;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.argeo.api.NodeConstants;
import org.argeo.api.security.AnonymousPrincipal;
import org.argeo.api.security.DataAdminPrincipal;
import org.argeo.api.security.NodeSecurityUtils;
//import org.apache.jackrabbit.core.security.AnonymousPrincipal;
//import org.apache.jackrabbit.core.security.SecurityConstants;
//import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.cms.internal.http.WebCmsSessionImpl;
import org.argeo.cms.internal.kernel.Activator;
import org.argeo.osgi.useradmin.AuthenticatingUser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

class CmsAuthUtils {
	// Standard
	final static String SHARED_STATE_NAME = AuthenticatingUser.SHARED_STATE_NAME;
	final static String SHARED_STATE_PWD = AuthenticatingUser.SHARED_STATE_PWD;
	final static String HEADER_AUTHORIZATION = "Authorization";
	final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

	// Argeo specific
	final static String SHARED_STATE_HTTP_REQUEST = "org.argeo.cms.auth.http.request";
	final static String SHARED_STATE_SPNEGO_TOKEN = "org.argeo.cms.auth.spnegoToken";
	final static String SHARED_STATE_SPNEGO_OUT_TOKEN = "org.argeo.cms.auth.spnegoOutToken";
	final static String SHARED_STATE_CERTIFICATE_CHAIN = "org.argeo.cms.auth.certificateChain";
	final static String SHARED_STATE_REMOTE_ADDR = "org.argeo.cms.auth.remote.addr";
	final static String SHARED_STATE_REMOTE_PORT = "org.argeo.cms.auth.remote.port";

	static void addAuthorization(Subject subject, Authorization authorization) {
		assert subject != null;
		checkSubjectEmpty(subject);
		assert authorization != null;

		// required for display name:
		subject.getPrivateCredentials().add(authorization);

		if (Activator.isSingleUser()) {
			subject.getPrincipals().add(new DataAdminPrincipal());
		}

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
			} else {
				name = new LdapName(authName);
				NodeSecurityUtils.checkUserName(name);
				userPrincipal = new X500Principal(name.toString());
				principals.add(userPrincipal);
				// principals.add(new ImpliedByPrincipal(NodeSecurityUtils.ROLE_USER_NAME,
				// userPrincipal));

				if (Activator.isSingleUser()) {
					principals.add(new ImpliedByPrincipal(NodeSecurityUtils.ROLE_ADMIN_NAME, userPrincipal));
				}
			}

			// Add roles provided by authorization
			for (String role : authorization.getRoles()) {
				LdapName roleName = new LdapName(role);
				if (roleName.equals(name)) {
					// skip
				} else if (roleName.equals(NodeSecurityUtils.ROLE_ANONYMOUS_NAME)) {
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

		// registerSessionAuthorization(request, subject, authorization, locale);
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
		subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
		subject.getPrincipals().removeAll(subject.getPrincipals(DataAdminPrincipal.class));

		subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(CmsSessionId.class));
		subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(Authorization.class));
		// Jackrabbit
		// subject.getPrincipals().removeAll(subject.getPrincipals(AdminPrincipal.class));
		// subject.getPrincipals().removeAll(subject.getPrincipals(AnonymousPrincipal.class));
	}

	synchronized static void registerSessionAuthorization(HttpServletRequest request, Subject subject,
			Authorization authorization, Locale locale) {
		// synchronized in order to avoid multiple registrations
		// TODO move it to a service in order to avoid static synchronization
		if (request != null) {
			HttpSession httpSession = request.getSession(false);
			assert httpSession != null;
			String httpSessId = httpSession.getId();
			String remoteUser = authorization.getName() != null ? authorization.getName()
					: NodeConstants.ROLE_ANONYMOUS;
			request.setAttribute(HttpContext.REMOTE_USER, remoteUser);
			request.setAttribute(HttpContext.AUTHORIZATION, authorization);

			CmsSessionImpl cmsSession = CmsSessionImpl.getByLocalId(httpSessId);
			if (cmsSession != null) {
				if (authorization.getName() != null) {
					if (cmsSession.getAuthorization().getName() == null) {
						cmsSession.close();
						cmsSession = null;
					} else if (!authorization.getName().equals(cmsSession.getAuthorization().getName())) {
						throw new CmsException("Inconsistent user " + authorization.getName()
								+ " for existing CMS session " + cmsSession);
					}
					// keyring
					if (cmsSession != null)
						subject.getPrivateCredentials().addAll(cmsSession.getSecretKeys());
				} else {// anonymous
					if (cmsSession.getAuthorization().getName() != null) {
						cmsSession.close();
						// TODO rather throw an exception ? log a warning ?
						cmsSession = null;
					}
				}
			} else if (cmsSession == null) {
				cmsSession = new WebCmsSessionImpl(subject, authorization, locale, request);
			}
			// request.setAttribute(CmsSession.class.getName(), cmsSession);
			if (cmsSession != null) {
				CmsSessionId nodeSessionId = new CmsSessionId(cmsSession.getUuid());
				if (subject.getPrivateCredentials(CmsSessionId.class).size() == 0)
					subject.getPrivateCredentials().add(nodeSessionId);
				else {
					UUID storedSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next()
							.getUuid();
					// if (storedSessionId.equals(httpSessionId.getValue()))
					throw new CmsException(
							"Subject already logged with session " + storedSessionId + " (not " + nodeSessionId + ")");
				}
			}
		} else {
			// TODO desktop, CLI
		}
	}

	public static CmsSession cmsSessionFromHttpSession(BundleContext bc, String httpSessionId) {
		Authorization authorization = null;
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class,
					"(" + CmsSession.SESSION_LOCAL_ID + "=" + httpSessionId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot get CMS session for id " + httpSessionId, e);
		}
		CmsSession cmsSession;
		if (sr.size() == 1) {
			cmsSession = bc.getService(sr.iterator().next());
//			locale = cmsSession.getLocale();
			authorization = cmsSession.getAuthorization();
			if (authorization.getName() == null)
				return null;// anonymous is not sufficient
		} else if (sr.size() == 0)
			return null;
		else
			throw new CmsException(sr.size() + ">1 web sessions detected for http session " + httpSessionId);
		return cmsSession;
	}

	public static <T extends Principal> T getSinglePrincipal(Subject subject, Class<T> clss) {
		Set<T> principals = subject.getPrincipals(clss);
		if (principals.isEmpty())
			return null;
		if (principals.size() > 1)
			throw new IllegalStateException("Only one " + clss + " principal expected in " + subject);
		return principals.iterator().next();
	}

	private CmsAuthUtils() {

	}

}
