package org.argeo.jackrabbit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.argeo.jcr.JcrCallback;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;

/** Migrate the data in a Jackrabbit repository. */
@Deprecated
public class JackrabbitDataModelMigration implements Comparable<JackrabbitDataModelMigration> {
	private final static Log log = LogFactory.getLog(JackrabbitDataModelMigration.class);

	private String dataModelNodePath;
	private String targetVersion;
	private URL migrationCnd;
	private JcrCallback dataModification;

	/**
	 * Expects an already started repository with the old data model to migrate.
	 * Expects to be run with admin rights (Repository.login() will be used).
	 * 
	 * @return true if a migration was performed and the repository needs to be
	 *         restarted and its caches cleared.
	 */
	public Boolean migrate(Session session) {
		long begin = System.currentTimeMillis();
		Reader reader = null;
		try {
			// check if already migrated
			if (!session.itemExists(dataModelNodePath)) {
//				log.warn("Node " + dataModelNodePath + " does not exist: nothing to migrate.");
				return false;
			}
//			Node dataModelNode = session.getNode(dataModelNodePath);
//			if (dataModelNode.hasProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION)) {
//				String currentVersion = dataModelNode.getProperty(
//						ArgeoNames.ARGEO_DATA_MODEL_VERSION).getString();
//				if (compareVersions(currentVersion, targetVersion) >= 0) {
//					log.info("Data model at version " + currentVersion
//							+ ", no need to migrate.");
//					return false;
//				}
//			}

			// apply transitional CND
			if (migrationCnd != null) {
				reader = new InputStreamReader(migrationCnd.openStream());
				CndImporter.registerNodeTypes(reader, session, true);
				session.save();
//				log.info("Registered migration node types from " + migrationCnd);
			}

			// modify data
			dataModification.execute(session);

			// apply changes
			session.save();

			long duration = System.currentTimeMillis() - begin;
//			log.info("Migration of data model " + dataModelNodePath + " to " + targetVersion + " performed in "
//					+ duration + "ms");
			return true;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new JcrException("Migration of data model " + dataModelNodePath + " to " + targetVersion + " failed.",
					e);
		} catch (ParseException | IOException e) {
			JcrUtils.discardQuietly(session);
			throw new RuntimeException(
					"Migration of data model " + dataModelNodePath + " to " + targetVersion + " failed.", e);
		} finally {
			JcrUtils.logoutQuietly(session);
			IOUtils.closeQuietly(reader);
		}
	}

	protected static int compareVersions(String version1, String version2) {
		// TODO do a proper version analysis and comparison
		return version1.compareTo(version2);
	}

	/** To be called on a stopped repository. */
	public static void clearRepositoryCaches(RepositoryConfig repositoryConfig) {
		try {
			String customeNodeTypesPath = "/nodetypes/custom_nodetypes.xml";
			// FIXME causes weird error in Eclipse
			repositoryConfig.getFileSystem().deleteFile(customeNodeTypesPath);
			if (log.isDebugEnabled())
				log.debug("Cleared " + customeNodeTypesPath);
		} catch (RuntimeException e) {
			throw e;
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} catch (FileSystemException e) {
			throw new RuntimeException("Cannot clear node types cache.",e);
		}

		// File customNodeTypes = new File(home.getPath()
		// + "/repository/nodetypes/custom_nodetypes.xml");
		// if (customNodeTypes.exists()) {
		// customNodeTypes.delete();
		// if (log.isDebugEnabled())
		// log.debug("Cleared " + customNodeTypes);
		// } else {
		// log.warn("File " + customNodeTypes + " not found.");
		// }
	}

	/*
	 * FOR USE IN (SORTED) SETS
	 */

	public int compareTo(JackrabbitDataModelMigration dataModelMigration) {
		// TODO make ordering smarter
		if (dataModelNodePath.equals(dataModelMigration.dataModelNodePath))
			return compareVersions(targetVersion, dataModelMigration.targetVersion);
		else
			return dataModelNodePath.compareTo(dataModelMigration.dataModelNodePath);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JackrabbitDataModelMigration))
			return false;
		JackrabbitDataModelMigration dataModelMigration = (JackrabbitDataModelMigration) obj;
		return dataModelNodePath.equals(dataModelMigration.dataModelNodePath)
				&& targetVersion.equals(dataModelMigration.targetVersion);
	}

	@Override
	public int hashCode() {
		return targetVersion.hashCode();
	}

	public void setDataModelNodePath(String dataModelNodePath) {
		this.dataModelNodePath = dataModelNodePath;
	}

	public void setTargetVersion(String targetVersion) {
		this.targetVersion = targetVersion;
	}

	public void setMigrationCnd(URL migrationCnd) {
		this.migrationCnd = migrationCnd;
	}

	public void setDataModification(JcrCallback dataModification) {
		this.dataModification = dataModification;
	}

	public String getDataModelNodePath() {
		return dataModelNodePath;
	}

	public String getTargetVersion() {
		return targetVersion;
	}

}
