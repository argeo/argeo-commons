package org.argeo.security.ui.admin.internal;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/** Centralize interaction with the UserTransaction among the UI */
public class UserTransactionProvider extends AbstractSourceProvider {
	public final static String TRANSACTION_STATE = SecurityAdminPlugin.PLUGIN_ID
			+ ".userTransactionState";
	public final static String STATUS_ACTIVE = "status.active";
	public final static String STATUS_NO_TRANSACTION = "status.noTransaction";

	private String transactionState = STATUS_NO_TRANSACTION;

	/* DEPENDENCY INJECTION */
	private UserTransaction userTransaction;

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { TRANSACTION_STATE };
	}

	@Override
	public Map<String, String> getCurrentState() {
		Map<String, String> currentState = new HashMap<String, String>(1);
		// TODO implement asking to the UserTransaction
		// String transactionState = isActive ? STATUS_ACTIVE :
		// STATUS_NO_TRANSACTION;
		currentState.put(TRANSACTION_STATE, transactionState);
		return currentState;
	}

	@Override
	public void dispose() {
	}

	/** Publish the ability to change the state. */
	public void setUserTransactionState(String newState) {
		transactionState = newState;
		// fireSourceChanged(ISources.WORKBENCH, TRANSACTION_STATE,
		// transactionState);
	}

	private void refreshState() {
		try {
			if (userTransaction != null) {
				if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION)
					transactionState = STATUS_NO_TRANSACTION;
				else if (userTransaction.getStatus() == Status.STATUS_ACTIVE)
					transactionState = STATUS_ACTIVE;
				fireSourceChanged(ISources.WORKBENCH, TRANSACTION_STATE,
						transactionState);
			}
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}

	/** Publish the ability to notify a state change */
	public void fireTransactionStateChange() {
		refreshState();
	}

	/** FIXME: Rather inject the UserTransaction. */
	@Deprecated
	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
		// dirty init
		refreshState();
	}
}