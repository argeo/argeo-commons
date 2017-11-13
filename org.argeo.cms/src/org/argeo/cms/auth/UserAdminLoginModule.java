package org.argeo.cms.auth;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
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
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
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

	private List<String> indexedUserProperties = Arrays.asList(
			new String[] { LdapAttrs.DN, LdapAttrs.mail.name(), LdapAttrs.uid.name(), LdapAttrs.authPassword.name() });

	// private state
	private BundleContext bc;
	private User authenticatedUser = null;
	private Locale locale;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		try {
			bc = FrameworkUtil.getBundle(UserAdminLoginModule.class).getBundleContext();
			this.callbackHandler = callbackHandler;
			this.sharedState = (Map<String, Object>) sharedState;
		} catch (Exception e) {
			throw new CmsException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		final String username;
		final char[] password;
		if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_PWD)) {
			// NB: required by Basic http auth
			username = (String) sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
			password = (char[]) sharedState.get(CmsAuthUtils.SHARED_STATE_PWD);
			// // TODO locale?
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
			locale = langCallback.getLocale();
			if (locale == null)
				locale = Locale.getDefault();
			// FIXME add it to Subject
			// UiContext.setLocale(locale);

			username = nameCallback.getName();
			if (username == null || username.trim().equals("")) {
				// authorization = userAdmin.getAuthorization(null);
				throw new CredentialNotFoundException("No credentials provided");
			}
			if (passwordCallback.getPassword() != null)
				password = passwordCallback.getPassword();
			else
				throw new CredentialNotFoundException("No credentials provided");
		}

		// User user = userAdmin.getUser(null, username);
		User user = searchForUser(userAdmin, username);
		if (user == null)
			return true;// expect Kerberos
		// throw new FailedLoginException("Invalid credentials");
		if (!user.hasCredential(null, password))
			return false;
		// throw new FailedLoginException("Invalid credentials");
		authenticatedUser = user;
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		Authorization authorization;
		if (callbackHandler == null) {// anonymous
			authorization = userAdmin.getAuthorization(null);
		} else {
			User authenticatingUser;
			Set<KerberosPrincipal> kerberosPrincipals = subject.getPrincipals(KerberosPrincipal.class);
			if (kerberosPrincipals.isEmpty()) {
				if (authenticatedUser == null) {
					if (log.isTraceEnabled())
						log.trace("Neither kerberos nor user admin login succeeded. Login failed.");
					return false;
				} else {
					authenticatingUser = authenticatedUser;
				}
			} else {
				KerberosPrincipal kerberosPrincipal = kerberosPrincipals.iterator().next();
				LdapName dn = IpaUtils.kerberosToDn(kerberosPrincipal.getName());
				authenticatingUser = new AuthenticatingUser(dn);
				if (authenticatedUser != null && !authenticatingUser.getName().equals(authenticatedUser.getName()))
					throw new LoginException("Kerberos login " + authenticatingUser.getName()
							+ " is inconsistent with user admin login " + authenticatedUser.getName());
			}
			authorization = Subject.doAs(subject, new PrivilegedAction<Authorization>() {

				@Override
				public Authorization run() {
					Authorization authorization = userAdmin.getAuthorization(authenticatingUser);
					return authorization;
				}

			});
			if (authorization == null)
				throw new LoginException(
						"User admin found no authorization for authenticated user " + authenticatingUser.getName());
		}
		// Log and monitor new login
		CmsAuthUtils.addAuthorization(subject, authorization, locale,
				(HttpServletRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST));
		if (log.isDebugEnabled())
			log.debug("Logged in to CMS: " + subject);
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (log.isTraceEnabled())
			log.trace("Logging out from CMS... " + subject);
		// boolean httpSessionLogoutOk = CmsAuthUtils.logoutSession(bc,
		// subject);
		CmsAuthUtils.cleanUp(subject);
		return true;
	}

	protected User searchForUser(UserAdmin userAdmin, String providedUsername) {
		try {
			// TODO check value null or empty
			Set<User> collectedUsers = new HashSet<>();
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
				return collectedUsers.iterator().next();
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
