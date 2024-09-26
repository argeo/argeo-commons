package org.argeo.cms.http.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Partial implementation of {@link HttpContext}.
 */
public abstract class AbstractCmsHttpContext extends HttpContext {
	private final HttpServer httpServer;
	private final String path;
	private final List<Filter> filters = Collections.synchronizedList(new ArrayList<>());

	private HttpHandler handler;
	private Authenticator authenticator;

	public AbstractCmsHttpContext(HttpServer httpServer, String path) {
		this.httpServer = httpServer;
		// TODO Is it really necessary? Make it more robust.
//		if (!path.endsWith("/"))
//			throw new IllegalArgumentException("Path " + path + " should end with a /");
		this.path = path;
	}

	@Override
	public HttpHandler getHandler() {
		return handler;
	}

	@Override
	public void setHandler(HttpHandler handler) {
		if (this.handler != null)
			throw new IllegalArgumentException("Handler is already set");
		Objects.requireNonNull(handler);
		this.handler = handler;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public HttpServer getServer() {
		return httpServer;
	}

	@Override
	public List<Filter> getFilters() {
		return filters;
	}

	@Override
	public Authenticator setAuthenticator(Authenticator auth) {
		synchronized (filters) {
			// current state
			Authenticator previousAuthenticator = authenticator;
			Optional<CmsAuthenticatorFilter> currentCmsAuthenticatorFilter = findAuthenticatorFilter();

			this.authenticator = auth;
			if (this.authenticator == null) {// authentication disabled
				currentCmsAuthenticatorFilter.ifPresent((filter) -> filters.remove(filter));
			} else {// authentication enabled
				if (currentCmsAuthenticatorFilter.isEmpty()) {
					// First time an authenticator is set, we assume that there are good reasons to
					// have non authenticated filters first. In most cases it should be first, in
					// order to be consistent with the JDK HTTP server behaviour.
					filters.add(new CmsAuthenticatorFilter());
				}
			}
			return previousAuthenticator;
		}
	}

	private Optional<CmsAuthenticatorFilter> findAuthenticatorFilter() {
		synchronized (filters) {
			for (Filter filter : filters) {
				if (filter instanceof CmsAuthenticatorFilter cmsAuthenticatorFilter)
					return Optional.of(cmsAuthenticatorFilter);
			}
		}
		return Optional.empty();
	}

	@Override
	public Authenticator getAuthenticator() {
		synchronized (filters) {
			return authenticator;
		}
	}

}
