package org.argeo.security.equinox;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

public class SpringLoginModule extends SecurityContextLoginModule {
	private final static Log log = LogFactory.getLog(SpringLoginModule.class);

	private AuthenticationManager authenticationManager;
	private Subject subject;

	private CallbackHandler callbackHandler;

	public SpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

	public boolean login() throws LoginException {
		// thread already logged in
		if (SecurityContextHolder.getContext().getAuthentication() != null)
			return super.login();

		if (subject.getPrincipals(Authentication.class).size() == 1) {
			registerAuthentication(subject.getPrincipals(Authentication.class)
					.iterator().next());
			return super.login();
		} else if (subject.getPrincipals(Authentication.class).size() > 1) {
			throw new LoginException(
					"Multiple Authentication principals not supported: "
							+ subject.getPrincipals(Authentication.class));
		} else {
			// ask for username and password
			Callback label = new TextOutputCallback(
					TextOutputCallback.INFORMATION, "Required login");
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);

			if (callbackHandler == null) {
				throw new LoginException("No call back handler available");
				// return false;
			}
			try {
				callbackHandler.handle(new Callback[] { label, nameCallback,
						passwordCallback });
			} catch (Exception e) {
				LoginException le = new LoginException(
						"Callback handling failed");
				le.initCause(e);
				throw le;
			}

			// Set user name and password
			String username = nameCallback.getName();
			String password = "";
			if (passwordCallback.getPassword() != null) {
				password = String.valueOf(passwordCallback.getPassword());
			}
			UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(
					username, password);

			try {
				Authentication authentication = authenticationManager
						.authenticate(credentials);
				registerAuthentication(authentication);
				return super.login();
			} catch (Exception e) {
				LoginException loginException = new LoginException(
						"Bad credentials");
				loginException.initCause(e);
				throw loginException;
			}
		}
	}

	@Override
	public boolean logout() throws LoginException {
		if (log.isDebugEnabled())
			log.debug("Log out "
					+ subject.getPrincipals().iterator().next().getName());
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

}
