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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.argeo.ArgeoException;

/**
 * Simplify atomic backups implementation, especially by managing VFS.
 */
public abstract class AbstractAtomicBackup implements AtomicBackup {
	private String name;
	private String compression = "bz2";

	protected abstract void writeBackup(FileObject targetFo);

	public AbstractAtomicBackup() {
	}

	public AbstractAtomicBackup(String name) {
		this.name = name;
	}

	public void init() {
		if (name == null)
			throw new ArgeoException("Atomic backup name must be set");
	}

	public void destroy() {

	}

	@Override
	public String backup(FileSystemManager fileSystemManager,
			String backupsBase, BackupContext backupContext,
			FileSystemOptions opts) {
		if (name == null)
			throw new ArgeoException("Atomic backup name must be set");

		FileObject targetFo = null;
		try {
			if (backupsBase.startsWith("sftp:"))
				SftpFileSystemConfigBuilder.getInstance()
						.setStrictHostKeyChecking(opts, "no");
			if (compression == null || compression.equals("none"))
				targetFo = fileSystemManager.resolveFile(backupsBase + '/'
						+ backupContext.getRelativeFolder() + '/' + name, opts);
			else if (compression.equals("bz2"))
				targetFo = fileSystemManager.resolveFile("bz2:" + backupsBase
						+ '/' + backupContext.getRelativeFolder() + '/' + name
						+ ".bz2" + "!" + name, opts);
			else if (compression.equals("gz"))
				targetFo = fileSystemManager.resolveFile("gz:" + backupsBase
						+ '/' + backupContext.getRelativeFolder() + '/' + name
						+ ".gz" + "!" + name, opts);
			else
				throw new ArgeoException("Unsupported compression "
						+ compression);

			writeBackup(targetFo);

			return targetFo.toString();
		} catch (Exception e) {
			throw new ArgeoException("Cannot backup " + name + " to "
					+ targetFo, e);
		} finally {
			BackupUtils.closeFOQuietly(targetFo);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setCompression(String compression) {
		this.compression = compression;
	}
}
