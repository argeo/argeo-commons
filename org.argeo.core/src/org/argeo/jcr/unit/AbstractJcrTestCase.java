package org.argeo.jcr.unit;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.JcrException;

import junit.framework.TestCase;

/** Base for unit tests with a JCR repository. */
public abstract class AbstractJcrTestCase extends TestCase {
	private final static Log log = LogFactory.getLog(AbstractJcrTestCase.class);

	private Repository repository;
	private Session session = null;

	public final static String LOGIN_CONTEXT_TEST_SYSTEM = "TEST_JACKRABBIT_ADMIN";

	// protected abstract File getRepositoryFile() throws Exception;

	protected abstract Repository createRepository() throws Exception;

	protected abstract void clearRepository(Repository repository) throws Exception;

	@Override
	protected void setUp() throws Exception {
		File homeDir = getHomeDir();
		FileUtils.deleteDirectory(homeDir);
		repository = createRepository();
	}

	@Override
	protected void tearDown() throws Exception {
		if (session != null) {
			session.logout();
			if (log.isTraceEnabled())
				log.trace("Logout session");
		}
		clearRepository(repository);
	}

	protected Session session() {
		if (session != null && session.isLive())
			return session;
		Session session;
		if (getLoginContext() != null) {
			LoginContext lc;
			try {
				lc = new LoginContext(getLoginContext());
				lc.login();
			} catch (LoginException e) {
				throw new IllegalStateException("JAAS login failed", e);
			}
			session = Subject.doAs(lc.getSubject(), new PrivilegedAction<Session>() {

				@Override
				public Session run() {
					return login();
				}

			});
		} else
			session = login();
		this.session = session;
		return this.session;
	}

	protected String getLoginContext() {
		return null;
	}

	protected Session login() {
		try {
			if (log.isTraceEnabled())
				log.trace("Login session");
			Subject subject = Subject.getSubject(AccessController.getContext());
			if (subject != null)
				return getRepository().login();
			else
				return getRepository().login(new SimpleCredentials("demo", "demo".toCharArray()));
		} catch (RepositoryException e) {
			throw new JcrException("Cannot login to repository", e);
		}
	}

	protected Repository getRepository() {
		return repository;
	}

	/**
	 * enables children class to set an existing repository in case it is not
	 * deleted on startup, to test migration by instance
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	protected File getHomeDir() {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				AbstractJcrTestCase.class.getSimpleName() + "-" + System.getProperty("user.name"));
		return homeDir;
	}

}
