package org.argeo.cms.acr;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.CmsConstants;

/**
 * Base for simple content providers based on a service supporting only one
 * namespace. Typically used in higher level applications for domain-specific
 * modelling.
 */
public abstract class AbstractSimpleContentProvider<SERVICE> implements ContentProvider {
	private final String namespaceUri;
	private final String defaultPrefix;
	private SERVICE service;
	private String mountPath;
	private String mountName;

	protected AbstractSimpleContentProvider(String namespaceUri, String defaultPrefix) {
		this(namespaceUri, defaultPrefix, null, null);
	}

	protected AbstractSimpleContentProvider(String namespaceUri, String defaultPrefix, SERVICE service,
			String mountPath) {
		this.namespaceUri = namespaceUri;
		this.defaultPrefix = defaultPrefix;
		this.service = service;
		setMountPath(mountPath);
	}

	/** The first level of content provided by the service. */
	protected abstract Iterator<Content> firstLevel(ProvidedSession session);

	/**
	 * Retrieve the content at this relative path. Root content is already dealt
	 * with.
	 */
	protected abstract ProvidedContent get(ProvidedSession session, List<String> segments);

	@Override
	public final ProvidedContent get(ProvidedSession session, String relativePath) {
		List<String> segments = ContentUtils.toPathSegments(relativePath);
		if (segments.size() == 0)
			return new ServiceContent(session);
		return get(session, segments);
	}

	public void start(Map<String, String> properties) {
		mountPath = properties.get(CmsConstants.ACR_MOUNT_PATH);
		if (mountPath == null)
			throw new IllegalStateException(CmsConstants.ACR_MOUNT_PATH + " must be specified.");
		setMountPath(mountPath);
	}

	private void setMountPath(String mountPath) {
		if (mountPath == null)
			return;
		this.mountPath = mountPath;
		List<String> mountSegments = ContentUtils.toPathSegments(mountPath);
		this.mountName = mountSegments.get(mountSegments.size() - 1);

	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (defaultPrefix.equals(prefix))
			return namespaceUri;
		throw new IllegalArgumentException("Only prefix " + defaultPrefix + " is supported");
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		if (namespaceUri.equals(namespaceURI))
			return Collections.singletonList(defaultPrefix).iterator();
		throw new IllegalArgumentException("Only namespace URI " + namespaceUri + " is supported");
	}

	@Override
	public String getMountPath() {
		return mountPath;
	}

	protected String getMountName() {
		return mountName;
	}

	protected SERVICE getService() {
		return service;
	}

	public void setService(SERVICE service) {
		this.service = service;
	}

	protected class ServiceContent extends AbstractContent {

		public ServiceContent(ProvidedSession session) {
			super(session);
		}

		@Override
		public ContentProvider getProvider() {
			return AbstractSimpleContentProvider.this;
		}

		@Override
		public QName getName() {
			return new ContentName(getMountName());
		}

		@Override
		public Content getParent() {
			return null;
		}

		@Override
		public Iterator<Content> iterator() {
			return firstLevel(getSession());
		}

	}

}
