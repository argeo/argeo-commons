package org.argeo.cms.websocket.javax.server;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.servlet.ServletHttpSession;

/**
 * <strong>Disabled until third party issues are solved.</strong>. Customises
 * the initialisation of a new web socket.
 */
public class CmsWebSocketConfigurator extends Configurator {
	public final static String WEBSOCKET_SUBJECT = "org.argeo.cms.websocket.subject";
	public final static String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";

	private final static CmsLog log = CmsLog.getLog(CmsWebSocketConfigurator.class);
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
		if (true)
			return;

		RemoteAuthSession httpSession = new ServletHttpSession(
				(javax.servlet.http.HttpSession) request.getHttpSession());
		if (log.isDebugEnabled() && httpSession != null)
			log.debug("Web socket HTTP session id: " + httpSession.getId());

		if (httpSession == null) {
			rejectResponse(response, null);
		}
		try {
			LoginContext lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER, new RemoteAuthCallbackHandler(httpSession));
			lc.login();
			if (log.isDebugEnabled())
				log.debug("Web socket logged-in as " + lc.getSubject());
			Subject.doAs(lc.getSubject(), new PrivilegedAction<Void>() {

				@Override
				public Void run() {
					sec.getUserProperties().put(REMOTE_USER, AccessController.getContext());
					return null;
				}

			});
		} catch (Exception e) {
			rejectResponse(response, e);
		}
	}

	/**
	 * Behaviour when the web socket could not be authenticated. Throws an
	 * {@link IllegalStateException} by default.
	 * 
	 * @param e can be null
	 */
	protected void rejectResponse(HandshakeResponse response, Exception e) {
		// violent implementation, as suggested in
		// https://stackoverflow.com/questions/21763829/jsr-356-how-to-abort-a-websocket-connection-during-the-handshake
//		throw new IllegalStateException("Web socket cannot be authenticated");
	}
}
