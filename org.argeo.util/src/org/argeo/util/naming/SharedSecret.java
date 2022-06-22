package org.argeo.util.naming;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.argeo.util.naming.ldap.AuthPassword;

public class SharedSecret extends AuthPassword {
	public final static String X_SHARED_SECRET = "X-SharedSecret";
	private final Instant expiry;

	public SharedSecret(String authInfo, String authValue) {
		super(authInfo, authValue);
		expiry = null;
	}

	public SharedSecret(AuthPassword authPassword) {
		super(authPassword);
		String authInfo = getAuthInfo();
		if (authInfo.length() == 16) {
			expiry = NamingUtils.ldapDateToInstant(authInfo);
		} else {
			expiry = null;
		}
	}

	public SharedSecret(ZonedDateTime expiryTimestamp, String value) {
		super(NamingUtils.instantToLdapDate(expiryTimestamp), value);
		expiry = expiryTimestamp.withZoneSameInstant(ZoneOffset.UTC).toInstant();
	}

	public SharedSecret(int hours, String value) {
		this(ZonedDateTime.now().plusHours(hours), value);
	}

	@Override
	protected String getExpectedAuthScheme() {
		return X_SHARED_SECRET;
	}

	public boolean isExpired() {
		if (expiry == null)
			return false;
		return expiry.isBefore(Instant.now());
	}

}
