package org.argeo.cms.ux.widgets;

import org.argeo.api.acr.Content;

/** A part displaying or editing a content. */
public interface ContentPart {
	Content getContent();

	@Deprecated
	Content getNode();

}
