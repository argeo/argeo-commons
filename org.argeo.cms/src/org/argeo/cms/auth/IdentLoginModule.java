package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.internal.kernel.Activator;
import org.argeo.ident.IdentClient;

public class IdentLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(IdentLoginModule.class);

	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map<String, Object> sharedState = null;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		if (callbackHandler == null)
			return false;
		HttpRequestCallback httpCallback = new HttpRequestCallback();
		try {
			callbackHandler.handle(new Callback[] { httpCallback });
		} catch (IOException e) {
			throw new LoginException("Cannot handle http callback: " + e.getMessage());
		} catch (UnsupportedCallbackException e) {
			return false;
		}
		HttpServletRequest request = httpCallback.getRequest();
		IdentClient identClient = Activator.getIdentClient(request.getRemoteAddr());
		if (identClient == null)
			return false;
		String identUsername;
		try {
			identUsername = identClient.getUsername(request.getLocalPort(), request.getRemotePort());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (identUsername != null) {
			if (log.isDebugEnabled())
				log.debug("Ident username: " + identUsername + " (local port: " + request.getLocalPort()
						+ ", remote port: " + request.getRemotePort() + ")");
			sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, identUsername);
			sharedState.put(CmsAuthUtils.SHARED_STATE_REMOTE_ADDR, request.getRemoteAddr());
			sharedState.put(CmsAuthUtils.SHARED_STATE_REMOTE_PORT, request.getRemotePort());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean commit() throws LoginException {
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		return true;
	}

}
