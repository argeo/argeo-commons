package org.argeo.cms.swt.widgets;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractSwtPart {
	protected final Composite area;

	public AbstractSwtPart(Composite parent, int style) {
		area = new Composite(parent, style);
		area.setLayout(CmsSwtUtils.noSpaceGridLayout());
	}

}
