package org.argeo.server.backup;

import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;

/** Performs the backup of a single component, typically a database dump */
public interface AtomicBackup {
	/** Name identifiying this backup */
	public String getName();

	/**
	 * Retrieves the data of the component in a format that allows to restore
	 * the component
	 * 
	 * @param backupContext
	 *            the context of this backup
	 * @return the VFS URI of the generated file or directory
	 */
	public String backup(FileSystemManager fileSystemManager,
			String backupsBase, BackupContext backupContext,
			FileSystemOptions opts);
}
