/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.jackrabbit.unit;

import javax.jcr.Repository;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.jcr.unit.AbstractJcrTestCase;

/** Factorizes configuration of an in memory transient repository */
public abstract class AbstractJackrabbitTestCase extends AbstractJcrTestCase {
	protected RepositoryImpl repositoryImpl;

	// protected File getRepositoryFile() throws Exception {
	// Resource res = new ClassPathResource(
	// "org/argeo/jackrabbit/unit/repository-memory.xml");
	// return res.getFile();
	// }

	protected Repository createRepository() throws Exception {
		// Repository repository = new TransientRepository(getRepositoryFile(),
		// getHomeDir());
		RepositoryConfig repositoryConfig = RepositoryConfig.create(
				AbstractJackrabbitTestCase.class
						.getResourceAsStream("repository-memory.xml"),
				getHomeDir().getAbsolutePath());
		RepositoryImpl repositoryImpl = RepositoryImpl.create(repositoryConfig);
		return repositoryImpl;
	}

	@Override
	protected void clearRepository(Repository repository) throws Exception {
		RepositoryImpl repositoryImpl = (RepositoryImpl) repository;
		if (repositoryImpl != null)
			repositoryImpl.shutdown();
		FileUtils.deleteDirectory(getHomeDir());
	}

}
