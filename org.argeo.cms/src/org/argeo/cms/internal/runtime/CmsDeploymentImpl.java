package org.argeo.cms.internal.runtime;

import static org.argeo.api.cms.CmsConstants.CONTEXT_PATH;

import java.util.Map;
import java.util.TreeMap;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.CmsSshd;
import org.argeo.cms.internal.http.CmsAuthenticator;
import org.argeo.cms.internal.http.PublicCmsAuthenticator;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** Reference implementation of {@link CmsDeployment}. */
public class CmsDeploymentImpl implements CmsDeployment {
	private final CmsLog log = CmsLog.getLog(getClass());

	private CmsState cmsState;

	// Expectations
	private boolean httpExpected = false;
	private boolean sshdExpected = false;

	// HTTP
	private HttpServer httpServer;
	private Map<String, HttpHandler> httpHandlers = new TreeMap<>();
	private Map<String, CmsAuthenticator> httpAuthenticators = new TreeMap<>();

	// SSHD
	private CmsSshd cmsSshd;

	public void start() {
		log.debug(() -> "CMS deployment available");
	}

	public void stop() {
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;

		String httpPort = this.cmsState.getDeployProperty(CmsDeployProperty.HTTP_PORT.getProperty());
		String httpsPort = this.cmsState.getDeployProperty(CmsDeployProperty.HTTPS_PORT.getProperty());
		httpExpected = httpPort != null || httpsPort != null;

		String sshdPort = this.cmsState.getDeployProperty(CmsDeployProperty.SSHD_PORT.getProperty());
		sshdExpected = sshdPort != null;
	}

	public void setHttpServer(HttpServer httpServer) {
		this.httpServer = httpServer;
		// create contexts whose handles had already been published
		for (String contextPath : httpHandlers.keySet()) {
			HttpHandler httpHandler = httpHandlers.get(contextPath);
			CmsAuthenticator authenticator = httpAuthenticators.get(contextPath);
			createHttpContext(contextPath, httpHandler, authenticator);
		}
	}

	public void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties) {
		final String contextPath = properties.get(CONTEXT_PATH);
		if (contextPath == null) {
			log.warn("Property " + CONTEXT_PATH + " not set on HTTP handler " + properties + ". Ignoring it.");
			return;
		}
		boolean isPublic = Boolean.parseBoolean(properties.get(CmsConstants.CONTEXT_PUBLIC));
		CmsAuthenticator authenticator = isPublic ? new PublicCmsAuthenticator() : new CmsAuthenticator();
		httpHandlers.put(contextPath, httpHandler);
		httpAuthenticators.put(contextPath, authenticator);
		if (httpServer == null) {
			return;
		} else {
			createHttpContext(contextPath, httpHandler, authenticator);
		}
	}

	public void createHttpContext(String contextPath, HttpHandler httpHandler, CmsAuthenticator authenticator) {
		HttpContext httpContext = httpServer.createContext(contextPath);
		// we want to set the authenticator BEFORE the handler actually becomes active
		httpContext.setAuthenticator(authenticator);
		httpContext.setHandler(httpHandler);
		log.debug(() -> "Added handler " + contextPath + " : " + httpHandler.getClass().getName());
	}

	public void removeHttpHandler(HttpHandler httpHandler, Map<String, String> properties) {
		final String contextPath = properties.get(CmsConstants.CONTEXT_PATH);
		if (contextPath == null)
			return; // ignore silently
		httpHandlers.remove(contextPath);
		if (httpServer == null)
			return;
		httpServer.removeContext(contextPath);
		log.debug(() -> "Removed handler " + contextPath + " : " + httpHandler.getClass().getName());
	}

	public boolean allExpectedServicesAvailable() {
		if (httpExpected && httpServer == null)
			return false;
		if (sshdExpected && cmsSshd == null)
			return false;
		return true;
	}

	public void setCmsSshd(CmsSshd cmsSshd) {
		this.cmsSshd = cmsSshd;
	}

}
