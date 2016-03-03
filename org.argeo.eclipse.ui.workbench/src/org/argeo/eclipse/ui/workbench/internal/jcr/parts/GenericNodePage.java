/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui.workbench.internal.jcr.parts;

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

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.WorkbenchConstants;
import org.argeo.jcr.JcrUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Main node editor page. Lists all properties of the current node and enable
 * access and editing for some of them.
 */

public class GenericNodePage extends FormPage implements WorkbenchConstants {
	// private final static Log log = LogFactory.getLog(GenericNodePage.class);

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
		tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		GridLayout twt = new GridLayout(3, false);
		twt.marginWidth = twt.marginHeight = 5;

		form.getBody().setLayout(twt);
		createPropertiesPart(form.getBody());
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
							if (currentNode.getSession().hasPendingChanges())
								currentNode.getSession().save();
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
			tk.createLabel(parent, prop.getName());
			tk.createLabel(parent,
					"[" + JcrUtils.getPropertyDefinitionAsString(prop) + "]");

			if (prop.getDefinition().isProtected()) {
				tk.createLabel(parent, formatReadOnlyPropertyValue(prop));
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
			if (prop.getType() == PropertyType.STRING && !prop.isMultiple()) {
				Text txt = tk.createText(parent, prop.getString(), SWT.WRAP
						| SWT.MULTI);
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

	private class ModifiedFieldListener implements ModifyListener {
		private static final long serialVersionUID = 2117484480773434646L;
		private AbstractFormPart formPart;

		public ModifiedFieldListener(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			formPart.markDirty();
		}
	}

}
