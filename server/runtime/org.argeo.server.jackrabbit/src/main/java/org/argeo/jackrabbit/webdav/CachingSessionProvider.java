package org.argeo.jackrabbit.webdav;

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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;

public class CachingSessionProvider implements SessionProvider {
	private static final String JCR_SESSIONS_ATTRIBUTE = "jcrSessions";

	private final static Log log = LogFactory
			.getLog(CachingSessionProvider.class);

	private Credentials credentials = null;

	public CachingSessionProvider() {
	}

	public CachingSessionProvider(Credentials credentials) {
		this.credentials = credentials;
	}

	@SuppressWarnings("unchecked")
	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {
		HttpSession httpSession = request.getSession();

		if (httpSession.getAttribute(JCR_SESSIONS_ATTRIBUTE) == null) {
			httpSession
					.setAttribute(JCR_SESSIONS_ATTRIBUTE, Collections
							.synchronizedMap(new HashMap<String, Session>()));
		}
		Map<String, Session> sessions = (Map<String, Session>) httpSession
				.getAttribute(JCR_SESSIONS_ATTRIBUTE);
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

}
