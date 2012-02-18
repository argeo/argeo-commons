package org.argeo.security.equinox;

import java.util.Map;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.NodeAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.providers.jaas.SecurityContextLoginModule;

/** Login module which caches one subject per thread. */
public class SpringLoginModule extends SecurityContextLoginModule {
	final static String NODE_REPO_URI = "argeo.node.repo.uri";

	private final static Log log = LogFactory.getLog(SpringLoginModule.class);

	private AuthenticationManager authenticationManager;

	private CallbackHandler callbackHandler;

	private Subject subject;

	private Long waitBetweenFailedLoginAttempts = 5 * 1000l;

	private Boolean remote = false;
	private Boolean anonymous = false;

	private String key = null;
	private String anonymousRole = "ROLE_ANONYMOUS";

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

			if (remote && anonymous)
				throw new LoginException(
						"Cannot have a Spring login module which is remote and anonymous");

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

			// deals first with public access since it's simple
			if (anonymous) {
				// TODO integrate with JCR?
				Object principal = UUID.randomUUID().toString();
				GrantedAuthority[] authorities = { new GrantedAuthorityImpl(
						anonymousRole) };
				AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
						key, principal, authorities);
				Authentication auth = authenticationManager
						.authenticate(anonymousToken);
				registerAuthentication(auth);
				return super.login();
			}

			if (callbackHandler == null)
				throw new LoginException("No call back handler available");

			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);
			final String defaultNodeUrl = "http://localhost:7070/org.argeo.jcr.webapp/remoting/node";
			final String defaultSecurityWorkspace = "security";
			NameCallback urlCallback = new NameCallback("Site URL",
					defaultNodeUrl);
			NameCallback securityWorkspaceCallback = new NameCallback(
					"Security Workspace", defaultSecurityWorkspace);

			// handle callbacks
			if (remote)
				callbackHandler.handle(new Callback[] { nameCallback,
						passwordCallback, urlCallback,
						securityWorkspaceCallback });
			else
				callbackHandler.handle(new Callback[] { nameCallback,
						passwordCallback });

			// create credentials
			String username = nameCallback.getName();
			if (username == null || username.trim().equals(""))
				return false;

			String password = "";
			if (passwordCallback.getPassword() != null)
				password = String.valueOf(passwordCallback.getPassword());

			NodeAuthenticationToken credentials;
			if (remote) {
				String url = urlCallback.getName();
				String workspace = securityWorkspaceCallback.getName();
				credentials = new NodeAuthenticationToken(username, password,
						url, workspace);
			} else {
				credentials = new NodeAuthenticationToken(username, password);
			}

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

	/** Authenticates on a remote node */
	public void setRemote(Boolean remote) {
		this.remote = remote;
	}

	/**
	 * Request anonymous authentication (incompatible with remote)
	 */
	public void setAnonymous(Boolean anonymous) {
		this.anonymous = anonymous;
	}

	/** Role identifying an anonymous user */
	public void setAnonymousRole(String anonymousRole) {
		this.anonymousRole = anonymousRole;
	}

	/** System key */
	public void setKey(String key) {
		this.key = key;
	}

}
