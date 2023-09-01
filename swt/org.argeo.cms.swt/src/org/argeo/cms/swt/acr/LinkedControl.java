package org.argeo.cms.swt.acr;

import java.net.URI;

import org.argeo.api.acr.Content;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.swt.widgets.StyledControl;
import org.eclipse.swt.widgets.Composite;

/**
 * A {@link StyledControl} which can link either to an internal {@link Content}
 * or an external URI.
 */
public abstract class LinkedControl extends StyledControl {

	private static final long serialVersionUID = -7603153425459801216L;

	private Content linkedContent;
	private URI plainUri;

	public LinkedControl(Composite parent, int swtStyle) {
		super(parent, swtStyle);
	}

	public void setLink(Content linkedContent) {
		if (plainUri != null)
			throw new IllegalStateException("An URI is already set");
		this.linkedContent = linkedContent;
	}

	public void setLink(URI uri) {
		if (linkedContent != null)
			throw new IllegalStateException("A linked content is already set");
		this.plainUri = uri;
	}

	public boolean isInternalLink() {
		if (!hasLink())
			throw new IllegalStateException("No link has been set");
		return linkedContent != null;
	}

	public boolean hasLink() {
		return plainUri != null || linkedContent != null;
	}

	public Content getLinkedContent() {
		return linkedContent;
	}

	public URI getPlainUri() {
		return plainUri;
	}

	public URI toUri() {
		if (plainUri != null)
			return plainUri;
		if (linkedContent != null)
			return URI.create("#" + ContentUtils.cleanPathForUrl(linkedContent.getPath()));
		return null;

	}

}
