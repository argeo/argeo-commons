package org.argeo.security.ui.admin.commands;

import javax.jcr.Session;

import org.argeo.security.UserAdminService;
import org.argeo.security.ui.admin.wizards.NewUserWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/** Command handler to set visible or open a Argeo user. */
public class NewUser extends AbstractHandler {
	private Session session;
	private UserAdminService userAdminService;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			NewUserWizard newUserWizard = new NewUserWizard(session,
					userAdminService);
			WizardDialog dialog = new WizardDialog(
					HandlerUtil.getActiveShell(event), newUserWizard);
			dialog.open();
		} catch (Exception e) {
			throw new ExecutionException("Cannot open wizard", e);
		}
		return null;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

}
