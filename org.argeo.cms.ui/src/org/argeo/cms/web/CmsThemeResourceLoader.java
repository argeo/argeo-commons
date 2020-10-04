package org.argeo.cms.web;

import java.io.IOException;
import java.io.InputStream;

import org.argeo.cms.ui.CmsTheme;
import org.eclipse.rap.rwt.service.ResourceLoader;

/** A RAP {@link ResourceLoader} based on a {@link CmsTheme}. */
public class CmsThemeResourceLoader implements ResourceLoader {
	private final CmsTheme theme;

	public CmsThemeResourceLoader(CmsTheme theme) {
		super();
		this.theme = theme;
	}

	@Override
	public InputStream getResourceAsStream(String resourceName) throws IOException {
		return theme.getResourceAsStream(resourceName);
	}

}
