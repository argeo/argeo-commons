package org.argeo.cms.internal.runtime;

import java.nio.file.Path;
import java.util.Map;

import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.acr.CmsContentRepository;
import org.argeo.cms.acr.fs.FsContentProvider;

public class DeployedContentRepository extends CmsContentRepository {
	private final static String ROOT_XML = "cr:root.xml";
	private CmsState cmsState;

	@Override
	public void start() {
		super.start();
		Path rootXml = KernelUtils.getOsgiInstancePath(ROOT_XML);
		initRootContentProvider(null);

//		Path srvPath = KernelUtils.getOsgiInstancePath(CmsConstants.SRV_WORKSPACE);
//		FsContentProvider srvContentProvider = new FsContentProvider("/" + CmsConstants.SRV_WORKSPACE, srvPath, false);
//		addProvider(srvContentProvider);
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

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}