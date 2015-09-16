package org.argeo.security.ui.admin.internal;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/** Notifies the UI on UserTransaction state change */
public class UserTransactionProvider extends AbstractSourceProvider {
	public final static String TRANSACTION_STATE = SecurityAdminPlugin.PLUGIN_ID
			+ ".userTransactionState";
	public final static String STATUS_ACTIVE = "status.active";
	public final static String STATUS_NO_TRANSACTION = "status.noTransaction";

	/* DEPENDENCY INJECTION */
	private UserTransaction userTransaction;

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { TRANSACTION_STATE };
	}

	@Override
	public Map<String, String> getCurrentState() {
		Map<String, String> currentState = new HashMap<String, String>(1);
		currentState.put(TRANSACTION_STATE, getInternalCurrentState());
		return currentState;
	}

	@Override
	public void dispose() {
	}

	private String getInternalCurrentState() {
		try {
			String transactionState;
			if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION)
				transactionState = STATUS_NO_TRANSACTION;
			else
				// if (userTransaction.getStatus() == Status.STATUS_ACTIVE)
				transactionState = STATUS_ACTIVE;
			return transactionState;
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}

	/** Publish the ability to notify a state change */
	public void fireTransactionStateChange() {
		fireSourceChanged(ISources.WORKBENCH, TRANSACTION_STATE,
				getInternalCurrentState());
	}

	/* DEPENDENCY INJECTION */
	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}