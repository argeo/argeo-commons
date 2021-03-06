package org.argeo.cms.internal.auth;

import java.security.Principal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;

/**
 * A {@link Principal} which has been implied by an {@link Authorization}. If it
 * is empty it means this is an additional identity, otherwise it lists the
 * users (typically the logged in user but possibly empty
 * {@link ImpliedByPrincipal}s) which have implied it. When an additional
 * identity is removed, the related {@link ImpliedByPrincipal}s can thus be
 * removed.
 */
public final class ImpliedByPrincipal implements Principal, Role {
	private final LdapName name;
	private Set<Principal> causes = new HashSet<Principal>();

	private int type = Role.ROLE;

	public ImpliedByPrincipal(String name, Principal userPrincipal) {
		try {
			this.name = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Badly formatted role name", e);
		}
		if (userPrincipal != null)
			causes.add(userPrincipal);
	}

	public ImpliedByPrincipal(LdapName name, Principal userPrincipal) {
		this.name = name;
		if (userPrincipal != null)
			causes.add(userPrincipal);
	}

	public String getName() {
		return name.toString();
	}

	public boolean addMember(Principal user) {
		throw new UnsupportedOperationException();
	}

	public boolean removeMember(Principal user) {
		throw new UnsupportedOperationException();
	}

	public boolean isMember(Principal member) {
		return causes.contains(member);
	}

	public Enumeration<? extends Principal> members() {
		return Collections.enumeration(causes);
	}

	/*
	 * USER ADMIN
	 */

	@Override
	/** Type of {@link Role}, if known. */
	public int getType() {
		return type;
	}

	@Override
	/** Not supported for the time being. */
	public Dictionary<String, Object> getProperties() {
		throw new UnsupportedOperationException();
	}

	/*
	 * OBJECT
	 */

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
