package org.argeo.server.backup;

import org.apache.commons.vfs.FileObject;

/** Backup utilities */
public class BackupUtils {
	/** Close a file object quietly even if it is null or throws an exception. */
	public static void closeFOQuietly(FileObject fo) {
		if (fo != null) {
			try {
				fo.close();
			} catch (Exception e) {
				// silent
			}
		}
	}
	
	/** Prevents instantiation */
	private BackupUtils() {
	}
}
