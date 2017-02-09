package org.argeo.cms.internal.kernel;

import java.nio.file.Path;

public class CmsPaths {
	public static Path getRepoDirPath(String cn) {
		return KernelUtils.getOsgiInstancePath(KernelConstants.DIR_REPOS + '/' + cn);
	}

	public static Path getRepoIndexesBase() {
		return KernelUtils.getOsgiInstancePath(KernelConstants.DIR_INDEXES);
	}

	private CmsPaths() {

	}
}
