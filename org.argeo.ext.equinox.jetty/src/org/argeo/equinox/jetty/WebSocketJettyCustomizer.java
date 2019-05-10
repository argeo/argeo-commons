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
//		servletContextHandler.addFilter(new FilterHolder(new Filter() {
//
//			@Override
//			public void init(FilterConfig filterConfig) throws ServletException {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//					throws IOException, ServletException {
//				HttpServletRequest httpRequest = (HttpServletRequest) request;
//				HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//				HttpRequestCallbackHandler callbackHandler = new HttpRequestCallbackHandler(httpRequest, httpResponse);
//				try {
//					LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, callbackHandler);
//					lc.login();
//
//					chain.doFilter(httpRequest, httpResponse);
//				} catch (LoginException e) {
//					httpResponse.setStatus(403);
//				}
//
//			}
//
//			@Override
//			public void destroy() {
//				// TODO Auto-generated method stub
//
//			}
//		}), "/vje/events", EnumSet.of(DispatcherType.REQUEST));
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
//
//						CmsSession cmsSession = getCmsSession(httpSessionId);
//						if (cmsSession == null) {
////							session.disconnect();
////							return;
//
////							try {
////								session.getUpgradeResponse().sendForbidden("Web Sockets must always be authenticated.");
////							} catch (IOException e) {
////								e.printStackTrace();
////							}
//						} else {
//							JsrSession jsrSession = (JsrSession) session;
//							String jsrId = jsrSession.getId();
//							System.out.println("JSR ID: " + jsrId);
//							jsrSession.getUserProperties().put(CmsSession.SESSION_LOCAL_ID, cmsSession.getLocalId());
//							jsrSession.getUserProperties().put(CmsSession.SESSION_UUID, cmsSession.getUuid());
//							jsrSession.getUserProperties().put(HttpContext.REMOTE_USER, cmsSession.getUserDn());
//							// httpSession.setAttribute(HttpContext.AUTHORIZATION,
//							// cmsSession.getAuthorization());
//						}
//					}
//
//					@Override
//					public void onSessionClosed(WebSocketSession session) {
//						// TODO Auto-generated method stub
//
//					}
//				});
			} catch (ServletException e) {
				throw new IllegalStateException("Cannot configure web sockets", e);
			}
			bc.registerService(javax.websocket.server.ServerContainer.class, serverContainer, null);
//			ServiceTracker<ServerEndpointConfig.Builder, ServerEndpointConfig.Builder> endpointsTracker = new ServiceTracker<ServerEndpointConfig.Builder, ServerEndpointConfig.Builder>(
//					bc, ServerEndpointConfig.Builder.class, null) {
//
//				@Override
//				public ServerEndpointConfig.Builder addingService(
//						ServiceReference<ServerEndpointConfig.Builder> reference) {
//
//					ServerEndpointConfig.Builder serverEndpointConfig = super.addingService(reference);
//					try {
//						serverContainer.addEndpoint(serverEndpointConfig.build());
//					} catch (DeploymentException e) {
//						throw new IllegalArgumentException("Cannot add end point " + reference, e);
//					}
//					return serverEndpointConfig;
//				}
//			};
//			endpointsTracker.open();
			// TODO log it properly
			// TODO close itproperly
		}

	}

//	private CmsSession getCmsSession(String httpSessionId) {
//		if (httpSessionId == null)
//			return null;
//
//		Collection<ServiceReference<CmsSession>> sr;
//		try {
//			sr = bc.getServiceReferences(CmsSession.class,
//					"(" + CmsSession.SESSION_LOCAL_ID + "=" + httpSessionId + ")");
//		} catch (InvalidSyntaxException e) {
//			throw new IllegalStateException("Cannot get CMS session for id " + httpSessionId, e);
//		}
//		if (sr.size() == 1) {
//			CmsSession cmsSession = bc.getService(sr.iterator().next());
//			Authorization authorization = cmsSession.getAuthorization();
//			if (authorization.getName() == null)
//				return null;// anonymous is not sufficient
//			return cmsSession;
//		} else {
//			return null;
//		}
//	}

}
