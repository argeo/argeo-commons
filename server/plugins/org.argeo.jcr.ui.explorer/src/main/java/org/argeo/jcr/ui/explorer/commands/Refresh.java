package org.argeo.jcr.ui.explorer.commands;

import java.util.Iterator;

import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.argeo.jcr.ui.explorer.utils.JcrUiUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Force the selected objects of the active view to be refreshed doing the
 * following:
 * <ol>
 * <li>The model objects are recomputed</li>
 * <li>the view is refreshed</li>
 * </ol>
 */
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
				if (obj instanceof TreeParent) {
					TreeParent tp = (TreeParent) obj;
					JcrUiUtils.forceRefreshIfNeeded(tp);
					view.refresh(obj);
				}
			}
		}
		return null;
	}
}
