package org.argeo.cms.maintenance;

import java.util.Map;

import javax.jcr.Session;

public interface DataMigration {
	/** Migrate data between two workspaces, at JCR level. */
	Boolean migrate(Session source, Session target);

	/**
	 * Keys are the source workspaces and values the target workspaces. If null
	 * is returned, only the default workspace will be migrated, to the default
	 * workspace of the target repository.
	 */
	Map<String, String> workspacesToMigrate();

}
