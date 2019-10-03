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
package org.argeo.maintenance.backup.vfs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.vfs2.FileSystemManager;

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
