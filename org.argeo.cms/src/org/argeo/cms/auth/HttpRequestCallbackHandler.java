package org.argeo.cms.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Callback handler populating {@link HttpRequestCallback}s with the provided
 * {@link HttpServletRequest}, and ignoring any other callback.
 */
public class HttpRequestCallbackHandler implements CallbackHandler {
	final private HttpRequest request;
	final private HttpResponse response;
	final private HttpSession httpSession;

	public HttpRequestCallbackHandler(HttpRequest request, HttpResponse response) {
		this.request = request;
		this.httpSession = request.getSession();
		this.response = response;
	}

	public HttpRequestCallbackHandler(HttpSession httpSession) {
		this.httpSession = httpSession;
		this.request = null;
		this.response = null;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks)
			if (callback instanceof HttpRequestCallback) {
				((HttpRequestCallback) callback).setRequest(request);
				((HttpRequestCallback) callback).setResponse(response);
				((HttpRequestCallback) callback).setHttpSession(httpSession);
			} else if (callback instanceof LanguageCallback) {
				((LanguageCallback) callback).setLocale(request.getLocale());
			}
	}

}
