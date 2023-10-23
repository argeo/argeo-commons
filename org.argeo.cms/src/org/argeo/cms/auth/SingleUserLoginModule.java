package org.argeo.cms.auth;

import java.util.Locale;
import java.util.Map;

import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.cms.directory.ldap.IpaUtils;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.cms.osgi.useradmin.OsUserUtils;
import org.osgi.service.useradmin.Authorization;

/** Login module for when the system is owned by a single user. */
public class SingleUserLoginModule implements LoginModule {
//	private final static CmsLog log = CmsLog.getLog(SingleUserLoginModule.class);

	private Subject subject;
	private Map<String, Object> sharedState = null;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		String username = System.getProperty("user.name");
		if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_OS_USERNAME)
				&& !username.equals(sharedState.get(CmsAuthUtils.SHARED_STATE_OS_USERNAME)))
			throw new CredentialException(
					"OS username already set with " + sharedState.get(CmsAuthUtils.SHARED_STATE_OS_USERNAME));
		if (!sharedState.containsKey(CmsAuthUtils.SHARED_STATE_OS_USERNAME))
			sharedState.put(CmsAuthUtils.SHARED_STATE_OS_USERNAME, username);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		String authorizationName;
		KerberosPrincipal kerberosPrincipal = CmsAuthUtils.getSinglePrincipal(subject, KerberosPrincipal.class);
		if (kerberosPrincipal != null) {
			LdapName userDn = IpaUtils.kerberosToDn(kerberosPrincipal.getName());
			X500Principal principal = new X500Principal(userDn.toString());
			authorizationName = principal.getName();
		} else {
			Object username = sharedState.get(CmsAuthUtils.SHARED_STATE_OS_USERNAME);
			if (username == null)
				throw new LoginException("No username available");
			String hostname = CmsContextImpl.getCmsContext().getCmsState().getHostname();
			String baseDn = ("." + hostname).replaceAll("\\.", ",dc=");
			X500Principal principal = new X500Principal(LdapAttr.uid + "=" + username + baseDn);
			authorizationName = principal.getName();
		}

		RemoteAuthRequest request = (RemoteAuthRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST);
		Locale locale = Locale.getDefault();
		if (request != null)
			locale = request.getLocale();
		if (locale == null)
			locale = Locale.getDefault();

		Authorization authorization = null;
		if (kerberosPrincipal != null) {
			authorization = new SingleUserAuthorization(authorizationName);
			CmsAuthUtils.addAuthorization(subject, authorization);
		} else {
			// next step with user admin will properly populate
		}

		// Add standard Java OS login
		OsUserUtils.loginAsSystemUser(subject);

		// additional principals (must be after Authorization registration)
//		Set<Principal> principals = subject.getPrincipals();
//		principals.add(principal);
//		principals.add(new ImpliedByPrincipal(NodeConstants.ROLE_ADMIN, principal));
//		principals.add(new DataAdminPrincipal());

		if (authorization != null)
			CmsAuthUtils.registerSessionAuthorization(request, subject, authorization, locale);

		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		CmsAuthUtils.cleanUp(subject);
		return true;
	}

}
