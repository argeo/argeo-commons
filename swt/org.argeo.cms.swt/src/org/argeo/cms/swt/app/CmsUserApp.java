package org.argeo.cms.swt.app;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;
import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.cms.AbstractCmsApp;
import org.argeo.cms.swt.CmsSwtUi;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.auth.CmsLogin;
import org.argeo.eclipse.ui.fs.SimpleFsBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class CmsUserApp extends AbstractCmsApp {
	private ContentRepository contentRepository;

	private FileSystemProvider cmsFileSystemProvider;

	@Override
	public Set<String> getUiNames() {
		Set<String> uiNames = new HashSet<>();
		uiNames.add("login");
		uiNames.add("data");
		return uiNames;
	}

	@Override
	public CmsUi initUi(Object uiParent) {
		Composite parent = (Composite) uiParent;
		String uiName = parent.getData(UI_NAME_PROPERTY) != null ? parent.getData(UI_NAME_PROPERTY).toString() : null;
		CmsSwtUi cmsUi = new CmsSwtUi(parent, SWT.NONE);
		if ("login".equals(uiName)) {
			CmsView cmsView = CmsSwtUtils.getCmsView(cmsUi);
			CmsLogin cmsLogin = new CmsLogin(cmsView, getCmsContext());
			cmsLogin.createUi(cmsUi);

		} else if ("data".equals(uiName)) {
			Content rootContent = contentRepository.get().get("/");
			AcrContentTreeView view = new AcrContentTreeView(cmsUi, 0, rootContent);
			view.setLayoutData(CmsSwtUtils.fillAll());

		} else if ("app".equals(uiName)) {
			Path rootPath = cmsFileSystemProvider.getPath(URI.create("cms:///"));
			SimpleFsBrowser view = new SimpleFsBrowser(cmsUi, 0);
			view.setInput(rootPath);
			view.setLayoutData(CmsSwtUtils.fillAll());

		}
		return cmsUi;
	}

	@Override
	public void refreshUi(CmsUi cmsUi, String state) {
	}

	@Override
	public void setState(CmsUi cmsUi, String state) {
		// TODO Auto-generated method stub

	}

	public void setContentRepository(ContentRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

	public void setCmsFileSystemProvider(FileSystemProvider cmsFileSystemProvider) {
		this.cmsFileSystemProvider = cmsFileSystemProvider;
	}

}