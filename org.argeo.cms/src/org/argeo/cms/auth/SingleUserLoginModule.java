package org.argeo.cms.auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.security.DataAdminPrincipal;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.naming.LdapAttrs;
import org.argeo.osgi.useradmin.IpaUtils;
import org.osgi.service.useradmin.Authorization;

/** Login module for when the system is owned by a single user. */
public class SingleUserLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(SingleUserLoginModule.class);

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
		if (!sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME))
			sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, username);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		X500Principal principal;
		KerberosPrincipal kerberosPrincipal = CmsAuthUtils.getSinglePrincipal(subject, KerberosPrincipal.class);
		if (kerberosPrincipal != null) {
			LdapName userDn = IpaUtils.kerberosToDn(kerberosPrincipal.getName());
			principal = new X500Principal(userDn.toString());
		} else {
			Object username = sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
			if (username == null)
				throw new LoginException("No username available");
			String hostname;
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				log.warn("Using localhost as hostname", e);
				hostname = "localhost";
			}
			String baseDn = ("." + hostname).replaceAll("\\.", ",dc=");
			principal = new X500Principal(LdapAttrs.uid + "=" + username + baseDn);
		}
		Set<Principal> principals = subject.getPrincipals();
		principals.add(principal);
		principals.add(new ImpliedByPrincipal(NodeConstants.ROLE_ADMIN, principal));
		principals.add(new DataAdminPrincipal());

		HttpServletRequest request = (HttpServletRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST);
		Locale locale = Locale.getDefault();
		if (request != null)
			locale = request.getLocale();
		if (locale == null)
			locale = Locale.getDefault();
		Authorization authorization = new SingleUserAuthorization();
		CmsAuthUtils.addAuthorization(subject, authorization);
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
