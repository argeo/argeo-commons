package org.argeo.cms.ui.viewers;

import javax.jcr.Property;

/** An editable part related to a JCR Property */
public interface PropertyPart extends ItemPart<Property> {
	public Property getProperty();
}
