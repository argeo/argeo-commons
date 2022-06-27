package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsUserManager;
import org.argeo.cms.acr.CmsContentRepository;
import org.argeo.cms.acr.directory.DirectoryContentProvider;
import org.argeo.cms.acr.fs.FsContentProvider;

public class DeployedContentRepository extends CmsContentRepository {
	private final static String ROOT_XML = "cr:root.xml";

	private final static CmsLog log = CmsLog.getLog(DeployedContentRepository.class);

	private CmsUserManager userManager;

	@Override
	public void start() {
		long begin = System.currentTimeMillis();
		try {
			super.start();
			Path rootXml = KernelUtils.getOsgiInstancePath(ROOT_XML);
			initRootContentProvider(null);

//		Path srvPath = KernelUtils.getOsgiInstancePath(CmsConstants.SRV_WORKSPACE);
//		FsContentProvider srvContentProvider = new FsContentProvider("/" + CmsConstants.SRV_WORKSPACE, srvPath, false);
//		addProvider(srvContentProvider);

			// run dir
			Path runDirPath = KernelUtils.getOsgiInstancePath(CmsContentRepository.RUN_BASE);
			Files.createDirectories(runDirPath);
			FsContentProvider runContentProvider = new FsContentProvider(CmsContentRepository.RUN_BASE, runDirPath);
			addProvider(runContentProvider);

			// users
			DirectoryContentProvider directoryContentProvider = new DirectoryContentProvider(
					CmsContentRepository.DIRECTORY_BASE, userManager);
			addProvider(directoryContentProvider);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot start content repository", e);
		}
		long duration = System.currentTimeMillis()-begin;
		log.debug(() -> "CMS content repository available (initialisation took "+duration+" ms)");
	}

	@Override
	public void stop() {
		super.stop();
	}

//	public void addContentProvider(ContentProvider provider, Map<String, Object> properties) {
////		String base = LangUtils.get(properties, CmsContentRepository.ACR_MOUNT_PATH_PROPERTY);
//		addProvider(provider);
//	}

//	public void removeContentProvider(ContentProvider provider, Map<String, Object> properties) {
//	}

	public void setUserManager(CmsUserManager userManager) {
		this.userManager = userManager;
	}

}
