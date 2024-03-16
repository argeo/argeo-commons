package org.argeo.api.cms.auth;

import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.argeo.api.cms.CmsConstants;

/** A programmatic role. */
public interface SystemRole {
	QName qName();

	/** Whether this role is implied for this authenticated user. */
	default boolean implied(Subject subject, String context) {
		return implied(qName(), subject, context);
	}

	/** Whether this role is implied for this distinguished name. */
	default boolean implied(String dn, String context) {
		String roleContext = RoleNameUtils.getContext(dn);
		QName roleName = RoleNameUtils.getLastRdnAsName(dn);
		return roleContext.equalsIgnoreCase(context) && qName().equals(roleName);
	}

	/**
	 * Whether this role is implied for this authenticated subject. If context is
	 * <code>null</code>, it is not considered; this should be used to build user
	 * interfaces, but not to authorise.
	 */
	static boolean implied(QName name, Subject subject, String context) {
		Set<ImpliedByPrincipal> roles = subject.getPrincipals(ImpliedByPrincipal.class);
		for (ImpliedByPrincipal role : roles) {
			if (role.isSystemRole()) {
				if (role.getRoleName().equals(name)) {
					// !! if context is not specified, it is considered irrelevant
					if (context == null)
						return true;
					if (role.getContext().equalsIgnoreCase(context)
							|| role.getContext().equals(CmsConstants.NODE_BASEDN))
						return true;
				}
			}
		}
		return false;
	}
}
