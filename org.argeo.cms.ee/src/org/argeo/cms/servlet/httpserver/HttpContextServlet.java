package org.argeo.cms.servlet.httpserver;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.servlet.ServletHttpSession;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * An {@link HttpServlet} which integrates an {@link HttpContext} and its
 * {@link Authenticator} in a servlet container.
 */
public class HttpContextServlet extends HttpServlet {
	private static final long serialVersionUID = 2321612280413662738L;

	private final HttpContext httpContext;

	public HttpContextServlet(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (ServletHttpExchange httpExchange = new ServletHttpExchange(httpContext, req, resp)) {
			ServletHttpSession httpSession = new ServletHttpSession(req.getSession());
			httpExchange.setAttribute(RemoteAuthSession.class.getName(), httpSession);
			Authenticator authenticator = httpContext.getAuthenticator();
			if (authenticator != null) {
				Authenticator.Result authenticationResult = authenticator.authenticate(httpExchange);
				if (authenticationResult instanceof Authenticator.Success) {
					HttpPrincipal httpPrincipal = ((Authenticator.Success) authenticationResult).getPrincipal();
					httpExchange.setPrincipal(httpPrincipal);
				} else if (authenticationResult instanceof Authenticator.Retry) {
					httpExchange.sendResponseHeaders((((Authenticator.Retry) authenticationResult).getResponseCode()),
							-1);
					resp.flushBuffer();
					return;
				} else if (authenticationResult instanceof Authenticator.Failure) {
					httpExchange.sendResponseHeaders(((Authenticator.Failure) authenticationResult).getResponseCode(),
							-1);
					resp.flushBuffer();
					return;
				} else {
					throw new UnsupportedOperationException(
							"Authentication result " + authenticationResult.getClass().getName() + " is not supported");
				}
			}

			HttpHandler httpHandler = httpContext.getHandler();
			httpHandler.handle(httpExchange);
		}
	}
}
