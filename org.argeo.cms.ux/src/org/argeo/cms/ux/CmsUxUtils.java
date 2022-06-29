package org.argeo.cms.ux;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.util.CurrentSubject;

public class CmsUxUtils {
	public static ContentSession getContentSession(ContentRepository contentRepository, CmsView cmsView) {
		return CurrentSubject.callAs(cmsView.getCmsSession().getSubject(), () -> contentRepository.get());
	}

	public static String getTitle(Content content) {
		return content.getName().getLocalPart();
	}

	/** singleton */
	private CmsUxUtils() {

	}
}
