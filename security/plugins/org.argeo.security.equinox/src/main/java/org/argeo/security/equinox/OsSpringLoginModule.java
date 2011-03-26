package org.argeo.security.equinox;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.argeo.security.OsAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

/** Login module which caches one subject per thread. */
public class OsSpringLoginModule extends SecurityContextLoginModule {
	// private final static Log log =
	// LogFactory.getLog(OsSpringLoginModule.class);

	private AuthenticationManager authenticationManager;

	private Subject subject;

	public OsSpringLoginModule() {

	}

	@SuppressWarnings("rawtypes")
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map sharedState, Map options) {
		super.initialize(subject, callbackHandler, sharedState, options);
		this.subject = subject;
	}

	public boolean login() throws LoginException {
		// thread already logged in
		if (SecurityContextHolder.getContext().getAuthentication() != null)
			return super.login();

		OsAuthenticationToken oat = new OsAuthenticationToken();
		Authentication authentication = authenticationManager.authenticate(oat);
		registerAuthentication(authentication);
		return super.login();
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
