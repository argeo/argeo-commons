package org.argeo.security.ldap;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.springframework.ldap.core.ContextSource;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.ldap.LdapUserDetailsManager;

/** Extends {@link LdapUserDetailsManager} by adding password encoding support. */
public class ArgeoLdapUserDetailsManager extends LdapUserDetailsManager {
	private PasswordEncoder passwordEncoder;
	private final Random random;

	public ArgeoLdapUserDetailsManager(ContextSource contextSource) {
		super(contextSource);
		this.random = createRandom();
	}

	private static Random createRandom() {
		try {
			return SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			return new Random(System.currentTimeMillis());
		}
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		super.changePassword(oldPassword, encodePassword(newPassword));
	}

	protected String encodePassword(String password) {
		if (!password.startsWith("{")) {
			byte[] salt = new byte[16];
			random.nextBytes(salt);
			return passwordEncoder.encodePassword(password, salt);
		} else {
			return password;
		}
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

}
