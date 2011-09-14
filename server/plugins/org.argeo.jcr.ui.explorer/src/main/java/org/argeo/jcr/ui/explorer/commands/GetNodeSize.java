package org.argeo.jcr.ui.explorer.commands;

import javax.jcr.Node;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.model.WorkspaceNode;
import org.argeo.jcr.ui.explorer.views.GenericJcrBrowser;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the generic node editor. */
public class GetNodeSize extends AbstractHandler {
	// private final static Log log = LogFactory.getLog(GetNodeSize.class);

	public final static String ID = "org.argeo.jcr.ui.explorer.getNodeSize";
	public final static String DEFAULT_ICON_REL_PATH = "icons/getSize.gif";
	public final static String DEFAULT_LABEL = JcrExplorerPlugin
			.getMessage("getNodeSizeCmdLbl");

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// JcrUtils.getRepositoryByAlias(repositoryRegister,
		// ArgeoJcrConstants.ALIAS_NODE);

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		GenericJcrBrowser view = (GenericJcrBrowser) HandlerUtil
				.getActiveWorkbenchWindow(event).getActivePage()
				.findView(HandlerUtil.getActivePartId(event));

		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {

			// We only get
			IStructuredSelection iss = (IStructuredSelection) selection;
			if (iss.size() > 1)
				ErrorFeedback.show(JcrExplorerPlugin
						.getMessage("warningInvalidMultipleSelection"), null);

			long size = 0;
			Node node;
			if (iss.getFirstElement() instanceof SingleJcrNode)
				node = ((SingleJcrNode) iss.getFirstElement()).getNode();
			else if (iss.getFirstElement() instanceof WorkspaceNode)
				node = ((WorkspaceNode) iss.getFirstElement()).getRootNode();
			else
				// unvalid object type
				return null;

			size = JcrUtils.getNodeApproxSize(node);

			String[] labels = { "OK" };
			Shell shell = HandlerUtil.getActiveWorkbenchWindow(event)
					.getShell();
			MessageDialog md = new MessageDialog(shell, "Node size", null,
					"Node size is: " + size / 1024 + " KB",
					MessageDialog.INFORMATION, labels, 0);
			md.open();
		}
		return null;
	}
}
