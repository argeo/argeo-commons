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
package org.argeo.security.ui.admin.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Launch a wizard to update various properties about users in JCR. */
public class UserBatchUpdate extends AbstractHandler {
	// private Repository repository;
	// private UserAdminService userAdminService;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Session session = null;
		// try {
		// session = repository.login();
		// UserBatchUpdateWizard userBatchUpdateWizard = new
		// UserBatchUpdateWizard(
		// session, userAdminService);
		// WizardDialog dialog = new WizardDialog(
		// HandlerUtil.getActiveShell(event), userBatchUpdateWizard);
		// dialog.open();
		// } catch (Exception e) {
		// throw new ExecutionException("Cannot open wizard", e);
		// } finally {
		// JcrUtils.logoutQuietly(session);
		// }
		return null;
	}

//	public void setRepository(Repository repository) {
//		this.repository = repository;
//	}
	//
	// public void setUserAdminService(UserAdminService userAdminService) {
	// this.userAdminService = userAdminService;
	// }

	// public void setJcrSecurityModel(JcrSecurityModel jcrSecurityModel) {
	// this.jcrSecurityModel = jcrSecurityModel;
	// }

}