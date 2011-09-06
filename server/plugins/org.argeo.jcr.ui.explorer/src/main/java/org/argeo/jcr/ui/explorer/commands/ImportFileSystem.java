package org.argeo.jcr.ui.explorer.commands;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.argeo.jcr.ui.explorer.wizards.ImportFileSystemWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class ImportFileSystem extends AbstractHandler {
	private static Log log = LogFactory.getLog(ImportFileSystem.class);

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		AbstractJcrBrowser view = (AbstractJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			try {
				if (obj instanceof Node) {
					Node folder = (Node) obj;
					// if (!folder.getPrimaryNodeType().getName()
					// .equals(NodeType.NT_FOLDER)) {
					// Error.show("Can only import to a folder node");
					// return null;
					// }
					ImportFileSystemWizard wizard = new ImportFileSystemWizard(
							folder);
					WizardDialog dialog = new WizardDialog(
							HandlerUtil.getActiveShell(event), wizard);
					dialog.open();
					view.refresh(folder);
				} else {
					Error.show("Can only import to a node");
				}
			} catch (Exception e) {
				Error.show("Cannot import files to " + obj, e);
			}
		}
		return null;
	}

}
