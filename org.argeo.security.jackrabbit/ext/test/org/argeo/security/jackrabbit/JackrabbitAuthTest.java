package org.argeo.security.jackrabbit;

import java.net.URL;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jackrabbit.unit.AbstractJackrabbitTestCase;

public class JackrabbitAuthTest extends AbstractJackrabbitTestCase {
	private final Log log = LogFactory.getLog(JackrabbitAuthTest.class);

	public void testLogin() throws Exception {
		// Subject subject = new Subject();
		// LoginContext loginContext = new LoginContext("UNIX",subject);
		// loginContext.login();

		Repository repository = getRepository();
		Session session = repository.login();
		log.debug(session.getUserID());
	}

	@Override
	protected Repository createRepository() throws Exception {
		URL url = getClass().getResource("test_jaas.config");
		System.setProperty("java.security.auth.login.config", url.toString());
		return super.createRepository();
	}

	@Override
	protected void clearRepository(Repository repository) throws Exception {
		System.setProperty("java.security.auth.login.config", "");
	}

	@Override
	protected String getRepositoryConfigResource() {
		return "/org/argeo/security/jackrabbit/repository-memory-test.xml";
	}

}