package org.argeo.jackrabbit.remote;

import javax.jcr.Repository;

import org.apache.jackrabbit.server.SessionProvider;

public class JcrRemotingServlet extends
		org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet {

	private static final long serialVersionUID = 3131835511468341309L;

	private final Repository repository;
	private final SessionProvider sessionProvider;

	public JcrRemotingServlet(Repository repository,
			SessionProvider sessionProvider) {
		this.repository = repository;
		this.sessionProvider = sessionProvider;
	}

	@Override
	protected Repository getRepository() {
		return repository;
	}

	@Override
	protected SessionProvider getSessionProvider() {
		return sessionProvider;
	}

}
