package org.argeo.cms.websocket.server;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.argeo.api.acr.ldap.NamingUtils;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.integration.CmsExceptionsChain;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Provides WebSocket access. */
@ServerEndpoint(value = "/cms/status/test/{topic}", configurator = CmsWebSocketConfigurator.class)
public class TestEndpoint implements EventHandler {
	private final static CmsLog log = CmsLog.getLog(TestEndpoint.class);

	final static String TOPICS_BASE = "/test";
	final static String INPUT = "input";
	final static String TOPIC = "topic";
	final static String VIEW_UID = "viewUid";
	final static String COMPUTATION_UID = "computationUid";
	final static String MESSAGES = "messages";
	final static String ERRORS = "errors";

	final static String EXCEPTION = "exception";
	final static String MESSAGE = "message";

	private BundleContext bc = FrameworkUtil.getBundle(TestEndpoint.class).getBundleContext();

	private String wsSessionId;
	private RemoteEndpoint.Basic remote;
	private ServiceRegistration<EventHandler> eventHandlerSr;

	// json
	private ObjectMapper objectMapper = new ObjectMapper();

	private WebSocketView view;

	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		Map<String, List<String>> parameters = NamingUtils.queryToMap(session.getRequestURI());
		String path = NamingUtils.getQueryValue(parameters, "path");
		log.debug("WS Path: " + path);

		wsSessionId = session.getId();

		// 24h timeout
		session.setMaxIdleTimeout(1000 * 60 * 60 * 24);

		Map<String, Object> userProperties = session.getUserProperties();
		Subject subject = null;
//		AccessControlContext accessControlContext = (AccessControlContext) userProperties
//				.get(ServletContextHelper.REMOTE_USER);
//		Subject subject = Subject.getSubject(accessControlContext);
//		// Deal with authentication failure
//		if (subject == null) {
//			try {
//				CloseReason.CloseCode closeCode = new CloseReason.CloseCode() {
//
//					@Override
//					public int getCode() {
//						return 4001;
//					}
//				};
//				session.close(new CloseReason(closeCode, "Unauthorized"));
//				if (log.isTraceEnabled())
//					log.trace("Unauthorized web socket " + wsSessionId + ". Closing with code " + closeCode.getCode()
//							+ ".");
//				return;
//			} catch (IOException e) {
//				// silent
//			}
//			return;// ignore
//		}

		if (log.isDebugEnabled())
			log.debug("WS#" + wsSessionId + " open for: " + subject);
		remote = session.getBasicRemote();
		view = new WebSocketView(subject);

		// OSGi events
		String[] topics = new String[] { TOPICS_BASE + "/*" };
		Hashtable<String, Object> ht = new Hashtable<>();
		ht.put(EventConstants.EVENT_TOPIC, topics);
		ht.put(EventConstants.EVENT_FILTER, "(" + VIEW_UID + "=" + view.getUid() + ")");
		eventHandlerSr = bc.registerService(EventHandler.class, this, ht);

		if (log.isDebugEnabled())
			log.debug("New view " + view.getUid() + " opened, via web socket.");
	}

	@OnMessage
	public void onWebSocketText(@PathParam("topic") String topic, Session session, String message)
			throws JsonMappingException, JsonProcessingException {
		try {
			if (log.isTraceEnabled())
				log.trace("WS#" + view.getUid() + " received:\n" + message + "\n");
//			JsonNode jsonNode = objectMapper.readTree(message);
//			String topic = jsonNode.get(TOPIC).textValue();

			final String computationUid = null;
//			if (MY_TOPIC.equals(topic)) {
//				view.checkRole(SPECIFIC_ROLE);
//				computationUid= process();
//			}
			remote.sendText("ACK " + topic);
		} catch (Exception e) {
			log.error("Error when receiving web socket message", e);
			sendSystemErrorMessage(e);
		}
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason) {
		if (eventHandlerSr != null)
			eventHandlerSr.unregister();
		if (view != null && log.isDebugEnabled())
			log.debug("WS#" + view.getUid() + " closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		if (view != null) {
			log.error("WS#" + view.getUid() + " ERROR", cause);
		} else {
			if (log.isTraceEnabled())
				log.error("Error in web socket session " + wsSessionId, cause);
		}
	}

	@Override
	public void handleEvent(Event event) {
		try {
			Object uid = event.getProperty(COMPUTATION_UID);
			Exception exception = (Exception) event.getProperty(EXCEPTION);
			if (exception != null) {
				CmsExceptionsChain systemErrors = new CmsExceptionsChain(exception);
				String sent = systemErrors.toJsonString(objectMapper);
				remote.sendText(sent);
				return;
			}
			String topic = event.getTopic();
			if (log.isTraceEnabled())
				log.trace("WS#" + view.getUid() + " " + topic + ": notify event " + topic + "#" + uid + ", " + event);
		} catch (Exception e) {
			log.error("Error when handling event for WebSocket", e);
			sendSystemErrorMessage(e);
		}

	}

	/** Sends an error message in JSON format. */
	protected void sendSystemErrorMessage(Exception e) {
		CmsExceptionsChain systemErrors = new CmsExceptionsChain(e);
		try {
			if (remote != null)
				remote.sendText(systemErrors.toJsonString(objectMapper));
		} catch (Exception e1) {
			log.error("Cannot send WebSocket system error messages " + systemErrors, e1);
		}
	}
}
