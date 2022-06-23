package org.argeo.cms.ui.viewers;

import org.argeo.cms.swt.EditablePart;

/** An editable part dynamically related to a Section */
public interface SectionPart extends EditablePart, NodePart {
	public String getPartId();

	public Section getSection();
}
