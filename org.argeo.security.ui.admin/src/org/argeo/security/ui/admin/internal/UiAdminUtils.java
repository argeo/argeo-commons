package org.argeo.security.ui.admin.internal;

import java.security.AccessController;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.argeo.ArgeoException;
import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.internal.providers.UserTransactionProvider;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** First effort to centralize back end methods used by the user admin UI */
public class UiAdminUtils {

	/** returns the local name of the current connected user */
	public final static String getUsername(UserAdmin userAdmin) {
		LdapName dn = getLdapName();
		return getUsername(getUser(userAdmin, dn));
	}

	public final static boolean isCurrentUser(User user) {
		String userName = getProperty(user, LdifName.dn.name());
		try {
			LdapName selfUserName = UiAdminUtils.getLdapName();
			LdapName userLdapName = new LdapName(userName);
			if (userLdapName.equals(selfUserName))
				return true;
			else
				return false;
		} catch (InvalidNameException e) {
			throw new ArgeoException("User " + user + " has an unvalid dn: "
					+ userName, e);
		}
	}

	public final static LdapName getLdapName() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		String name = subject.getPrincipals(X500Principal.class).iterator()
				.next().toString();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new ArgeoException("Invalid user dn " + name, e);
		}
		return dn;
	}

	public final static User getUser(UserAdmin userAdmin, LdapName dn) {
		User user = userAdmin.getUser(LdifName.dn.name(), dn.toString());
		return user;
	}

	public final static String getUsername(User user) {
		String cn = getProperty(user, LdifName.dn.name());
		if (isEmpty(cn))
			cn = getProperty(user, LdifName.uid.name());
		return cn;
	}

	/** Simply retrieves a display name of the relevant domain */
	public final static String getDomainName(User user) {
		String dn = (String) user.getProperties().get(LdifName.dn.name());
		if (dn.endsWith(UserAdminConstants.SYSTEM_ROLE_BASE_DN))
			return "System roles";
		try {
			LdapName name;
			name = new LdapName(dn);
			List<Rdn> rdns = name.getRdns();
			String dname = null;
			int i = 0;
			loop: while (i < rdns.size()) {
				Rdn currrRdn = rdns.get(i);
				if (!"dc".equals(currrRdn.getType()))
					break loop;
				else {
					String currVal = (String) currrRdn.getValue();
					dname = dname == null ? currVal : currVal + "." + dname;
				}
				i++;
			}
			return dname;
		} catch (InvalidNameException e) {
			throw new ArgeoException("Unable to get domain name for " + dn, e);
		}
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
			esp.fireTransactionStateChange();
		} catch (Exception e) {
			throw new ArgeoException("Unable to begin transaction", e);
		}
	}
}