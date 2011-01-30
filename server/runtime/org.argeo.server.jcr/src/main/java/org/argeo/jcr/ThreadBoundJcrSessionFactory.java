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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/** Proxy JCR sessions and attach them to calling threads. */
public class ThreadBoundJcrSessionFactory implements FactoryBean,
		DisposableBean {
	private final static Log log = LogFactory
			.getLog(ThreadBoundJcrSessionFactory.class);

	private Repository repository;
	private List<Session> activeSessions = Collections
			.synchronizedList(new ArrayList<Session>());

	private ThreadLocal<Session> session = new ThreadLocal<Session>();
	private boolean destroying = false;
	private final Session proxiedSession;

	private String defaultUsername = "demo";
	private String defaultPassword = "demo";

	public ThreadBoundJcrSessionFactory() {
		Class<?>[] interfaces = { Session.class };
		proxiedSession = (Session) Proxy.newProxyInstance(getClass()
				.getClassLoader(), interfaces, new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				Session threadSession = session.get();
				if (threadSession == null) {
					if ("logout".equals(method.getName()))// no need to login
						return Void.TYPE;
					threadSession = login();
					session.set(threadSession);
				}

				Object ret = method.invoke(threadSession, args);
				if ("logout".equals(method.getName())) {
					session.remove();
					if (!destroying)
						activeSessions.remove(threadSession);
					if (log.isTraceEnabled())
						log.trace("Logged out from JCR session "
								+ threadSession + "; userId="
								+ threadSession.getUserID());
				}
				return ret;
			}
		});
	}

	protected Session login() {
		Session newSession = null;
		// first try to login without credentials, assuming the underlying login
		// module will have dealt with authentication (typically using Spring
		// Security)
		try {
			newSession = repository.login();
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
				newSession = repository.login(sc);
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot log in to repository", e);
			}

		// Log and monitor new session
		if (log.isTraceEnabled())
			log.trace("Logged in to JCR session " + newSession + "; userId="
					+ newSession.getUserID());
		activeSessions.add(newSession);
		return newSession;
	}

	public Object getObject() {
		return proxiedSession;
	}

	public void destroy() throws Exception {
		if (log.isDebugEnabled())
			log.debug("Cleaning up " + activeSessions.size()
					+ " active JCR sessions...");

		destroying = true;
		for (Session sess : activeSessions) {
			sess.logout();
		}
		activeSessions.clear();
	}

	public Class<? extends Session> getObjectType() {
		return Session.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setDefaultUsername(String defaultUsername) {
		this.defaultUsername = defaultUsername;
	}

	public void setDefaultPassword(String defaultPassword) {
		this.defaultPassword = defaultPassword;
	}

}
