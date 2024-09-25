package org.argeo.cms.http.server;

import java.io.IOException;

import org.argeo.cms.internal.http.PublicCmsAuthenticator;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class CmsAuthenticatorFilter extends Filter {

	@Override
	public String description() {
		return "Argeo CMS Authenticator System";
	}

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		if (!(exchange instanceof AbstractCmsHttpExchange))
			throw new IllegalArgumentException("Only implementations of " + HttpExchange.class.getSimpleName()
					+ " based on " + AbstractCmsHttpExchange.class.getName() + " are supported");
		AbstractCmsHttpExchange httpExchange = (AbstractCmsHttpExchange) exchange;

		HttpContext httpContext = httpExchange.getHttpContext();
		Authenticator authenticator = httpContext.getAuthenticator();
		if (authenticator == null)
			throw new IllegalStateException(
					"Only HTTP context with an " + Authenticator.class.getSimpleName() + " are supported. Use "
							+ PublicCmsAuthenticator.class.getName() + " if you need anonymous access.");
		try {
//			RemoteAuthSession httpSession = new ServletHttpSession(req.getSession());
			// httpExchange.setAttribute(RemoteAuthSession.class.getName(), httpSession);
			if (authenticator != null) {
				Authenticator.Result authenticationResult = authenticator.authenticate(httpExchange);
				if (authenticationResult instanceof Authenticator.Success) {
					HttpPrincipal httpPrincipal = ((Authenticator.Success) authenticationResult).getPrincipal();
					httpExchange.setPrincipal(httpPrincipal);
				} else if (authenticationResult instanceof Authenticator.Retry) {
					httpExchange.sendResponseHeaders((((Authenticator.Retry) authenticationResult).getResponseCode()),
							-1);
//					resp.flushBuffer();
					return;
				} else if (authenticationResult instanceof Authenticator.Failure) {
					httpExchange.sendResponseHeaders(((Authenticator.Failure) authenticationResult).getResponseCode(),
							-1);
//					resp.flushBuffer();
					return;
				} else {
					throw new UnsupportedOperationException(
							"Authentication result " + authenticationResult.getClass().getName() + " is not supported");
				}
			}

			chain.doFilter(httpExchange);
		} finally {
// TODO deal with unexpected failure, in order not to let the authentication in an undefined state
		}
	}

}
