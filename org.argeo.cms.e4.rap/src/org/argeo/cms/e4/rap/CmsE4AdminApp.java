package org.argeo.cms.e4.rap;

public class CmsE4AdminApp extends AbstractRapE4App {
	public CmsE4AdminApp() {
		setPageTitle("CMS Admin");
		setE4Xmi("org.argeo.cms.e4/e4xmi/cms-devops.e4xmi");
		setPath("/devops");
	}

}