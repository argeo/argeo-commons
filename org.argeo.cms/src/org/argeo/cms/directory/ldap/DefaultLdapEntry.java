package org.argeo.cms.directory.ldap;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.ldap.LdapName;

import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.api.acr.ldap.LdapObj;
import org.argeo.api.cms.directory.DirectoryDigestUtils;

/** An entry in an LDAP (or LDIF) directory. */
public class DefaultLdapEntry implements LdapEntry {
	private final AbstractLdapDirectory directory;

	private final LdapName dn;

	private AttributeDictionary properties;
	private AttributeDictionary credentials;

//	private String primaryObjectClass;
//	private List<String> objectClasses = new ArrayList<>();

	protected DefaultLdapEntry(AbstractLdapDirectory directory, LdapName dn) {
		Objects.requireNonNull(directory);
		Objects.requireNonNull(dn);
		this.directory = directory;
		this.dn = dn;

		// Object classes
//		Objects.requireNonNull(initialAttributes);
//		try {
//			NamingEnumeration<?> en = initialAttributes.get(LdapAttrs.objectClass.name()).getAll();
//			String first = null;
//			attrs: while (en.hasMore()) {
//				String v = en.next().toString();
//				if (v.equalsIgnoreCase(LdapObjs.top.name()))
//					continue attrs;
//				if (first == null)
//					first = v;
//				if (v.equalsIgnoreCase(getDirectory().getUserObjectClass()))
//					primaryObjectClass = getDirectory().getUserObjectClass();
//				else if (v.equalsIgnoreCase(getDirectory().getGroupObjectClass()))
//					primaryObjectClass = getDirectory().getGroupObjectClass();
//				objectClasses.add(v);
//			}
//			if (primaryObjectClass == null) {
//				if (first == null)
//					throw new IllegalStateException("Could not find primary object class");
//				primaryObjectClass = first;
//			}
//		} catch (NamingException e) {
//			throw new IllegalStateException("Cannot find object classes", e);
//		}

	}

	@Override
	public LdapName getDn() {
		return dn;
	}

	public synchronized Attributes getAttributes() {
		return isEditing() ? getModifiedAttributes() : getDirectory().getDirectoryDao().doGetAttributes(dn);
	}

	@Override
	public List<LdapName> getReferences(String attributeId) {
		Attribute memberAttribute = getAttributes().get(attributeId);
		if (memberAttribute == null)
			return new ArrayList<LdapName>();
		try {
			List<LdapName> roles = new ArrayList<LdapName>();
			NamingEnumeration<?> values = memberAttribute.getAll();
			while (values.hasMore()) {
				LdapName dn = new LdapName(values.next().toString());
				roles.add(dn);
			}
			return roles;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get members", e);
		}

	}

	/** Should only be called from working copy thread. */
	protected synchronized Attributes getModifiedAttributes() {
		assert getWc() != null;
		return getWc().getModifiedData().get(getDn());
	}

	protected synchronized boolean isEditing() {
		return getWc() != null && getModifiedAttributes() != null;
	}

	private synchronized LdapEntryWorkingCopy getWc() {
		return directory.getWorkingCopy();
	}

	protected synchronized void startEditing() {
//		if (frozen)
//			throw new IllegalStateException("Cannot edit frozen view");
		if (directory.isReadOnly())
			throw new IllegalStateException("User directory is read-only");
		assert getModifiedAttributes() == null;
		getWc().startEditing(this);
		// modifiedAttributes = (Attributes) publishedAttributes.clone();
	}

	public synchronized void publishAttributes(Attributes modifiedAttributes) {
//		publishedAttributes = modifiedAttributes;
	}

	/*
	 * PROPERTIES
	 */
	@Override
	public Dictionary<String, Object> getProperties() {
		if (properties == null)
			properties = new AttributeDictionary(false);
		return properties;
	}

	public Dictionary<String, Object> getCredentials() {
		if (credentials == null)
			credentials = new AttributeDictionary(true);
		return credentials;
	}

