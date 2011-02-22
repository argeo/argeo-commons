package org.argeo.server.jackrabbit.webdav;

import java.util.Enumeration;

import javax.jcr.Repository;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.server.SessionProvider;
import org.springframework.web.servlet.mvc.ServletWrappingController;

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
		return new CachingSessionProvider();
	}

}
