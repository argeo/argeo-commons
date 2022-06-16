package org.argeo.cms.swt.acr;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.spi.ProvidedContent;
import org.eclipse.swt.widgets.Composite;

/** A composite which can (optionally) manage a content. */
public class ContentComposite extends Composite {
	private static final long serialVersionUID = -1447009015451153367L;

	public ContentComposite(Composite parent, int style, Content item) {
		super(parent, style);
		setData(item);
	}

	public Content getContent() {
		return (Content) getData();
	}

	@Deprecated
	public Content getNode() {
		return getContent();
	}

	protected ProvidedContent getProvidedContent() {
		return (ProvidedContent) getContent();
	}

	public String getSessionLocalId() {
		return getProvidedContent().getSessionLocalId();
	}

	protected void itemUpdated() {
		layout();
	}

	public void setContent(Content content) {
		setData(content);
		itemUpdated();
	}
}
