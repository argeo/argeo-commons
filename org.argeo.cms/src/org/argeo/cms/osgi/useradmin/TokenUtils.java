package org.argeo.cms.osgi.useradmin;

import static org.argeo.api.acr.ldap.LdapAttr.description;
import static org.argeo.api.acr.ldap.LdapAttr.owner;

import java.security.Principal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import org.argeo.api.acr.ldap.NamingUtils;
import org.osgi.service.useradmin.Group;

/**
 * Canonically implements the Argeo token conventions.
 */
public class TokenUtils {
	public static Set<String> tokensUsed(Subject subject, String tokensBaseDn) {
		Set<String> res = new HashSet<>();
		for (Principal principal : subject.getPrincipals()) {
			String name = principal.getName();
			if (name.endsWith(tokensBaseDn)) {
				try {
					LdapName ldapName = new LdapName(name);
					String token = ldapName.getRdn(ldapName.size()).getValue().toString();
					res.add(token);
				} catch (InvalidNameException e) {
					throw new IllegalArgumentException("Invalid principal " + principal, e);
				}
			}
		}
		return res;
	}

	/** The user related to this token group */
	public static String userDn(Group tokenGroup) {
		return (String) tokenGroup.getProperties().get(owner.name());
	}

	public static boolean isExpired(Group tokenGroup) {
		return isExpired(tokenGroup, Instant.now());

	}

	public static boolean isExpired(Group tokenGroup, Instant instant) {
		String expiryDateStr = (String) tokenGroup.getProperties().get(description.name());
		if (expiryDateStr != null) {
			Instant expiryDate = NamingUtils.ldapDateToInstant(expiryDateStr);
			if (expiryDate.isBefore(instant)) {
				return true;
			}
		}
		return false;
	}

//	private final String token;
//
//	public TokenUtils(String token) {
//		this.token = token;
//	}
//
//	public String getToken() {
//		return token;
//	}
//
//	@Override
//	public int hashCode() {
//		return token.hashCode();
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if ((obj instanceof TokenUtils) && ((TokenUtils) obj).token.equals(token))
//			return true;
//		return false;
//	}
//
//	@Override
//	public String toString() {
//		return "Token #" + hashCode();
//	}

}
