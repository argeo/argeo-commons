package org.argeo.server.backup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.vfs.FileSystemManager;

/** Simple implementation of a backup context */
public class SimpleBackupContext implements BackupContext {
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
	private final Date timestamp;
	private final String name;

	private final FileSystemManager fileSystemManager;

	public SimpleBackupContext(FileSystemManager fileSystemManager,
			String backupsBase, String name) {
		this.name = name;
		this.timestamp = new Date();
		this.fileSystemManager = fileSystemManager;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getTimestampAsString() {
		return dateFormat.format(timestamp);
	}

	public String getSystemName() {
		return name;
	}

	public String getRelativeFolder() {
		return name + '/' + getTimestampAsString();
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}

	public FileSystemManager getFileSystemManager() {
		return fileSystemManager;
	}

}
