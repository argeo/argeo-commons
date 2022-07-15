package org.argeo.cms.acr.dav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.util.dav.DavResponse;

public class DavContent extends AbstractContent {
	private final DavContentProvider provider;
	private final URI uri;

	private Set<QName> keyNames;
	private Optional<Map<QName, String>> values;

	public DavContent(ProvidedSession session, DavContentProvider provider, URI uri, Set<QName> keyNames) {
		this(session, provider, uri, keyNames, Optional.empty());
	}

	public DavContent(ProvidedSession session, DavContentProvider provider, URI uri, Set<QName> keyNames,
			Optional<Map<QName, String>> values) {
		super(session);
		this.provider = provider;
		this.uri = uri;
		this.keyNames = keyNames;
		this.values = values;
	}

	@Override
	public QName getName() {
		String fileName = ContentUtils.getParentPath(uri.getPath())[1];
		ContentName name = NamespaceUtils.parsePrefixedName(provider, fileName);
		return name;
	}

	@Override
	public Content getParent() {
		try {
			String parentPath = ContentUtils.getParentPath(uri.getPath())[0];
			URI parentUri = new URI(uri.getScheme(), uri.getHost(), parentPath, null);
			return provider.getDavContent(getSession(), parentUri);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot create parent", e);
		}
	}

	@Override
	public Iterator<Content> iterator() {
		Iterator<DavResponse> responses = provider.getDavClient().listChildren(uri);
		return new DavResponseIterator(responses);
	}

	@Override
	protected Iterable<QName> keys() {
		return keyNames;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		if (values.isEmpty()) {
			DavResponse response = provider.getDavClient().get(uri);
			values = Optional.of(response.getProperties());
		}
		String valueStr = values.get().get(key);
		if (valueStr == null)
			return Optional.empty();
		// TODO convert
		return Optional.of((A) valueStr);
	}

	@Override
	public ContentProvider getProvider() {
		return provider;
	}

	class DavResponseIterator implements Iterator<Content> {
		private final Iterator<DavResponse> responses;

		public DavResponseIterator(Iterator<DavResponse> responses) {
			this.responses = responses;
		}

		@Override
		public boolean hasNext() {
			return responses.hasNext();
		}

		@Override
		public Content next() {
			DavResponse response = responses.next();
			String relativePath = response.getHref();
			URI contentUri = provider.relativePathToUri(relativePath);
			return new DavContent(getSession(), provider, contentUri, response.getPropertyNames());
		}

	}
}
