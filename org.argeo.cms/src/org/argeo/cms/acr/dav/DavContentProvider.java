package org.argeo.cms.acr.dav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.argeo.api.acr.RuntimeNamespaceContext;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.dav.DavClient;
import org.argeo.cms.dav.DavResponse;

public class DavContentProvider implements ContentProvider {
	private String mountPath;
	private URI baseUri;

	private DavClient davClient;

	public DavContentProvider(String mountPath, URI baseUri) {
		this.mountPath = mountPath;
		this.baseUri = baseUri;
		if (!baseUri.getPath().endsWith("/"))
			throw new IllegalArgumentException("Base URI " + baseUri + " path does not end with /");
		this.davClient = new DavClient();
	}

	@Override
	public String getNamespaceURI(String prefix) {
		// FIXME retrieve mappings from WebDav
		return RuntimeNamespaceContext.getNamespaceContext().getNamespaceURI(prefix);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		// FIXME retrieve mappings from WebDav
		return RuntimeNamespaceContext.getNamespaceContext().getPrefixes(namespaceURI);
	}

	@Override
	public ProvidedContent get(ProvidedSession session, String relativePath) {
		URI contentUri = relativePathToUri(relativePath);
		return getDavContent(session, contentUri);
	}

	DavContent getDavContent(ProvidedSession session, URI uri) {
		DavResponse response = davClient.get(uri);
		return new DavContent(session, this, uri, response.getPropertyNames());
	}

	@Override
	public boolean exists(ProvidedSession session, String relativePath) {
		URI contentUri = relativePathToUri(relativePath);
		return davClient.exists(contentUri);
	}

	@Override
	public String getMountPath() {
		return mountPath;
	}

	DavClient getDavClient() {
		return davClient;
	}

	URI relativePathToUri(String relativePath) {
		try {
			// TODO check last slash
			String path = relativePath.startsWith("/") ? relativePath : baseUri.getPath() + relativePath;
			URI uri = new URI(baseUri.getScheme(), baseUri.getHost(), path, baseUri.getFragment());
			return uri;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot build URI for " + relativePath + " relatively to " + baseUri, e);
		}
	}
}
