package org.argeo.demo.i18n.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.demo.i18n.I18nDemoMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * Offers two main sections : one to display a text area with a summary of all
 * variations between a version and its predecessor and one tree view that
 * enable browsing
 * */
public class MultiSectionPage extends FormPage {
	private final static Log log = LogFactory.getLog(MultiSectionPage.class);

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

		createHistorySection(form.getBody());
		createTreeSection(form.getBody());
	}

	protected void createTreeSection(Composite parent) {
		// Section Layout & MetaData
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setText(I18nDemoMessages.get().MultiSelectionPage_DescriptionSectionTitle);

		// Section Body
		Composite body = tk.createComposite(section, SWT.FILL);
		// WARNING : 2 following lines are compulsory or body won't be
		// displayed.
		body.setLayout(new GridLayout());
		section.setClient(body);

		body.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setExpanded(true);
	}

	protected void createHistorySection(Composite parent) {

		// Section Layout
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(TableWrapData.FILL_GRAB));
		TableWrapLayout twt = new TableWrapLayout();
		section.setLayout(twt);

		// Set title of the section
		section.setText(I18nDemoMessages.get().MultiSelectionPage_DetailsSectionTitle);

		final Text styledText = tk.createText(section, "", SWT.FULL_SELECTION
				| SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setClient(styledText);
		styledText.setEditable(false);
		section.setExpanded(false);

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
