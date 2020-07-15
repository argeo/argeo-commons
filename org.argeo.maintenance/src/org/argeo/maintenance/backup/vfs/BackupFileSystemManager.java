package org.argeo.maintenance.backup.vfs;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.bzip2.Bzip2FileProvider;
import org.apache.commons.vfs2.provider.ftp.FtpFileProvider;
import org.apache.commons.vfs2.provider.gzip.GzipFileProvider;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.ram.RamFileProvider;
import org.apache.commons.vfs2.provider.sftp.SftpFileProvider;
import org.apache.commons.vfs2.provider.url.UrlFileProvider;
import org.argeo.maintenance.MaintenanceException;

/**
 * Programatically configured VFS file system manager which can be declared as a
 * bean and associated with a life cycle (methods
 * {@link DefaultFileSystemManager#init()} and
 * {@link DefaultFileSystemManager#close()}). Supports bz2, file, ram, gzip,
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
			throw new MaintenanceException("Cannot configure backup file provider", e);
		}
	}
}
