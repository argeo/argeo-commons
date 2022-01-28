package org.argeo.cms.jcr.internal;

import java.nio.file.Path;

/** Centralises access to the default node deployment directories. */
public class CmsPaths {
	public static Path getRepoDirPath(String cn) {
		return KernelUtils.getOsgiInstancePath(KernelConstants.DIR_REPOS + '/' + cn);
	}

	public static Path getRepoIndexesBase() {
		return KernelUtils.getOsgiInstancePath(KernelConstants.DIR_INDEXES);
	}

	/** Singleton. */
	private CmsPaths() {
	}
}
