package org.argeo.cms.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Callback handler populating {@link HttpRequestCallback}s with the provided
 * {@link HttpServletRequest}, and ignoring any other callback.
 */
public class HttpRequestCallbackHandler implements CallbackHandler {
	final private HttpServletRequest request;
	final private HttpServletResponse response;

	public HttpRequestCallbackHandler(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks)
			if (callback instanceof HttpRequestCallback) {
				((HttpRequestCallback) callback).setRequest(request);
				((HttpRequestCallback) callback).setResponse(response);
			} else if (callback instanceof LanguageCallback) {
				((LanguageCallback) callback).setLocale(request.getLocale());
			}
	}

}
