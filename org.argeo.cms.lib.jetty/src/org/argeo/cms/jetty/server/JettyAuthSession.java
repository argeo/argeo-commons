package org.argeo.cms.jetty.server;

import org.argeo.cms.auth.RemoteAuthSession;
import org.eclipse.jetty.server.Session;

/** A {@link RemoteAuthSession} based on Jetty {@link Session}. */
public class JettyAuthSession implements RemoteAuthSession, Session.API {
	private final Session jettySession;

	public JettyAuthSession(Session jettySession) {
		this.jettySession = jettySession;
	}

	@Override
	public boolean isValid() {
		return jettySession.isValid();
	}

	@Override
	public String getId() {
		return jettySession.getId();
	}

	/*
	 * Jetty API
	 */

	@Override
	public Session getSession() {
		return jettySession;
	}

}
