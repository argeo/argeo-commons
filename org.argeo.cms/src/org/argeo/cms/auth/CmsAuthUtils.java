package org.argeo.cms.auth;

import static org.argeo.api.cms.CmsConstants.ROLE_ADMIN;
import static org.argeo.api.cms.CmsConstants.ROLE_ANONYMOUS;
import static org.argeo.api.cms.CmsConstants.ROLE_USER;
import static org.argeo.api.cms.CmsConstants.ROLE_USER_ADMIN;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

//import javax.naming.InvalidNameException;
//import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.AnonymousPrincipal;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.api.cms.DataAdminPrincipal;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.cms.internal.auth.RemoteCmsSessionImpl;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.osgi.useradmin.AuthenticatingUser;
import org.osgi.service.useradmin.Authorization;

/** Centralises security related registrations. */
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

	final static String SINGLE_USER_LOCAL_ID = "single-user";

	private final static List<String> RESERVED_ROLES = Collections
			.unmodifiableList(Arrays.asList(new String[] { ROLE_ADMIN, ROLE_ANONYMOUS, ROLE_USER, ROLE_USER_ADMIN }));

	static void addAuthorization(Subject subject, Authorization authorization) {
		assert subject != null;
		checkSubjectEmpty(subject);
		assert authorization != null;

		// required for display name:
		subject.getPrivateCredentials().add(authorization);

		boolean singleUser = authorization instanceof SingleUserAuthorization;

		Set<Principal> principals = subject.getPrincipals();
//		try {
		String authName = authorization.getName();

		// determine user's principal
//			final LdapName name;
		final Principal userPrincipal;
		if (authName == null) {
//				name = NodeSecurityUtils.ROLE_ANONYMOUS_NAME;
			userPrincipal = new AnonymousPrincipal();
			principals.add(userPrincipal);
		} else {
//				name = new LdapName(authName);
			checkUserName(authName);
			userPrincipal = new X500Principal(authName.toString());
			principals.add(userPrincipal);

			if (singleUser) {
				principals.add(new ImpliedByPrincipal(CmsConstants.ROLE_ADMIN, userPrincipal));
				principals.add(new DataAdminPrincipal());
			}
		}

		// Add roles provided by authorization
		for (String role : authorization.getRoles()) {
//				LdapName roleName = new LdapName(role);
			if (role.equals(authName)) {
				// skip
			} else if (role.equals(CmsConstants.ROLE_ANONYMOUS)) {
				// skip
			} else {
//					NodeSecurityUtils.checkImpliedPrincipalName(role);
				principals.add(new ImpliedByPrincipal(role, userPrincipal));
				if (role.equals(CmsConstants.ROLE_ADMIN))
					principals.add(new DataAdminPrincipal());
			}
		}

//		} catch (InvalidNameException e) {
//			throw new IllegalArgumentException("Cannot commit", e);
//		}
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

	@SuppressWarnings("unused")
	synchronized static void registerSessionAuthorization(RemoteAuthRequest request, Subject subject,
			Authorization authorization, Locale locale) {
		// synchronized in order to avoid multiple registrations
		// TODO move it to a service in order to avoid static synchronization
		if (request != null) {
			RemoteAuthSession httpSession = request.getSession();
			assert httpSession != null;
			String httpSessId = httpSession.getId();
			boolean anonymous = authorization.getName() == null;
			String remoteUser = !anonymous ? authorization.getName() : CmsConstants.ROLE_ANONYMOUS;
			request.setAttribute(RemoteAuthRequest.REMOTE_USER, remoteUser);
			request.setAttribute(RemoteAuthRequest.AUTHORIZATION, authorization);

			CmsSessionImpl cmsSession;
			CmsSessionImpl currentLocalSession = CmsContextImpl.getCmsContext().getCmsSessionByLocalId(httpSessId);
			if (currentLocalSession != null) {
				boolean currentLocalSessionAnonymous = currentLocalSession.getAuthorization().getName() == null;
				if (!anonymous) {
					if (currentLocalSessionAnonymous) {
						currentLocalSession.close();
						// new CMS session
						UUID cmsSessionUuid = CmsContextImpl.getCmsContext().getUuidFactory().timeUUID();
						cmsSession = new RemoteCmsSessionImpl(cmsSessionUuid, subject, authorization, locale, request);
						CmsContextImpl.getCmsContext().registerCmsSession(cmsSession);
					} else if (!authorization.getName().equals(currentLocalSession.getAuthorization().getName())) {
						throw new IllegalStateException("Inconsistent user " + authorization.getName()
								+ " for existing CMS session " + currentLocalSession);
					} else {
						// keep current session
						cmsSession = currentLocalSession;
						// credentials
						// TODO control it more??
						subject.getPrivateCredentials().addAll(cmsSession.getSubject().getPrivateCredentials());
						subject.getPublicCredentials().addAll(cmsSession.getSubject().getPublicCredentials());
					}
				} else {// anonymous
					if (!currentLocalSessionAnonymous) {
						currentLocalSession.close();
						throw new IllegalStateException(
								"Existing CMS session " + currentLocalSession + " was not logged out properly.");
					}
					// keep current session
					cmsSession = currentLocalSession;
				}
			} else {
				// new CMS session
				UUID cmsSessionUuid = CmsContextImpl.getCmsContext().getUuidFactory().timeUUID();
				cmsSession = new RemoteCmsSessionImpl(cmsSessionUuid, subject, authorization, locale, request);
				CmsContextImpl.getCmsContext().registerCmsSession(cmsSession);
			}

			if (cmsSession == null)// should be dead code (cf. SuppressWarning of the method)
				throw new IllegalStateException("CMS session cannot be null");

			CmsSessionId nodeSessionId = new CmsSessionId(cmsSession.getUuid());
			if (subject.getPrivateCredentials(CmsSessionId.class).size() == 0) {
				subject.getPrivateCredentials().add(nodeSessionId);
			} else {
				UUID storedSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next().getUuid();
				if (!storedSessionId.equals(nodeSessionId.getUuid()))
					throw new IllegalStateException(
							"Subject already logged with session " + storedSessionId + " (not " + nodeSessionId + ")");
			}
		} else {
			CmsSessionImpl cmsSession = CmsContextImpl.getCmsContext().getCmsSessionByLocalId(SINGLE_USER_LOCAL_ID);
			if (cmsSession == null) {
				UUID cmsSessionUuid = CmsContextImpl.getCmsContext().getUuidFactory().timeUUID();
				cmsSession = new CmsSessionImpl(cmsSessionUuid, subject, authorization, locale, SINGLE_USER_LOCAL_ID);
				CmsContextImpl.getCmsContext().registerCmsSession(cmsSession);
			}
			CmsSessionId nodeSessionId = new CmsSessionId(cmsSession.getUuid());
			subject.getPrivateCredentials().add(nodeSessionId);
		}
	}

//	public static CmsSessionImpl cmsSessionFromHttpSession(BundleContext bc, String httpSessionId) {
//		Authorization authorization = null;
//		Collection<ServiceReference<CmsSession>> sr;
//		try {
//			sr = bc.getServiceReferences(CmsSession.class,
//					"(" + CmsSession.SESSION_LOCAL_ID + "=" + httpSessionId + ")");
//		} catch (InvalidSyntaxException e) {
//			throw new IllegalArgumentException("Cannot get CMS session for id " + httpSessionId, e);
//		}
//		CmsSessionImpl cmsSession;
//		if (sr.size() == 1) {
//			cmsSession = (CmsSessionImpl) bc.getService(sr.iterator().next());
////			locale = cmsSession.getLocale();
//			authorization = cmsSession.getAuthorization();
//			if (authorization.getName() == null)
//				return null;// anonymous is not sufficient
//		} else if (sr.size() == 0)
//			return null;
//		else
//			throw new IllegalStateException(sr.size() + ">1 web sessions detected for http session " + httpSessionId);
//		return cmsSession;
//	}

	public static <T extends Principal> T getSinglePrincipal(Subject subject, Class<T> clss) {
		Set<T> principals = subject.getPrincipals(clss);
		if (principals.isEmpty())
			return null;
		if (principals.size() > 1)
			throw new IllegalStateException("Only one " + clss + " principal expected in " + subject);
		return principals.iterator().next();
	}

	private static void checkUserName(String name) throws IllegalArgumentException {
		if (RESERVED_ROLES.contains(name))
			throw new IllegalArgumentException(name + " is a reserved name");
	}

	private CmsAuthUtils() {

	}

}
