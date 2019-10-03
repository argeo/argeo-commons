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
package org.argeo.cms.ui.workbench.internal.jcr.parts;

import javax.jcr.Node;

import org.argeo.cms.ui.jcr.JcrTreeContentProvider;
import org.argeo.cms.ui.jcr.NodeLabelProvider;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.jcr.DefaultNodeEditor;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/** List all children of the current node */
public class ChildNodesPage extends FormPage {
	// private final static Log log = LogFactory.getLog(ChildNodesPage.class);

	private Node currentNode;

	private JcrTreeContentProvider nodeContentProvider;
	private TreeViewer nodesViewer;

	public ChildNodesPage(FormEditor editor, String title, Node currentNode) {
		super(editor, "ChildNodesPage", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();
			form.setText(WorkbenchUiPlugin.getMessage("childNodesPageTitle"));
			Composite innerBox = form.getBody();
			// Composite innerBox = new Composite(body, SWT.NO_FOCUS);
			GridLayout twt = new GridLayout(1, false);
			twt.marginWidth = twt.marginHeight = 5;
			innerBox.setLayout(twt);
			if (!currentNode.hasNodes()) {
				managedForm.getToolkit().createLabel(innerBox, WorkbenchUiPlugin.getMessage("warningNoChildNode"));
			} else {
				nodeContentProvider = new JcrTreeContentProvider();
				nodesViewer = createNodeViewer(innerBox, nodeContentProvider);
				nodesViewer.setInput(currentNode);
			}
		} catch (Exception e) {
			throw new EclipseUiException("Cannot create children page for " + currentNode, e);
		}
	}

	protected TreeViewer createNodeViewer(Composite parent, final ITreeContentProvider nodeContentProvider) {

		final TreeViewer tmpNodeViewer = new TreeViewer(parent, SWT.BORDER);
		Tree tree = tmpNodeViewer.getTree();
		tree.setLinesVisible(true);
		tmpNodeViewer.getTree().setLayoutData(EclipseUiUtils.fillAll());
		tmpNodeViewer.setContentProvider(nodeContentProvider);
		tmpNodeViewer.setLabelProvider(new NodeLabelProvider());
		tmpNodeViewer.addDoubleClickListener(new DClickListener());
		return tmpNodeViewer;
	}

	public class DClickListener implements IDoubleClickListener {

		public void doubleClick(DoubleClickEvent event) {
			if (event.getSelection() == null || event.getSelection().isEmpty())
				return;
			Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
			if (obj instanceof Node) {
				Node node = (Node) obj;
				try {
					GenericNodeEditorInput gnei = new GenericNodeEditorInput(node);
					WorkbenchUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.openEditor(gnei, DefaultNodeEditor.ID);
				} catch (PartInitException pie) {
					throw new EclipseUiException("Cannot open editor for " + node, pie);
				}
			}
		}
	}
}
