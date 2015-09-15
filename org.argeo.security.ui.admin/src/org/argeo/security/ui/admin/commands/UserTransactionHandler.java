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
package org.argeo.security.ui.admin.commands;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Manage the transaction that is bound to the current perspective */
public class UserTransactionHandler extends AbstractHandler {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID
			+ ".userTransactionHandler";

	public final static String PARAM_COMMAND_ID = "param.commandId";

	public final static String TRANSACTION_BEGIN = "transaction.begin";
	public final static String TRANSACTION_COMMIT = "transaction.commit";
	public final static String TRANSACTION_ROLLBACK = "transaction.rollback";

	private UserTransaction userTransaction;

	public Object execute(ExecutionEvent event) throws ExecutionException {

		String commandId = event.getParameter(PARAM_COMMAND_ID);
		try {
			if (TRANSACTION_BEGIN.equals(commandId)) {
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION)
					throw new ArgeoException("A transaction already exists");
				else
					userTransaction.begin();
			} else if (TRANSACTION_COMMIT.equals(commandId)) {
				if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION)
					throw new ArgeoException("No transaction.");
				else
					userTransaction.commit();
			} else if (TRANSACTION_ROLLBACK.equals(commandId)) {
				if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION)
					throw new ArgeoException("No transaction to rollback.");
				else
					userTransaction.rollback();
			}
			UiAdminUtils.notifyTransactionStateChange(userTransaction);
		} catch (ArgeoException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgeoException("Unable to call " + commandId + " on "
					+ userTransaction, e);
		}
		//
		// IWorkbenchWindow iww = HandlerUtil.getActiveWorkbenchWindow(event);
		// if (iww == null)
		// return null;
		// IWorkbenchPage activePage = iww.getActivePage();
		// IWorkbenchPart part = activePage.getActivePart();
		// if (part instanceof UsersView)
		// ((UsersView) part).refresh();
		// else if (part instanceof GroupsView)
		// ((GroupsView) part).refresh();
		return null;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}