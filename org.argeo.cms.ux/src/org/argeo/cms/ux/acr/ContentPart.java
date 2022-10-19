package org.argeo.cms.ux.acr;

import org.argeo.api.acr.Content;

/** A part displaying or editing a content. */
public interface ContentPart {
	Content getContent();

	@Deprecated
	default Content getNode() {
		return getContent();
	}

}
