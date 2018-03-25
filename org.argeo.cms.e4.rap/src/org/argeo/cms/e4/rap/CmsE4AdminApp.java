package org.argeo.cms.e4.rap;

public class CmsE4AdminApp extends AbstractRapE4App {
	public CmsE4AdminApp() {
		setPageTitle("CMS Admin");
		setE4Xmi("org.argeo.cms.e4/cms-admin.e4xmi");
		setPath("/admin");
	}

}
