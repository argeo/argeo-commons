package org.argeo.cms.ux.acr;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.ux.CmsEditable;
import org.argeo.cms.acr.CmsContent;
import org.argeo.cms.ux.AbstractCmsEditable;

/** {@link CmsEditable} semantics for a {@link Content}. */
public class ContentCmsEditable extends AbstractCmsEditable {

	private final boolean canEdit;
	/** The path of this content, relative to its content provider. */
	private final String relativePath;
	private final ProvidedSession session;
	private final ContentProvider provider;

	public ContentCmsEditable(Content content) {
		ProvidedContent providedContent = (ProvidedContent) content;
		canEdit = providedContent.canEdit();
		session = providedContent.getSession();
		provider = providedContent.getProvider();
		relativePath = CmsContent.relativize(provider.getMountPath(), content.getPath());
	}

	@Override
	public Boolean canEdit() {
		return canEdit;
	}

	@Override
	public Boolean isEditing() {
		return provider.isOpenForEdit(session, relativePath);
	}

	@Override
	public void startEditing() {
		provider.openForEdit(session, relativePath);
	}

	@Override
	public void stopEditing() {
		provider.freeze(session, relativePath);
	}

}
