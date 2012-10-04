package org.argeo.server.backup;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

/** Backup utilities */
public class BackupUtils {
	/** Close a file object quietly even if it is null or throws an exception. */
	public static void closeFOQuietly(FileObject fo) {
		if (fo != null) {
			try {
				fo.close();
			} catch (FileSystemException e) {
				// silent
			}
		}
	}

	/** Prevents instantiation */
	private BackupUtils() {
	}
}
