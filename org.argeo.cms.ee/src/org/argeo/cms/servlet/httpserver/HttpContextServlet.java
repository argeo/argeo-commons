package org.argeo.cms.servlet.httpserver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

public class HttpContextServlet extends HttpServlet {
	private static final long serialVersionUID = 2321612280413662738L;

	private final HttpContext httpContext;

	public HttpContextServlet(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try (ServletHttpExchange httpExchange = new ServletHttpExchange(httpContext, req, resp)) {
			Authenticator authenticator = httpContext.getAuthenticator();
			if (authenticator != null) {
				Authenticator.Result authenticationResult = authenticator.authenticate(httpExchange);
				if (authenticationResult instanceof Authenticator.Success) {
					HttpPrincipal httpPrincipal = ((Authenticator.Success) authenticationResult).getPrincipal();
					httpExchange.setPrincipal(httpPrincipal);
				} else if (authenticationResult instanceof Authenticator.Retry) {
					resp.setStatus(((Authenticator.Retry) authenticationResult).getResponseCode());
					return;
				} else if (authenticationResult instanceof Authenticator.Failure) {
					resp.setStatus(((Authenticator.Failure) authenticationResult).getResponseCode());
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
