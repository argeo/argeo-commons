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
package org.argeo.jackrabbit;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.jcr.ArgeoJcrException;
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
	public Boolean migrate(Session session) {
		long begin = System.currentTimeMillis();
		Reader reader = null;
		try {
			// check if already migrated
			if (!session.itemExists(dataModelNodePath)) {
				log.warn("Node " + dataModelNodePath
						+ " does not exist: nothing to migrate.");
				return false;
			}
			Node dataModelNode = session.getNode(dataModelNodePath);
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
			if (migrationCnd != null) {
				reader = new InputStreamReader(migrationCnd.getInputStream());
				CndImporter.registerNodeTypes(reader, session, true);
				session.save();
				log.info("Registered migration node types from " + migrationCnd);
			}

			// modify data
			dataModification.execute(session);

			// apply changes
			session.save();

			long duration = System.currentTimeMillis() - begin;
			log.info("Migration of data model " + dataModelNodePath + " to "
					+ targetVersion + " performed in " + duration + "ms");
			return true;
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoJcrException("Migration of data model "
					+ dataModelNodePath + " to " + targetVersion + " failed.",
					e);
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
			//repositoryConfig.getFileSystem().deleteFile(customeNodeTypesPath);
			if (log.isDebugEnabled())
				log.debug("Cleared " + customeNodeTypesPath);
		} catch (Exception e) {
			throw new ArgeoJcrException("Cannot clear caches", e);
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

	public String getDataModelNodePath() {
		return dataModelNodePath;
	}

	public String getTargetVersion() {
		return targetVersion;
	}

}
