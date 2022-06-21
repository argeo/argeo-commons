package org.argeo.cms.internal.auth;

import static org.argeo.api.acr.RuntimeNamespaceContext.getNamespaceContext;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.argeo.api.acr.CrName;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.cms.auth.RoleNameUtils;
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
	private Set<Principal> causes = new HashSet<Principal>();

	private QName roleName;
//	private int type = Role.ROLE;

	private boolean systemRole = false;
	private String context;

	public ImpliedByPrincipal(String name, Principal userPrincipal) {
		this.name = name;
		String cn = RoleNameUtils.getLastRdnValue(name);
		roleName = NamespaceUtils.parsePrefixedName(getNamespaceContext(), cn);
		if (roleName.getNamespaceURI().equals(CrName.ROLE_NAMESPACE_URI)) {
			systemRole = true;
		}
		context = RoleNameUtils.getContext(name);
//		try {
//			this.name = new LdapName(name);
//		} catch (InvalidNameException e) {
//			throw new IllegalArgumentException("Badly formatted role name", e);
//		}
		if (userPrincipal != null)
			causes.add(userPrincipal);
	}

//	public ImpliedByPrincipal(LdapName name, Principal userPrincipal) {
//		this.name = name;
//		if (userPrincipal != null)
//			causes.add(userPrincipal);
//	}

	public String getName() {
		return name;
	}

	/*
	 * USER ADMIN
	 */
//	public boolean addMember(Principal user) {
//		throw new UnsupportedOperationException();
//	}
//
//	public boolean removeMember(Principal user) {
//		throw new UnsupportedOperationException();
//	}
//
//	public boolean isMember(Principal member) {
//		return causes.contains(member);
//	}
//
//	public Enumeration<? extends Principal> members() {
//		return Collections.enumeration(causes);
//	}
//
//
//	/** Type of {@link Role}, if known. */
//	public int getType() {
//		return type;
//	}
//
//	/** Not supported for the time being. */
//	public Dictionary<String, Object> getProperties() {
//		throw new UnsupportedOperationException();
//	}

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
		// if (this == obj)
		// return true;
		if (obj instanceof ImpliedByPrincipal) {
			ImpliedByPrincipal that = (ImpliedByPrincipal) obj;
			// TODO check members too?
			return name.equals(that.name);
		}
		return false;
	}

	@Override
	public String toString() {
		// return name.toString() + " implied by " + causes;
		return name.toString();
	}
}
