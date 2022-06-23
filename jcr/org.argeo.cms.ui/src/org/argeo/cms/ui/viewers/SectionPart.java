package org.argeo.cms.ui.viewers;

import org.argeo.cms.swt.SwtEditablePart;

/** An editable part dynamically related to a Section */
public interface SectionPart extends SwtEditablePart, NodePart {
	public String getPartId();

	public Section getSection();
}
