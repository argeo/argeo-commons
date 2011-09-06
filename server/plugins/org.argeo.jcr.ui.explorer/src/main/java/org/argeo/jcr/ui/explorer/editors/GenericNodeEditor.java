package org.argeo.jcr.ui.explorer.editors;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * Parent Abstract GR multitab editor. Insure the presence of a GrBackend
 */
public class GenericNodeEditor extends FormEditor {

	private final static Log log = LogFactory.getLog(GenericNodeEditor.class);
	public final static String ID = "org.argeo.jcr.ui.explorer.genericNodeEditor";

	private Node currentNode;

	private GenericNodePage networkDetailsPage;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		GenericNodeEditorInput nei = (GenericNodeEditorInput) getEditorInput();
		this.setPartName(JcrUtils.lastPathElement(nei.getPath()));
	}

	@Override
	protected void addPages() {
		EmptyNodePage enp = new EmptyNodePage(this, "Empty node page");
		try {
			addPage(enp);
		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add an empty page ", e);
		}
	}

	private void addPagesAfterNodeSet() {
		try {
			networkDetailsPage = new GenericNodePage(this,
					JcrExplorerPlugin.getMessage("genericNodePageTitle"),
					currentNode);
			addPage(networkDetailsPage);
			this.setActivePage(networkDetailsPage.getIndex());
		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add page ", e);
		}
	}

	@Override
	public void doSaveAs() {
		// unused compulsory method
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			// Automatically commit all pages of the editor
			commitPages(true);
			firePropertyChange(PROP_DIRTY);
		} catch (Exception e) {
			throw new ArgeoException("Error while saving node", e);
		}

	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	Node getCurrentNode() {
		return currentNode;
	}

	public void setCurrentNode(Node currentNode) {
		boolean nodeWasNull = this.currentNode == null;
		this.currentNode = currentNode;
		if (nodeWasNull) {
			this.removePage(0);
			addPagesAfterNodeSet();
		}
	}
}
