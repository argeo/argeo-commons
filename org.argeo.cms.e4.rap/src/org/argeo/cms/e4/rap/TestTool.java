package org.argeo.cms.e4.rap;

import javax.annotation.PostConstruct;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class TestTool {

	@PostConstruct
	public void createGui(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		combo.add("First");
		combo.add("Second");
	}
}