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

package org.argeo.jcr.unit;

import java.io.File;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

public abstract class AbstractJcrTestCase extends TestCase {
	private final static Log log = LogFactory.getLog(AbstractJcrTestCase.class);

	private Repository repository;
	private Session session = null;

	protected abstract File getRepositoryFile() throws Exception;

	protected abstract Repository createRepository() throws Exception;

	@Override
	protected void setUp() throws Exception {
		File homeDir = getHomeDir();
		FileUtils.deleteDirectory(homeDir);
		repository = createRepository();
	}

	protected File getHomeDir() {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				AbstractJcrTestCase.class.getSimpleName() + "-"
						+ System.getProperty("user.name"));
		return homeDir;
	}

	@Override
	protected void tearDown() throws Exception {
		if (session != null) {
			session.logout();
			if (log.isDebugEnabled())
				log.debug("Logout session");
		}
	}

	protected Session session() {
		if (session == null) {
			try {
				if (log.isDebugEnabled())
					log.debug("Login session");
				session = getRepository().login(
						new SimpleCredentials("demo", "demo".toCharArray()));
			} catch (Exception e) {
				throw new ArgeoException("Cannot login to repository", e);
			}
		}
		return session;
	}

	protected Repository getRepository() {
		return repository;
	}
}
