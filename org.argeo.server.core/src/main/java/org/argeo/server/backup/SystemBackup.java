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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.UserAuthenticator;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.argeo.ArgeoException;

/**
 * Combines multiple backups and transfer them to a remote location. Purges
 * remote and local data based on certain criteria.
 */
public class SystemBackup implements Runnable {
	private final static Log log = LogFactory.getLog(SystemBackup.class);

	private FileSystemManager fileSystemManager;
	private UserAuthenticator userAuthenticator = null;

	private String backupsBase;
	private String systemName;

	private List<AtomicBackup> atomicBackups = new ArrayList<AtomicBackup>();
	private BackupPurge backupPurge = new SimpleBackupPurge();

	private Map<String, UserAuthenticator> remoteBases = new HashMap<String, UserAuthenticator>();

	@Override
	public void run() {
		if (atomicBackups.size() == 0)
			throw new ArgeoException("No atomic backup listed");
		List<String> failures = new ArrayList<String>();

		SimpleBackupContext backupContext = new SimpleBackupContext(
				fileSystemManager, backupsBase, systemName);

		// purge older backups
		FileSystemOptions opts = new FileSystemOptions();
		try {
			DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(
					opts, userAuthenticator);
		} catch (FileSystemException e) {
			throw new ArgeoException("Cannot create authentication", e);
		}

		try {

			backupPurge.purge(fileSystemManager, backupsBase, systemName,
					backupContext.getDateFormat(), opts);
		} catch (Exception e) {
			failures.add("Purge " + backupsBase + " failed: " + e.getMessage());
			log.error("Purge of " + backupsBase + " failed", e);
		}

		// perform backup
		for (AtomicBackup atomickBackup : atomicBackups) {
			try {
				String target = atomickBackup.backup(fileSystemManager,
						backupsBase, backupContext, opts);
				if (log.isDebugEnabled())
					log.debug("Performed backup " + target);
			} catch (Exception e) {
				String msg = "Atomic backup " + atomickBackup.getName()
						+ " failed: " + ArgeoException.chainCausesMessages(e);
				failures.add(msg);
				log.error(msg);
				if (log.isTraceEnabled())
					log.trace(
							"Stacktrace of atomic backup "
									+ atomickBackup.getName() + " failure.", e);
			}
		}

		// dispatch to remote
		for (String remoteBase : remoteBases.keySet()) {
			FileObject localBaseFo = null;
			FileObject remoteBaseFo = null;
			UserAuthenticator auth = remoteBases.get(remoteBase);

			// authentication
			FileSystemOptions remoteOpts = new FileSystemOptions();
			try {
				DefaultFileSystemConfigBuilder.getInstance()
						.setUserAuthenticator(remoteOpts, auth);
				backupPurge.purge(fileSystemManager, remoteBase, systemName,
						backupContext.getDateFormat(), remoteOpts);
			} catch (Exception e) {
				failures.add("Purge " + remoteBase + " failed: "
						+ e.getMessage());
				log.error("Cannot purge " + remoteBase, e);
			}

			try {
				localBaseFo = fileSystemManager.resolveFile(backupsBase + '/'
						+ backupContext.getRelativeFolder(), opts);
				remoteBaseFo = fileSystemManager.resolveFile(remoteBase + '/'
						+ backupContext.getRelativeFolder(), remoteOpts);
				remoteBaseFo.copyFrom(localBaseFo, Selectors.SELECT_ALL);
				if (log.isDebugEnabled())
					log.debug("Copied backup to " + remoteBaseFo + " from "
							+ localBaseFo);
				// }
			} catch (Exception e) {
				failures.add("Dispatch to " + remoteBase + " failed: "
						+ e.getMessage());
				log.error(
						"Cannot dispatch backups from "
								+ backupContext.getRelativeFolder() + " to "
								+ remoteBase, e);
			}
			BackupUtils.closeFOQuietly(localBaseFo);
			BackupUtils.closeFOQuietly(remoteBaseFo);
		}

		int failureCount = 0;
		if (failures.size() > 0) {
			StringBuffer buf = new StringBuffer();
			for (String failure : failures) {
				buf.append('\n').append(failureCount).append(" - ")
						.append(failure);
				failureCount++;
			}
			throw new ArgeoException(failureCount
					+ " error(s) when running the backup,"
					+ " check the logs and the backups as soon as possible."
					+ buf);
		}
	}

	public void setFileSystemManager(FileSystemManager fileSystemManager) {
		this.fileSystemManager = fileSystemManager;
	}

	public void setBackupsBase(String backupsBase) {
		this.backupsBase = backupsBase;
	}

	public void setSystemName(String name) {
		this.systemName = name;
	}

	public void setAtomicBackups(List<AtomicBackup> atomicBackups) {
		this.atomicBackups = atomicBackups;
	}

	public void setBackupPurge(BackupPurge backupPurge) {
		this.backupPurge = backupPurge;
	}

	public void setUserAuthenticator(UserAuthenticator userAuthenticator) {
		this.userAuthenticator = userAuthenticator;
	}

	public void setRemoteBases(Map<String, UserAuthenticator> remoteBases) {
		this.remoteBases = remoteBases;
	}

	public static void main(String args[]) {
		while (true) {
			try {
				StandardFileSystemManager fsm = new StandardFileSystemManager();
				fsm.init();

				SystemBackup systemBackup = new SystemBackup();
				systemBackup.setSystemName("mySystem");
				systemBackup
						.setBackupsBase("/home/mbaudier/dev/src/commons/server/runtime/org.argeo.server.core/target");
				systemBackup.setFileSystemManager(fsm);

				List<AtomicBackup> atomicBackups = new ArrayList<AtomicBackup>();

				MySqlBackup mySqlBackup = new MySqlBackup("root", "", "test");
				atomicBackups.add(mySqlBackup);
				PostgreSqlBackup postgreSqlBackup = new PostgreSqlBackup(
						"argeo", "argeo", "gis_template");
				atomicBackups.add(postgreSqlBackup);
				SvnBackup svnBackup = new SvnBackup(
						"/home/mbaudier/tmp/testsvnrepo");
				atomicBackups.add(svnBackup);

				systemBackup.setAtomicBackups(atomicBackups);

				Map<String, UserAuthenticator> remoteBases = new HashMap<String, UserAuthenticator>();
				StaticUserAuthenticator userAuthenticator = new StaticUserAuthenticator(
						null, "demo", "demo");
				remoteBases.put("sftp://localhost/home/mbaudier/test",
						userAuthenticator);
				systemBackup.setRemoteBases(remoteBases);

				systemBackup.run();

				fsm.close();
			} catch (FileSystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}

			// wait
			try {
				Thread.sleep(120 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
