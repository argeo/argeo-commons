package org.argeo.security.ui.admin.internal;

import org.argeo.cms.CmsException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/** Manage transaction and part refresh while updating the security model */
public class PartStateChanged implements IPartListener, IStartup {
	// private final static Log log = LogFactory.getLog(PartStateChanged.class);
	// private IContextActivation contextActivation;

	@Override
	public void earlyStartup() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage iwp = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					if (iwp != null)
						iwp.addPartListener(new PartStateChanged());
				} catch (Exception e) {
					throw new CmsException(
							"Error while registering the PartStateChangedListener",
							e);
				}
			}
		});
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		// Nothing to do
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		// Nothing to do
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		// Nothing to do
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		// Nothing to do
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		// Nothing to do
	}
}