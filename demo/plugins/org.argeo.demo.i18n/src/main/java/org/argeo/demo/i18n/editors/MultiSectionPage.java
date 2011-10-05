package org.argeo.demo.i18n.editors;

import org.argeo.demo.i18n.I18nDemoMessages;
import org.argeo.demo.i18n.I18nDemoPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Offers two main sections : one to display a text area with a summary of all
 * variations between a version and its predecessor and one tree view that
 * enable browsing
 * */
public class MultiSectionPage extends FormPage {
	// private final static Log log = LogFactory.getLog(MultiSectionPage.class);

	// this page UI components
	private FormToolkit tk;

	public MultiSectionPage(FormEditor editor, String title) {
		super(editor, "MultiSectionPage", title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		tk = managedForm.getToolkit();
		GridLayout twt = new GridLayout(1, false);
		twt.marginWidth = twt.marginHeight = 5;
		Composite body = form.getBody();
		body.setLayout(twt);

		createDetailsSection(form.getBody());
		createDescriptionSection(form.getBody());
	}

	protected void createDescriptionSection(Composite parent) {
		// Section Layout & MetaData
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setText(I18nDemoMessages.get().MultiSectionPage_DescriptionSectionTitle);

		// Section Body
		Composite body = tk.createComposite(section, SWT.FILL);
		// WARNING : 2 following lines are compulsory or body won't be
		// displayed.
		body.setLayout(new GridLayout());
		section.setClient(body);

		body.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setExpanded(true);

		// button line
		Button b1 = new Button(body, SWT.PUSH | SWT.FILL);
		b1.setText(I18nDemoMessages.get().MultiSectionPage_Btn1Lbl);
		Button b2 = new Button(body, SWT.PUSH | SWT.FILL);
		b2.setText(I18nDemoMessages.get().MultiSectionPage_Btn2Lbl);
		Button b3 = new Button(body, SWT.PUSH | SWT.FILL);
		b3.setText(I18nDemoMessages.get().MultiSectionPage_Btn3Lbl);

		addAListener(b1);
		addAListener(b2);
		addAListener(b3);
	}

	private void addAListener(Button b) {
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageBox mb = new MessageBox(I18nDemoPlugin.getDefault()
						.getWorkbench().getActiveWorkbenchWindow().getShell(),
						SWT.OK);
				// Title
				mb.setText(I18nDemoMessages.get().MultiSectionPage_PopupTitle);
				// Message
				mb.setMessage(I18nDemoMessages.get().MultiSectionPage_PopupText);
				mb.open();
			}
		});
	}

	protected void createDetailsSection(Composite parent) {

		// Section Layout
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(TableWrapData.FILL_GRAB));
		GridLayout gd = new GridLayout();
		section.setLayout(gd);

		// Set title of the section
		section.setText(I18nDemoMessages.get().MultiSectionPage_DetailsSectionTitle);

		final Text styledText = tk.createText(section, "", SWT.FULL_SELECTION
				| SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		styledText.setEditable(false);
		styledText
				.setText(I18nDemoMessages.get().MultiSectionPage_DescriptionSectionTxt);
		section.setExpanded(false);

		section.setClient(styledText);

		AbstractFormPart part = new AbstractFormPart() {
			public void commit(boolean onSave) {
			}

			public void refresh() {
				super.refresh();
			}
		};
		getManagedForm().addPart(part);
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
	}
}
