/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.ui.commands;

import org.argeo.security.ui.dialogs.ChangePasswordDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.springframework.security.userdetails.UserDetailsManager;

/** Opens the change password dialog. */
public class OpenChangePasswordDialog extends AbstractHandler {
	private UserDetailsManager userDetailsManager;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ChangePasswordDialog dialog = new ChangePasswordDialog(
				HandlerUtil.getActiveShell(event), userDetailsManager);
		if (dialog.open() == Dialog.OK) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
					"Password changed", "Password changed.");
		}
		return null;
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

}
