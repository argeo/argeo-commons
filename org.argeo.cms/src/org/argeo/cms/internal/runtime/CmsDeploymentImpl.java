package org.argeo.cms.internal.runtime;

import static org.argeo.api.cms.CmsConstants.CONTEXT_PATH;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSshd;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
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
	private CompletableFuture<HttpServer> httpServer = new CompletableFuture<>();
	private Map<String, HttpHandler> httpHandlers = new TreeMap<>();
	private Map<String, CmsAuthenticator> httpAuthenticators = new TreeMap<>();

	// SSHD
	private CompletableFuture<CmsSshd> cmsSshd = new CompletableFuture<>();

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
		if (!httpExpected)
			httpServer.complete(null);

		String sshdPort = this.cmsState.getDeployProperty(CmsDeployProperty.SSHD_PORT.getProperty());
		sshdExpected = sshdPort != null;
		if (!sshdExpected)
			cmsSshd.complete(null);
	}

	public void setHttpServer(HttpServer httpServer) {
		Objects.requireNonNull(httpServer);
		if (this.httpServer.isDone())
			if (httpExpected)
				throw new IllegalStateException("HTTP server is already set");
			else
				return;// ignore
		// create contexts whose handlers had already been published
		synchronized (httpHandlers) {
			synchronized (httpAuthenticators) {
				this.httpServer.complete(httpServer);
				for (String contextPath : httpHandlers.keySet()) {
					HttpHandler httpHandler = httpHandlers.get(contextPath);
					CmsAuthenticator authenticator = httpAuthenticators.get(contextPath);
					createHttpContext(contextPath, httpHandler, authenticator);
				}
			}
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
		synchronized (httpHandlers) {
			synchronized (httpAuthenticators) {
				httpHandlers.put(contextPath, httpHandler);
				httpAuthenticators.put(contextPath, authenticator);
			}
		}
		if (!httpServer.isDone()) {
			return;
		} else {
			createHttpContext(contextPath, httpHandler, authenticator);
		}
	}

	public void createHttpContext(String contextPath, HttpHandler httpHandler, CmsAuthenticator authenticator) {
		if (!httpExpected) {
			if (log.isTraceEnabled())
				log.warn("Ignore HTTP context " + contextPath + " as we don't provide an HTTP server");
			return;
		}
		if (!this.httpServer.isDone())
			throw new IllegalStateException("HTTP server is not set");
		// TODO use resultNow when switching to Java 21
		HttpContext httpContext = httpServer.join().createContext(contextPath);
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
		if (!httpExpected || !httpServer.isDone())
			return;
		// TODO use resultNow when switching to Java 21
		httpServer.join().removeContext(contextPath);
		log.debug(() -> "Removed handler " + contextPath + " : " + httpHandler.getClass().getName());
	}

	public boolean allExpectedServicesAvailable() {
		if (httpExpected && !httpServer.isDone())
			return false;
		if (sshdExpected && !cmsSshd.isDone())
			return false;
		return true;
	}

	public void setCmsSshd(CmsSshd cmsSshd) {
		Objects.requireNonNull(cmsSshd);
		this.cmsSshd.complete(cmsSshd);
	}

	@Override
	public CompletionStage<HttpServer> getHttpServer() {
		return httpServer.minimalCompletionStage();
	}

	@Override
	public CompletionStage<CmsSshd> getCmsSshd() {
		return cmsSshd.minimalCompletionStage();
	}

}
