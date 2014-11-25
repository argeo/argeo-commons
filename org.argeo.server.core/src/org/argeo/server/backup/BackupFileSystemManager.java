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

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.bzip2.Bzip2FileProvider;
import org.apache.commons.vfs.provider.ftp.FtpFileProvider;
import org.apache.commons.vfs.provider.gzip.GzipFileProvider;
import org.apache.commons.vfs.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs.provider.ram.RamFileProvider;
import org.apache.commons.vfs.provider.sftp.SftpFileProvider;
import org.apache.commons.vfs.provider.url.UrlFileProvider;
import org.argeo.ArgeoException;

/**
 * Programatically configured VFS file system manager which can be declared as a
 * bean and associated with a life cycle (methods
 * {@link DefaultFileSystemManager#init()} and
 * {@link DefaultFileSystemManager#closet()}). Supports bz2, file, ram, gzip,
 * ftp, sftp
 */
public class BackupFileSystemManager extends DefaultFileSystemManager {

	public BackupFileSystemManager() {
		super();
		try {
			addProvider("file", new DefaultLocalFileProvider());
			addProvider("bz2", new Bzip2FileProvider());
			addProvider("ftp", new FtpFileProvider());
			addProvider("sftp", new SftpFileProvider());
			addProvider("gzip", new GzipFileProvider());
			addProvider("ram", new RamFileProvider());
			setDefaultProvider(new UrlFileProvider());
		} catch (FileSystemException e) {
			throw new ArgeoException("Cannot configure backup file provider", e);
		}
	}
}
