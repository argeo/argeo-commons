package org.argeo.cms.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Callback handler populating {@link RemoteAuthCallback}s with the provided
 * {@link HttpServletRequest}, and ignoring any other callback.
 */
public class RemoteAuthCallbackHandler implements CallbackHandler {
	final private RemoteAuthRequest request;
	final private RemoteAuthResponse response;
	final private RemoteAuthSession httpSession;

	public RemoteAuthCallbackHandler(RemoteAuthRequest request, RemoteAuthResponse response) {
		this.request = request;
		this.httpSession = request.getSession();
		this.response = response;
	}

	public RemoteAuthCallbackHandler(RemoteAuthSession httpSession) {
		this.httpSession = httpSession;
		this.request = null;
		this.response = null;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks)
			if (callback instanceof RemoteAuthCallback) {
				((RemoteAuthCallback) callback).setRequest(request);
				((RemoteAuthCallback) callback).setResponse(response);
				((RemoteAuthCallback) callback).setHttpSession(httpSession);
			} else if (callback instanceof LanguageCallback) {
				((LanguageCallback) callback).setLocale(request.getLocale());
			}
	}

}
