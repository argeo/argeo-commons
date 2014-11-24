package org.argeo.cms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.rap.rwt.service.ResourceLoader;

/** {@link ResourceLoader} implementation wrapping an {@link URL}. */
@Deprecated
public class UrlResourceLoader implements ResourceLoader {
	private final URL url;

	public UrlResourceLoader(URL url) {
		super();
		this.url = url;
	}

	@Override
	public InputStream getResourceAsStream(String resourceName)
			throws IOException {
		return url.openStream();
	}

}
