package org.argeo.cms.internal.backup;

import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.cms.DataMigration;
import org.argeo.jcr.JcrUtils;

/** Migrate data between two workspaces, at JCR level. */
public class RepositoryMigration implements PrivilegedExceptionAction<Boolean> {
	private final Repository sourceRepository;
	private final Repository targetRepository;
	private final DataMigration dataMigration;

	private Credentials sourceCredentials = null;
	private Credentials targetCredentials = null;

	public RepositoryMigration(Repository sourceRepository,
			Repository targetRepository, DataMigration dataMigration) {
		this.sourceRepository = sourceRepository;
		this.targetRepository = targetRepository;
		this.dataMigration = dataMigration;
	}

	@Override
	public Boolean run() throws Exception {
		Map<String, String> wk = dataMigration.workspacesToMigrate();
		if (wk == null)
			return migrate(sourceRepository, null, targetRepository, null);
		else {
			for (String sourceWorkspace : wk.keySet()) {
				String targetWorkspace = wk.get(sourceWorkspace);
				boolean ok = migrate(sourceRepository, sourceWorkspace,
						targetRepository, targetWorkspace);
				if (!ok)
					return false;
			}
			return true;
		}
	}

	protected final boolean migrate(Repository sourceRepository,
			String sourceWorkspace, Repository targetRepository,
			String targetWorkspace) throws RepositoryException {
		Session source = null, target = null;
		try {
			source = sourceRepository.login(sourceCredentials, sourceWorkspace);
			target = targetRepository.login(targetCredentials, targetWorkspace);
			return dataMigration.migrate(source, target);
		} finally {
			JcrUtils.logoutQuietly(source);
			JcrUtils.logoutQuietly(target);
		}
	}

	public void setSourceCredentials(Credentials sourceCredentials) {
		this.sourceCredentials = sourceCredentials;
	}

	public void setTargetCredentials(Credentials targetCredentials) {
		this.targetCredentials = targetCredentials;
	}

}
