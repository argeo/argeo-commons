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
import org.argeo.security.SiteAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

/** Login module which caches one subject per thread. */
public class SpringLoginModule extends SecurityContextLoginModule {
	private final static Log log = LogFactory.getLog(SpringLoginModule.class);

	private AuthenticationManager authenticationManager;

	private CallbackHandler callbackHandler;

	private Subject subject;
	
	private Long waitBetweenFailedLoginAttempts = 5*1000l;

	public SpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		this.callbackHandler = callbackHandler;
		this.subject = subject;
	}

	public boolean login() throws LoginException {
		try {
			// thread already logged in
			if (SecurityContextHolder.getContext().getAuthentication() != null)
				return super.login();

			// reset all principals and credentials
			if (log.isTraceEnabled())
				log.trace("Resetting all principals and credentials of "
						+ subject);
			if (subject.getPrincipals() != null)
				subject.getPrincipals().clear();
			if (subject.getPrivateCredentials() != null)
				subject.getPrivateCredentials().clear();
			if (subject.getPublicCredentials() != null)
				subject.getPublicCredentials().clear();

			// ask for username and password
			Callback label = new TextOutputCallback(
					TextOutputCallback.INFORMATION, "Required login");
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);

			// NameCallback urlCallback = new NameCallback("Site URL");

			if (callbackHandler == null)
				throw new LoginException("No call back handler available");
			callbackHandler.handle(new Callback[] { label, nameCallback,
					passwordCallback });

			// Set user name and password
			String username = nameCallback.getName();
			if (username == null || username.trim().equals(""))
				return false;

			String password = "";
			if (passwordCallback.getPassword() != null)
				password = String.valueOf(passwordCallback.getPassword());

			// String url = urlCallback.getName();
			// TODO: set it via system properties
			String workspace = null;

			SiteAuthenticationToken credentials = new SiteAuthenticationToken(
					username, password, null, workspace);

			Authentication authentication;
			try {
				authentication = authenticationManager
						.authenticate(credentials);
			} catch (BadCredentialsException e) {
				// wait between failed login attempts
				Thread.sleep(waitBetweenFailedLoginAttempts);
				throw e;
			}
			registerAuthentication(authentication);
			boolean res = super.login();
			return res;
		} catch (LoginException e) {
			throw e;
		} catch (ThreadDeath e) {
			LoginException le = new LoginException(
					"Spring Security login thread died");
			le.initCause(e);
			throw le;
		} catch (Exception e) {
			LoginException le = new LoginException(
					"Spring Security login failed");
			le.initCause(e);
			throw le;
		}
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().clear();
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
