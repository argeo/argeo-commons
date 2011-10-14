package org.argeo.jackrabbit.remote;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;

/** To be injected, typically of scope="session" */
public class SimpleSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 2270957712453841368L;

	private final static Log log = LogFactory
			.getLog(SimpleSessionProvider.class);

	private transient Map<String, Session> sessions;

	private Boolean openSessionInView = true;

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		if (openSessionInView) {
			return rep.login(workspace);
		} else {
			// since sessions is transient it can't be restored from the session
			if (sessions == null)
				sessions = Collections
						.synchronizedMap(new HashMap<String, Session>());

			if (!sessions.containsKey(workspace)) {
				try {
					Session session = rep.login(null, workspace);
					if (log.isTraceEnabled())
						log.trace("User " + session.getUserID()
								+ " logged into " + request.getServletPath());
					sessions.put(workspace, session);
					return session;
				} catch (Exception e) {
					throw new ArgeoException("Cannot open session", e);
				}
			} else {
				Session session = sessions.get(workspace);
				if (!session.isLive()) {
					sessions.remove(workspace);
					session = rep.login(null, workspace);
					sessions.put(workspace, session);
				}
				return session;
			}
		}
	}

	public void releaseSession(Session session) {
		if (log.isTraceEnabled())
			log.trace("Releasing JCR session " + session);
		if (openSessionInView)
			JcrUtils.logoutQuietly(session);
	}

	public void init() {
	}

	public void dispose() {
		if (sessions != null)
			for (String workspace : sessions.keySet()) {
				Session session = sessions.get(workspace);
				if (session.isLive()) {
					session.logout();
					if (log.isDebugEnabled())
						log.debug("Logged out JCR session " + session);
				}
			}
	}
}
