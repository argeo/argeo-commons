package org.argeo.jcr;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

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
		try {
			Session sess = repository.login();
			if (log.isTraceEnabled())
				log.trace("Log in to JCR session " + sess + "; userId="
						+ sess.getUserID());
			// Thread.dumpStack();
			activeSessions.add(sess);
			return sess;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot log in to repository", e);
		}
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

}
