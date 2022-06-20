package org.argeo.cms.swt.app;

import java.util.HashSet;
import java.util.Set;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;
import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsUi;
import org.argeo.api.cms.CmsView;
import org.argeo.cms.AbstractCmsApp;
import org.argeo.cms.swt.CmsSwtUi;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.auth.CmsLogin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class CmsUserApp extends AbstractCmsApp {
	private ContentRepository contentRepository;

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

}