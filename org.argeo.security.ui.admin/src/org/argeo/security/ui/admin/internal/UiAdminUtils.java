package org.argeo.security.ui.admin.internal;

import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.security.ui.admin.internal.providers.UserTransactionProvider;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

/** First effort to centralize back end methods used by the user admin UI */
public class UiAdminUtils {
	/*
	 * INTERNAL METHODS: Below methods are meant to stay here and are not part
	 * of a potential generic backend to manage the useradmin
	 */
	/** Easily notify the ActiveWindow that the transaction had a state change */
	public final static void notifyTransactionStateChange(
			UserTransaction userTransaction) {
		try {
			IWorkbenchWindow aww = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			ISourceProviderService sourceProviderService = (ISourceProviderService) aww
					.getService(ISourceProviderService.class);
			UserTransactionProvider esp = (UserTransactionProvider) sourceProviderService
					.getSourceProvider(UserTransactionProvider.TRANSACTION_STATE);
			esp.fireTransactionStateChange();
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}
}