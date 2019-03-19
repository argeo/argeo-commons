package org.argeo.cms.e4.rap;

public class CmsE4AdminApp extends AbstractRapE4App {
	public CmsE4AdminApp() {
		setPageTitle("Argeo CMS DevOps");
		setE4Xmi("org.argeo.cms.e4/e4xmi/cms-devops.e4xmi");
		setPath("/devops");
	}

}
