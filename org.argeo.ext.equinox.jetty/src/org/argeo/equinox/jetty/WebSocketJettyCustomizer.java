package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletException;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class WebSocketJettyCustomizer extends JettyCustomizer {
	private BundleContext bc = FrameworkUtil.getBundle(WebSocketJettyCustomizer.class).getBundleContext();

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		ServletContextHandler servletContextHandler = (ServletContextHandler) context;
		new WebSocketInit(servletContextHandler).start();
		return super.customizeContext(context, settings);
	}

	/** Configure websocket container asynchronously as it may take some time */
	private class WebSocketInit extends Thread {
		ServletContextHandler servletContextHandler;

		public WebSocketInit(ServletContextHandler servletContextHandler) {
			super("WebSocket Init");
			this.servletContextHandler = servletContextHandler;
		}

		@Override
		public void run() {
			ServerContainer serverContainer;
			try {
				serverContainer = WebSocketServerContainerInitializer.configureContext(servletContextHandler);
//				serverContainer.addSessionListener(new WebSocketSessionListener() {
//
//					@Override
//					public void onSessionOpened(WebSocketSession session) {
//						UpgradeRequest upgradeRequest = session.getUpgradeRequest();
//						UpgradeResponse upgradeResponse = session.getUpgradeResponse();
//						List<String> acceptHeader = upgradeResponse.getHeaders("Sec-WebSocket-Accept");
//						if (acceptHeader.contains("no"))
//							try {
//								upgradeResponse.sendForbidden("FORBIDDEN");
//								return;
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						List<HttpCookie> cookies = upgradeRequest.getCookies();
//
//						System.out.println("Upgrade request cookies : " + cookies);
//						String httpSessionId = null;
//						if (cookies != null) {
//							for (HttpCookie cookie : cookies) {
//								if (cookie.getName().equals("JSESSIONID")) {
//									httpSessionId = cookie.getValue();
//								}
//							}
//						}
//
//						if (httpSessionId == null) {
//							HttpSession httpSession = (HttpSession) upgradeRequest.getSession();
//							if (httpSession == null) {
////							session.disconnect();
////							return;
//							} else {
//								httpSessionId = httpSession.getId();
//								System.out.println("Upgrade request session ID : " + httpSession.getId());
//							}
//						}
//
//						if (httpSessionId != null) {
//							int dotIdx = httpSessionId.lastIndexOf('.');
//							if (dotIdx > 0) {
//								httpSessionId = httpSessionId.substring(0, dotIdx);
//							}
//						}
//					}
//
//					@Override
//					public void onSessionClosed(WebSocketSession session) {
//					}
//				});
			} catch (ServletException e) {
				throw new IllegalStateException("Cannot configure web sockets", e);
			}
			bc.registerService(javax.websocket.server.ServerContainer.class, serverContainer, null);
		}

	}
}
