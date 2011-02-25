package org.argeo.jackrabbit.remote;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;

/** To be injected, typically of scope="session" */
public class SimpleSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 2270957712453841368L;

	private final static Log log = LogFactory
			.getLog(SimpleSessionProvider.class);

	private transient Map<String, Session> sessions = Collections
			.synchronizedMap(new HashMap<String, Session>());

	private Credentials credentials = null;

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		if (!sessions.containsKey(workspace)) {
			Session session = rep.login(credentials, workspace);
			sessions.put(workspace, session);
			return session;
		} else {
			Session session = sessions.get(workspace);
			if (!session.isLive()) {
				sessions.remove(workspace);
				session = rep.login(credentials, workspace);
				sessions.put(workspace, session);
			}
			return session;
		}
	}

	public void releaseSession(Session session) {
		if (log.isDebugEnabled())
			log.debug("Releasing JCR session " + session);
		// session.logout();
		// FIXME: find a way to log out when the HTTP session is expired
	}

	public void dispose() {
		for (String workspace : sessions.keySet()) {
			Session session = sessions.get(workspace);
			if (session.isLive())
				session.logout();
		}
	}
}
