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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.argeo.cms.ui.CmsConstants;
import org.argeo.cms.ui.jcr.FullVersioningTreeContentProvider;
import org.argeo.cms.ui.jcr.JcrDClickListener;
import org.argeo.cms.ui.jcr.VersionLabelProvider;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.WorkbenchConstants;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.PropertyDiff;
import org.argeo.jcr.VersionDiff;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
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
 */
public class NodeVersionHistoryPage extends FormPage implements WorkbenchConstants {
	// private final static Log log = LogFactory
	// .getLog(NodeVersionHistoryPage.class);

	// Utils
	protected DateFormat timeFormatter = new SimpleDateFormat(CmsConstants.DATE_TIME_FORMAT);

	// business objects
	private Node currentNode;

	// this page UI components
	private FullVersioningTreeContentProvider nodeContentProvider;
	private TreeViewer nodesViewer;
	private FormToolkit tk;

	public NodeVersionHistoryPage(FormEditor editor, String title, Node currentNode) {
		super(editor, "NodeVersionHistoryPage", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(WorkbenchUiPlugin.getMessage("nodeVersionHistoryPageTitle"));
		tk = managedForm.getToolkit();
		Composite innerBox = form.getBody();
		// Composite innerBox = new Composite(body, SWT.NO_FOCUS);
		GridLayout twt = new GridLayout(1, false);
		twt.marginWidth = twt.marginHeight = 5;
		innerBox.setLayout(twt);
		try {
			if (!currentNode.isNodeType(NodeType.MIX_VERSIONABLE)) {
				tk.createLabel(innerBox, WorkbenchUiPlugin.getMessage("warningUnversionableNode"));
			} else {
				createHistorySection(innerBox);
				createTreeSection(innerBox);
			}
		} catch (RepositoryException e) {
			throw new EclipseUiException("Unable to check if node is versionable", e);
		}
	}

	protected void createTreeSection(Composite parent) {
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setText(WorkbenchUiPlugin.getMessage("versionTreeSectionTitle"));

		Composite body = tk.createComposite(section, SWT.FILL);
		section.setClient(body);
		section.setExpanded(true);
		body.setLayoutData(new GridData(GridData.FILL_BOTH));
		body.setLayout(new GridLayout());

		nodeContentProvider = new FullVersioningTreeContentProvider();
		nodesViewer = createNodeViewer(body, nodeContentProvider);
		nodesViewer.setInput(currentNode);
	}

	protected TreeViewer createNodeViewer(Composite parent, final ITreeContentProvider nodeContentProvider) {

		final TreeViewer tmpNodeViewer = new TreeViewer(parent, SWT.MULTI);

		tmpNodeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tmpNodeViewer.setContentProvider(nodeContentProvider);
		tmpNodeViewer.setLabelProvider(new VersionLabelProvider());
		tmpNodeViewer.addDoubleClickListener(new JcrDClickListener(tmpNodeViewer));
		return tmpNodeViewer;
	}

	protected void createHistorySection(Composite parent) {

		// Section Layout
		Section section = tk.createSection(parent, Section.TWISTIE);
		section.setLayoutData(new GridData(TableWrapData.FILL_GRAB));
		TableWrapLayout twt = new TableWrapLayout();
		section.setLayout(twt);

		// Set title of the section
		section.setText(WorkbenchUiPlugin.getMessage("versionHistorySectionTitle"));

		final Text styledText = tk.createText(section, "",
				SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		section.setClient(styledText);
		styledText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
		refreshHistory(styledText);
		styledText.setEditable(false);
		section.setExpanded(false);

		AbstractFormPart part = new AbstractFormPart() {
			public void commit(boolean onSave) {
			}

			public void refresh() {
				super.refresh();
				refreshHistory(styledText);
			}
		};
		getManagedForm().addPart(part);
	}

	protected void refreshHistory(Text styledText) {
		try {
			List<VersionDiff> lst = listHistoryDiff();
			StringBuffer main = new StringBuffer("");

			for (int i = lst.size() - 1; i >= 0; i--) {
				if (i == 0)
					main.append("Creation (");
				else
					main.append("Update " + i + " (");

				if (lst.get(i).getUserId() != null)
					main.append("UserId : " + lst.get(i).getUserId());

				if (lst.get(i).getUserId() != null && lst.get(i).getUpdateTime() != null)
					main.append(", ");

				if (lst.get(i).getUpdateTime() != null)
					main.append("Date : " + timeFormatter.format(lst.get(i).getUpdateTime().getTime()) + ")\n");

				StringBuffer buf = new StringBuffer("");
				Map<String, PropertyDiff> diffs = lst.get(i).getDiffs();
				for (String prop : diffs.keySet()) {
					PropertyDiff pd = diffs.get(prop);
					// String propName = pd.getRelPath();
					Value refValue = pd.getReferenceValue();
					Value newValue = pd.getNewValue();
					String refValueStr = "";
					String newValueStr = "";

					if (refValue != null) {
						if (refValue.getType() == PropertyType.DATE) {
							refValueStr = timeFormatter.format(refValue.getDate().getTime());
						} else
							refValueStr = refValue.getString();
					}
					if (newValue != null) {
						if (newValue.getType() == PropertyType.DATE) {
							newValueStr = timeFormatter.format(newValue.getDate().getTime());
						} else
							newValueStr = newValue.getString();
					}

					if (pd.getType() == PropertyDiff.MODIFIED) {
						buf.append(prop).append(": ");
						buf.append(refValueStr);
						buf.append(" > ");
						buf.append(newValueStr);
						buf.append("\n");
					} else if (pd.getType() == PropertyDiff.ADDED && !"".equals(newValueStr)) {
						// we don't list property that have been added with an
						// empty string as value
						buf.append(prop).append(": ");
						buf.append(" + ");
						buf.append(newValueStr);
						buf.append("\n");
					} else if (pd.getType() == PropertyDiff.REMOVED) {
						buf.append(prop).append(": ");
						buf.append(" - ");
						buf.append(refValueStr);
						buf.append("\n");
					}
				}
				buf.append("\n");
				main.append(buf);
			}
			styledText.setText(main.toString());
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot generate history for node", e);
		}
	}

	public List<VersionDiff> listHistoryDiff() {
		try {
			List<VersionDiff> res = new ArrayList<VersionDiff>();
			VersionManager versionManager = currentNode.getSession().getWorkspace().getVersionManager();
			VersionHistory versionHistory = versionManager.getVersionHistory(currentNode.getPath());

			VersionIterator vit = versionHistory.getAllLinearVersions();
			while (vit.hasNext()) {
				Version version = vit.nextVersion();
				Node node = version.getFrozenNode();
				Version predecessor = null;
				try {
					predecessor = version.getLinearPredecessor();
				} catch (Exception e) {
					// no predecessor seems to throw an exception even if it
					// shouldn't...
				}
				if (predecessor == null) {// original
				} else {
					Map<String, PropertyDiff> diffs = JcrUtils.diffProperties(predecessor.getFrozenNode(), node);
					if (!diffs.isEmpty()) {
						String lastUserName = null;
						Calendar lastUpdate = null;
						try {
							if (currentNode.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
								lastUserName = node.getProperty(Property.JCR_LAST_MODIFIED_BY).getString();
								lastUpdate = node.getProperty(Property.JCR_LAST_MODIFIED).getDate();
							} else
								lastUpdate = version.getProperty(Property.JCR_CREATED).getDate();

						} catch (Exception e) {
							// Silent that info is optional
						}
						VersionDiff vd = new VersionDiff(lastUserName, lastUpdate, diffs);
						res.add(vd);
					}
				}
			}
			return res;
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot generate history for node ");
		}

	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
	}
}
