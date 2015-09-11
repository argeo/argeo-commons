package org.argeo.security.ui.admin.internal;

import java.security.AccessController;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

/** First effort to centralize back end methods used by the user admin UI */
public class UiAdminUtils {
	public final static String getUsername() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		Principal principal = subject.getPrincipals(X500Principal.class)
				.iterator().next();
		return principal.getName();

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

}