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

package org.argeo.server.jackrabbit.unit;

import java.io.File;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.ArgeoException;

public abstract class AbstractJcrTestCase extends TestCase {
	private TransientRepository repository;
	private Session session = null;

	protected abstract File getRepositoryFile() throws Exception;

	@Override
	protected void setUp() throws Exception {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				AbstractJcrTestCase.class.getSimpleName() + "-"
						+ System.getProperty("user.name"));
		FileUtils.deleteDirectory(homeDir);
		repository = new TransientRepository(getRepositoryFile(), homeDir);
	}

	@Override
	protected void tearDown() throws Exception {
		if (session != null)
			session.logout();
	}

	protected Session session() {
		if (session == null) {
			try {
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
