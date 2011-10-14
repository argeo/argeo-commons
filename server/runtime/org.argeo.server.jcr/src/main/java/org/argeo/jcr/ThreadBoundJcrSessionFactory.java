/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.jcr;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/** Proxy JCR sessions and attach them to calling threads. */
public abstract class ThreadBoundJcrSessionFactory {
	private final static Log log = LogFactory
			.getLog(ThreadBoundJcrSessionFactory.class);

	private Repository repository;
	/** can be injected as list, only used if repository is null */
	private List<Repository> repositories;

	private ThreadLocal<Session> session = new ThreadLocal<Session>();
	private final Session proxiedSession;
	/** If workspace is null, default will be used. */
	private String workspace = null;

	private String defaultUsername = "demo";
	private String defaultPassword = "demo";
	private Boolean forceDefaultCredentials = false;

	private boolean active = true;

	// monitoring
	private final List<Thread> threads = Collections
			.synchronizedList(new ArrayList<Thread>());
	private final Map<Long, Session> activeSessions = Collections
			.synchronizedMap(new HashMap<Long, Session>());
	private MonitoringThread monitoringThread;

	public ThreadBoundJcrSessionFactory() {
		Class<?>[] interfaces = { Session.class };
		proxiedSession = (Session) Proxy.newProxyInstance(getClass()
				.getClassLoader(), interfaces,
				new JcrSessionInvocationHandler());
	}

	/** Logs in to the repository using various strategies. */
	protected synchronized Session login() {
		if (!isActive())
			throw new ArgeoException("Thread bound session factory inactive");

		// discard session previously attached to this thread
		Thread thread = Thread.currentThread();
		if (activeSessions.containsKey(thread.getId())) {
			Session oldSession = activeSessions.remove(thread.getId());
			oldSession.logout();
			session.remove();
		}

		Session newSession = null;
		// first try to login without credentials, assuming the underlying login
		// module will have dealt with authentication (typically using Spring
		// Security)
		if (!forceDefaultCredentials)
			try {
				newSession = repository().login(workspace);
			} catch (LoginException e1) {
				log.warn("Cannot login without credentials: " + e1.getMessage());
				// invalid credentials, go to the next step
			} catch (RepositoryException e1) {
				// other kind of exception, fail
				throw new ArgeoException("Cannot log in to repository", e1);
			}

		// log using default username / password (useful for testing purposes)
		if (newSession == null)
			try {
				SimpleCredentials sc = new SimpleCredentials(defaultUsername,
						defaultPassword.toCharArray());
				newSession = repository().login(sc, workspace);
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot log in to repository", e);
			}

		session.set(newSession);
		// Log and monitor new session
		if (log.isTraceEnabled())
			log.trace("Logged in to JCR session " + newSession + "; userId="
					+ newSession.getUserID());

		// monitoring
		activeSessions.put(thread.getId(), newSession);
		threads.add(thread);
		return newSession;
	}

	public Object getObject() {
		return proxiedSession;
	}

	public void init() throws Exception {
		monitoringThread = new MonitoringThread();
		monitoringThread.start();
	}

	public synchronized void dispose() throws Exception {
		if (activeSessions.size() == 0)
			return;

		if (log.isDebugEnabled())
			log.debug("Cleaning up " + activeSessions.size()
					+ " active JCR sessions...");

		deactivate();
		for (Session sess : activeSessions.values()) {
			JcrUtils.logoutQuietly(sess);
		}
		activeSessions.clear();
	}

	protected Boolean isActive() {
		return active;
	}

	protected synchronized void deactivate() {
		active = false;
		notifyAll();
	}

	protected synchronized void removeSession(Thread thread) {
		if (!isActive())
			return;
		activeSessions.remove(thread.getId());
		threads.remove(thread);
	}

	protected synchronized void cleanDeadThreads() {
		if (!isActive())
			return;
		Iterator<Thread> it = threads.iterator();
		while (it.hasNext()) {
			Thread thread = it.next();
			if (!thread.isAlive() && isActive()) {
				if (activeSessions.containsKey(thread.getId())) {
					Session session = activeSessions.get(thread.getId());
					activeSessions.remove(thread.getId());
					session.logout();
					if (log.isDebugEnabled())
						log.debug("Cleaned up JCR session (userID="
								+ session.getUserID() + ") from dead thread "
								+ thread.getId());
				}
				it.remove();
			}
		}
		try {
			wait(1000);
		} catch (InterruptedException e) {
			// silent
		}
	}

	public Class<? extends Session> getObjectType() {
		return Session.class;
	}

	public boolean isSingleton() {
		return true;
	}

	/**
	 * Called before a method is actually called, allowing to check the session
	 * or re-login it (e.g. if authentication has changed). The default
	 * implementation returns the session.
	 */
	protected Session preCall(Session session) {
		return session;
	}

	protected Repository repository() {
		if (repository != null)
			return repository;
		if (repositories != null) {
			// hardened for OSGi dynamic services
			Iterator<Repository> it = repositories.iterator();
			if (it.hasNext())
				return it.next();
		}
		throw new ArgeoException("No repository injected");
	}

	// /** Useful for declarative registration of OSGi services (blueprint) */
	// public void register(Repository repository, Map<?, ?> params) {
	// this.repository = repository;
	// }
	//
	// /** Useful for declarative registration of OSGi services (blueprint) */
	// public void unregister(Repository repository, Map<?, ?> params) {
	// this.repository = null;
	// }

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setRepositories(List<Repository> repositories) {
		this.repositories = repositories;
	}

	public void setDefaultUsername(String defaultUsername) {
		this.defaultUsername = defaultUsername;
	}

	public void setDefaultPassword(String defaultPassword) {
		this.defaultPassword = defaultPassword;
	}

	public void setForceDefaultCredentials(Boolean forceDefaultCredentials) {
		this.forceDefaultCredentials = forceDefaultCredentials;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	protected class JcrSessionInvocationHandler implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable, RepositoryException {
			Session threadSession = session.get();
			if (threadSession == null) {
				if ("logout".equals(method.getName()))// no need to login
					return Void.TYPE;
				else if ("toString".equals(method.getName()))// maybe logging
					return "Uninitialized Argeo thread bound JCR session";
				threadSession = login();
			}

			preCall(threadSession);
			Object ret;
			try {
				ret = method.invoke(threadSession, args);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RepositoryException)
					throw (RepositoryException) cause;
				else
					throw cause;
			}
			if ("logout".equals(method.getName())) {
				session.remove();
				Thread thread = Thread.currentThread();
				removeSession(thread);
				if (log.isTraceEnabled())
					log.trace("Logged out JCR session (userId="
							+ threadSession.getUserID() + ") on thread "
							+ thread.getId());
			}
			return ret;
		}
	}

	/** Monitors registered thread in order to clean up dead ones. */
	private class MonitoringThread extends Thread {

		public MonitoringThread() {
			super("ThreadBound JCR Session Monitor");
		}

		@Override
		public void run() {
			while (isActive()) {
				cleanDeadThreads();
			}
		}

	}
}
