package org.argeo.security.ui.admin.commands;

import org.argeo.security.ui.admin.wizards.NewUserWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/** Command handler to set visible or open a Argeo user. */
public class NewUser extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			NewUserWizard newUserWizard = new NewUserWizard();
			WizardDialog dialog = new WizardDialog(
					HandlerUtil.getActiveShell(event), newUserWizard);
			dialog.open();
		} catch (Exception e) {
			throw new ExecutionException("Cannot open editor", e);
		}
		return null;
	}
}
