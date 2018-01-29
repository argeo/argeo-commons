package org.argeo.osgi.useradmin;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.User;

/**
 * A special user type used during authentication in order to provide the
 * credentials required for scoping the user admin.
 */
public class AuthenticatingUser implements User {
	/** From com.sun.security.auth.module.*LoginModule */
	public final static String SHARED_STATE_NAME = "javax.security.auth.login.name";
	/** From com.sun.security.auth.module.*LoginModule */
	public final static String SHARED_STATE_PWD = "javax.security.auth.login.password";

	private final String name;
	private final Dictionary<String, Object> credentials;

	public AuthenticatingUser(LdapName name) {
		this.name = name.toString();
		this.credentials = new Hashtable<>();
	}

	public AuthenticatingUser(String name, Dictionary<String, Object> credentials) {
		this.name = name;
		this.credentials = credentials;
	}

	public AuthenticatingUser(String name, char[] password) {
		this.name = name;
		credentials = new Hashtable<>();
		credentials.put(SHARED_STATE_NAME, name);
		byte[] pwd = DigestUtils.charsToBytes(password);
		credentials.put(SHARED_STATE_PWD, pwd);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getType() {
		return User.USER;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Dictionary getProperties() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Dictionary getCredentials() {
		return credentials;
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		throw new UnsupportedOperationException();
	}

}