	/*
	 * CREDENTIALS
	 */
	@Override
	public boolean hasCredential(String key, Object value) {
		if (key == null) {
			// TODO check other sources (like PKCS12)
			// String pwd = new String((char[]) value);
			// authPassword (RFC 312 https://tools.ietf.org/html/rfc3112)
			char[] password = DirectoryDigestUtils.bytesToChars(value);

			if (getDirectory().getForcedPassword() != null
					&& getDirectory().getForcedPassword().equals(new String(password)))
				return true;

			AuthPassword authPassword = AuthPassword.matchAuthValue(getAttributes(), password);
			if (authPassword != null) {
				if (authPassword.getAuthScheme().equals(SharedSecret.X_SHARED_SECRET)) {
					SharedSecret onceToken = new SharedSecret(authPassword);
					if (onceToken.isExpired()) {
						// AuthPassword.remove(getAttributes(), onceToken);
						return false;
					} else {
						// boolean wasRemoved = AuthPassword.remove(getAttributes(), onceToken);
						return true;
					}
					// TODO delete expired tokens?
				} else {
					// TODO implement SHA
					throw new UnsupportedOperationException(
							"Unsupported authPassword scheme " + authPassword.getAuthScheme());
				}
			}

			// Regular password
//			byte[] hashedPassword = hash(password, DigestUtils.PASSWORD_SCHEME_PBKDF2_SHA256);
			if (hasCredential(LdapAttr.userPassword.name(), DirectoryDigestUtils.charsToBytes(password)))
				return true;
			return false;
		}

		Object storedValue = getCredentials().get(key);
		if (storedValue == null || value == null)
			return false;
		if (!(value instanceof String || value instanceof byte[]))
			return false;
		if (storedValue instanceof String && value instanceof String)
			return storedValue.equals(value);
		if (storedValue instanceof byte[] && value instanceof byte[]) {
			String storedBase64 = new String((byte[]) storedValue, US_ASCII);
			String passwordScheme = null;
			if (storedBase64.charAt(0) == '{') {
				int index = storedBase64.indexOf('}');
				if (index > 0) {
					passwordScheme = storedBase64.substring(1, index);
					String storedValueBase64 = storedBase64.substring(index + 1);
					byte[] storedValueBytes = Base64.getDecoder().decode(storedValueBase64);
					char[] passwordValue = DirectoryDigestUtils.bytesToChars((byte[]) value);
					byte[] valueBytes;
					if (DirectoryDigestUtils.PASSWORD_SCHEME_SHA.equals(passwordScheme)) {
						valueBytes = DirectoryDigestUtils.toPasswordScheme(passwordScheme, passwordValue, null, null,
								null);
					} else if (DirectoryDigestUtils.PASSWORD_SCHEME_PBKDF2_SHA256.equals(passwordScheme)) {
						// see https://www.thesubtlety.com/post/a-389-ds-pbkdf2-password-checker/
						byte[] iterationsArr = Arrays.copyOfRange(storedValueBytes, 0, 4);
						BigInteger iterations = new BigInteger(iterationsArr);
						byte[] salt = Arrays.copyOfRange(storedValueBytes, iterationsArr.length,
								iterationsArr.length + 64);
						byte[] keyArr = Arrays.copyOfRange(storedValueBytes, iterationsArr.length + salt.length,
								storedValueBytes.length);
						int keyLengthBits = keyArr.length * 8;
						valueBytes = DirectoryDigestUtils.toPasswordScheme(passwordScheme, passwordValue, salt,
								iterations.intValue(), keyLengthBits);
					} else {
						throw new UnsupportedOperationException("Unknown password scheme " + passwordScheme);
					}
					return Arrays.equals(storedValueBytes, valueBytes);
				}
			}
		}
//		if (storedValue instanceof byte[] && value instanceof byte[]) {
//			return Arrays.equals((byte[]) storedValue, (byte[]) value);
//		}
		return false;
	}

	/** Hash the password */
	private static byte[] sha1hash(char[] password) {
		byte[] hashedPassword = ("{SHA}" + Base64.getEncoder()
				.encodeToString(DirectoryDigestUtils.sha1(DirectoryDigestUtils.charsToBytes(password))))
				.getBytes(StandardCharsets.UTF_8);
		return hashedPassword;
	}

	public AbstractLdapDirectory getDirectory() {
		return directory;
	}

