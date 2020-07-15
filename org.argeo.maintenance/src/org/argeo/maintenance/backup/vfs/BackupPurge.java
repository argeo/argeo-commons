package org.argeo.maintenance.backup.vfs;

import java.text.DateFormat;

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;

/** Purges previous backups */
public interface BackupPurge {
	/**
	 * Purge the backups identified by these arguments. Although these are the
	 * same fields as a {@link BackupContext} we don't pass it as argument since
	 * we want to use this interface to purge remote backups as well (that is,
	 * with a different base), or outside the scope of a running backup.
	 */
	public void purge(FileSystemManager fileSystemManager, String base,
			String name, DateFormat dateFormat, FileSystemOptions opts);
}
