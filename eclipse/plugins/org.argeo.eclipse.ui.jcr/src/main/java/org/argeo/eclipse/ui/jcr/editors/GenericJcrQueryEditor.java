package org.argeo.eclipse.ui.jcr.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/** Executes any JCR query. */
public class GenericJcrQueryEditor extends AbstractJcrQueryEditor {
	private Text queryField;

	@Override
	public void createQueryForm(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		queryField = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		queryField.setText(initialQuery);
		queryField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Button execute = new Button(parent, SWT.PUSH);
		execute.setText("Execute");

		Listener executeListener = new Listener() {
			public void handleEvent(Event event) {
				executeQuery(queryField.getText());
			}
		};

		execute.addListener(SWT.Selection, executeListener);
		// queryField.addListener(SWT.DefaultSelection, executeListener);
	}

	@Override
	public void setFocus() {
		queryField.setFocus();
	}
}
