package org.argeo.demo.i18n.utils;

import org.argeo.ArgeoException;
import org.argeo.demo.i18n.I18nDemoPlugin;
import org.argeo.demo.i18n.editors.SimpleMultitabEditor;
import org.argeo.demo.i18n.editors.SimpleMultitabEditorInput;
import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PartInitException;

/**
 * Centralizes the management of double click on a NodeTreeViewer
 */
public class GenericDoubleClickListener implements IDoubleClickListener {

	// private final static Log log = LogFactory
	// .getLog(GenericDoubleClickListener.class);

	// private TreeViewer treeViewer;

	public GenericDoubleClickListener(TreeViewer treeViewer) {
		// this.treeViewer = treeViewer;
	}

	public void doubleClick(DoubleClickEvent event) {
		Object obj = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
		if (obj instanceof TreeParent) {
			try {
				TreeParent tp = (TreeParent) obj;
				// open an editor
				SimpleMultitabEditorInput smei = new SimpleMultitabEditorInput(
						tp.getName());
				I18nDemoPlugin.getDefault().getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.openEditor(smei, SimpleMultitabEditor.ID);
			} catch (PartInitException pie) {
				throw new ArgeoException(
						"Unexpected exception while opening node editor", pie);
			}
		}
		// else do nothing
	}
}
