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

import java.io.File;

import javax.jcr.Repository;

import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.jcr.unit.AbstractJcrTestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/** Factorizes configuration of an in memory transient repository */
public abstract class AbstractInternalJackrabbitTestCase extends
		AbstractJcrTestCase {
	protected File getRepositoryFile() throws Exception {
		Resource res = new ClassPathResource(
				"org/argeo/server/jcr/repository-memory.xml");
		return res.getFile();
	}

	protected Repository createRepository() throws Exception {
		Repository repository = new TransientRepository(getRepositoryFile(),
				getHomeDir());
		return repository;
	}

}
