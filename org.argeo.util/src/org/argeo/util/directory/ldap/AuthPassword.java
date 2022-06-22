package org.argeo.util.directory.ldap;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.argeo.util.naming.LdapAttrs;

/** LDAP authPassword field according to RFC 3112 */
public class AuthPassword implements CallbackHandler {
	private final String authScheme;
	private final String authInfo;
	private final String authValue;

	public AuthPassword(String value) {
		StringTokenizer st = new StringTokenizer(value, "$");
		// TODO make it more robust, deal with bad formatting
		this.authScheme = st.nextToken().trim();
		this.authInfo = st.nextToken().trim();
		this.authValue = st.nextToken().trim();

		String expectedAuthScheme = getExpectedAuthScheme();
		if (expectedAuthScheme != null && !authScheme.equals(expectedAuthScheme))
			throw new IllegalArgumentException(
					"Auth scheme " + authScheme + " is not compatible with " + expectedAuthScheme);
	}

	protected AuthPassword(String authInfo, String authValue) {
		this.authScheme = getExpectedAuthScheme();
		if (authScheme == null)
			throw new IllegalArgumentException("Expected auth scheme cannot be null");
		this.authInfo = authInfo;
		this.authValue = authValue;
	}

	protected AuthPassword(AuthPassword authPassword) {
		this.authScheme = authPassword.getAuthScheme();
		this.authInfo = authPassword.getAuthInfo();
		this.authValue = authPassword.getAuthValue();
	}

	protected String getExpectedAuthScheme() {
		return null;
	}

	protected boolean matchAuthValue(Object object) {
		return authValue.equals(object.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AuthPassword))
			return false;
		AuthPassword authPassword = (AuthPassword) obj;
		return authScheme.equals(authPassword.authScheme) && authInfo.equals(authPassword.authInfo)
				&& authValue.equals(authValue);
	}

	public boolean keyEquals(AuthPassword authPassword) {
		return authScheme.equals(authPassword.authScheme) && authInfo.equals(authPassword.authInfo);
	}

	@Override
	public int hashCode() {
		return authValue.hashCode();
	}

	@Override
	public String toString() {
		return toAuthPassword();
	}

	public final String toAuthPassword() {
		return getAuthScheme() + '$' + authInfo + '$' + authValue;
	}

	public String getAuthScheme() {
		return authScheme;
	}

	public String getAuthInfo() {
		return authInfo;
	}

	public String getAuthValue() {
		return authValue;
	}

	public static AuthPassword matchAuthValue(Attributes attributes, char[] value) {
		try {
			Attribute authPassword = attributes.get(LdapAttrs.authPassword.name());
			if (authPassword != null) {
				NamingEnumeration<?> values = authPassword.getAll();
				while (values.hasMore()) {
					Object val = values.next();
					AuthPassword token = new AuthPassword(val.toString());
					String auth;
					if (Arrays.binarySearch(value, '$') >= 0) {
						auth = token.authInfo + '$' + token.authValue;
					} else {
						auth = token.authValue;
					}
					if (Arrays.equals(auth.toCharArray(), value))
						return token;
					// if (token.matchAuthValue(value))
					// return token;
				}
			}
			return null;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot check attribute", e);
		}
	}

	public static boolean remove(Attributes attributes, AuthPassword value) {
		Attribute authPassword = attributes.get(LdapAttrs.authPassword.name());
		return authPassword.remove(value.toAuthPassword());
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback)
				((NameCallback) callback).setName(toAuthPassword());
			else if (callback instanceof PasswordCallback)
				((PasswordCallback) callback).setPassword(getAuthValue().toCharArray());
		}
	}

}
