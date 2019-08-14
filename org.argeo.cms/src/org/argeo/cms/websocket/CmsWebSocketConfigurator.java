package org.argeo.cms.websocket;

import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpSession;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.node.NodeConstants;

public final class CmsWebSocketConfigurator extends Configurator {
	private final static Log log = LogFactory.getLog(CmsWebSocketConfigurator.class);
	final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

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
		HttpSession httpSession = (HttpSession) request.getHttpSession();
		if (log.isDebugEnabled() && httpSession != null)
			log.debug("Web socket HTTP session id: " + httpSession.getId());

		if (httpSession == null) {
			rejectResponse(response);
		}
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					new HttpRequestCallbackHandler(httpSession));
			lc.login();
			if (log.isDebugEnabled())
				log.debug("Web socket logged-in as " + lc.getSubject());
			sec.getUserProperties().put("subject", lc.getSubject());
		} catch (LoginException e) {
			rejectResponse(response);
		}

//		List<String> authHeaders = request.getHeaders().get(HEADER_WWW_AUTHENTICATE);
//		String authHeader;
//		if (authHeaders != null && authHeaders.size() == 1) {
//			authHeader = authHeaders.get(0);
//		} else {
//			return;
//		}
	}

	private void rejectResponse(HandshakeResponse response) {
		// violent implementation, as suggested in
		// https://stackoverflow.com/questions/21763829/jsr-356-how-to-abort-a-websocket-connection-during-the-handshake
		throw new IllegalStateException("Web socket cannot be authenticated");
	}
}
