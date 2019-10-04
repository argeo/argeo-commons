package org.argeo.cms.ui.workbench.internal.useradmin;

import org.eclipse.swt.widgets.Display;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/** Convenience class to insure the call to refresh is done in the UI thread */
public abstract class UiUserAdminListener implements UserAdminListener {

	private final Display display;

	public UiUserAdminListener(Display display) {
		this.display = display;
	}

	@Override
	public void roleChanged(final UserAdminEvent event) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				roleChangedToUiThread(event);
			}
		});
	}

	public abstract void roleChangedToUiThread(UserAdminEvent event);
}
