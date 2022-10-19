package org.argeo.cms.swt.widgets;

import org.argeo.cms.ux.widgets.AbstractGuidedFormPage;
import org.eclipse.swt.widgets.Composite;

public abstract class SwtGuidedFormPage extends AbstractGuidedFormPage {

	public SwtGuidedFormPage(String pageName) {
		super(pageName);
	}

	public abstract void createControl(Composite parent);
}
