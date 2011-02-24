package org.argeo.jackrabbit.webdav;

import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.server.SessionProvider;

public class JcrRemotingServlet extends
		org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet {

	private static final long serialVersionUID = 3131835511468341309L;

	private final Repository repository;

	public JcrRemotingServlet(Repository repository) {
		this.repository = repository;
	}

	@Override
	protected Repository getRepository() {
		return repository;
	}

	@Override
	protected SessionProvider getSessionProvider() {
		return new CachingSessionProvider(new SimpleCredentials("demo",
				"demo".toCharArray()));
	}

}
