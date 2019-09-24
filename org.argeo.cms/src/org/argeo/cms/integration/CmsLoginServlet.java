package org.argeo.cms.integration;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.auth.CmsSessionId;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.node.NodeConstants;
import org.osgi.service.useradmin.Authorization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

/** Externally authenticate an http session. */
public class CmsLoginServlet extends HttpServlet {
	private static final long serialVersionUID = 2478080654328751539L;
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		LoginContext lc = null;
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		if (username != null && password != null) {
			try {
				lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
						new HttpRequestCallbackHandler(request, response) {
							public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
								for (Callback callback : callbacks) {
									if (callback instanceof NameCallback && username != null)
										((NameCallback) callback).setName(username);
									else if (callback instanceof PasswordCallback && password != null)
										((PasswordCallback) callback).setPassword(password.toCharArray());
									else if (callback instanceof HttpRequestCallback) {
										((HttpRequestCallback) callback).setRequest(request);
										((HttpRequestCallback) callback).setResponse(response);
									}
								}
							}
						});
				lc.login();

				CmsSessionId cmsSessionId = (CmsSessionId) lc.getSubject().getPrivateCredentials(CmsSessionId.class)
						.toArray()[0];
				Authorization authorization = (Authorization) lc.getSubject().getPrivateCredentials(Authorization.class)
						.toArray()[0];

				JsonWriter jsonWriter = gson.newJsonWriter(response.getWriter());
				jsonWriter.beginObject();
				// Authorization
				jsonWriter.name("username").value(authorization.getName());
				jsonWriter.name("displayName").value(authorization.toString());
				// Roles
				jsonWriter.name("roles").beginArray();
				for (String role : authorization.getRoles())
					if (!role.equals(authorization.getName()))
						jsonWriter.value(role);
				jsonWriter.endArray();
				// CMS session
				jsonWriter.name("cmsSession").beginObject();
				jsonWriter.name("uuid").value(cmsSessionId.getUuid().toString());
				jsonWriter.endObject();

				// extensions
				enrichJson(jsonWriter);

				jsonWriter.endObject();

				String redirectTo = redirectTo(request);
				if (redirectTo != null)
					response.sendRedirect(redirectTo);
			} catch (LoginException e) {
				response.setStatus(403);
				return;
			}
		} else {
			response.setStatus(403);
			return;
		}
	}

	/**
	 * To be overridden. The object will be ended by the caller. Does nothing by
	 * default.
	 */
	protected void enrichJson(JsonWriter jsonWriter) {

	}

	/** Does nothing by default. */
	protected void loginSucceeded(LoginContext lc, HttpServletRequest request, HttpServletResponse response) {

	}

	/** Send HTTP code 403 by default. */
	protected void loginFailed(LoginContext lc, HttpServletRequest request, HttpServletResponse response) {

	}

	protected String redirectTo(HttpServletRequest request) {
		return null;
	}
}
