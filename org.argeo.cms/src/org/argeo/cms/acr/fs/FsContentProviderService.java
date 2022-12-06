package org.argeo.cms.acr.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsState;

public class FsContentProviderService extends FsContentProvider {
	private CmsState cmsState;

	public void start(Map<String, String> properties) {
		mountPath = properties.get(CmsConstants.ACR_MOUNT_PATH);
		Objects.requireNonNull(mountPath);
		if (!mountPath.startsWith("/"))
			throw new IllegalArgumentException("Mount path must start with /");

		String relPath = mountPath.substring(1);
		rootPath = cmsState.getDataPath(relPath);
		try {
			Files.createDirectories(rootPath);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Cannot initialize FS content provider " + mountPath + " with base" + rootPath, e);
		}

		initNamespaces();
	}

	public void stop() {
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
