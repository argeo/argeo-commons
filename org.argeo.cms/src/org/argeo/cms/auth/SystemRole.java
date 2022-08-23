package org.argeo.cms.auth;

import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;

public interface SystemRole {
	QName getName();

	default boolean implied(Subject subject, String context) {
		Set<ImpliedByPrincipal> roles = subject.getPrincipals(ImpliedByPrincipal.class);
		for (ImpliedByPrincipal role : roles) {
			if (role.isSystemRole()) {
				if (role.getRoleName().equals(getName())) {
					if (role.getContext().equalsIgnoreCase(context)
							|| role.getContext().equals(CmsConstants.NODE_BASEDN))
						return true;
				}
			}
		}
		return false;
	}

}
