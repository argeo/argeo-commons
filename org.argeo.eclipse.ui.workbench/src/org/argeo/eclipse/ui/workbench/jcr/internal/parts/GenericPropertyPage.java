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
package org.argeo.eclipse.ui.workbench.jcr.internal.parts;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.workbench.WorkbenchConstants;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.jcr.internal.PropertyLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Generic editor property page. Lists all properties of current node as a
 * complex tree. TODO: enable editing
 */

public class GenericPropertyPage extends FormPage implements WorkbenchConstants {
	// private final static Log log =
	// LogFactory.getLog(GenericPropertyPage.class);

	// Main business Objects
	private Node currentNode;

	public GenericPropertyPage(FormEditor editor, String title, Node currentNode) {
		super(editor, "id", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(WorkbenchUiPlugin.getMessage("genericNodePageTitle"));
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		form.getBody().setLayout(layout);

		createComplexTree(form.getBody());
	}

	private TreeViewer createComplexTree(Composite parent) {
		int style = SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION;
		Tree tree = new Tree(parent, style);
		createColumn(tree, "Property", SWT.LEFT, 200);
		createColumn(tree, "Value(s)", SWT.LEFT, 300);
		createColumn(tree, "Attributes", SWT.LEFT, 65);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		TreeViewer result = new TreeViewer(tree);
		result.setContentProvider(new TreeContentProvider());
		result.setLabelProvider(new PropertyLabelProvider());
		result.setInput(currentNode);
		result.expandAll();
		return result;
	}

	private static TreeColumn createColumn(Tree parent, String name, int style,
			int width) {
		TreeColumn result = new TreeColumn(parent, style);
		result.setText(name);
		result.setWidth(width);
		result.setMoveable(true);
		result.setResizable(true);
		return result;
	}

	//
	// private void createPropertiesPart(Composite parent) {
	// try {
	//
	// PropertyIterator pi = currentNode.getProperties();
	//
	// // Initializes form part
	// AbstractFormPart part = new AbstractFormPart() {
	// public void commit(boolean onSave) {
	// try {
	// if (onSave) {
	// ListIterator<Control> it = modifyableProperties
	// .listIterator();
	// while (it.hasNext()) {
	// // we only support Text controls for the time
	// // being
	// Text curControl = (Text) it.next();
	// String value = curControl.getText();
	// currentNode.setProperty((String) curControl
	// .getData(JCR_PROPERTY_NAME), value);
	// }
	//
	// // We only commit when onSave = true,
	// // thus it is still possible to save after a tab
	// // change.
	// super.commit(onSave);
	// }
	// } catch (RepositoryException re) {
	// throw new ArgeoException(
	// "Unexpected error while saving properties", re);
	// }
	// }
	// };
	//
	// while (pi.hasNext()) {
	// Property prop = pi.nextProperty();
	// addPropertyLine(parent, part, prop);
	// }
	//
	// getManagedForm().addPart(part);
	// } catch (RepositoryException re) {
	// throw new ArgeoException(
	// "Error during creation of network details section", re);
	// }
	//
	// }
	//
	// private void addPropertyLine(Composite parent, AbstractFormPart part,
	// Property prop) {
	// try {
	// tk.createLabel(parent, prop.getName());
	// tk.createLabel(parent,
	// "[" + JcrUtils.getPropertyDefinitionAsString(prop) + "]");
	//
	// if (prop.getDefinition().isProtected()) {
	// tk.createLabel(parent, formatReadOnlyPropertyValue(prop));
	// } else
	// addModifyableValueWidget(parent, part, prop);
	// } catch (RepositoryException re) {
	// throw new ArgeoException("Cannot get property " + prop, re);
	// }
	// }
	//
	// private String formatReadOnlyPropertyValue(Property prop) {
	// try {
	// String strValue;
	//
	// if (prop.getType() == PropertyType.BINARY)
	// strValue = "<binary>";
	// else if (prop.isMultiple())
	// strValue = Arrays.asList(prop.getValues()).toString();
	// else if (prop.getType() == PropertyType.DATE)
	// strValue = timeFormatter.format(prop.getValue().getDate()
	// .getTime());
	// else
	// strValue = prop.getValue().getString();
	//
	// return strValue;
	// } catch (RepositoryException re) {
	// throw new ArgeoException(
	// "Unexpected error while formatting read only property value",
	// re);
	// }
	// }
	//
	// private Control addModifyableValueWidget(Composite parent,
	// AbstractFormPart part, Property prop) {
	// GridData gd;
	// try {
	// if (prop.getType() == PropertyType.STRING) {
	// Text txt = tk.createText(parent, prop.getString());
	// gd = new GridData(GridData.FILL_HORIZONTAL);
	// txt.setLayoutData(gd);
	// txt.addModifyListener(new ModifiedFieldListener(part));
	// txt.setData(JCR_PROPERTY_NAME, prop.getName());
	// modifyableProperties.add(txt);
	// } else {
	// // unsupported property type for editing, we create a read only
	// // label.
	// return tk
	// .createLabel(parent, formatReadOnlyPropertyValue(prop));
	// }
	// return null;
	// } catch (RepositoryException re) {
	// throw new ArgeoException(
	// "Unexpected error while formatting read only property value",
	// re);
	// }
	//
	// }

	// Multiple Value Model
	// protected class MultipleValueItem {
	// private int index;
	// private Value value;
	//
	// public MultipleValueItem(int index, Value value) {
	// this.index = index;
	// this.value = value;
	// }
	//
	// public int getIndex() {
	// return index;
	// }
	//
	// public Object getValue() {
	// return value;
	// }
	// }

	private class TreeContentProvider implements ITreeContentProvider {
		private static final long serialVersionUID = -6162736530019406214L;

		public Object[] getElements(Object parent) {
			Object[] props = null;
			try {

				if (parent instanceof Node) {
					Node node = (Node) parent;
					PropertyIterator pi;
					pi = node.getProperties();
					List<Property> propList = new ArrayList<Property>();
					while (pi.hasNext()) {
						propList.add(pi.nextProperty());
					}
					props = propList.toArray();
				}
			} catch (RepositoryException e) {
				throw new ArgeoException(
						"Unexpected exception while listing node properties", e);
			}
			return props;
		}

		public Object getParent(Object child) {
			return null;
		}

		public Object[] getChildren(Object parent) {
			Object[] result = null;
			if (parent instanceof Property) {
				Property prop = (Property) parent;
				try {

					if (prop.isMultiple()) {
						Value[] values = prop.getValues();
						// List<MultipleValueItem> list = new
						// ArrayList<MultipleValueItem>();
						// for (int i = 0; i < values.length; i++) {
						// MultipleValueItem mvi = new MultipleValueItem(i,
						// values[i]);
						// list.add(mvi);
						// }

						return values;
					}
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unexpected error getting multiple values property.",
							e);
				}
			}
			return result;
		}

		public boolean hasChildren(Object parent) {
			try {
				if (parent instanceof Property
						&& ((Property) parent).isMultiple()) {
					return true;
				}
			} catch (RepositoryException e) {
				throw new ArgeoException(
						"Unexpected exception while checking if property is multiple",
						e);
			}
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}
	}
}
