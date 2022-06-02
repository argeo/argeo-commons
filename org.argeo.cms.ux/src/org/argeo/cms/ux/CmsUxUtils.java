package org.argeo.cms.ux;

import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.cms.CmsView;
import org.argeo.cms.auth.CurrentUser;

public class CmsUxUtils {
	public static ContentSession getContentSession(ContentRepository contentRepository, CmsView cmsView) {
		return CurrentUser.callAs(cmsView.getCmsSession().getSubject(), () -> contentRepository.get());
	}

	/** singleton */
	private CmsUxUtils() {

	}
}
