package org.argeo.jackrabbit;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrCallback;
import org.argeo.jcr.JcrUtils;
import org.springframework.core.io.Resource;

/** Migrate the data in a Jackrabbit repository. */
public class JackrabbitDataModelMigration implements
		Comparable<JackrabbitDataModelMigration> {
	private final static Log log = LogFactory
			.getLog(JackrabbitDataModelMigration.class);

	private String dataModelNodePath;
	private String targetVersion;
	private Resource migrationCnd;
	private JcrCallback dataModification;

	/**
	 * Expects an already started repository with the old data model to migrate.
	 * Expects to be run with admin rights (Repository.login() will be used).
	 * 
	 * @return true if a migration was performed and the repository needs to be
	 *         restarted and its caches cleared.
	 */
	public Boolean migrate(Session adminSession) {
		long begin = System.currentTimeMillis();
		Reader reader = null;
		try {
			// check if already migrated
			if (!adminSession.itemExists(dataModelNodePath)) {
				log.warn("Node " + dataModelNodePath
						+ " does not exist: nothing to migrate.");
				return false;
			}
			Node dataModelNode = adminSession.getNode(dataModelNodePath);
			if (dataModelNode.hasProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION)) {
				String currentVersion = dataModelNode.getProperty(
						ArgeoNames.ARGEO_DATA_MODEL_VERSION).getString();
				if (compareVersions(currentVersion, targetVersion) >= 0) {
					log.info("Data model at version " + currentVersion
							+ ", no need to migrate.");
					return false;
				}
			}

			// apply transitional CND
			reader = new InputStreamReader(migrationCnd.getInputStream());
			CndImporter.registerNodeTypes(reader, adminSession, true);

			// modify data
			dataModification.execute(adminSession);

			// set data model version
			dataModelNode.setProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION,
					targetVersion);

			// apply changes
			adminSession.save();

			long duration = System.currentTimeMillis() - begin;
			log.info("Migration of data model " + dataModelNodePath + " to "
					+ targetVersion + " performed in " + duration + "ms");
			return true;
		} catch (Exception e) {
			JcrUtils.discardQuietly(adminSession);
			throw new ArgeoException("Migration of data model "
					+ dataModelNodePath + " to " + targetVersion + " failed.",
					e);
		} finally {
			JcrUtils.logoutQuietly(adminSession);
			IOUtils.closeQuietly(reader);
		}
	}

	protected static int compareVersions(String version1, String version2) {
		// TODO do a proper version analysis and comparison
		return version1.compareTo(version2);
	}

	/** To be called on a stopped repository. */
	public static void clearRepositoryCaches(File home) {
		File customNodeTypes = new File(home.getPath()
				+ "/repository/nodetypes/custom_nodetypes.xml");
		if (customNodeTypes.exists()) {
			customNodeTypes.delete();
			if (log.isDebugEnabled())
				log.debug("Cleared " + customNodeTypes);
		} else {
			log.warn("File " + customNodeTypes + " not found.");
		}
	}

	/*
	 * FOR USE IN (SORTED) SETS
	 */

	public int compareTo(JackrabbitDataModelMigration dataModelMigration) {
		// TODO make ordering smarter
		if (dataModelNodePath.equals(dataModelMigration.dataModelNodePath))
			return compareVersions(targetVersion,
					dataModelMigration.targetVersion);
		else
			return dataModelNodePath
					.compareTo(dataModelMigration.dataModelNodePath);
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

	public void setMigrationCnd(Resource migrationCnd) {
		this.migrationCnd = migrationCnd;
	}

	public void setDataModification(JcrCallback dataModification) {
		this.dataModification = dataModification;
	}

}
