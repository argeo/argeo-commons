package org.argeo.cms.auth;

import static org.argeo.naming.LdapAttrs.cn;

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
import org.argeo.api.NodeConstants;
import org.argeo.api.security.CryptoKeyring;
import org.argeo.cms.internal.kernel.Activator;
import org.argeo.naming.LdapAttrs;
import org.argeo.osgi.useradmin.AuthenticatingUser;
import org.argeo.osgi.useradmin.IpaUtils;
import org.argeo.osgi.useradmin.OsUserUtils;
import org.argeo.osgi.useradmin.TokenUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Use the {@link UserAdmin} in the OSGi registry as the basis for
 * authentication.
 */
public class UserAdminLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(UserAdminLoginModule.class);

	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, Object> sharedState = null;

	private List<String> indexedUserProperties = Arrays
			.asList(new String[] { LdapAttrs.mail.name(), LdapAttrs.uid.name(), LdapAttrs.authPassword.name() });

	// private state
	private BundleContext bc;
	private User authenticatedUser = null;
	private Locale locale;

	private Authorization bindAuthorization = null;

	private boolean singleUser = Activator.isSingleUser();

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
			throw new IllegalStateException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		UserAdmin userAdmin = Activator.getUserAdmin();
		final String username;
		final char[] password;
		Object certificateChain = null;
		boolean preauth = false;
		if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_PWD)) {
			// NB: required by Basic http auth
			username = (String) sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
			password = (char[]) sharedState.get(CmsAuthUtils.SHARED_STATE_PWD);
			// // TODO locale?
		} else if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN)) {
			String certDn = (String) sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
//			LdapName ldapName;
//			try {
//				ldapName = new LdapName(certificateName);
//			} catch (InvalidNameException e) {
//				e.printStackTrace();
//				return false;
//			}
//			username = ldapName.getRdn(ldapName.size() - 1).getValue().toString();
			username = certDn;
			certificateChain = sharedState.get(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN);
			password = null;
		} else if (sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_REMOTE_ADDR)
				&& sharedState.containsKey(CmsAuthUtils.SHARED_STATE_REMOTE_PORT)) {// ident
			username = (String) sharedState.get(CmsAuthUtils.SHARED_STATE_NAME);
			password = null;
			preauth = true;
		} else if (singleUser) {
			username = OsUserUtils.getOsUsername();
			password = null;
			// TODO retrieve from http session
			locale = Locale.getDefault();
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
			// Locale.setDefault(locale);

			username = nameCallback.getName();
			if (username == null || username.trim().equals("")) {
				// authorization = userAdmin.getAuthorization(null);
				throw new CredentialNotFoundException("No credentials provided");
			}
			if (passwordCallback.getPassword() != null)
				password = passwordCallback.getPassword();
			else
				throw new CredentialNotFoundException("No credentials provided");
			sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, username);
			sharedState.put(CmsAuthUtils.SHARED_STATE_PWD, password);
		}
		User user = searchForUser(userAdmin, username);

		// Tokens
		if (user == null) {
			String token = username;
			Group tokenGroup = searchForToken(userAdmin, token);
			if (tokenGroup != null) {
				Authorization tokenAuthorization = getAuthorizationFromToken(userAdmin, tokenGroup);
				if (tokenAuthorization != null) {
					bindAuthorization = tokenAuthorization;
					authenticatedUser = (User) userAdmin.getRole(bindAuthorization.getName());
					return true;
				}
			}
		}

		if (user == null)
			return true;// expect Kerberos

		if (password != null) {
			// try bind first
			try {
				AuthenticatingUser authenticatingUser = new AuthenticatingUser(user.getName(), password);
				bindAuthorization = userAdmin.getAuthorization(authenticatingUser);
				// TODO check tokens as well
				if (bindAuthorization != null) {
					authenticatedUser = user;
					return true;
				}
			} catch (Exception e) {
				// silent
				if (log.isTraceEnabled())
					log.trace("Bind failed", e);
			}

			// works only if a connection password is provided
			if (!user.hasCredential(null, password)) {
				return false;
			}
		} else if (certificateChain != null) {
			// TODO check CRLs/OSCP validity?
			// NB: authorization in commit() will work only if an LDAP connection password
			// is provided
		} else if (singleUser) {
			// TODO verify IP address?
		} else if (preauth) {
			// ident
		} else {
			throw new CredentialNotFoundException("No credentials provided");
		}

		authenticatedUser = user;
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		if (locale != null)
			subject.getPublicCredentials().add(locale);

		if (singleUser) {
			OsUserUtils.loginAsSystemUser(subject);
		}
		UserAdmin userAdmin = Activator.getUserAdmin();
		Authorization authorization;
		if (callbackHandler == null) {// anonymous
			authorization = userAdmin.getAuthorization(null);
		} else if (bindAuthorization != null) {// bind
			authorization = bindAuthorization;
		} else {// Kerberos
			User authenticatingUser;
			Set<KerberosPrincipal> kerberosPrincipals = subject.getPrincipals(KerberosPrincipal.class);
			if (kerberosPrincipals.isEmpty()) {
				if (authenticatedUser == null) {
					if (log.isTraceEnabled())
						log.trace("Neither kerberos nor user admin login succeeded. Login failed.");
					throw new CredentialNotFoundException("Bad credentials.");
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
		HttpServletRequest request = (HttpServletRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST);
		CmsAuthUtils.addAuthorization(subject, authorization);

		// Unlock keyring (underlying login to the JCR repository)
		char[] password = (char[]) sharedState.get(CmsAuthUtils.SHARED_STATE_PWD);
		if (password != null) {
			ServiceReference<CryptoKeyring> keyringSr = bc.getServiceReference(CryptoKeyring.class);
			if (keyringSr != null) {
				CryptoKeyring keyring = bc.getService(keyringSr);
				Subject.doAs(subject, new PrivilegedAction<Void>() {

					@Override
					public Void run() {
						try {
							keyring.unlock(password);
						} catch (Exception e) {
							e.printStackTrace();
							log.warn("Could not unlock keyring with the password provided by " + authorization.getName()
									+ ": " + e.getMessage());
						}
						return null;
					}

				});
			}
		}

		// Register CmsSession with initial subject
		CmsAuthUtils.registerSessionAuthorization(request, subject, authorization, locale);

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
		CmsAuthUtils.cleanUp(subject);
		return true;
	}

	protected User searchForUser(UserAdmin userAdmin, String providedUsername) {
		try {
			// TODO check value null or empty
			Set<User> collectedUsers = new HashSet<>();
			// try dn
			User user = null;
			// try all indexes
			for (String attr : indexedUserProperties) {
				user = userAdmin.getUser(attr, providedUsername);
				if (user != null)
					collectedUsers.add(user);
			}
			if (collectedUsers.size() == 1) {
				user = collectedUsers.iterator().next();
				return user;
			} else if (collectedUsers.size() > 1) {
				log.warn(collectedUsers.size() + " users for provided username" + providedUsername);
			}
			// try DN as a last resort
			try {
				user = (User) userAdmin.getRole(providedUsername);
				if (user != null)
					return user;
			} catch (Exception e) {
				// silent
			}
			return null;
		} catch (Exception e) {
			if (log.isTraceEnabled())
				log.warn("Cannot search for user " + providedUsername, e);
			return null;
		}

	}

	protected Group searchForToken(UserAdmin userAdmin, String token) {
		String dn = cn + "=" + token + "," + NodeConstants.TOKENS_BASEDN;
		Group tokenGroup = (Group) userAdmin.getRole(dn);
		return tokenGroup;
	}

	protected Authorization getAuthorizationFromToken(UserAdmin userAdmin, Group tokenGroup) {
		if (TokenUtils.isExpired(tokenGroup))
			return null;
//		String expiryDateStr = (String) tokenGroup.getProperties().get(description.name());
//		if (expiryDateStr != null) {
//			Instant expiryDate = NamingUtils.ldapDateToInstant(expiryDateStr);
//			if (expiryDate.isBefore(Instant.now())) {
//				if (log.isDebugEnabled())
//					log.debug("Token " + tokenGroup.getName() + " has expired.");
//				return null;
//			}
//		}
		String userDn = TokenUtils.userDn(tokenGroup);
		User user = (User) userAdmin.getRole(userDn);
		Authorization auth = userAdmin.getAuthorization(user);
		return auth;
	}
}
