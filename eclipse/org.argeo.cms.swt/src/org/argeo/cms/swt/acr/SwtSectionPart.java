package org.argeo.cms.swt.acr;

import org.argeo.cms.ux.acr.ContentPart;
import org.argeo.cms.ux.widgets.EditablePart;

/** An editable part dynamically related to a Section */
public interface SwtSectionPart extends EditablePart, ContentPart {
	public String getPartId();

	public SwtSection getSection();
}
