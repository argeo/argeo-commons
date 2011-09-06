package org.argeo.jcr.ui.explorer.editors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class GenericNodePage extends FormPage implements JcrExplorerConstants {
	private final static Log log = LogFactory.getLog(GenericNodePage.class);

	// local constants
	private final static String JCR_PROPERTY_NAME = "jcr:name";

	// Utils
	protected DateFormat timeFormatter = new SimpleDateFormat(DATE_TIME_FORMAT);

	// Main business Objects
	private Node currentNode;

	// This page widgets
	private FormToolkit tk;
	private List<Control> modifyableProperties = new ArrayList<Control>();

	public GenericNodePage(FormEditor editor, String title, Node currentNode) {
		super(editor, "id", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			tk = managedForm.getToolkit();
			ScrolledForm form = managedForm.getForm();
			GridLayout twt = new GridLayout(3, false);
			twt.marginWidth = twt.marginHeight = 0;

			form.getBody().setLayout(twt);

			createPropertiesPart(form.getBody());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createPropertiesPart(Composite parent) {
		try {

			PropertyIterator pi = currentNode.getProperties();

			// Initializes form part
			AbstractFormPart part = new AbstractFormPart() {
				public void commit(boolean onSave) {
					try {
						if (onSave) {
							ListIterator<Control> it = modifyableProperties
									.listIterator();
							while (it.hasNext()) {
								// we only support Text controls for the time
								// being
								Text curControl = (Text) it.next();
								String value = curControl.getText();
								currentNode.setProperty((String) curControl
										.getData(JCR_PROPERTY_NAME), value);
							}

							// We only commit when onSave = true,
							// thus it is still possible to save after a tab
							// change.
							super.commit(onSave);
						}
					} catch (RepositoryException re) {
						throw new ArgeoException(
								"Unexpected error while saving properties", re);
					}
				}
			};

			while (pi.hasNext()) {
				Property prop = pi.nextProperty();
				addPropertyLine(parent, part, prop);
			}

			getManagedForm().addPart(part);
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Error during creation of network details section", re);
		}

	}

	private void addPropertyLine(Composite parent, AbstractFormPart part,
			Property prop) {
		try {
			Label lbl = tk.createLabel(parent, prop.getName());
			lbl = tk.createLabel(parent,
					"[" + JcrUtils.getPropertyDefinitionAsString(prop) + "]");

			if (prop.getDefinition().isProtected()) {
				lbl = tk.createLabel(parent, formatReadOnlyPropertyValue(prop));
			} else
				addModifyableValueWidget(parent, part, prop);
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot get property " + prop, re);
		}
	}

	private String formatReadOnlyPropertyValue(Property prop) {
		try {
			String strValue;

			if (prop.getType() == PropertyType.BINARY)
				strValue = "<binary>";
			else if (prop.isMultiple())
				strValue = Arrays.asList(prop.getValues()).toString();
			else if (prop.getType() == PropertyType.DATE)
				strValue = timeFormatter.format(prop.getValue().getDate()
						.getTime());
			else
				strValue = prop.getValue().getString();

			return strValue;
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while formatting read only property value",
					re);
		}
	}

	private Control addModifyableValueWidget(Composite parent,
			AbstractFormPart part, Property prop) {
		GridData gd;
		try {
			if (prop.getType() == PropertyType.STRING) {
				Text txt = tk.createText(parent, prop.getString());
				gd = new GridData(GridData.FILL_HORIZONTAL);
				txt.setLayoutData(gd);
				txt.addModifyListener(new ModifiedFieldListener(part));
				txt.setData(JCR_PROPERTY_NAME, prop.getName());
				modifyableProperties.add(txt);
			} else {
				// unsupported property type for editing, we create a read only
				// label.
				return tk
						.createLabel(parent, formatReadOnlyPropertyValue(prop));
			}
			return null;
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while formatting read only property value",
					re);
		}

	}

	//
	// LISTENERS
	//

	private class ModifiedFieldListener implements ModifyListener {

		private AbstractFormPart formPart;

		public ModifiedFieldListener(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			formPart.markDirty();
		}
	}

}
