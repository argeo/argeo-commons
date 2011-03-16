package org.argeo.security.equinox;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

/** Login module which caches one subject per thread. */
public class SpringLoginModule extends SecurityContextLoginModule {
	private AuthenticationManager authenticationManager;

	private CallbackHandler callbackHandler;

	public SpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		// this.subject.set(subject);
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {
		// thread already logged in
		if (SecurityContextHolder.getContext().getAuthentication() != null)
			return super.login();

		// if (getSubject().getPrincipals(Authentication.class).size() == 1) {
		// registerAuthentication(getSubject()
		// .getPrincipals(Authentication.class).iterator().next());
		// return super.login();
		// } else if (getSubject().getPrincipals(Authentication.class).size() >
		// 1) {
		// throw new LoginException(
		// "Multiple Authentication principals not supported: "
		// + getSubject().getPrincipals(Authentication.class));
		// } else {
		// ask for username and password
		Callback label = new TextOutputCallback(TextOutputCallback.INFORMATION,
				"Required login");
		NameCallback nameCallback = new NameCallback("User");
		PasswordCallback passwordCallback = new PasswordCallback("Password",
				false);
		NameCallback urlCallback = new NameCallback("Site URL");

		if (callbackHandler == null) {
			throw new LoginException("No call back handler available");
			// return false;
		}
		try {
			callbackHandler.handle(new Callback[] { label, nameCallback,
					passwordCallback, urlCallback });
		} catch (Exception e) {
			LoginException le = new LoginException("Callback handling failed");
			le.initCause(e);
			throw le;
		}

		// Set user name and password
		String username = nameCallback.getName();
		String password = "";
		if (passwordCallback.getPassword() != null) {
			password = String.valueOf(passwordCallback.getPassword());
		}
		String url = urlCallback.getName();
		// TODO: set it via system properties
		String workspace = null;

		// UsernamePasswordAuthenticationToken credentials = new
		// UsernamePasswordAuthenticationToken(
		// username, password);
		SiteAuthenticationToken credentials = new SiteAuthenticationToken(
				username, password, url, workspace);

		try {
			Authentication authentication = authenticationManager
					.authenticate(credentials);
			registerAuthentication(authentication);
			boolean res = super.login();
			// if (log.isDebugEnabled())
			// log.debug("User " + username + " logged in");
			return res;
		} catch (BadCredentialsException bce) {
			throw bce;
		} catch (Exception e) {
			LoginException loginException = new LoginException(
					"Bad credentials");
			loginException.initCause(e);
			throw loginException;
		}
		// }
	}

	@Override
	public boolean logout() throws LoginException {
		return super.logout();
	}

	/**
	 * Register an {@link Authentication} in the security context.
	 * 
	 * @param authentication
	 *            has to implement {@link Authentication}.
	 */
	protected void registerAuthentication(Object authentication) {
		SecurityContextHolder.getContext().setAuthentication(
				(Authentication) authentication);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	// protected Subject getSubject() {
	// return subject.get();
	// }

}
