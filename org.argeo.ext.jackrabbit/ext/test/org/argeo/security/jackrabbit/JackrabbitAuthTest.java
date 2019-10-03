package org.argeo.security.jackrabbit;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jackrabbit.unit.AbstractJackrabbitTestCase;

public class JackrabbitAuthTest extends AbstractJackrabbitTestCase {
	private final Log log = LogFactory.getLog(JackrabbitAuthTest.class);

	public void testLogin() throws Exception {
		// FIXME properly log in
		if(true)
			return;
		Session session = session();
		log.debug(session.getUserID());
		assertEquals("admin", session.getUserID());
		// Subject subject = new Subject();
		// LoginContext loginContext = new LoginContext("SYSTEM", subject);
		// loginContext.login();
		// Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
		//
		// @Override
		// public Void run() throws Exception {
		// Repository repository = getRepository();
		// Session session = repository.login();
		// log.debug(session.getUserID());
		// return null;
		// }
		// });
	}

	@Override
	protected String getLoginContext() {
		return LOGIN_CONTEXT_TEST_SYSTEM;
	}

	@Override
	protected Repository createRepository() throws Exception {
		return super.createRepository();
	}

	@Override
	protected void clearRepository(Repository repository) throws Exception {
		// System.setProperty("java.security.auth.login.config", "");
	}

	@Override
	protected String getRepositoryConfigResource() {
		return "/org/argeo/security/jackrabbit/repository-memory-test.xml";
	}

}
