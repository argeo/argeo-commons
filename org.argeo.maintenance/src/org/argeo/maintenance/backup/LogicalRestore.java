package org.argeo.maintenance.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.BundleContext;

/** Restores a backup in the format defined by {@link LogicalBackup}. */
public class LogicalRestore implements Runnable {
	private final Repository repository;
	private final BundleContext bundleContext;
	private final Path basePath;

	public LogicalRestore(BundleContext bundleContext, Repository repository, Path basePath) {
		this.repository = repository;
		this.basePath = basePath;
		this.bundleContext = bundleContext;
	}

	@Override
	public void run() {
		Path workspaces = basePath.resolve(LogicalBackup.WORKSPACES_BASE);
		try (DirectoryStream<Path> xmls = Files.newDirectoryStream(workspaces, "*.xml")) {
			for (Path workspacePath : xmls) {
				String workspaceName = FilenameUtils.getBaseName(workspacePath.getFileName().toString());
				Session session = JcrUtils.loginOrCreateWorkspace(repository, workspaceName);
				try (InputStream in = Files.newInputStream(workspacePath)) {
					session.getWorkspace().importXML("/", in,
							ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
				} finally {
					JcrUtils.logoutQuietly(session);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Cannot restore backup from " + basePath, e);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot restore backup from " + basePath, e);
		}
	}

}
