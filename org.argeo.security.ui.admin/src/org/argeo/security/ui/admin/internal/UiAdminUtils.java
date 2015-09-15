package org.argeo.security.ui.admin.internal;

import java.security.AccessController;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** First effort to centralize back end methods used by the user admin UI */
public class UiAdminUtils {
	public final static String getUsername() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Principal principal = subject.getPrincipals(X500Principal.class)
				.iterator().next();
		return principal.getName();

	}

	public final static String getUsername(User user) {
		String cn = getProperty(user, UserAdminConstants.KEY_CN);
		if (isEmpty(cn))
			cn = getProperty(user, UserAdminConstants.KEY_UID);
		return cn;
	}

	public final static String getProperty(Role role, String key) {
		Object obj = role.getProperties().get(key);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}

	public final static String getDefaultCn(String firstName, String lastName) {
		return (firstName.trim() + " " + lastName.trim() + " ").trim();
	}

	/*
	 * INTERNAL METHODS: Below methods are meant to stay here and are not part
	 * of a potential generic backend to manage the useradmin
	 */
	public final static boolean notNull(String string) {
		if (string == null)
			return false;
		else
			return !"".equals(string.trim());
	}

	public final static boolean isEmpty(String string) {
		if (string == null)
			return true;
		else
			return "".equals(string.trim());
	}

	/** Must be called from the UI Thread. */
	public final static void beginTransactionIfNeeded(
			UserTransaction userTransaction) {
		try {
			if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
				userTransaction.begin();
				notifyTransactionStateChange(userTransaction);
			}
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}

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
			esp.setUserTransaction(userTransaction);
			esp.fireTransactionStateChange();
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}
}