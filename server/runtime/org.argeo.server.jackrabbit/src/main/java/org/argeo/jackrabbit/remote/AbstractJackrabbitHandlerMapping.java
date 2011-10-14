package org.argeo.jackrabbit.remote;

import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.jcr.mvc.MultipleRepositoryHandlerMapping;

/** Base class for Jackrabbit ahndler mappings. */
public abstract class AbstractJackrabbitHandlerMapping extends
		MultipleRepositoryHandlerMapping {
	private SessionProvider sessionProvider;

	protected SessionProvider getSessionProvider() {
		return sessionProvider;
	}

	public void setSessionProvider(SessionProvider sessionProvider) {
		this.sessionProvider = sessionProvider;
	}

}
