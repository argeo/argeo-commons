/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.ui.workbench.internal.useradmin.commands;

import org.argeo.cms.ui.workbench.internal.useradmin.UserAdminWrapper;
import org.argeo.cms.ui.workbench.internal.useradmin.parts.UserBatchUpdateWizard;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/** Launch a wizard to perform batch process on users */
public class UserBatchUpdate extends AbstractHandler {

	/* DEPENDENCY INJECTION */
	private UserAdminWrapper uaWrapper;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		UserBatchUpdateWizard wizard = new UserBatchUpdateWizard(uaWrapper);
		wizard.setWindowTitle("User batch processing");
		WizardDialog dialog = new WizardDialog(
				HandlerUtil.getActiveShell(event), wizard);
		dialog.open();
		return null;
	}

	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.uaWrapper = userAdminWrapper;
	}
}