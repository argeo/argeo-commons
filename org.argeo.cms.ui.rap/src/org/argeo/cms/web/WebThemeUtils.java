package org.argeo.cms.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.cms.CmsTheme;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.service.ResourceLoader;

/** Web specific utilities around theming. */
public class WebThemeUtils {
	private final static Log log = LogFactory.getLog(WebThemeUtils.class);

	public static void apply(Application application, CmsTheme theme) {
		ResourceLoader resourceLoader = new CmsThemeResourceLoader(theme);
		resources: for (String path : theme.getImagesPaths()) {
			if (path.startsWith("target/"))
				continue resources; // skip maven output
			application.addResource(path, resourceLoader);
			if (log.isTraceEnabled())
				log.trace("Theme " + theme.getThemeId() + ": added resource " + path);
		}
		for (String path : theme.getRapCssPaths()) {
			application.addStyleSheet(theme.getThemeId(), path, resourceLoader);
			if (log.isDebugEnabled())
				log.debug("Theme " + theme.getThemeId() + ": added RAP CSS " + path);
		}
	}

}
