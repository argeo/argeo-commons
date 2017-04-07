package org.argeo.cms.auth;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.User;

/**
 * A special user type used during authentication in order to provide the
 * credentials required for scoping the user admin.
 */
class AuthenticatingUser implements User {
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
		credentials.put(CmsAuthUtils.SHARED_STATE_NAME, name);
		byte[] pwd = charsToBytes(password);
		credentials.put(CmsAuthUtils.SHARED_STATE_PWD, pwd);
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


	static byte[] charsToBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	static char[] bytesToChars(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		CharBuffer charBuffer = Charset.forName("UTF-8").decode(byteBuffer);
		char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return chars;
	}


}
