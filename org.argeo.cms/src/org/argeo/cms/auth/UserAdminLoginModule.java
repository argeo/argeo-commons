package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.ArgeoException;
import org.argeo.cms.internal.kernel.Activator;
import org.argeo.eclipse.ui.specific.UiContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule, AuthConstants {
	// private final static Log log =
	// LogFactory.getLog(UserAdminLoginModule.class);
	//
	// private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, Object> sharedState = null;

	private boolean isAnonymous = false;

	// private HttpServletRequest request = null;
	private BundleContext bc;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		try {
			bc = Activator.getBundleContext();
			// this.subject = subject;
			this.callbackHandler = callbackHandler;
			this.sharedState = (Map<String, Object>) sharedState;
			if (options.containsKey("anonymous"))
				isAnonymous = Boolean.parseBoolean(options.get("anonymous").toString());
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		UserAdmin userAdmin = bc.getService(bc.getServiceReference(UserAdmin.class));
		Authorization authorization = null;
		if (isAnonymous) {
			authorization = userAdmin.getAuthorization(null);
		} else {
			// HttpRequestCallback httpCallback = new HttpRequestCallback();
			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback("Password", false);
			LanguageCallback langCallback = new LanguageCallback();
			try {
				callbackHandler.handle(new Callback[] { nameCallback, passwordCallback, langCallback });
			} catch (IOException e) {
				throw new LoginException("Cannot handle callback: " + e.getMessage());
			} catch (ThreadDeath e) {
				throw new ThreadDeathLoginException("Callbackhandler thread died", e);
			} catch (UnsupportedCallbackException e) {
				return false;
			}

			// check http
			// request = httpCallback.getRequest();
			// authorization = checkHttp();

			// i18n
			Locale locale = langCallback.getLocale();
			if (locale == null)
				locale = Locale.getDefault();
			UiContext.setLocale(locale);

			authorization = (Authorization) sharedState.get(SHARED_STATE_AUTHORIZATION);

			if (authorization == null) {
				// create credentials
				final String username = nameCallback.getName();
				if (username == null || username.trim().equals("")) {
					// authorization = userAdmin.getAuthorization(null);
					throw new CredentialNotFoundException("No credentials provided");
				} else {
					char[] password = {};
					if (passwordCallback.getPassword() != null)
						password = passwordCallback.getPassword();
					else
						throw new CredentialNotFoundException("No credentials provided");

					User user = userAdmin.getUser(null, username);
					if (user == null)
						throw new FailedLoginException("Invalid credentials");
					if (!user.hasCredential(null, password))
						throw new FailedLoginException("Invalid credentials");
					// return false;

					// Log and monitor new login
					// if (log.isDebugEnabled())
					// log.debug("Logged in to CMS with username [" + username +
					// "]");

					authorization = userAdmin.getAuthorization(user);
				}
			}
		}
		if (!sharedState.containsKey(SHARED_STATE_AUTHORIZATION))
			sharedState.put(SHARED_STATE_AUTHORIZATION, authorization);
		// subject.getPrivateCredentials().add(authorization);
		return true;
	}

	// private Authorization checkHttp() {
	// Authorization authorization = null;
	// if (request != null) {
	// authorization = (Authorization)
	// request.getAttribute(HttpContext.AUTHORIZATION);
	// if (authorization == null) {
	// String sessionId = request.getSession().getId();
	// authorization = (Authorization)
	// request.getSession().getAttribute(HttpContext.AUTHORIZATION);
	// if (authorization == null) {
	// Collection<ServiceReference<CmsSession>> sr;
	// try {
	// sr = bc.getServiceReferences(CmsSession.class,
	// "(" + CmsSession.CMS_SESSION_ID + "=" + sessionId + ")");
	// } catch (InvalidSyntaxException e) {
	// throw new CmsException("Cannot get CMS session for id " + sessionId, e);
	// }
	// if (sr.size() == 1) {
	// CmsSession cmsSession = bc.getService(sr.iterator().next());
	// authorization = cmsSession.getAuthorization();
	// if (log.isTraceEnabled())
	// log.trace("Retrieved authorization from " + cmsSession);
	// }
	// }
	// }
	// }
	// return authorization;
	// }

	@Override
	public boolean commit() throws LoginException {
		// Authorization authorization =
		// subject.getPrivateCredentials(Authorization.class).iterator().next();
		// if (request != null && authorization.getName() != null) {
		// request.setAttribute(HttpContext.REMOTE_USER,
		// authorization.getName());
		// request.setAttribute(HttpContext.AUTHORIZATION, authorization);
		//
		// HttpSession httpSession = request.getSession();
		// if (httpSession.getAttribute(HttpContext.AUTHORIZATION) == null) {
		//
		// String sessionId = request.getSession().getId();
		// Collection<ServiceReference<CmsSession>> sr;
		// try {
		// sr = bc.getServiceReferences(CmsSession.class,
		// "(" + CmsSession.CMS_SESSION_ID + "=" + sessionId + ")");
		// } catch (InvalidSyntaxException e) {
		// throw new CmsException("Cannot get CMS session for id " + sessionId,
		// e);
		// }
		// CmsSession cmsSession;
		// if (sr.size() == 1) {
		// cmsSession = bc.getService(sr.iterator().next());
		// } else if (sr.size() == 0) {
		// Hashtable<String, String> props = new Hashtable<>();
		// props.put(CmsSession.CMS_DN, authorization.getName());
		// props.put(CmsSession.CMS_SESSION_ID, sessionId);
		// cmsSession = new CmsSessionImpl(sessionId, authorization);
		// bc.registerService(CmsSession.class, cmsSession, props);
		// if (log.isDebugEnabled())
		// log.debug("Initialized " + cmsSession + " for " +
		// authorization.getName());
		// } else
		// throw new CmsException(sr.size() + " CMS sessions registered for " +
		// sessionId);
		// cmsSession.addHttpSession(request);
		// if (log.isTraceEnabled())
		// log.trace("Added " + request.getServletPath() + " to " + cmsSession +
		// " (" + request.getRequestURI()
		// + ")");
		// httpSession.setAttribute(HttpContext.AUTHORIZATION, authorization);
		// }
		// subject.getPrivateCredentials().add(request.getSession());
		// }
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		// cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// Set<HttpSession> httpSession =
		// subject.getPrivateCredentials(HttpSession.class);
		// Iterator<HttpSession> it = httpSession.iterator();
		// while (it.hasNext()) {
		// HttpSession sess = it.next();
		// sess.setAttribute(HttpContext.AUTHORIZATION, null);
		// // sess.setMaxInactiveInterval(1);// invalidate session
		//
		// // TODO log out CMS session
		// }
		// subject.getPrivateCredentials().removeAll(httpSession);
		//
		// cleanUp();
		return true;
	}

	// private void cleanUp() {
	// subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(Authorization.class));
	// subject = null;
	// }

}
