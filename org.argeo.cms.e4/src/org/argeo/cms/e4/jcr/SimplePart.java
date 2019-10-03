package org.argeo.cms.e4.jcr;

import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class SimplePart {

	@PostConstruct
	void init(Composite parent) {
		parent.setLayout(new GridLayout());
		Label label = new Label(parent, SWT.NONE);
		label.setText("Hello e4 World");
	}

}
