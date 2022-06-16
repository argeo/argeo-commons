package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.cms.acr.CmsContentRepository;
import org.argeo.cms.acr.fs.FsContentProvider;
import org.argeo.util.OS;

public class DeployedContentRepository extends CmsContentRepository {
	private final static String ROOT_XML = "cr:root.xml";

	@Override
	public void start() {
		try {
			super.start();
			Path rootXml = KernelUtils.getOsgiInstancePath(ROOT_XML);
			initRootContentProvider(null);

//		Path srvPath = KernelUtils.getOsgiInstancePath(CmsConstants.SRV_WORKSPACE);
//		FsContentProvider srvContentProvider = new FsContentProvider("/" + CmsConstants.SRV_WORKSPACE, srvPath, false);
//		addProvider(srvContentProvider);

			Path runDirPath =  KernelUtils.getOsgiInstancePath(CmsContentRepository.RUN_BASE);
			Files.createDirectories(runDirPath);
			FsContentProvider runContentProvider = new FsContentProvider(CmsContentRepository.RUN_BASE, runDirPath);
			addProvider(runContentProvider);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot start content repository", e);
		}
	}

	@Override
	public void stop() {
		super.stop();
	}

	public void addContentProvider(ContentProvider provider, Map<String, Object> properties) {
//		String base = LangUtils.get(properties, CmsContentRepository.ACR_MOUNT_PATH_PROPERTY);
		addProvider(provider);
	}

	public void removeContentProvider(ContentProvider provider, Map<String, Object> properties) {
	}

}
