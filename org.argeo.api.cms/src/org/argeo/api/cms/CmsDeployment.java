package org.argeo.api.cms;

import java.util.concurrent.CompletionStage;

import com.sun.net.httpserver.HttpServer;

/** A configured node deployment. */
public interface CmsDeployment {
	/** The local HTTP server, or null if none is expected. */
	CompletionStage<HttpServer> getHttpServer();
	
	/** The local SSH server, or null if none is expected. */
	CompletionStage<CmsSshd> getCmsSshd();
}
