package org.argeo.cms.jcr.internal.servlet;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.api.NodeConstants;
import org.argeo.api.cms.CmsSession;
import org.argeo.jcr.JcrUtils;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
public class CmsSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = -1358136599534938466L;

	private final static Log log = LogFactory.getLog(CmsSessionProvider.class);

	private final String alias;

	private LinkedHashMap<Session, CmsDataSession> cmsSessions = new LinkedHashMap<>();

	public CmsSessionProvider(String alias) {
		this.alias = alias;
	}

	public Session getSession(HttpServletRequest request, Repository rep, String workspace)
			throws javax.jcr.LoginException, ServletException, RepositoryException {

		// a client is scanning parent URLs.
//		if (workspace == null)
//			return null;

//		CmsSessionImpl cmsSession = WebCmsSessionImpl.getCmsSession(request);
		// FIXME retrieve CMS session
		CmsSession cmsSession = null;
		if (log.isTraceEnabled()) {
			log.trace("Get JCR session from " + cmsSession);
		}
		if (cmsSession == null)
			throw new IllegalStateException("Cannot find a session for request " + request.getRequestURI());
		CmsDataSession cmsDataSession = new CmsDataSession(cmsSession);
		Session session = cmsDataSession.getDataSession(alias, workspace, rep);
		cmsSessions.put(session, cmsDataSession);
		return session;
	}

	public void releaseSession(Session session) {
//		JcrUtils.logoutQuietly(session);
		if (cmsSessions.containsKey(session)) {
			CmsDataSession cmsDataSession = cmsSessions.get(session);
			cmsDataSession.releaseDataSession(alias, session);
		} else {
			log.warn("JCR session " + session + " not found in CMS session list. Logging it out...");
			JcrUtils.logoutQuietly(session);
		}
	}

	static class CmsDataSession {
		private CmsSession cmsSession;

		private Map<String, Session> dataSessions = new HashMap<>();
		private Set<String> dataSessionsInUse = new HashSet<>();
		private Set<Session> additionalDataSessions = new HashSet<>();

		private CmsDataSession(CmsSession cmsSession) {
			this.cmsSession = cmsSession;
		}

		public Session newDataSession(String cn, String workspace, Repository repository) {
			checkValid();
			return login(repository, workspace);
		}

		public synchronized Session getDataSession(String cn, String workspace, Repository repository) {
			checkValid();
			// FIXME make it more robust
			if (workspace == null)
				workspace = NodeConstants.SYS_WORKSPACE;
			String path = cn + '/' + workspace;
			if (dataSessionsInUse.contains(path)) {
				try {
					wait(1000);
					if (dataSessionsInUse.contains(path)) {
						Session session = login(repository, workspace);
						additionalDataSessions.add(session);
						if (log.isTraceEnabled())
							log.trace("Additional data session " + path + " for " + cmsSession.getUserDn());
						return session;
					}
				} catch (InterruptedException e) {
					// silent
				}
			}

			Session session = null;
			if (dataSessions.containsKey(path)) {
				session = dataSessions.get(path);
			} else {
				session = login(repository, workspace);
				dataSessions.put(path, session);
				if (log.isTraceEnabled())
					log.trace("New data session " + path + " for " + cmsSession.getUserDn());
			}
			dataSessionsInUse.add(path);
			return session;
		}

		private Session login(Repository repository, String workspace) {
			try {
				return Subject.doAs(cmsSession.getSubject(), new PrivilegedExceptionAction<Session>() {
					@Override
					public Session run() throws Exception {
						return repository.login(workspace);
					}
				});
			} catch (PrivilegedActionException e) {
				throw new IllegalStateException("Cannot log in " + cmsSession.getUserDn() + " to JCR", e);
			}
		}

		public synchronized void releaseDataSession(String cn, Session session) {
			if (additionalDataSessions.contains(session)) {
				JcrUtils.logoutQuietly(session);
				additionalDataSessions.remove(session);
				if (log.isTraceEnabled())
					log.trace("Remove additional data session " + session);
				return;
			}
			String path = cn + '/' + session.getWorkspace().getName();
			if (!dataSessionsInUse.contains(path))
				log.warn("Data session " + path + " was not in use for " + cmsSession.getUserDn());
			dataSessionsInUse.remove(path);
			Session registeredSession = dataSessions.get(path);
			if (session != registeredSession)
				log.warn("Data session " + path + " not consistent for " + cmsSession.getUserDn());
			if (log.isTraceEnabled())
				log.trace("Released data session " + session + " for " + path);
			notifyAll();
		}

		private void checkValid() {
			if (!cmsSession.isValid())
				throw new IllegalStateException(
						"CMS session " + cmsSession.getUuid() + " is not valid since " + cmsSession.getEnd());
		}

		private void close() {
			// FIXME class this when CMS session is closed
			synchronized (this) {
				// TODO check data session in use ?
				for (String path : dataSessions.keySet())
					JcrUtils.logoutQuietly(dataSessions.get(path));
				for (Session session : additionalDataSessions)
					JcrUtils.logoutQuietly(session);
			}
		}
	}
}
