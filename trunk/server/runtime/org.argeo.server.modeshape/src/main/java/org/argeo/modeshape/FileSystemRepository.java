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
package org.argeo.modeshape;

import java.util.UUID;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.jcr.JcrUtils;
import org.modeshape.connector.filesystem.FileSystemSource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

public class FileSystemRepository {
	public void init() {
		try {
			// Required in order to load mime type definitions
			Thread.currentThread().setContextClassLoader(JcrConfiguration.class.getClassLoader());
			JcrConfiguration config = new JcrConfiguration();
			config.repositorySource("fsSource")
					.usingClass(FileSystemSource.class)
					.setDescription("The repository for our content")
					.setProperty("workspaceRootPath", "/home/mbaudier/tmp")
					.setProperty("defaultWorkspaceName", "prod")
					.setProperty("predefinedWorkspaceNames",
							new String[] { "staging", "dev" })
					.setProperty(
							"rootNodeUuid",
							UUID.fromString("fd129c12-81a8-42ed-aa4b-820dba49e6f0"))
					.setProperty("updatesAllowed", "true")
					.setProperty("creatingWorkspaceAllowed", "false");
			config.repository("fsRepo").setSource("fsSource");

			JcrEngine jcrEngine = config.build();
			jcrEngine.start();
			Repository repository = jcrEngine.getRepository("fsRepo");
			Session session = repository.login(new SimpleCredentials("demo",
					"demo".toCharArray()));
			JcrUtils.debug(session.getRootNode());
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
}
