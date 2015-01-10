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
package org.argeo.server.backup;

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;

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
