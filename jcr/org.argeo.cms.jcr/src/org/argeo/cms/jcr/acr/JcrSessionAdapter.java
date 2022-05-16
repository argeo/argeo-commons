package org.argeo.cms.jcr.acr;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;

/** Manages JCR {@link Session} in an ACR context. */
class JcrSessionAdapter {
	private Repository repository;
	private Subject subject;

	private Map<Thread, Map<String, Session>> threadSessions = Collections.synchronizedMap(new HashMap<>());

	private boolean closed = false;

	private Thread lastRetrievingThread = null;

	public JcrSessionAdapter(Repository repository, Subject subject) {
		this.repository = repository;
		this.subject = subject;
	}

	public synchronized void close() {
		for (Map<String, Session> sessions : threadSessions.values()) {
			for (Session session : sessions.values()) {
				JcrUtils.logoutQuietly(session);
			}
			sessions.clear();
		}
		threadSessions.clear();
		closed = true;
	}

	public synchronized Session getSession(String workspace) {
		if (closed)
			throw new IllegalStateException("JCR session adapter is closed.");

		Thread currentThread = Thread.currentThread();
		if (lastRetrievingThread == null)
			lastRetrievingThread = currentThread;

		Map<String, Session> threadSession = threadSessions.get(currentThread);
		if (threadSession == null) {
			threadSession = new HashMap<>();
			threadSessions.put(currentThread, threadSession);
		}

		Session session = threadSession.get(workspace);
		if (session == null) {
			session = Subject.doAs(subject, (PrivilegedAction<Session>) () -> {
				try {
					Session sess = repository.login(workspace);
					return sess;
				} catch (RepositoryException e) {
					throw new IllegalStateException("Cannot log in to " + workspace, e);
				}
			});
			threadSession.put(workspace, session);
		}

		if (lastRetrievingThread != currentThread) {
			try {
				session.refresh(true);
			} catch (RepositoryException e) {
				throw new JcrException("Cannot refresh JCR session " + session, e);
			}
		}
		lastRetrievingThread = currentThread;
		return session;
	}

}
