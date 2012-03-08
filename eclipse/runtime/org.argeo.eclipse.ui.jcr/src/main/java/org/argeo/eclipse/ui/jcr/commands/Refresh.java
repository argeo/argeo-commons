package org.argeo.eclipse.ui.jcr.commands;

import java.util.Iterator;

import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Call the refresh method of the active AbstractJcrBrowser instance.
 * 
 * Warning: this method only refreshes the viewer, if the model is "stale", e.g.
 * if some changes in the underlying data have not yet been propagated to the
 * model, the view will not display up-to-date information.
 */
@Deprecated
public class Refresh extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		AbstractJcrBrowser view = (AbstractJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));
		if (selection != null && selection instanceof IStructuredSelection) {
			Iterator<?> it = ((IStructuredSelection) selection).iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				view.refresh(obj);
			}
		}
		return null;
	}
}