	public LdapDirectoryDao getDirectoryDao() {
		return directory.getDirectoryDao();
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof LdapEntry) {
			LdapEntry that = (LdapEntry) obj;
			return this.dn.equals(that.getDn());
		}
		return false;
	}

	@Override
	public String toString() {
		return dn.toString();
	}

	private static boolean isAsciiPrintable(String str) {
		if (str == null) {
			return false;
		}
		int sz = str.length();
		for (int i = 0; i < sz; i++) {
			if (isAsciiPrintable(str.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	private static boolean isAsciiPrintable(char ch) {
		return ch >= 32 && ch < 127;
	}

	protected class AttributeDictionary extends Dictionary<String, Object> {
		private final List<String> effectiveKeys = new ArrayList<String>();
		private final List<String> attrFilter;
		private final Boolean includeFilter;

		public AttributeDictionary(Boolean credentials) {
			this.attrFilter = getDirectory().getCredentialAttributeIds();
			this.includeFilter = credentials;
			try {
				NamingEnumeration<String> ids = getAttributes().getIDs();
				while (ids.hasMore()) {
					String id = ids.next();
					if (credentials && attrFilter.contains(id))
						effectiveKeys.add(id);
					else if (!credentials && !attrFilter.contains(id))
						effectiveKeys.add(id);
				}
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot initialise attribute dictionary", e);
			}
			if (!credentials)
				effectiveKeys.add(LdapAttr.objectClasses.name());
		}

		@Override
		public int size() {
			return effectiveKeys.size();
		}

		@Override
		public boolean isEmpty() {
			return effectiveKeys.size() == 0;
		}

		@Override
		public Enumeration<String> keys() {
			return Collections.enumeration(effectiveKeys);
		}

		@Override
		public Enumeration<Object> elements() {
			final Iterator<String> it = effectiveKeys.iterator();
			return new Enumeration<Object>() {

				@Override
				public boolean hasMoreElements() {
					return it.hasNext();
				}

				@Override
				public Object nextElement() {
					String key = it.next();
					return get(key);
				}

			};
		}

		@Override
		public Object get(Object key) {
			try {
				Attribute attr = !key.equals(LdapAttr.objectClasses.name()) ? getAttributes().get(key.toString())
						: getAttributes().get(LdapAttr.objectClass.name());
				if (attr == null)
					return null;
				Object value = attr.get();
				if (value instanceof byte[]) {
					if (key.equals(LdapAttr.userPassword.name()))
						// TODO other cases (certificates, images)
						return value;
					value = new String((byte[]) value, StandardCharsets.UTF_8);
				}
				if (attr.size() == 1)
					return value;
				// special case for object class
				if (key.equals(LdapAttr.objectClass.name())) {
					// TODO support multiple object classes
					NamingEnumeration<?> en = attr.getAll();
					String first = null;
					attrs: while (en.hasMore()) {
						String v = en.next().toString();
						if (v.equalsIgnoreCase(LdapObj.top.name()))
							continue attrs;
						if (first == null)
							first = v;
						if (v.equalsIgnoreCase(getDirectory().getUserObjectClass()))
							return getDirectory().getUserObjectClass();
						else if (v.equalsIgnoreCase(getDirectory().getGroupObjectClass()))
							return getDirectory().getGroupObjectClass();
					}
					if (first != null)
						return first;
					throw new IllegalStateException("Cannot find objectClass in " + value);
				} else {
					NamingEnumeration<?> en = attr.getAll();
					StringJoiner values = new StringJoiner("\n");
					while (en.hasMore()) {
						String v = en.next().toString();
						values.add(v);
					}
					return values.toString();
				}
//				else
//					return value;
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot get value for attribute " + key, e);
			}
		}

		@Override
		public Object put(String key, Object value) {
			Objects.requireNonNull(value, "Value for key " + key + " is null");
			try {
				if (key == null) {
					// FIXME remove this "feature", a key should be specified
					// TODO persist to other sources (like PKCS12)
					char[] password = DirectoryDigestUtils.bytesToChars(value);
					byte[] hashedPassword = sha1hash(password);
					return put(LdapAttr.userPassword.name(), hashedPassword);
				}
				if (key.startsWith("X-")) {
					return put(LdapAttr.authPassword.name(), value);
				}

				// start editing
				getDirectory().checkEdit();
				if (!isEditing())
					startEditing();

				// object classes special case.
				if (key.equals(LdapAttr.objectClasses.name())) {
					Attribute attribute = new BasicAttribute(LdapAttr.objectClass.name());
					String[] objectClasses = value.toString().split("\n");
					for (String objectClass : objectClasses) {
						if (objectClass.trim().equals(""))
							continue;
						attribute.add(objectClass);
					}
					Attribute previousAttribute = getModifiedAttributes().put(attribute);
					if (previousAttribute != null)
						return previousAttribute.get();
					else
						return null;
				}

				if (!(value instanceof String || value instanceof byte[]))
					throw new IllegalArgumentException("Value must be String or byte[]");

				if (includeFilter && !attrFilter.contains(key))
					throw new IllegalArgumentException("Key " + key + " not included");
				else if (!includeFilter && attrFilter.contains(key))
					throw new IllegalArgumentException("Key " + key + " excluded");

				Attribute attribute = getModifiedAttributes().get(key.toString());
				// if (attribute == null) // block unit tests
				attribute = new BasicAttribute(key.toString());
				if (value instanceof String && !isAsciiPrintable(((String) value)))
					attribute.add(((String) value).getBytes(StandardCharsets.UTF_8));
				else
					attribute.add(value);
				Attribute previousAttribute = getModifiedAttributes().put(attribute);
				if (previousAttribute != null)
					return previousAttribute.get();
				else
					return null;
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot get value for attribute " + key, e);
			}
		}

		@Override
		public Object remove(Object key) {
			getDirectory().checkEdit();
			if (!isEditing())
				startEditing();

			if (includeFilter && !attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key + " not included");
			else if (!includeFilter && attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key + " excluded");

			try {
				Attribute attr = getModifiedAttributes().remove(key.toString());
				if (attr != null)
					return attr.get();
				else
					return null;
			} catch (NamingException e) {
				throw new IllegalStateException("Cannot remove attribute " + key, e);
			}
		}

	}

}
