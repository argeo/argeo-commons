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
package org.argeo.cms.internal.backup;

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