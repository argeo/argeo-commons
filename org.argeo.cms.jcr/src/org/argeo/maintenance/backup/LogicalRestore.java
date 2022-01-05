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

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.BundleContext;

/** Restores a backup in the format defined by {@link LogicalBackup}. */
public class LogicalRestore implements Runnable {
	private final static CmsLog log = CmsLog.getLog(LogicalRestore.class);

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
		try {
			// import jcr:system first
//			Session defaultSession = NodeUtils.openDataAdminSession(repository, null);
//			try (DirectoryStream<Path> xmls = Files.newDirectoryStream(
//					workspaces.resolve(NodeConstants.SYS_WORKSPACE + LogicalBackup.JCR_VERSION_STORAGE_PATH),
//					"*.xml")) {
//				for (Path xml : xmls) {
//					try (InputStream in = Files.newInputStream(xml)) {
//						defaultSession.getWorkspace().importXML(LogicalBackup.JCR_VERSION_STORAGE_PATH, in,
//								ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
//						if (log.isDebugEnabled())
//							log.debug("Restored " + xml + " to " + defaultSession.getWorkspace().getName() + ":");
//					}
//				}
//			} finally {
//				Jcr.logout(defaultSession);
//			}

			// non-system content
			try (DirectoryStream<Path> workspaceDirs = Files.newDirectoryStream(workspaces)) {
				for (Path workspacePath : workspaceDirs) {
					String workspaceName = workspacePath.getFileName().toString();
					Session session = JcrUtils.loginOrCreateWorkspace(repository, workspaceName);
					try (DirectoryStream<Path> xmls = Files.newDirectoryStream(workspacePath, "*.xml")) {
						xmls: for (Path xml : xmls) {
							if (xml.getFileName().toString().startsWith("rep:"))
								continue xmls;
							try (InputStream in = Files.newInputStream(xml)) {
								session.getWorkspace().importXML("/", in,
										ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
								if (log.isDebugEnabled())
									log.debug("Restored " + xml + " to workspace " + workspaceName);
							}
						}
					} finally {
						Jcr.logout(session);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Cannot restore backup from " + basePath, e);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot restore backup from " + basePath, e);
		}
	}

}
