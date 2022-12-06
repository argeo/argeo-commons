package org.argeo.cms.internal.auth;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.cms.RoleNameUtils;
import org.osgi.service.useradmin.Authorization;

/**
 * A {@link Principal} which has been implied by an {@link Authorization}. If it
 * is empty it means this is an additional identity, otherwise it lists the
 * users (typically the logged in user but possibly empty
 * {@link ImpliedByPrincipal}s) which have implied it. When an additional
 * identity is removed, the related {@link ImpliedByPrincipal}s can thus be
 * removed.
 */
public final class ImpliedByPrincipal implements Principal {
	private final String name;
	private final QName roleName;
	private final boolean systemRole;
	private final String context;

	private Set<Principal> causes = new HashSet<Principal>();

	public ImpliedByPrincipal(String name, Principal userPrincipal) {
		this.name = name;
		roleName = RoleNameUtils.getLastRdnAsName(name);
		systemRole = RoleNameUtils.isSystemRole(roleName);
		context = RoleNameUtils.getContext(name);
		if (userPrincipal != null)
			causes.add(userPrincipal);
	}

	public String getName() {
		return name;
	}

	/*
	 * OBJECT
	 */

	public QName getRoleName() {
		return roleName;
	}

	public String getContext() {
		return context;
	}

	public boolean isSystemRole() {
		return systemRole;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ImpliedByPrincipal) {
			ImpliedByPrincipal that = (ImpliedByPrincipal) obj;
			// TODO check members too?
			return name.equals(that.name);
		}
		return false;
	}

	@Override
	public String toString() {
		return name.toString();
	}
}
