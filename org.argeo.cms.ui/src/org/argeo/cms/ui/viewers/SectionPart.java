package org.argeo.cms.ui.viewers;


/** An editable part dynamically related to a Section */
public interface SectionPart extends EditablePart, NodePart {
	public String getPartId();

	public Section getSection();
}
