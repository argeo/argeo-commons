package org.argeo.cms.auth;

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import org.argeo.cms.CmsException;
import org.argeo.osgi.useradmin.IpaUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.UserAdmin;

public class IpaLoginModule implements LoginModule {
	private BundleContext bc;
	private Subject subject;
	private Map<String, Object> sharedState = null;
	private CallbackHandler callbackHandler;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
		this.callbackHandler = callbackHandler;
		try {
			bc = FrameworkUtil.getBundle(IpaLoginModule.class).getBundleContext();
			assert bc != null;
		} catch (Exception e) {
			throw new CmsException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		Authorization authorization = null;
		Set<KerberosPrincipal> kerberosPrincipals = subject.getPrincipals(KerberosPrincipal.class);
		if (kerberosPrincipals.isEmpty()) {
			if(callbackHandler!=null)
				throw new LoginException("Cannot be anonymous if callback handler is set");
			authorization = userAdmin.getAuthorization(null);
		} else {
			KerberosPrincipal kerberosPrincipal = kerberosPrincipals.iterator().next();
			LdapName dn = IpaUtils.kerberosToDn(kerberosPrincipal.getName());
			AuthenticatingUser authenticatingUser = new AuthenticatingUser(dn);
			authorization = Subject.doAs(subject, new PrivilegedAction<Authorization>() {

				@Override
				public Authorization run() {
					Authorization authorization = userAdmin.getAuthorization(authenticatingUser);
					return authorization;
				}

			});
		}
		if (authorization == null)
			return false;
		CmsAuthUtils.addAuthentication(subject, authorization);
		HttpServletRequest request = (HttpServletRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST);
		if (request != null) {
			CmsAuthUtils.registerSessionAuthorization(bc, request, subject, authorization);
		}
		return true;
	}


	@Override
	public boolean abort() throws LoginException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean logout() throws LoginException {
		return CmsAuthUtils.logoutSession(bc, subject);
	}

}
