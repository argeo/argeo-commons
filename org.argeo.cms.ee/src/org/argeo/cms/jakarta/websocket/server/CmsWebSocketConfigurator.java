package org.argeo.cms.jakarta.websocket.server;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;
import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.jakarta.servlet.CmsServletContext;

/**
 * <strong>Disabled until third party issues are solved.</strong>. Customises
 * the initialisation of a new web socket.
 */
public class CmsWebSocketConfigurator extends Configurator {

	private final static CmsLog log = CmsLog.getLog(CmsWebSocketConfigurator.class);

	private final String httpAuthRealm = "Argeo";

	@Override
	public boolean checkOrigin(String originHeaderValue) {
		return true;
	}

	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
		try {
			return endpointClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot get endpoint instance", e);
		}
	}

	@Override
	public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
		return requested;
	}

	@Override
	public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
		if ((requested == null) || (requested.size() == 0))
			return "";
		if ((supported == null) || (supported.isEmpty()))
			return "";
		for (String possible : requested) {
			if (possible == null)
				continue;
			if (supported.contains(possible))
				return possible;
		}
		return "";
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
//		if (true)
//			return;

		WebSocketHandshakeRequest remoteAuthRequest = new WebSocketHandshakeRequest(request);
		WebSocketHandshakeResponse remoteAuthResponse = new WebSocketHandshakeResponse(response);
//		RemoteAuthSession httpSession = new ServletHttpSession(
//				(javax.servlet.http.HttpSession) request.getHttpSession());
		RemoteAuthSession remoteAuthSession = remoteAuthRequest.getSession();
		if (log.isDebugEnabled() && remoteAuthSession != null)
			log.debug("Web socket HTTP session id: " + remoteAuthSession.getId());

//		if (remoteAuthSession == null) {
//			rejectResponse(response, null);
//		}
		ClassLoader currentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(CmsServletContext.class.getClassLoader());
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthRequest, remoteAuthResponse));
			lc.login();
		} catch (LoginException e) {
			if (authIsRequired(remoteAuthRequest, remoteAuthResponse)) {
				int statusCode = RemoteAuthUtils.askForWwwAuth(remoteAuthRequest, remoteAuthResponse, httpAuthRealm,
						true);
//				remoteAuthResponse.setHeader("Status-Code", Integer.toString(statusCode));
				return;
			} else {
				lc = RemoteAuthUtils.anonymousLogin(remoteAuthRequest, remoteAuthResponse);
			}
			if (lc == null) {
				rejectResponse(response, e);
				return;
			}
		} finally {
			Thread.currentThread().setContextClassLoader(currentThreadContextClassLoader);
		}

//		Subject subject = lc.getSubject();
//		Subject.doAs(subject, new PrivilegedAction<Void>() {
//
//			@Override
//			public Void run() {
//				// TODO also set login context in order to log out ?
//				RemoteAuthUtils.configureRequestSecurity(remoteAuthRequest);
//				return null;
//			}
//
//		});
	}

	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return true;
	}

	/**
	 * Behaviour when the web socket could not be authenticated. Throws an
	 * {@link IllegalStateException} by default.
	 * 
	 * @param e can be null
	 */
	protected void rejectResponse(HandshakeResponse response, Exception e) {
		response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
		// violent implementation, as suggested in
		// https://stackoverflow.com/questions/21763829/jsr-356-how-to-abort-a-websocket-connection-during-the-handshake
//		throw new IllegalStateException("Web socket cannot be authenticated");
	}
}
