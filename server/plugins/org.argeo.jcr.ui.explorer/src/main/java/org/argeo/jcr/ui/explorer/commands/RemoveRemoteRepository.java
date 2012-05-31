package org.argeo.jcr.ui.explorer.commands;

import org.argeo.jcr.ui.explorer.model.RemoteRepositoryNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/** Remove a registered remote repository */
public class RemoveRemoteRepository extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();

		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();

			if (obj instanceof RemoteRepositoryNode) {
				((RemoteRepositoryNode) obj).remove();
				view.refresh(null);
			}
		}
		return null;
	}

}
