package org.argeo.cms.auth;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.naming.LdapAttrs;
import org.argeo.osgi.useradmin.IpaUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(UserAdminLoginModule.class);

	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, Object> sharedState = null;

	// private boolean isAnonymous = false;
	private List<String> indexedUserProperties = Arrays
			.asList(new String[] { LdapAttrs.uid.name(), LdapAttrs.mail.name(), LdapAttrs.cn.name() });

	// private state
	private BundleContext bc;
	// private Authorization authorization;
	private User authenticatedUser = null;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		try {
			bc = FrameworkUtil.getBundle(UserAdminLoginModule.class).getBundleContext();
			assert bc != null;
			// this.subject = subject;
			this.callbackHandler = callbackHandler;
			this.sharedState = (Map<String, Object>) sharedState;
			// if (options.containsKey("anonymous"))
			// isAnonymous =
			// Boolean.parseBoolean(options.get("anonymous").toString());
		} catch (Exception e) {
			throw new CmsException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		Authorization sharedAuth = (Authorization) sharedState.get(CmsAuthUtils.SHARED_STATE_AUTHORIZATION);
		if (sharedAuth != null) {
			if (callbackHandler == null && sharedAuth.getName() != null)
				throw new LoginException("Shared authorization should be anonymous");
			return false;
		}
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		if (callbackHandler == null) {// anonymous
//			authorization = userAdmin.getAuthorization(null);
//			sharedState.put(CmsAuthUtils.SHARED_STATE_AUTHORIZATION, authorization);
			return true;
		}

		final String username;
		final char[] password;
		if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_PWD)) {
			username = (String) sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
			password = (char[]) sharedState.get(CmsAuthUtils.SHARED_STATE_PWD);
			// // TODO locale?
			// // NB: raw user name is used
			// AuthenticatingUser authenticatingUser = new
			// AuthenticatingUser(username, password);
			// authorization = userAdmin.getAuthorization(authenticatingUser);
		} else {
			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback("Password", false);
			LanguageCallback langCallback = new LanguageCallback();
			try {
				callbackHandler.handle(new Callback[] { nameCallback, passwordCallback, langCallback });
			} catch (IOException e) {
				throw new LoginException("Cannot handle callback: " + e.getMessage());
			} catch (UnsupportedCallbackException e) {
				return false;
			}

			// i18n
			Locale locale = langCallback.getLocale();
			if (locale == null)
				locale = Locale.getDefault();
			UiContext.setLocale(locale);

			username = nameCallback.getName();
			if (username == null || username.trim().equals("")) {
				// authorization = userAdmin.getAuthorization(null);
				throw new CredentialNotFoundException("No credentials provided");
			}
			if (passwordCallback.getPassword() != null)
				password = passwordCallback.getPassword();
			else
				throw new CredentialNotFoundException("No credentials provided");
			// FIXME move Argeo specific convention from user admin to here
		}

		// User user = userAdmin.getUser(null, username);
		User user = searchForUser(userAdmin, username);
		if (user == null)
			return true;// expect Kerberos
		// throw new FailedLoginException("Invalid credentials");
		if (!user.hasCredential(null, password))
			throw new FailedLoginException("Invalid credentials");
		authenticatedUser = user;
		// return false;

		// Log and monitor new login
		// if (log.isDebugEnabled())
		// log.debug("Logged in to CMS with username [" + username +
		// "]");

		// authorization = userAdmin.getAuthorization(user);
		// assert authorization != null;
		//
		// sharedState.put(CmsAuthUtils.SHARED_STATE_AUTHORIZATION,
		// authorization);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		// if (authorization == null) {
		// return false;
		// // throw new LoginException("Authorization should not be null");
		// } else {
		// CmsAuthUtils.addAuthentication(subject, authorization);
		// return true;
		// }
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		Authorization authorization = null;
		User authenticatingUser;
		Set<KerberosPrincipal> kerberosPrincipals = subject.getPrincipals(KerberosPrincipal.class);
		if (kerberosPrincipals.isEmpty()) {
			if (callbackHandler == null) {
				authorization = userAdmin.getAuthorization(null);
			}
			if (authenticatedUser == null) {
				return false;
			} else {
				authenticatingUser = authenticatedUser;
			}
		} else {
			KerberosPrincipal kerberosPrincipal = kerberosPrincipals.iterator().next();
			LdapName dn = IpaUtils.kerberosToDn(kerberosPrincipal.getName());
			authenticatingUser = new AuthenticatingUser(dn);
		}
		if (authorization == null)
			authorization = Subject.doAs(subject, new PrivilegedAction<Authorization>() {

				@Override
				public Authorization run() {
					Authorization authorization = userAdmin.getAuthorization(authenticatingUser);
					return authorization;
				}

			});
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
//		authorization = null;
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		CmsAuthUtils.cleanUp(subject);
		return true;
	}

	protected User searchForUser(UserAdmin userAdmin, String providedUsername) {
		try {
			// TODO check value null or empty
			List<User> collectedUsers = new ArrayList<User>();
			// try dn
			User user = null;
			try {
				user = (User) userAdmin.getRole(providedUsername);
				if (user != null)
					collectedUsers.add(user);
			} catch (Exception e) {
				// silent
			}
			// try all indexes
			for (String attr : indexedUserProperties) {
				user = userAdmin.getUser(attr, providedUsername);
				if (user != null)
					collectedUsers.add(user);
			}
			if (collectedUsers.size() == 1)
				return collectedUsers.get(0);
			else if (collectedUsers.size() > 1)
				log.warn(collectedUsers.size() + " users for provided username" + providedUsername);
			return null;
		} catch (Exception e) {
			if (log.isTraceEnabled())
				log.warn("Cannot search for user " + providedUsername, e);
			return null;
		}

	}
}
