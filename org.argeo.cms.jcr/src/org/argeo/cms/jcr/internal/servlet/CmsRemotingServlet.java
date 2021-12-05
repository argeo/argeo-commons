package org.argeo.cms.jcr.internal.servlet;

import java.util.Map;

import javax.jcr.Repository;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.argeo.api.NodeConstants;

/** A {@link JcrRemotingServlet} based on {@link CmsSessionProvider}. */
public class CmsRemotingServlet extends JcrRemotingServlet {
	private static final long serialVersionUID = 6459455509684213633L;
	private Repository repository;
	private SessionProvider sessionProvider;

	public CmsRemotingServlet() {
	}

	public CmsRemotingServlet(String alias, Repository repository) {
		this.repository = repository;
		this.sessionProvider = new CmsSessionProvider(alias);
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository, Map<String, String> properties) {
		this.repository = repository;
		String alias = properties.get(NodeConstants.CN);
		if (alias != null)
			sessionProvider = new CmsSessionProvider(alias);
		else
			throw new IllegalArgumentException("Only aliased repositories are supported");
	}

	@Override
	protected SessionProvider getSessionProvider() {
		return sessionProvider;
	}

}
