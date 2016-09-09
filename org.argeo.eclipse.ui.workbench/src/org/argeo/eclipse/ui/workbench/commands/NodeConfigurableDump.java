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
package org.argeo.eclipse.ui.workbench.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.workbench.internal.jcr.model.SingleJcrNodeElem;
import org.argeo.jcr.JcrUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * First draft of a wizard that enable configurable recursive dump of the
 * current selected Node (Only one at a time). Enable among other to export
 * children Nodes and to choose to export binaries or not. It is useful to
 * retrieve business data from live systems to prepare migration or test locally
 */
public class NodeConfigurableDump extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.ID
			+ ".nodeConfigurableDump";

	private final static DateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm");

	public final static int EXPORT_NODE = 0;
	public final static int EXPORT_CHILDREN = 1;
	public final static int EXPORT_GRAND_CHILDREN = 2;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection))
			return null;

		Iterator<?> lst = ((IStructuredSelection) selection).iterator();
		if (lst.hasNext()) {
			Object element = lst.next();
			if (element instanceof SingleJcrNodeElem) {
				SingleJcrNodeElem sjn = (SingleJcrNodeElem) element;
				Node node = sjn.getNode();

				ConfigureDumpWizard wizard = new ConfigureDumpWizard(
						HandlerUtil.getActiveShell(event),
						"Import Resource CSV");
				WizardDialog dialog = new WizardDialog(
						HandlerUtil.getActiveShell(event), wizard);
				int result = dialog.open();

				if (result == Window.OK) {

					String dateVal = df.format(new GregorianCalendar()
							.getTime());
					try {

						Path tmpDirPath = Files.createTempDirectory(dateVal
								+ "-NodeDump-");
						List<Node> toExport = retrieveToExportNodes(node,
								wizard.currExportType);

						for (Node currNode : toExport) {
							FileOutputStream fos;
							String fileName = wizard.prefix
									+ JcrUtils.replaceInvalidChars(currNode
											.getName()) + "_" + dateVal
									+ ".xml";
							File currFile = new File(tmpDirPath.toString()
									+ "/" + fileName);
							currFile.createNewFile();
							fos = new FileOutputStream(currFile);
							node.getSession().exportSystemView(
									currNode.getPath(), fos,
									!wizard.includeBinaries, false);
							fos.flush();
							fos.close();
						}
					} catch (RepositoryException e) {
						throw new EclipseUiException(
								"Unable to perform SystemExport on " + node, e);
					} catch (IOException e) {
						throw new EclipseUiException("Unable to SystemExport "
								+ node, e);
					}
				}
			}
		}
		return null;
	}

	private List<Node> retrieveToExportNodes(Node node, int currExportType)
			throws RepositoryException {
		List<Node> nodes = new ArrayList<Node>();
		switch (currExportType) {
		case EXPORT_NODE:
			nodes.add(node);
			return nodes;
		case EXPORT_CHILDREN:
			return JcrUtils.nodeIteratorToList(node.getNodes());
		case EXPORT_GRAND_CHILDREN:
			NodeIterator nit = node.getNodes();
			while (nit.hasNext())
				nodes.addAll(JcrUtils.nodeIteratorToList(nit.nextNode()
						.getNodes()));
			return nodes;

		default:
			return nodes;
		}
	}

	// private synchronized void openGeneratedFile(String path, String fileName)
	// {
	// Map<String, String> params = new HashMap<String, String>();
	// params.put(OpenFile.PARAM_FILE_NAME, fileName);
	// params.put(OpenFile.PARAM_FILE_URI, "file://" + path);
	// CommandUtils.callCommand("org.argeo.security.ui.specific.openFile",
	// params);
	// }

	private class ConfigureDumpWizard extends Wizard {

		// parameters
		protected String prefix;
		protected int currExportType = EXPORT_NODE;
		protected boolean includeBinaries = false;

		// UI Objects
		private BasicPage page;
		private Text prefixTxt;
		private Button includeBinaryBtn;
		private Button b1, b2, b3;

		public ConfigureDumpWizard(Shell parentShell, String title) {
			setWindowTitle(title);
		}

		@Override
		public void addPages() {
			try {
				page = new BasicPage("Main page");
				addPage(page);
			} catch (Exception e) {
				throw new EclipseUiException("Cannot add page to wizard", e);
			}
		}

		@Override
		public boolean performFinish() {
			prefix = prefixTxt.getText();
			if (b1.getSelection())
				currExportType = EXPORT_NODE;
			else if (b2.getSelection())
				currExportType = EXPORT_CHILDREN;
			else if (b3.getSelection())
				currExportType = EXPORT_GRAND_CHILDREN;
			includeBinaries = includeBinaryBtn.getSelection();
			return true;
		}

		@Override
		public boolean performCancel() {
			return true;
		}

		@Override
		public boolean canFinish() {
			String errorMsg = "No prefix defined.";
			if ("".equals(prefixTxt.getText().trim())) {
				page.setErrorMessage(errorMsg);
				return false;
			} else {
				page.setErrorMessage(null);
				return true;
			}
		}

		protected class BasicPage extends WizardPage {
			private static final long serialVersionUID = 1L;

			public BasicPage(String pageName) {
				super(pageName);
				setTitle("Configure dump before launching");
				setMessage("Define the parameters of the dump to launch");
			}

			public void createControl(Composite parent) {
				parent.setLayout(noSpaceGridLayout());

				// Main Layout
				Composite mainCmp = new Composite(parent, SWT.NONE);
				mainCmp.setLayout(new GridLayout(2, false));
				mainCmp.setLayoutData(fillAll());

				// The path
				createBoldLabel(mainCmp, "Prefix");
				prefixTxt = new Text(mainCmp, SWT.SINGLE | SWT.BORDER);
				prefixTxt.setLayoutData(fillAll());
				prefixTxt.addModifyListener(new ModifyListener() {
					private static final long serialVersionUID = 1L;

					@Override
					public void modifyText(ModifyEvent event) {
						if (prefixTxt.getText() != null)
							getWizard().getContainer().updateButtons();
					}
				});

				new Label(mainCmp, SWT.SEPARATOR | SWT.HORIZONTAL)
						.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
								false, 2, 1));

				// Which node to export
				Label typeLbl = new Label(mainCmp, SWT.RIGHT);
				typeLbl.setText(" Type");
				typeLbl.setFont(EclipseUiUtils.getBoldFont(mainCmp));
				typeLbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
						false, 1, 3));

				b1 = new Button(mainCmp, SWT.RADIO);
				b1.setText("Export this node");
				b1.setSelection(true);
				b2 = new Button(mainCmp, SWT.RADIO);
				b2.setText("Export children nodes");
				b3 = new Button(mainCmp, SWT.RADIO);
				b3.setText("Export grand-children nodes");

				new Label(mainCmp, SWT.SEPARATOR | SWT.HORIZONTAL)
						.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
								false, 2, 1));

				createBoldLabel(mainCmp, "Files and images");
				includeBinaryBtn = new Button(mainCmp, SWT.CHECK);
				includeBinaryBtn.setText("Include binaries");

				prefixTxt.setFocus();
				setControl(mainCmp);
			}
		}
	}

	private Label createBoldLabel(Composite parent, String value) {
		Label label = new Label(parent, SWT.RIGHT);
		label.setText(" " + value);
		label.setFont(EclipseUiUtils.getBoldFont(parent));
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		return label;
	}

	// TODO rather use EclipseUiUtils methods
	private GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	private GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	private GridData fillAll() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}
}