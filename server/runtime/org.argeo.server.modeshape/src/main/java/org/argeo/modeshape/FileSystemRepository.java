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
